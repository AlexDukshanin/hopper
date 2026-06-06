package com.alex.hopper.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alex.hopper.data.CollectionSummary
import com.alex.hopper.data.RailRepository
import com.alex.hopper.data.WagonCollection
import com.alex.hopper.data.WagonEntry
import com.alex.hopper.exchange.CollectionImportMode
import com.alex.hopper.exchange.CollectionExchangeManager
import com.alex.hopper.exchange.QrShareSession
import com.alex.hopper.data.WagonCondition
import com.alex.hopper.settings.AppIconManager
import com.alex.hopper.settings.AppIconMode
import com.alex.hopper.settings.AppSettings
import com.alex.hopper.settings.AppThemeMode
import com.alex.hopper.settings.CollectionLayoutMode
import com.alex.hopper.settings.NewEntryPosition
import com.alex.hopper.settings.ScanFrameSettings
import com.alex.hopper.settings.UserSettingsRepository
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CameraUiState(
    val isProcessing: Boolean = false,
    val statusMessage: String = "Наведите номер вагона в рамку и нажмите кнопку снизу.",
    val errorMessage: String? = null,
)

data class QrImportUiState(
    val scannedChunks: Int = 0,
    val totalChunks: Int? = null,
    val isImporting: Boolean = false,
    val statusMessage: String = "Наведите камеру на QR-код Hopper.",
    val errorMessage: String? = null,
)

data class PendingImportConflict(
    val uri: Uri,
    val collectionName: String,
    val existingCollectionId: Long,
    val entryCount: Int,
)

sealed interface AppEvent {
    data class OpenEntry(val entryId: Long) : AppEvent
    data class OpenCollection(
        val collectionId: Long,
        val message: String,
    ) : AppEvent
    data object OpenQrShare : AppEvent
    data class ShareFile(
        val uri: Uri,
        val mimeType: String,
        val chooserTitle: String,
    ) : AppEvent
    data class Snackbar(
        val message: String,
        val durationMillis: Long = 2_000,
    ) : AppEvent
    data class PhotoUpdated(val message: String) : AppEvent
    data class PendingDelete(
        val entryIds: List<Long>,
        val message: String,
        val popBack: Boolean,
    ) : AppEvent
}

