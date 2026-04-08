package com.alex.hopper.data

import android.graphics.Bitmap
import com.alex.hopper.ocr.OcrEngine
import com.alex.hopper.ocr.OcrResult
import com.alex.hopper.ocr.WagonNumberExtractor
import com.alex.hopper.settings.AppSettings
import com.alex.hopper.settings.NewEntryPosition
import com.alex.hopper.storage.PhotoStorage
import java.io.File
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
        scanBitmap: Bitmap?,
        collectionId: Long,
    ): Long = withContext(Dispatchers.IO) {
        val collection = collectionDao.getCollectionById(collectionId)
            ?: error("Подборка для сохранения снимка не найдена")
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
}
