package com.alex.hopper.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alex.hopper.data.CollectionSummary
import com.alex.hopper.data.RailRepository
import com.alex.hopper.data.WagonCollection
import com.alex.hopper.data.WagonEntry
import com.alex.hopper.settings.AppIconManager
import com.alex.hopper.settings.AppIconMode
import com.alex.hopper.settings.AppSettings
import com.alex.hopper.settings.AppThemeMode
import com.alex.hopper.settings.CollectionLayoutMode
import com.alex.hopper.settings.NewEntryPosition
import com.alex.hopper.settings.UserSettingsRepository
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

sealed interface AppEvent {
    data class OpenEntry(val entryId: Long) : AppEvent
    data class OpenCollection(val collectionId: Long) : AppEvent
    data class Snackbar(
        val message: String,
        val durationMillis: Long = 2_000,
    ) : AppEvent
    data class PhotoUpdated(val message: String) : AppEvent
    data class PendingDelete(
        val entryId: Long,
        val message: String,
        val popBack: Boolean,
    ) : AppEvent
}

class MainViewModel(
    private val repository: RailRepository,
    private val settingsRepository: UserSettingsRepository,
    private val appIconManager: AppIconManager,
) : ViewModel() {
    private val pendingDeleteEntry = MutableStateFlow<WagonEntry?>(null)
    private val _selectedCollectionId = MutableStateFlow<Long?>(null)
    private val _cameraState = MutableStateFlow(CameraUiState())
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 1)

    val settings: StateFlow<AppSettings> = settingsRepository.settings
    val selectedCollectionId: StateFlow<Long?> = _selectedCollectionId.asStateFlow()
    val cameraState: StateFlow<CameraUiState> = _cameraState.asStateFlow()
    val events = _events.asSharedFlow()

    val collections: StateFlow<List<CollectionSummary>> = repository.observeCollections()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val allEntries: StateFlow<List<WagonEntry>> = repository.observeAllEntries()
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
        pendingDeleteEntry,
    ) { items, pending ->
        items.filterNot { it.id == pending?.id }
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

    fun createCaptureFile(): File = repository.createCaptureFile()

    fun selectCollection(collectionId: Long) {
        _selectedCollectionId.value = collectionId
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            val collectionId = repository.createCollection(name)
            _selectedCollectionId.value = collectionId
            _events.emit(AppEvent.OpenCollection(collectionId))
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

    fun onCaptureError(message: String) {
        _cameraState.value = CameraUiState(
            statusMessage = message,
            errorMessage = message,
        )
    }

    fun processCapture(
        file: File,
        scanBitmap: Bitmap?,
        collectionId: Long,
    ) {
        viewModelScope.launch {
            _cameraState.value = CameraUiState(
                isProcessing = true,
                statusMessage = "Снимок сохранен. Распознаю номер...",
            )

            runCatching { repository.saveCapturedPhoto(file, scanBitmap, collectionId) }
                .onSuccess { entryId ->
                    if (settings.value.newEntryPosition == NewEntryPosition.First) {
                        repository.moveEntry(entryId, 0)
                    }
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

    fun updatePrimaryNumber(id: Long, number: String) {
        viewModelScope.launch {
            repository.updatePrimaryNumber(id, number)
            _events.emit(AppEvent.Snackbar("Номер обновлен", durationMillis = 1_400))
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

    fun updateJournalDescription(description: String) {
        val collectionId = _selectedCollectionId.value ?: return
        viewModelScope.launch {
            repository.updateCollectionDescription(collectionId, description)
            _events.emit(AppEvent.Snackbar("Описание сохранено", durationMillis = 1_200))
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

    fun toggleDirection() {
        settingsRepository.toggleDirection()
        showSnackbar("Направление изменено", durationMillis = 1_000)
    }

    fun updateDirectionLabels(
        primaryLabel: String,
        secondaryLabel: String,
    ) {
        settingsRepository.setDirectionLabels(primaryLabel, secondaryLabel)
        showSnackbar("Названия обновлены", durationMillis = 1_200)
    }

    fun setNewEntryPosition(position: NewEntryPosition) {
        settingsRepository.setNewEntryPosition(position)
        showSnackbar("Порядок обновлен", durationMillis = 1_000)
    }

    fun setIncludeDirectionInCopy(include: Boolean) {
        settingsRepository.setIncludeDirectionInCopy(include)
        showSnackbar("Копирование обновлено", durationMillis = 1_000)
    }

    fun setShowPhotosInJournal(show: Boolean) {
        settingsRepository.setShowPhotosInJournal(show)
    }

    fun requestDelete(entry: WagonEntry, popBack: Boolean) {
        viewModelScope.launch {
            val previousPending = pendingDeleteEntry.value
            if (previousPending != null) {
                repository.deleteEntry(previousPending)
            }
            pendingDeleteEntry.value = entry
            _events.emit(
                AppEvent.PendingDelete(
                    entryId = entry.id,
                    message = "Карточка удалена",
                    popBack = popBack,
                ),
            )
        }
    }

    fun restorePendingDelete(entryId: Long) {
        if (pendingDeleteEntry.value?.id == entryId) {
            pendingDeleteEntry.value = null
        }
    }

    fun confirmPendingDelete(entryId: Long) {
        viewModelScope.launch {
            val entry = pendingDeleteEntry.value?.takeIf { it.id == entryId } ?: return@launch
            repository.deleteEntry(entry)
            pendingDeleteEntry.value = null
        }
    }

    class Factory(
        private val repository: RailRepository,
        private val settingsRepository: UserSettingsRepository,
        private val appIconManager: AppIconManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository, settingsRepository, appIconManager) as T
            }
            error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
