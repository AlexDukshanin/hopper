package com.alex.hopper.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.alex.hopper.ocr.OcrEngine
import com.alex.hopper.ocr.OcrResult
import com.alex.hopper.ocr.WagonNumberExtractor
import com.alex.hopper.settings.AppSettings
import com.alex.hopper.settings.NewEntryPosition
import com.alex.hopper.settings.ScanFrameSettings
import com.alex.hopper.storage.PhotoStorage
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class CollectionSnapshot(
    val collection: WagonCollection,
    val entries: List<WagonEntry>,
)

data class ImportedCollectionEntry(
    val positionIndex: Long,
    val createdAt: Long,
    val imagePath: String,
    val primaryNumber: String?,
    val candidateNumbers: List<String>,
    val recognizedText: String,
    val note: String,
    val isLoaded: Boolean,
)

class RailRepository(
    private val wagonEntryDao: WagonEntryDao,
    private val collectionDao: CollectionDao,
    private val ocrEngine: OcrEngine,
    private val extractor: WagonNumberExtractor,
    private val photoStorage: PhotoStorage,
) {
    fun observeCollections(): Flow<List<CollectionSummary>> = collectionDao.observeCollections()

    fun observeCollection(id: Long): Flow<WagonCollection?> = collectionDao.observeCollection(id)

    fun observeAllEntries(): Flow<List<WagonEntry>> = wagonEntryDao.observeAllEntries()

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

    suspend fun getCollectionSnapshot(collectionId: Long): CollectionSnapshot? = withContext(Dispatchers.IO) {
        val collection = collectionDao.getCollectionById(collectionId) ?: return@withContext null
        CollectionSnapshot(
            collection = collection,
            entries = wagonEntryDao.getEntriesOrdered(collectionId),
        )
    }

    suspend fun importTransferredCollection(
        name: String,
        description: String,
        primaryDirectionLabel: String,
        secondaryDirectionLabel: String,
        isPrimaryDirectionOnTop: Boolean,
        newEntryPosition: NewEntryPosition,
        includeDirectionInCopy: Boolean,
        entries: List<ImportedCollectionEntry>,
    ): Long = withContext(Dispatchers.IO) {
        val collectionId = collectionDao.insert(
            WagonCollection(
                name = name.trim().ifBlank { "Подборка" },
                description = description.trim(),
                createdAt = System.currentTimeMillis(),
                primaryDirectionLabel = primaryDirectionLabel.ifBlank {
                    WagonCollection.DEFAULT_PRIMARY_DIRECTION_LABEL
                },
                secondaryDirectionLabel = secondaryDirectionLabel.ifBlank {
                    WagonCollection.DEFAULT_SECONDARY_DIRECTION_LABEL
                },
                isPrimaryDirectionOnTop = isPrimaryDirectionOnTop,
                newEntryPosition = newEntryPosition,
                includeDirectionInCopy = includeDirectionInCopy,
            ),
        )

        try {
            entries.sortedBy(ImportedCollectionEntry::positionIndex).forEachIndexed { index, entry ->
                wagonEntryDao.insert(
                    WagonEntry(
                        collectionId = collectionId,
                        positionIndex = index.toLong(),
                        createdAt = entry.createdAt,
                        imagePath = entry.imagePath,
                        primaryNumber = entry.primaryNumber,
                        candidateNumbers = entry.candidateNumbers,
                        recognizedText = entry.recognizedText,
                        note = entry.note,
                        isLoaded = entry.isLoaded,
                    ),
                )
            }
            collectionId
        } catch (exception: Exception) {
            wagonEntryDao.deleteByCollectionId(collectionId)
            collectionDao.deleteById(collectionId)
            throw exception
        }
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

    suspend fun toggleCollectionDirection(id: Long) = withContext(Dispatchers.IO) {
        updateCollection(id) { collection ->
            collection.copy(isPrimaryDirectionOnTop = !collection.isPrimaryDirectionOnTop)
        }
    }

    suspend fun updateCollectionDirectionLabels(
        id: Long,
        primaryLabel: String,
        secondaryLabel: String,
    ) = withContext(Dispatchers.IO) {
        updateCollection(id) { collection ->
            collection.copy(
                primaryDirectionLabel = primaryLabel.ifBlank {
                    WagonCollection.DEFAULT_PRIMARY_DIRECTION_LABEL
                },
                secondaryDirectionLabel = secondaryLabel.ifBlank {
                    WagonCollection.DEFAULT_SECONDARY_DIRECTION_LABEL
                },
            )
        }
    }

    suspend fun updateCollectionNewEntryPosition(
        id: Long,
        position: NewEntryPosition,
    ) = withContext(Dispatchers.IO) {
        updateCollection(id) { collection ->
            collection.copy(newEntryPosition = position)
        }
    }

    suspend fun updateCollectionIncludeDirectionInCopy(
        id: Long,
        include: Boolean,
    ) = withContext(Dispatchers.IO) {
        updateCollection(id) { collection ->
            collection.copy(includeDirectionInCopy = include)
        }
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

    suspend fun importLegacyCollectionSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        val collections = collectionDao.getCollections()
        if (collections.isEmpty()) return@withContext

        collections.forEach { collection ->
            collectionDao.insert(
                collection.copy(
                    primaryDirectionLabel = settings.primaryDirectionLabel.ifBlank {
                        WagonCollection.DEFAULT_PRIMARY_DIRECTION_LABEL
                    },
                    secondaryDirectionLabel = settings.secondaryDirectionLabel.ifBlank {
                        WagonCollection.DEFAULT_SECONDARY_DIRECTION_LABEL
                    },
                    isPrimaryDirectionOnTop = settings.isPrimaryDirectionOnTop,
                    newEntryPosition = settings.newEntryPosition,
                    includeDirectionInCopy = settings.includeDirectionInCopy,
                ),
            )
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
        scanFrameSettings: ScanFrameSettings,
        collectionId: Long,
    ): Long = withContext(Dispatchers.IO) {
        val collection = collectionDao.getCollectionById(collectionId)
            ?: error("Подборка для сохранения снимка не найдена")
        val scanBitmap = runCatching {
            createScanBitmap(photoFile, scanFrameSettings)
        }.onFailure { exception ->
            Log.w(OcrLogTag, "Failed to prepare scan bitmap from ${photoFile.name}", exception)
        }.getOrNull()
        val scanRecognition = runCatching {
            if (scanBitmap != null) {
                ocrEngine.recognize(scanBitmap)
            } else {
                ocrEngine.recognize(photoFile)
            }
        }.onFailure { exception ->
            Log.e(OcrLogTag, "Scan-frame OCR failed for ${photoFile.name}", exception)
        }.getOrDefault(OcrResult.EMPTY)
        val scanExtracted = extractor.extract(scanRecognition)
        val recognition = if (scanBitmap != null && scanExtracted.primaryNumber == null) {
            runCatching { ocrEngine.recognize(photoFile) }
                .onFailure { exception ->
                    Log.e(OcrLogTag, "Full-photo OCR fallback failed for ${photoFile.name}", exception)
                }
                .getOrDefault(scanRecognition)
        } else {
            scanRecognition
        }
        val extracted = extractor.extract(recognition)
        if (recognition.fullText.isBlank()) {
            Log.w(OcrLogTag, "OCR returned empty text for ${photoFile.name}")
        }
        val positionIndex = resolveNextPositionIndex(collection)

        wagonEntryDao.insert(
            WagonEntry(
                collectionId = collectionId,
                positionIndex = positionIndex,
                createdAt = System.currentTimeMillis(),
                imagePath = photoFile.absolutePath,
                primaryNumber = extracted.primaryNumber,
                candidateNumbers = extracted.allNumbers,
                recognizedText = recognition.fullText,
                note = "",
                isLoaded = false,
            ),
        )
    }

    suspend fun createEmptyEntry(collectionId: Long): Long = withContext(Dispatchers.IO) {
        val collection = collectionDao.getCollectionById(collectionId)
            ?: error("Подборка для добавления пустой карточки не найдена")
        val positionIndex = resolveNextPositionIndex(collection)

        wagonEntryDao.insert(
            WagonEntry(
                collectionId = collectionId,
                positionIndex = positionIndex,
                createdAt = System.currentTimeMillis(),
                imagePath = "",
                primaryNumber = null,
                candidateNumbers = emptyList(),
                recognizedText = "",
                note = "",
                isLoaded = false,
            ),
        )
    }

    suspend fun updateNote(id: Long, note: String) = withContext(Dispatchers.IO) {
        wagonEntryDao.updateNote(id, note.trim())
    }

    suspend fun updatePrimaryNumber(id: Long, number: String) = withContext(Dispatchers.IO) {
        wagonEntryDao.updatePrimaryNumber(id, number.filter(Char::isDigit).take(12).ifBlank { null })
    }

    suspend fun updateLoadState(id: Long, isLoaded: Boolean) = withContext(Dispatchers.IO) {
        wagonEntryDao.updateLoadState(id, isLoaded)
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

    suspend fun deleteEntries(entries: List<WagonEntry>) = withContext(Dispatchers.IO) {
        entries.forEach { entry ->
            if (entry.imagePath.isNotBlank()) {
                File(entry.imagePath).takeIf(File::exists)?.delete()
            }
        }
        entries.forEach { entry ->
            wagonEntryDao.deleteById(entry.id)
        }
    }

    private suspend fun updateCollection(
        id: Long,
        transform: (WagonCollection) -> WagonCollection,
    ) {
        val current = collectionDao.getCollectionById(id) ?: return
        collectionDao.insert(transform(current))
    }

    private suspend fun resolveNextPositionIndex(collection: WagonCollection): Long {
        return if (collection.newEntryPosition == NewEntryPosition.First) {
            wagonEntryDao.shiftPositionIndexes(collection.id, 0)
            0L
        } else {
            (wagonEntryDao.getMaxPositionIndex(collection.id) ?: -1L) + 1L
        }
    }

    private fun createScanBitmap(
        photoFile: File,
        scanFrameSettings: ScanFrameSettings,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(photoFile.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        val bitmap = BitmapFactory.decodeFile(
            photoFile.absolutePath,
            BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, 1800)
            },
        ) ?: return null
        val orientedBitmap = applyExifOrientation(bitmap, photoFile)

        val cropWidth = (orientedBitmap.width * scanFrameSettings.widthFraction).roundToInt()
            .coerceIn(1, orientedBitmap.width)
        val cropHeight = (orientedBitmap.height * scanFrameSettings.heightFraction).roundToInt()
            .coerceIn(1, orientedBitmap.height)
        val left = (orientedBitmap.width * scanFrameSettings.leftFraction).roundToInt()
            .coerceIn(0, (orientedBitmap.width - cropWidth).coerceAtLeast(0))
        val top = (orientedBitmap.height * scanFrameSettings.topFraction).roundToInt()
            .coerceIn(0, (orientedBitmap.height - cropHeight).coerceAtLeast(0))

        return Bitmap.createBitmap(
            orientedBitmap,
            left,
            top,
            cropWidth,
            cropHeight,
        )
    }

    private fun applyExifOrientation(
        bitmap: Bitmap,
        photoFile: File,
    ): Bitmap {
        val orientation = runCatching {
            ExifInterface(photoFile).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

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
            else -> return bitmap
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true,
        )
    }

    private fun calculateSampleSize(
        width: Int,
        height: Int,
        maxDimension: Int,
    ): Int {
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height
        while (currentWidth > maxDimension || currentHeight > maxDimension) {
            sampleSize *= 2
            currentWidth /= 2
            currentHeight /= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    private companion object {
        const val OcrLogTag = "HopperOcr"
    }
}
