package com.alex.hopper.exchange

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.alex.hopper.data.CollectionSnapshot
import com.alex.hopper.data.ImportedCollectionEntry
import com.alex.hopper.data.RailRepository
import com.alex.hopper.storage.PhotoStorage
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SharedCollectionFile(
    val uri: Uri,
    val mimeType: String,
    val fileName: String,
)

data class ImportedCollectionResult(
    val collectionId: Long,
    val collectionName: String,
    val entryCount: Int,
)

class CollectionExchangeManager(
    private val context: Context,
    private val repository: RailRepository,
    private val photoStorage: PhotoStorage,
) {
    suspend fun createShareFile(
        collectionId: Long,
        includePhotos: Boolean,
    ): SharedCollectionFile = withContext(Dispatchers.IO) {
        val snapshot = repository.getCollectionSnapshot(collectionId)
            ?: error("Подборка для отправки не найдена")

        val exportDirectory = File(context.cacheDir, EXPORT_DIRECTORY).apply { mkdirs() }
        val safeName = snapshot.collection.name.toArchiveName()
        val archiveFile = File(
            exportDirectory,
            "${safeName}_${System.currentTimeMillis()}$FILE_EXTENSION",
        )

        val photoNames = buildPhotoNames(snapshot, includePhotos)

        ZipOutputStream(BufferedOutputStream(FileOutputStream(archiveFile))).use { output ->
            output.putNextEntry(ZipEntry(MANIFEST_NAME))
            output.write(buildManifest(snapshot, photoNames).toByteArray(Charsets.UTF_8))
            output.closeEntry()

            if (includePhotos) {
                snapshot.entries.forEach { entry ->
                    val photoName = photoNames[entry.id] ?: return@forEach
                    val photoFile = File(entry.imagePath)
                    if (!photoFile.exists()) return@forEach

                    output.putNextEntry(ZipEntry("$PHOTOS_DIRECTORY/$photoName"))
                    photoFile.inputStream().use { input -> input.copyTo(output) }
                    output.closeEntry()
                }
            }
        }

        SharedCollectionFile(
            uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archiveFile,
            ),
            mimeType = MIME_TYPE,
            fileName = archiveFile.name,
        )
    }

    suspend fun importFromUri(uri: Uri): ImportedCollectionResult = withContext(Dispatchers.IO) {
        val importDirectory = File(context.cacheDir, IMPORT_DIRECTORY).apply { mkdirs() }
        val tempArchive = File(importDirectory, "import_${UUID.randomUUID()}$FILE_EXTENSION")
        val importedPhotoFiles = mutableListOf<File>()

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempArchive.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Не удалось открыть файл Hopper")

            ZipFile(tempArchive).use { zipFile ->
                val manifestEntry = zipFile.getEntry(MANIFEST_NAME)
                    ?: error("Файл Hopper поврежден: не найден manifest")
                val manifestText = zipFile.getInputStream(manifestEntry)
                    .bufferedReader()
                    .use { it.readText() }
                val payload = parseManifest(manifestText)

                val importedEntries = payload.entries.mapIndexed { index, entry ->
                    val importedPhotoPath = entry.photoFileName?.let { photoName ->
                        val photoEntry = zipFile.getEntry("$PHOTOS_DIRECTORY/$photoName")
                            ?: error("В архиве не найдено фото $photoName")
                        val importedPhoto = photoStorage.createCaptureFile()
                        zipFile.getInputStream(photoEntry).use { input ->
                            importedPhoto.outputStream().use { output -> input.copyTo(output) }
                        }
                        importedPhotoFiles += importedPhoto
                        importedPhoto.absolutePath
                    }.orEmpty()

                    ImportedCollectionEntry(
                        positionIndex = index.toLong(),
                        createdAt = entry.createdAt,
                        imagePath = importedPhotoPath,
                        primaryNumber = entry.primaryNumber,
                        candidateNumbers = entry.candidateNumbers,
                        recognizedText = entry.recognizedText,
                        note = entry.note,
                        isLoaded = entry.isLoaded,
                    )
                }

                val importedCollectionId = repository.importTransferredCollection(
                    name = payload.collectionName,
                    description = payload.collectionDescription,
                    entries = importedEntries,
                )

                ImportedCollectionResult(
                    collectionId = importedCollectionId,
                    collectionName = payload.collectionName,
                    entryCount = importedEntries.size,
                )
            }
        } catch (exception: Exception) {
            importedPhotoFiles.forEach(File::delete)
            throw exception
        } finally {
            tempArchive.delete()
        }
    }

    private fun buildPhotoNames(
        snapshot: CollectionSnapshot,
        includePhotos: Boolean,
    ): Map<Long, String> {
        if (!includePhotos) return emptyMap()

        return snapshot.entries.mapIndexedNotNull { index, entry ->
            val imagePath = entry.imagePath.takeIf(String::isNotBlank) ?: return@mapIndexedNotNull null
            val photoFile = File(imagePath)
            if (!photoFile.exists()) return@mapIndexedNotNull null

            val baseName = photoFile.name.substringBeforeLast('.').toArchiveName()
            entry.id to "${(index + 1).toString().padStart(3, '0')}_$baseName.jpg"
        }.toMap()
    }

    private fun buildManifest(
        snapshot: CollectionSnapshot,
        photoNames: Map<Long, String>,
    ): String {
        val entriesJson = JSONArray()
        snapshot.entries.forEach { entry ->
            entriesJson.put(
                JSONObject().apply {
                    put("createdAt", entry.createdAt)
                    put("primaryNumber", entry.primaryNumber ?: JSONObject.NULL)
                    put("candidateNumbers", JSONArray(entry.candidateNumbers))
                    put("recognizedText", entry.recognizedText)
                    put("note", entry.note)
                    put("isLoaded", entry.isLoaded)
                    put("photoFileName", photoNames[entry.id] ?: JSONObject.NULL)
                },
            )
        }

        return JSONObject().apply {
            put("formatVersion", FORMAT_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put(
                "collection",
                JSONObject().apply {
                    put("name", snapshot.collection.name)
                    put("description", snapshot.collection.description)
                },
            )
            put("entries", entriesJson)
        }.toString(2)
    }

    private fun parseManifest(json: String): TransferPayload {
        val root = JSONObject(json)
        val formatVersion = root.optInt("formatVersion", -1)
        require(formatVersion == FORMAT_VERSION) {
            "Эта версия Hopper не поддерживает такой файл"
        }

        val collection = root.optJSONObject("collection")
            ?: error("Файл Hopper поврежден: не найдены данные подборки")
        val entriesJson = root.optJSONArray("entries") ?: JSONArray()

        val entries = buildList(entriesJson.length()) {
            repeat(entriesJson.length()) { index ->
                val item = entriesJson.optJSONObject(index) ?: return@repeat
                add(
                    TransferEntryPayload(
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        primaryNumber = if (item.isNull("primaryNumber")) {
                            null
                        } else {
                            item.optString("primaryNumber").takeIf(String::isNotBlank)
                        },
                        candidateNumbers = item.optJSONArray("candidateNumbers").toStringList(),
                        recognizedText = item.optString("recognizedText"),
                        note = item.optString("note"),
                        isLoaded = item.optBoolean("isLoaded", false),
                        photoFileName = if (item.isNull("photoFileName")) {
                            null
                        } else {
                            item.optString("photoFileName").takeIf(String::isNotBlank)
                        },
                    ),
                )
            }
        }

        return TransferPayload(
            collectionName = collection.optString("name").ifBlank { "Подборка" },
            collectionDescription = collection.optString("description"),
            entries = entries,
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList(length()) {
            repeat(length()) { index ->
                val value = optString(index)
                if (value.isNotBlank()) {
                    add(value)
                }
            }
        }
    }

    private fun String.toArchiveName(): String {
        val cleaned = lowercase()
            .replace(Regex("[^a-z0-9а-яё]+"), "_")
            .trim('_')
            .ifBlank { "hopper_collection" }
        return cleaned.take(40)
    }

    private data class TransferPayload(
        val collectionName: String,
        val collectionDescription: String,
        val entries: List<TransferEntryPayload>,
    )

    private data class TransferEntryPayload(
        val createdAt: Long,
        val primaryNumber: String?,
        val candidateNumbers: List<String>,
        val recognizedText: String,
        val note: String,
        val isLoaded: Boolean,
        val photoFileName: String?,
    )

    companion object {
        const val MIME_TYPE = "application/vnd.com.alex.hopper.collection"

        private const val FORMAT_VERSION = 1
        private const val MANIFEST_NAME = "manifest.json"
        private const val PHOTOS_DIRECTORY = "photos"
        private const val EXPORT_DIRECTORY = "shared_collections"
        private const val IMPORT_DIRECTORY = "imported_collections"
        private const val FILE_EXTENSION = ".hopper"
    }
}