class MainViewModel(
    private val repository: RailRepository,
    private val settingsRepository: UserSettingsRepository,
    private val appIconManager: AppIconManager,
    private val collectionExchangeManager: CollectionExchangeManager,
) : ViewModel() {
    private val pendingDeleteEntries = MutableStateFlow<List<WagonEntry>>(emptyList())
    private val _selectedCollectionId = MutableStateFlow<Long?>(null)
    private val _cameraState = MutableStateFlow(CameraUiState())
    private val _qrShareSession = MutableStateFlow<QrShareSession?>(null)
    private val _qrImportState = MutableStateFlow(QrImportUiState())
    private val _pendingImportConflict = MutableStateFlow<PendingImportConflict?>(null)
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 1)
    private var activeQrTransferId: String? = null
    private var activeQrTransferTotalChunks: Int = 0
    private val activeQrChunks = linkedMapOf<Int, String>()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
    val selectedCollectionId: StateFlow<Long?> = _selectedCollectionId.asStateFlow()
    val cameraState: StateFlow<CameraUiState> = _cameraState.asStateFlow()
    val qrShareSession: StateFlow<QrShareSession?> = _qrShareSession.asStateFlow()
    val qrImportState: StateFlow<QrImportUiState> = _qrImportState.asStateFlow()
    val pendingImportConflict: StateFlow<PendingImportConflict?> = _pendingImportConflict.asStateFlow()
    val events = _events.asSharedFlow()

    val collections: StateFlow<List<CollectionSummary>> = repository.observeCollections()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val allEntries: StateFlow<List<WagonEntry>> = repository.observeAllEntries()
        .catch {
            _events.emit(
                AppEvent.Snackbar(
                    message = "Не удалось открыть глобальный поиск",
                    durationMillis = 1_400,
                ),
            )
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val currentCollection: StateFlow<WagonCollection?> = selectedCollectionId
        .flatMapLatest { collectionId ->
            if (collectionId == null) {
                flowOf(null)
            } else {
                repository.observeCollection(collectionId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val entries: StateFlow<List<WagonEntry>> = combine(
        selectedCollectionId.flatMapLatest { collectionId ->
            if (collectionId == null) {
                flowOf(emptyList())
            } else {
                repository.observeEntries(collectionId)
            }
        },
        pendingDeleteEntries,
    ) { items, pending ->
        val pendingIds = pending.map(WagonEntry::id).toSet()
        items.filterNot { it.id in pendingIds }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    init {
        appIconManager.apply(settings.value.appIconMode)

        viewModelScope.launch {
            repository.importLegacyJournalDescription(settings.value.journalDescription)
        }

        if (!settingsRepository.hasMigratedCollectionScopedJournalSettings()) {
            viewModelScope.launch {
                repository.importLegacyCollectionSettings(settings.value)
                settingsRepository.markCollectionScopedJournalSettingsMigrated()
            }
        }

        viewModelScope.launch {
            collections.collect { items ->
                val currentId = _selectedCollectionId.value
                when {
                    items.isEmpty() -> _selectedCollectionId.value = null
                    currentId == null || items.none { it.id == currentId } -> {
                        _selectedCollectionId.value = items.first().id
                    }
                }
            }
        }
    }

    fun observeEntry(id: Long): Flow<WagonEntry?> = repository.observeEntry(id)

    fun observeCollection(collectionId: Long): Flow<WagonCollection?> =
        repository.observeCollection(collectionId)

    fun observeEntries(collectionId: Long): Flow<List<WagonEntry>> = combine(
        repository.observeEntries(collectionId),
        pendingDeleteEntries,
    ) { items, pending ->
        val pendingIds = pending.map(WagonEntry::id).toSet()
        items.filterNot { it.id in pendingIds }
    }

    fun createCaptureFile(): File = repository.createCaptureFile()

    fun selectCollection(collectionId: Long) {
        _selectedCollectionId.value = collectionId
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            val collectionId = repository.createCollection(name)
            _selectedCollectionId.value = collectionId
            _events.emit(AppEvent.Snackbar("Подборка создана", durationMillis = 1_000))
        }
    }

    fun renameCollection(
        collectionId: Long,
        name: String,
    ) {
        viewModelScope.launch {
            repository.renameCollection(collectionId, name)
            _events.emit(AppEvent.Snackbar("Название обновлено", durationMillis = 1_000))
        }
    }

    fun deleteCollection(collectionId: Long) {
        viewModelScope.launch {
            repository.deleteCollection(collectionId)
            if (_selectedCollectionId.value == collectionId) {
                _selectedCollectionId.value = null
            }
            _events.emit(AppEvent.Snackbar("Подборка удалена", durationMillis = 1_000))
        }
    }

    fun resetCameraState() {
        _cameraState.value = CameraUiState()
    }

    fun clearQrShareSession() {
        _qrShareSession.value = null
    }

    fun resetQrImportState() {
        activeQrTransferId = null
        activeQrTransferTotalChunks = 0
        activeQrChunks.clear()
        _qrImportState.value = QrImportUiState()
    }

    fun onCaptureError(message: String) {
        _cameraState.value = CameraUiState(
            statusMessage = message,
            errorMessage = message,
        )
    }

    fun processCapture(
        file: File,
        scanFrameSettings: ScanFrameSettings,
        collectionId: Long,
    ) {
        viewModelScope.launch {
            _cameraState.value = CameraUiState(
                isProcessing = true,
                statusMessage = "Снимок сохранен. Распознаю номер...",
            )

            runCatching { repository.saveCapturedPhoto(file, scanFrameSettings, collectionId) }
                .onSuccess { entryId ->
                    _cameraState.value = CameraUiState(
                        statusMessage = "Запись добавлена в журнал.",
                    )
                    _events.emit(AppEvent.OpenEntry(entryId))
                }
                .onFailure { exception ->
                    _cameraState.value = CameraUiState(
                        statusMessage = "Не удалось обработать снимок.",
                        errorMessage = exception.message ?: "Ошибка распознавания.",
                    )
                }
        }
    }

    fun createEmptyEntry(collectionId: Long) {
        viewModelScope.launch {
            runCatching { repository.createEmptyEntry(collectionId) }
                .onSuccess {
                    _events.emit(
                        AppEvent.Snackbar(
                            message = "Пустая карточка добавлена",
                            durationMillis = 1_000,
                        ),
                    )
                }
                .onFailure { exception ->
                    _events.emit(
                        AppEvent.Snackbar(
                            message = exception.message ?: "Не удалось добавить пустую карточку",
                            durationMillis = 1_600,
                        ),
                    )
                }
        }
    }

    fun updatePrimaryNumber(id: Long, number: String) {
        viewModelScope.launch {
            repository.updatePrimaryNumber(id, number)
            _events.emit(AppEvent.Snackbar("Номер обновлен", durationMillis = 1_400))
        }
    }

    fun updateCondition(
        id: Long,
        condition: WagonCondition,
    ) {
        viewModelScope.launch {
            repository.updateCondition(id, condition)
        }
    }

    fun moveEntry(
        entryId: Long,
        targetIndex: Int,
    ) {
        viewModelScope.launch {
            repository.moveEntry(entryId, targetIndex)
            _events.emit(AppEvent.Snackbar("Карточка перемещена", durationMillis = 1_000))
        }
    }

    fun saveNote(id: Long, note: String) {
        viewModelScope.launch {
            repository.updateNote(id, note)
            _events.emit(AppEvent.Snackbar("Заметка сохранена", durationMillis = 1_400))
        }
    }

    fun updateJournalDescription(
        collectionId: Long,
        description: String,
    ) {
        viewModelScope.launch {
            repository.updateCollectionDescription(collectionId, description)
            _events.emit(AppEvent.Snackbar("Сохранено", durationMillis = 1_200))
        }
    }

    fun showSnackbar(
        message: String,
        durationMillis: Long = 2_000,
    ) {
        viewModelScope.launch {
            _events.emit(AppEvent.Snackbar(message, durationMillis))
        }
    }

    fun replacePhoto(entryId: Long, file: File) {
        viewModelScope.launch {
            _cameraState.value = CameraUiState(
                isProcessing = true,
                statusMessage = "Сохраняю новое фото...",
            )

            runCatching { repository.replacePhoto(entryId, file) }
                .onSuccess {
                    _cameraState.value = CameraUiState(
                        statusMessage = "Фото обновлено",
                    )
                    _events.emit(AppEvent.PhotoUpdated("Фото заменено"))
                }
                .onFailure { exception ->
                    if (file.exists()) {
                        file.delete()
                    }
                    _cameraState.value = CameraUiState(
                        statusMessage = "Не удалось заменить фото.",
                        errorMessage = exception.message ?: "Ошибка замены фото.",
                    )
                }
        }
    }

    fun deletePhoto(entryId: Long) {
        viewModelScope.launch {
            runCatching { repository.deletePhoto(entryId) }
                .onSuccess {
                    _events.emit(AppEvent.Snackbar("Фото удалено", durationMillis = 1_400))
                }
                .onFailure { exception ->
                    _events.emit(
                        AppEvent.Snackbar(
                            message = exception.message ?: "Не удалось удалить фото",
                            durationMillis = 1_600,
                        ),
                    )
                }
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        settingsRepository.setThemeMode(themeMode)
    }

    fun setAppIconMode(appIconMode: AppIconMode) {
        settingsRepository.setAppIconMode(appIconMode)
        appIconManager.apply(appIconMode)
        showSnackbar("Иконка обновлена", durationMillis = 1_000)
    }

    fun setNumberFontSize(fontSizeSp: Float) {
        settingsRepository.setNumberFontSizeSp(fontSizeSp)
    }

    fun setCollectionLayoutMode(mode: CollectionLayoutMode) {
        settingsRepository.setCollectionLayoutMode(mode)
    }

    fun toggleDirection(collectionId: Long) {
        viewModelScope.launch {
            repository.toggleCollectionDirection(collectionId)
            _events.emit(AppEvent.Snackbar("Направление изменено", durationMillis = 1_000))
        }
    }

    fun updateDirectionLabels(
        collectionId: Long,
        primaryLabel: String,
        secondaryLabel: String,
    ) {
        viewModelScope.launch {
            repository.updateCollectionDirectionLabels(collectionId, primaryLabel, secondaryLabel)
            _events.emit(AppEvent.Snackbar("Названия обновлены", durationMillis = 1_200))
        }
    }

    fun setNewEntryPosition(
        collectionId: Long,
        position: NewEntryPosition,
    ) {
        viewModelScope.launch {
            repository.updateCollectionNewEntryPosition(collectionId, position)
            _events.emit(AppEvent.Snackbar("Порядок обновлен", durationMillis = 1_000))
        }
    }

    fun setIncludeDirectionInCopy(
        collectionId: Long,
        include: Boolean,
    ) {
        viewModelScope.launch {
            repository.updateCollectionIncludeDirectionInCopy(collectionId, include)
            _events.emit(AppEvent.Snackbar("Копирование обновлено", durationMillis = 1_000))
        }
    }

    fun setShowPhotosInJournal(show: Boolean) {
        settingsRepository.setShowPhotosInJournal(show)
    }

    fun setPhotoQualityJpeg(quality: Int) {
        settingsRepository.setPhotoQualityJpeg(quality)
        showSnackbar("Качество фото: JPEG $quality", durationMillis = 1_000)
    }

    fun setSharePhotoQualityJpeg(quality: Int) {
        settingsRepository.setSharePhotoQualityJpeg(quality)
        showSnackbar("Качество отправки: JPEG $quality", durationMillis = 1_000)
    }

    fun setScanFrameWidthFraction(value: Float) {
        settingsRepository.setScanFrameWidthFraction(value)
    }

    fun setScanFrameHeightFraction(value: Float) {
        settingsRepository.setScanFrameHeightFraction(value)
    }

    fun setScanFrameTopFraction(value: Float) {
        settingsRepository.setScanFrameTopFraction(value)
    }

    fun setScanFrameSettings(scanFrameSettings: ScanFrameSettings) {
        settingsRepository.setScanFrameSettings(scanFrameSettings)
        showSnackbar("Рамка обновлена", durationMillis = 1_000)
    }

    fun resetScanFrameSettings() {
        settingsRepository.resetScanFrameSettings()
        showSnackbar("Рамка сброшена", durationMillis = 1_000)
    }

    fun shareCollectionFile(
        collectionId: Long,
        includePhotos: Boolean,
    ) {
        viewModelScope.launch {
            runCatching {
                collectionExchangeManager.createShareFile(
                    collectionId = collectionId,
                    includePhotos = includePhotos,
                    jpegQuality = settings.value.sharePhotoQualityJpeg,
                )
            }.onSuccess { sharedFile ->
                _events.emit(
                    AppEvent.ShareFile(
                        uri = sharedFile.uri,
                        mimeType = sharedFile.mimeType,
                        chooserTitle = if (includePhotos) {
                            "Отправить подборку с фото"
                        } else {
                            "Отправить подборку без фото"
                        },
                    ),
                )
            }.onFailure { exception ->
                _events.emit(
                    AppEvent.Snackbar(
                        message = exception.message ?: "Не удалось подготовить файл подборки",
                        durationMillis = 1_600,
                    ),
                )
            }
        }
    }

    fun prepareCollectionQrShare(collectionId: Long) {
        viewModelScope.launch {
            runCatching {
                collectionExchangeManager.createQrShareSession(collectionId)
            }.onSuccess { session ->
                _qrShareSession.value = session
                _events.emit(AppEvent.OpenQrShare)
            }.onFailure { exception ->
                _events.emit(
                    AppEvent.Snackbar(
                        message = exception.message ?: "Не удалось подготовить QR-код подборки",
                        durationMillis = 1_600,
                    ),
                )
            }
        }
    }

    fun importCollectionFromUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val preview = collectionExchangeManager.inspectImportUri(uri)
                val existingCollection = repository.findCollectionByName(preview.collectionName)
                preview to existingCollection
            }.onSuccess { (preview, existingCollection) ->
                if (existingCollection != null) {
                    _pendingImportConflict.value = PendingImportConflict(
                        uri = uri,
                        collectionName = preview.collectionName,
                        existingCollectionId = existingCollection.id,
                        entryCount = preview.entryCount,
                    )
                } else {
                    importCollectionFromUriInternal(
                        uri = uri,
                        mode = CollectionImportMode.KeepOriginal,
                    )
                }
            }
                .onFailure { exception ->
                    _events.emit(
                        AppEvent.Snackbar(
                            message = exception.message ?: "Не удалось импортировать подборку",
                            durationMillis = 1_800,
                        ),
                    )
                }
        }
    }

    fun dismissPendingImportConflict() {
        _pendingImportConflict.value = null
    }

    fun notifyImportAccessDenied() {
        viewModelScope.launch {
            _events.emit(
                AppEvent.Snackbar(
                    message = "Нет доступа к выбранному Hopper-файлу",
                    durationMillis = 1_800,
                ),
            )
        }
    }

    fun replaceImportedCollection() {
        val pendingConflict = _pendingImportConflict.value ?: return
        _pendingImportConflict.value = null
        importCollectionFromUriInternal(
            uri = pendingConflict.uri,
            mode = CollectionImportMode.ReplaceExisting(pendingConflict.existingCollectionId),
        )
    }

    fun importCollectionAsCopy() {
        val pendingConflict = _pendingImportConflict.value ?: return
        _pendingImportConflict.value = null
        importCollectionFromUriInternal(
            uri = pendingConflict.uri,
            mode = CollectionImportMode.CreateCopy,
        )
    }

    private fun importCollectionFromUriInternal(
        uri: Uri,
        mode: CollectionImportMode,
    ) {
        viewModelScope.launch {
            runCatching { collectionExchangeManager.importFromUri(uri, mode) }
                .onSuccess { result ->
                    _selectedCollectionId.value = result.collectionId
                    _events.emit(
                        AppEvent.OpenCollection(
                            collectionId = result.collectionId,
                            message = "Подборка \"${result.collectionName}\" импортирована",
                        ),
                    )
                }
                .onFailure { exception ->
                    _events.emit(
                        AppEvent.Snackbar(
                            message = exception.message ?: "Не удалось импортировать подборку",
                            durationMillis = 1_800,
                        ),
                    )
                }
        }
    }

    fun onQrCodeScanned(rawValue: String) {
        if (_qrImportState.value.isImporting) return

        val chunk = collectionExchangeManager.parseQrChunk(rawValue) ?: return
        val currentTransferId = activeQrTransferId
        if (currentTransferId != null && currentTransferId != chunk.transferId) {
            _qrImportState.value = _qrImportState.value.copy(
                errorMessage = "Сначала завершите текущий набор QR-кодов.",
                statusMessage = "Сканируется другая подборка: ${activeQrChunks.size} из $activeQrTransferTotalChunks.",
            )
            return
        }

        if (currentTransferId == null) {
            activeQrTransferId = chunk.transferId
            activeQrTransferTotalChunks = chunk.total
            activeQrChunks.clear()
        }

        val wasAdded = activeQrChunks.putIfAbsent(chunk.index, rawValue) == null
        val scannedCount = activeQrChunks.size
        val totalChunks = activeQrTransferTotalChunks

        _qrImportState.value = QrImportUiState(
            scannedChunks = scannedCount,
            totalChunks = totalChunks,
            isImporting = false,
            statusMessage = if (wasAdded) {
                "Получено $scannedCount из $totalChunks QR-кодов."
            } else {
                "Этот QR уже считан: $scannedCount из $totalChunks."
            },
            errorMessage = null,
        )

        if (scannedCount != totalChunks) return

        _qrImportState.value = _qrImportState.value.copy(
            isImporting = true,
            statusMessage = "Импортирую подборку из QR-кодов...",
            errorMessage = null,
        )

        viewModelScope.launch {
            runCatching {
                collectionExchangeManager.importFromQrChunks(activeQrChunks.values)
            }.onSuccess { result ->
                _qrImportState.value = _qrImportState.value.copy(
                    isImporting = true,
                    statusMessage = "Подборка импортирована. Открываю журнал...",
                    errorMessage = null,
                )
                _selectedCollectionId.value = result.collectionId
                _events.emit(
                    AppEvent.OpenCollection(
                        collectionId = result.collectionId,
                        message = "Подборка \"${result.collectionName}\" импортирована из QR",
                    ),
                )
            }.onFailure { exception ->
                val message = exception.message ?: "Не удалось импортировать подборку из QR-кодов"
                resetQrImportState()
                _qrImportState.value = QrImportUiState(
                    statusMessage = "Не удалось импортировать подборку.",
                    errorMessage = message,
                )
            }
        }
    }

    fun requestDelete(entry: WagonEntry, popBack: Boolean) {
        requestDeleteEntries(
            entries = listOf(entry),
            popBack = popBack,
            message = "Карточка удалена",
        )
    }

    fun requestDeleteEntries(entries: List<WagonEntry>) {
        val count = entries.size
        val message = if (count == 1) {
            "Карточка удалена"
        } else {
            "Удалено $count карточки"
        }
        requestDeleteEntries(
            entries = entries,
            popBack = false,
            message = message,
        )
    }

    private fun requestDeleteEntries(
        entries: List<WagonEntry>,
        popBack: Boolean,
        message: String,
    ) {
        viewModelScope.launch {
            if (entries.isEmpty()) return@launch
            val previousPending = pendingDeleteEntries.value
            if (previousPending.isNotEmpty()) {
                repository.deleteEntries(previousPending)
            }
            pendingDeleteEntries.value = entries
            _events.emit(
                AppEvent.PendingDelete(
                    entryIds = entries.map(WagonEntry::id),
                    message = message,
                    popBack = popBack,
                ),
            )
        }
    }

    fun restorePendingDelete(entryIds: List<Long>) {
        if (pendingDeleteEntries.value.hasSameIds(entryIds)) {
            pendingDeleteEntries.value = emptyList()
        }
    }

    fun confirmPendingDelete(entryIds: List<Long>) {
        viewModelScope.launch {
            val entries = pendingDeleteEntries.value.takeIf { it.hasSameIds(entryIds) } ?: return@launch
            repository.deleteEntries(entries)
            pendingDeleteEntries.value = emptyList()
        }
    }

    class Factory(
        private val repository: RailRepository,
        private val settingsRepository: UserSettingsRepository,
        private val appIconManager: AppIconManager,
        private val collectionExchangeManager: CollectionExchangeManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(
                    repository,
                    settingsRepository,
                    appIconManager,
                    collectionExchangeManager,
                ) as T
            }
            error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

private fun List<WagonEntry>.hasSameIds(entryIds: List<Long>): Boolean {
    return size == entryIds.size && map(WagonEntry::id).toSet() == entryIds.toSet()
}
