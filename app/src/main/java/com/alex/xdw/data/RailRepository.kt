package com.alex.xdw.data

import android.graphics.Bitmap
import com.alex.xdw.ocr.OcrEngine
import com.alex.xdw.ocr.OcrResult
import com.alex.xdw.ocr.WagonNumberExtractor
import com.alex.xdw.storage.PhotoStorage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RailRepository(
    private val dao: WagonEntryDao,
    private val ocrEngine: OcrEngine,
    private val extractor: WagonNumberExtractor,
    private val photoStorage: PhotoStorage,
) {
    fun observeEntries(): Flow<List<WagonEntry>> = dao.observeEntries()

    fun observeEntry(id: Long): Flow<WagonEntry?> = dao.observeEntry(id)

    fun createCaptureFile(): File = photoStorage.createCaptureFile()

    suspend fun saveCapturedPhoto(
        photoFile: File,
        scanBitmap: Bitmap?,
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

        dao.insert(
            WagonEntry(
                positionIndex = (dao.getMaxPositionIndex() ?: -1L) + 1L,
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
        dao.updateNote(id, note.trim())
    }

    suspend fun updatePrimaryNumber(id: Long, number: String) = withContext(Dispatchers.IO) {
        dao.updatePrimaryNumber(id, number.filter(Char::isDigit).take(12).ifBlank { null })
    }

    suspend fun moveEntry(
        entryId: Long,
        targetIndex: Int,
    ) = withContext(Dispatchers.IO) {
        val ordered = dao.getEntriesOrdered().toMutableList()
        val currentIndex = ordered.indexOfFirst { it.id == entryId }
        if (currentIndex == -1) return@withContext

        val movingEntry = ordered.removeAt(currentIndex)
        val boundedTargetIndex = targetIndex.coerceIn(0, ordered.size)
        ordered.add(boundedTargetIndex, movingEntry)
        dao.replaceOrder(ordered)
    }

    suspend fun replacePhoto(entryId: Long, newPhotoFile: File) = withContext(Dispatchers.IO) {
        val entry = dao.getEntryById(entryId)
            ?: error("Запись для замены фото не найдена")
        val previousPath = entry.imagePath

        dao.updateImagePath(entryId, newPhotoFile.absolutePath)

        if (previousPath.isNotBlank() && previousPath != newPhotoFile.absolutePath) {
            File(previousPath).takeIf(File::exists)?.delete()
        }
    }

    suspend fun deletePhoto(entryId: Long) = withContext(Dispatchers.IO) {
        val entry = dao.getEntryById(entryId)
            ?: error("Запись для удаления фото не найдена")

        if (entry.imagePath.isNotBlank()) {
            File(entry.imagePath).takeIf(File::exists)?.delete()
        }
        dao.updateImagePath(entryId, "")
    }

    suspend fun deleteEntry(entry: WagonEntry) = withContext(Dispatchers.IO) {
        if (entry.imagePath.isNotBlank()) {
            File(entry.imagePath).takeIf(File::exists)?.delete()
        }
        dao.deleteById(entry.id)
    }
}
