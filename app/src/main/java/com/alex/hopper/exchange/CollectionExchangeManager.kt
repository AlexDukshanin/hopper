package com.alex.hopper.exchange

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.alex.hopper.data.CollectionSnapshot
import com.alex.hopper.data.ImportedCollectionEntry
import com.alex.hopper.data.RailRepository
import com.alex.hopper.data.WagonCollection
import com.alex.hopper.data.WagonCondition
import com.alex.hopper.settings.NewEntryPosition
import com.alex.hopper.storage.PhotoStorage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
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

data class ImportPreview(
    val collectionName: String,
    val entryCount: Int,
)

data class QrShareSession(
    val collectionName: String,
    val chunks: List<String>,
)

data class QrTransferChunk(
    val transferId: String,
    val index: Int,
    val total: Int,
    val data: String,
)

sealed interface CollectionImportMode {
    data object KeepOriginal : CollectionImportMode
    data object CreateCopy : CollectionImportMode
    data class ReplaceExisting(val collectionId: Long) : CollectionImportMode
}

class CollectionExchangeManager(
    private val context: Context,
    private val repository: RailRepository,
    private val photoStorage: PhotoStorage,
) {
    suspend fun createShareFile(
        collectionId: Long,
        includePhotos: Boolean,
        jpegQuality: Int,
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
                    writeCompressedPhoto(
                        photoFile = photoFile,
                        output = output,
                        jpegQuality = jpegQuality,
                    )
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

    suspend fun createQrShareSession(collectionId: Long): QrShareSession = withContext(Dispatchers.IO) {
        val snapshot = repository.getCollectionSnapshot(collectionId)
            ?: error("Подборка для отправки не найдена")
        val manifest = buildManifest(snapshot, emptyMap())
        QrShareSession(
            collectionName = snapshot.collection.name,
            chunks = buildQrChunks(manifest),
        )
    }

    suspend fun inspectImportUri(uri: Uri): ImportPreview = withImportedArchive(uri) { tempArchive ->
        ZipFile(tempArchive).use { zipFile ->
            val payload = zipFile.readTransferPayload()
            ImportPreview(
                collectionName = payload.collectionName,
                entryCount = payload.entries.size,
            )
        }
    }

    suspend fun importFromUri(
        uri: Uri,
        mode: CollectionImportMode = CollectionImportMode.KeepOriginal,
    ): ImportedCollectionResult = withImportedArchive(uri) { tempArchive ->
        val importedPhotoFiles = mutableListOf<File>()
        try {
            ZipFile(tempArchive).use { zipFile ->
                val payload = zipFile.readTransferPayload()
                importPayload(payload, mode) { entry ->
                    entry.photoFileName?.let { photoName ->
                        val photoEntry = zipFile.getEntry("$PHOTOS_DIRECTORY/$photoName")
                            ?: error("В архиве не найдено фото $photoName")
                        val importedPhoto = photoStorage.createCaptureFile()
                        zipFile.getInputStream(photoEntry).use { input ->
                            importedPhoto.outputStream().use { output -> input.copyTo(output) }
                        }
                        importedPhotoFiles += importedPhoto
                        importedPhoto.absolutePath
                    }.orEmpty()
                }
            }
        } catch (exception: Exception) {
            importedPhotoFiles.forEach(File::delete)
            throw exception
        }
    }

    suspend fun importFromQrChunks(chunks: Collection<String>): ImportedCollectionResult = withContext(Dispatchers.IO) {
        val parsedChunks = chunks.map { rawChunk ->
            parseQrChunk(rawChunk) ?: error("Некорректный QR-код Hopper")
        }
        require(parsedChunks.isNotEmpty()) {
            "Не удалось прочитать QR-коды Hopper"
        }

        val transferId = parsedChunks.first().transferId
        val totalChunks = parsedChunks.first().total
        require(parsedChunks.all { it.transferId == transferId && it.total == totalChunks }) {
            "Сканированы QR-коды из разных подборок"
        }

        val indexedChunks = parsedChunks.associateBy(QrTransferChunk::index)
        require(indexedChunks.size == totalChunks) {
            "Не хватает частей QR-кода: ${indexedChunks.size} из $totalChunks"
        }

        val encodedPayload = buildString {
            for (index in 1..totalChunks) {
                append(indexedChunks[index]?.data ?: error("Не найдена часть $index"))
            }
        }
        val manifestText = decodeQrPayload(encodedPayload)
        importPayload(parseManifest(manifestText))
    }

    fun parseQrChunk(rawValue: String): QrTransferChunk? {
        if (!rawValue.startsWith(QR_PREFIX)) return null
        val parts = rawValue.split(QR_DELIMITER, limit = 5)
        if (parts.size != 5) return null

        val transferId = parts[1].takeIf(String::isNotBlank) ?: return null
        val index = parts[2].toIntOrNull() ?: return null
        val total = parts[3].toIntOrNull() ?: return null
        val data = parts[4].takeIf(String::isNotBlank) ?: return null
        if (index !in 1..total || total <= 0) return null

        return QrTransferChunk(
            transferId = transferId,
            index = index,
            total = total,
            data = data,
        )
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
                    put("wagonCondition", entry.condition.name)
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
                    put("primaryDirectionLabel", snapshot.collection.primaryDirectionLabel)
                    put("secondaryDirectionLabel", snapshot.collection.secondaryDirectionLabel)
                    put("isPrimaryDirectionOnTop", snapshot.collection.isPrimaryDirectionOnTop)
                    put("newEntryPosition", snapshot.collection.newEntryPosition.name)
                    put("includeDirectionInCopy", snapshot.collection.includeDirectionInCopy)
                },
            )
            put("entries", entriesJson)
        }.toString(2)
    }

    private suspend fun importPayload(
        payload: TransferPayload,
        mode: CollectionImportMode = CollectionImportMode.KeepOriginal,
        resolvePhotoPath: suspend (TransferEntryPayload) -> String = { "" },
    ): ImportedCollectionResult {
        val importedEntries = payload.entries.mapIndexed { index, entry ->
            ImportedCollectionEntry(
                positionIndex = index.toLong(),
                createdAt = entry.createdAt,
                imagePath = resolvePhotoPath(entry),
                primaryNumber = entry.primaryNumber,
                candidateNumbers = entry.candidateNumbers,
                recognizedText = entry.recognizedText,
                note = entry.note,
                condition = entry.condition,
            )
        }

        val resolvedCollectionName = when (mode) {
            CollectionImportMode.KeepOriginal -> payload.collectionName
            CollectionImportMode.CreateCopy -> repository.createUniqueCopyCollectionName(payload.collectionName)
            is CollectionImportMode.ReplaceExisting -> payload.collectionName
        }

        val importedCollectionId = repository.importTransferredCollection(
            name = resolvedCollectionName,
            description = payload.collectionDescription,
            primaryDirectionLabel = payload.primaryDirectionLabel,
            secondaryDirectionLabel = payload.secondaryDirectionLabel,
            isPrimaryDirectionOnTop = payload.isPrimaryDirectionOnTop,
            newEntryPosition = payload.newEntryPosition,
            includeDirectionInCopy = payload.includeDirectionInCopy,
            entries = importedEntries,
            replaceCollectionId = (mode as? CollectionImportMode.ReplaceExisting)?.collectionId,
        )

        return ImportedCollectionResult(
            collectionId = importedCollectionId,
            collectionName = resolvedCollectionName,
            entryCount = importedEntries.size,
        )
    }

    private suspend fun <T> withImportedArchive(
        uri: Uri,
        block: suspend (File) -> T,
    ): T = withContext(Dispatchers.IO) {
        val importDirectory = File(context.cacheDir, IMPORT_DIRECTORY).apply { mkdirs() }
        val tempArchive = File(importDirectory, "import_${UUID.randomUUID()}$FILE_EXTENSION")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempArchive.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Не удалось открыть файл Hopper")
            block(tempArchive)
        } finally {
            tempArchive.delete()
        }
    }

    private fun ZipFile.readTransferPayload(): TransferPayload {
        val manifestEntry = getEntry(MANIFEST_NAME)
            ?: error("Файл Hopper поврежден: не найден manifest")
        val manifestText = getInputStream(manifestEntry)
            .bufferedReader()
            .use { it.readText() }
        return parseManifest(manifestText)
    }

    private fun buildQrChunks(manifest: String): List<String> {
        val compressed = gzip(manifest.toByteArray(Charsets.UTF_8))
        val encodedPayload = Base64.encodeToString(
            compressed,
            Base64.NO_WRAP or Base64.URL_SAFE,
        )
        val transferId = UUID.randomUUID().toString().substringBefore('-')
        val totalChunks = encodedPayload.length
            .let { length ->
                if (length == 0) 1 else ((length - 1) / QR_CHUNK_DATA_SIZE) + 1
            }

        return buildList(totalChunks) {
            repeat(totalChunks) { index ->
                val start = index * QR_CHUNK_DATA_SIZE
                val end = minOf(encodedPayload.length, start + QR_CHUNK_DATA_SIZE)
                val chunk = encodedPayload.substring(start, end)
                add(
                    listOf(
                        QR_PREFIX,
                        transferId,
                        (index + 1).toString(),
                        totalChunks.toString(),
                        chunk,
                    ).joinToString(QR_DELIMITER),
                )
            }
        }
    }

    private fun decodeQrPayload(encodedPayload: String): String {
        val compressed = Base64.decode(encodedPayload, Base64.NO_WRAP or Base64.URL_SAFE)
        val uncompressed = GZIPInputStream(ByteArrayInputStream(compressed)).use { input ->
            input.readBytes()
        }
        return uncompressed.toString(Charsets.UTF_8)
    }

    private fun gzip(input: ByteArray): ByteArray {
        val byteStream = ByteArrayOutputStream()
        GZIPOutputStream(byteStream).use { output ->
            output.write(input)
        }
        return byteStream.toByteArray()
    }

    private fun writeCompressedPhoto(
        photoFile: File,
        output: ZipOutputStream,
        jpegQuality: Int,
    ) {
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        if (bitmap == null) {
            photoFile.inputStream().use { input -> input.copyTo(output) }
            return
        }

        val normalizedBitmap = bitmap.applyExifOrientation(photoFile)

        try {
            normalizedBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                jpegQuality.coerceIn(60, 92),
                output,
            )
        } finally {
            if (normalizedBitmap !== bitmap) {
                normalizedBitmap.recycle()
            }
            bitmap.recycle()
        }
    }

    private fun Bitmap.applyExifOrientation(photoFile: File): Bitmap {
        val exifInterface = runCatching { ExifInterface(photoFile.absolutePath) }.getOrNull() ?: return this
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(270f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(90f)
            }
            else -> return this
        }

        return Bitmap.createBitmap(
            this,
            0,
            0,
            width,
            height,
            matrix,
            true,
        )
    }

    private fun parseManifest(json: String): TransferPayload {
        val root = JSONObject(json)
        val formatVersion = root.optInt("formatVersion", -1)
        require(formatVersion in SUPPORTED_FORMAT_VERSIONS) {
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
                        condition = item.optString("wagonCondition")
                            .takeIf(String::isNotBlank)
                            ?.let(WagonCondition::fromStoredValue)
                            ?: if (item.optBoolean("isLoaded", false)) {
                                WagonCondition.Loaded
                            } else {
                                WagonCondition.Empty
                            },
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
            primaryDirectionLabel = collection.optString("primaryDirectionLabel").ifBlank {
                WagonCollection.DEFAULT_PRIMARY_DIRECTION_LABEL
            },
            secondaryDirectionLabel = collection.optString("secondaryDirectionLabel").ifBlank {
                WagonCollection.DEFAULT_SECONDARY_DIRECTION_LABEL
            },
            isPrimaryDirectionOnTop = collection.optBoolean("isPrimaryDirectionOnTop", true),
            newEntryPosition = collection.optString("newEntryPosition")
                .takeIf(String::isNotBlank)
                ?.let { stored ->
                    NewEntryPosition.entries.firstOrNull { it.name == stored }
                }
                ?: NewEntryPosition.Last,
            includeDirectionInCopy = collection.optBoolean("includeDirectionInCopy", true),
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
        val primaryDirectionLabel: String,
        val secondaryDirectionLabel: String,
        val isPrimaryDirectionOnTop: Boolean,
        val newEntryPosition: NewEntryPosition,
        val includeDirectionInCopy: Boolean,
        val entries: List<TransferEntryPayload>,
    )

    private data class TransferEntryPayload(
        val createdAt: Long,
        val primaryNumber: String?,
        val candidateNumbers: List<String>,
        val recognizedText: String,
        val note: String,
        val condition: WagonCondition,
        val photoFileName: String?,
    )

    companion object {
        const val MIME_TYPE = "application/vnd.com.alex.hopper.collection"
        const val OCTET_STREAM_MIME_TYPE = "application/octet-stream"
        const val ZIP_MIME_TYPE = "application/zip"
        const val ZIP_LEGACY_MIME_TYPE = "application/x-zip-compressed"
        const val FILE_EXTENSION = ".hopper"
        val SUPPORTED_IMPORT_MIME_TYPES = arrayOf(
            MIME_TYPE,
            ZIP_MIME_TYPE,
            ZIP_LEGACY_MIME_TYPE,
            OCTET_STREAM_MIME_TYPE,
            "*/*",
        )

        private const val FORMAT_VERSION = 3
        private val SUPPORTED_FORMAT_VERSIONS = setOf(1, 2, 3)
        private const val MANIFEST_NAME = "manifest.json"
        private const val PHOTOS_DIRECTORY = "photos"
        private const val EXPORT_DIRECTORY = "shared_collections"
        private const val IMPORT_DIRECTORY = "imported_collections"
        private const val QR_PREFIX = "HQR1"
        private const val QR_DELIMITER = "|"
        private const val QR_CHUNK_DATA_SIZE = 700
    }
}
