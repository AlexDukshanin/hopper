package com.alex.hopper.data

import android.graphics.Bitmap
import com.alex.hopper.ocr.OcrEngine
import com.alex.hopper.ocr.OcrResult
import com.alex.hopper.ocr.WagonNumberExtractor
import com.alex.hopper.storage.PhotoStorage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RailRepository(
    private val wagonEntryDao: WagonEntryDao,
    private val collectionDao: CollectionDao,
    private val ocrEngine: OcrEngine,
    private val extractor: WagonNumberExtractor,
    private val photoStorage: PhotoStorage,
) {
    fun observeCollections(): Flow<List<CollectionSummary>> = collectionDao.observeCollections()

    fun observeCollection(id: Long): Flow<WagonCollection?> = collectionDao.observeCollection(id)

    fun observeEntries(collectionId: Long): Flow<List<WagonEntry>> = wagonEntryDao.observeEntries(collectionId)

    fun observeEntry(id: Long): Flow<WagonEntry?> = wagonEntryDao.observeEntry(id)

    fun createCaptureFile(): File = photoStorage.createCaptureFile()

    suspend fun createCollection(name: String): Long = withContext(Dispatchers.IO) {
        val existingCount = collectionDao.getCollections().size
        val resolvedName = name.trim().ifBlank { "Подборка ${existingCount + 1}" }
        collectionDao.insert(
            WagonCollection(
                name = resolvedName,
                description = "",
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun renameCollection(
        id: Long,
        name: String,
    ) = withContext(Dispatchers.IO) {
        val current = collectionDao.getCollectionById(id) ?: return@withContext
        val resolvedName = name.trim().ifBlank { current.name }
        collectionDao.updateName(id, resolvedName)
    }

    suspend fun updateCollectionDescription(
        id: Long,
        description: String,
    ) = withContext(Dispatchers.IO) {
        collectionDao.updateDescription(id, description.trim())
    }

    suspend fun importLegacyJournalDescription(description: String) = withContext(Dispatchers.IO) {
        if (description.isBlank()) return@withContext

        val collections = collectionDao.getCollections()
        if (collections.size != 1) return@withContext

        val onlyCollection = collections.first()
        if (onlyCollection.description.isBlank()) {
            collectionDao.updateDescription(onlyCollection.id, description.trim())
        }
    }

    suspend fun deleteCollection(collectionId: Long) = withContext(Dispatchers.IO) {
        wagonEntryDao.getEntriesByCollectionId(collectionId).forEach { entry ->
            if (entry.imagePath.isNotBlank()) {
                File(entry.imagePath).takeIf(File::exists)?.delete()
            }
        }
        wagonEntryDao.deleteByCollectionId(collectionId)
        collectionDao.deleteById(collectionId)
    }

    suspend fun saveCapturedPhoto(
        photoFile: File,
        scanBitmap: Bitmap?,
        collectionId: Long,
    ): Long = withContext(Dispatchers.IO) {
        val scanRecognition = runCatching {
            if (scanBitmap != null) {
                ocrEngine.recognize(scanBitmap)
            } else {
                ocrEngine.recognize(photoFile)
            }
        }
            .getOrDefault(OcrResult.EMPTY)
        val scanExtracted = extractor.extract(scanRecognition)
        val recognition = if (scanBitmap != null && scanExtracted.primaryNumber == null) {
            runCatching { ocrEngine.recognize(photoFile) }.getOrDefault(scanRecognition)
        } else {
            scanRecognition
        }
        val extracted = extractor.extract(recognition)

        wagonEntryDao.insert(
            WagonEntry(
                collectionId = collectionId,
                positionIndex = (wagonEntryDao.getMaxPositionIndex(collectionId) ?: -1L) + 1L,
                createdAt = System.currentTimeMillis(),
                imagePath = photoFile.absolutePath,
                primaryNumber = extracted.primaryNumber,
                candidateNumbers = extracted.allNumbers,
                recognizedText = recognition.fullText,
                note = "",
            ),
        )
    }

    suspend fun updateNote(id: Long, note: String) = withContext(Dispatchers.IO) {
        wagonEntryDao.updateNote(id, note.trim())
    }

    suspend fun updatePrimaryNumber(id: Long, number: String) = withContext(Dispatchers.IO) {
        wagonEntryDao.updatePrimaryNumber(id, number.filter(Char::isDigit).take(12).ifBlank { null })
    }

    suspend fun moveEntry(
        entryId: Long,
        targetIndex: Int,
    ) = withContext(Dispatchers.IO) {
        val movingEntry = wagonEntryDao.getEntryById(entryId) ?: return@withContext
        val ordered = wagonEntryDao.getEntriesOrdered(movingEntry.collectionId).toMutableList()
        val currentIndex = ordered.indexOfFirst { it.id == entryId }
        if (currentIndex == -1) return@withContext

        ordered.removeAt(currentIndex)
        val boundedTargetIndex = targetIndex.coerceIn(0, ordered.size)
        ordered.add(boundedTargetIndex, movingEntry)
        wagonEntryDao.replaceOrder(ordered)
    }

    suspend fun replacePhoto(entryId: Long, newPhotoFile: File) = withContext(Dispatchers.IO) {
        val entry = wagonEntryDao.getEntryById(entryId)
            ?: error("Запись для замены фото не найдена")
        val previousPath = entry.imagePath

        wagonEntryDao.updateImagePath(entryId, newPhotoFile.absolutePath)

        if (previousPath.isNotBlank() && previousPath != newPhotoFile.absolutePath) {
            File(previousPath).takeIf(File::exists)?.delete()
        }
    }

    suspend fun deletePhoto(entryId: Long) = withContext(Dispatchers.IO) {
        val entry = wagonEntryDao.getEntryById(entryId)
            ?: error("Запись для удаления фото не найдена")

        if (entry.imagePath.isNotBlank()) {
            File(entry.imagePath).takeIf(File::exists)?.delete()
        }
        wagonEntryDao.updateImagePath(entryId, "")
    }

    suspend fun deleteEntry(entry: WagonEntry) = withContext(Dispatchers.IO) {
        if (entry.imagePath.isNotBlank()) {
            File(entry.imagePath).takeIf(File::exists)?.delete()
        }
        wagonEntryDao.deleteById(entry.id)
    }
}
