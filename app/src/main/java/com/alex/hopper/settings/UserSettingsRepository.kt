package com.alex.hopper.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

enum class AppThemeMode {
    SupabaseDark,
    StripeStyle,
    AirbnbStyle,
    HybridClean,
}

enum class NewEntryPosition {
    First,
    Last,
}

enum class CollectionLayoutMode {
    Grid,
    List,
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.HybridClean,
    val appIconMode: AppIconMode = AppIconMode.Yellow,
    val numberFontSizeSp: Float = 20f,
    val primaryDirectionLabel: String = "ЗАПАД",
    val secondaryDirectionLabel: String = "ВОСТОК",
    val isPrimaryDirectionOnTop: Boolean = true,
    val journalDescription: String = "",
    val newEntryPosition: NewEntryPosition = NewEntryPosition.Last,
    val includeDirectionInCopy: Boolean = true,
    val showPhotosInJournal: Boolean = true,
    val collectionLayoutMode: CollectionLayoutMode = CollectionLayoutMode.Grid,
    val photoQualityJpeg: Int = 92,
    val sharePhotoQualityJpeg: Int = 72,
) {
    val topDirectionLabel: String
        get() = if (isPrimaryDirectionOnTop) primaryDirectionLabel else secondaryDirectionLabel

    val bottomDirectionLabel: String
        get() = if (isPrimaryDirectionOnTop) secondaryDirectionLabel else primaryDirectionLabel
}

class UserSettingsRepository(
    context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(loadSettings())

    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setThemeMode(themeMode: AppThemeMode) {
        preferences.edit()
            .putString(KEY_THEME_MODE, themeMode.name)
            .apply()
        _settings.value = loadSettings()
    }

    fun setAppIconMode(appIconMode: AppIconMode) {
        preferences.edit()
            .putString(KEY_APP_ICON_MODE, appIconMode.name)
            .apply()
        _settings.value = loadSettings()
    }

    fun setNumberFontSizeSp(fontSizeSp: Float) {
        preferences.edit()
            .putFloat(KEY_NUMBER_FONT_SIZE, fontSizeSp.coerceIn(MIN_NUMBER_SIZE, MAX_NUMBER_SIZE))
            .apply()
        _settings.value = loadSettings()
    }

    fun toggleDirection() {
        preferences.edit()
            .putBoolean(KEY_PRIMARY_ON_TOP, !_settings.value.isPrimaryDirectionOnTop)
            .apply()
        _settings.value = loadSettings()
    }

    fun setDirectionLabels(
        primaryLabel: String,
        secondaryLabel: String,
    ) {
        preferences.edit()
            .putString(KEY_PRIMARY_LABEL, primaryLabel.ifBlank { DEFAULT_PRIMARY_LABEL })
            .putString(KEY_SECONDARY_LABEL, secondaryLabel.ifBlank { DEFAULT_SECONDARY_LABEL })
            .apply()
        _settings.value = loadSettings()
    }

    fun setJournalDescription(description: String) {
        preferences.edit()
            .putString(KEY_JOURNAL_DESCRIPTION, description)
            .apply()
        _settings.value = loadSettings()
    }

    fun setNewEntryPosition(position: NewEntryPosition) {
        preferences.edit()
            .putString(KEY_NEW_ENTRY_POSITION, position.name)
            .apply()
        _settings.value = loadSettings()
    }

    fun setIncludeDirectionInCopy(include: Boolean) {
        preferences.edit()
            .putBoolean(KEY_INCLUDE_DIRECTION_IN_COPY, include)
            .apply()
        _settings.value = loadSettings()
    }

    fun setShowPhotosInJournal(show: Boolean) {
        preferences.edit()
            .putBoolean(KEY_SHOW_PHOTOS_IN_JOURNAL, show)
            .apply()
        _settings.value = loadSettings()
    }

    fun setCollectionLayoutMode(mode: CollectionLayoutMode) {
        preferences.edit()
            .putString(KEY_COLLECTION_LAYOUT_MODE, mode.name)
            .apply()
        _settings.value = loadSettings()
    }

    fun setPhotoQualityJpeg(quality: Int) {
        preferences.edit()
            .putInt(KEY_PHOTO_QUALITY_JPEG, quality.coerceIn(MIN_PHOTO_QUALITY_JPEG, MAX_PHOTO_QUALITY_JPEG))
            .apply()
        _settings.value = loadSettings()
    }

    fun setSharePhotoQualityJpeg(quality: Int) {
        preferences.edit()
            .putInt(
                KEY_SHARE_PHOTO_QUALITY_JPEG,
                quality.coerceIn(MIN_PHOTO_QUALITY_JPEG, MAX_PHOTO_QUALITY_JPEG),
            )
            .apply()
        _settings.value = loadSettings()
    }

    private fun loadSettings(): AppSettings {
        val themeMode = when (preferences.getString(KEY_THEME_MODE, AppThemeMode.HybridClean.name)) {
            "VercelStyle",
            "AppleLight",
            "AirtableLight",
            "FigmaLight",
            null,
            -> AppThemeMode.HybridClean
            AppThemeMode.SupabaseDark.name -> AppThemeMode.SupabaseDark
            AppThemeMode.StripeStyle.name -> AppThemeMode.StripeStyle
            AppThemeMode.AirbnbStyle.name -> AppThemeMode.AirbnbStyle
            AppThemeMode.HybridClean.name -> AppThemeMode.HybridClean
            else -> AppThemeMode.HybridClean
        }

        val appIconMode = preferences.getString(KEY_APP_ICON_MODE, AppIconMode.Yellow.name)
            ?.let { stored ->
                AppIconMode.entries.firstOrNull { it.name == stored }
            }
            ?: AppIconMode.Yellow

        val newEntryPosition = preferences.getString(KEY_NEW_ENTRY_POSITION, NewEntryPosition.Last.name)
            ?.let { stored ->
                NewEntryPosition.entries.firstOrNull { it.name == stored }
            }
            ?: NewEntryPosition.Last

        val collectionLayoutMode = preferences.getString(
            KEY_COLLECTION_LAYOUT_MODE,
            CollectionLayoutMode.Grid.name,
        )?.let { stored ->
            CollectionLayoutMode.entries.firstOrNull { it.name == stored }
        } ?: CollectionLayoutMode.Grid

        val photoQualityJpeg = when {
            preferences.contains(KEY_PHOTO_QUALITY_JPEG) -> {
                preferences.getInt(KEY_PHOTO_QUALITY_JPEG, DEFAULT_PHOTO_QUALITY_JPEG)
                    .coerceIn(MIN_PHOTO_QUALITY_JPEG, MAX_PHOTO_QUALITY_JPEG)
            }
            else -> when (preferences.getString(KEY_PHOTO_QUALITY_MODE, null)) {
                "High" -> 92
                "Standard" -> 82
                "Compact" -> 72
                else -> DEFAULT_PHOTO_QUALITY_JPEG
            }
        }

        val sharePhotoQualityJpeg = preferences.getInt(
            KEY_SHARE_PHOTO_QUALITY_JPEG,
            DEFAULT_SHARE_PHOTO_QUALITY_JPEG,
        ).coerceIn(MIN_PHOTO_QUALITY_JPEG, MAX_PHOTO_QUALITY_JPEG)

        val numberFontSizeSp = preferences.getFloat(KEY_NUMBER_FONT_SIZE, 20f)
            .coerceIn(MIN_NUMBER_SIZE, MAX_NUMBER_SIZE)
            .roundToInt()
            .toFloat()

        return AppSettings(
            themeMode = themeMode,
            appIconMode = appIconMode,
            numberFontSizeSp = numberFontSizeSp,
            primaryDirectionLabel = preferences.getString(KEY_PRIMARY_LABEL, DEFAULT_PRIMARY_LABEL)
                ?: DEFAULT_PRIMARY_LABEL,
            secondaryDirectionLabel = preferences.getString(KEY_SECONDARY_LABEL, DEFAULT_SECONDARY_LABEL)
                ?: DEFAULT_SECONDARY_LABEL,
            isPrimaryDirectionOnTop = preferences.getBoolean(KEY_PRIMARY_ON_TOP, true),
            journalDescription = preferences.getString(KEY_JOURNAL_DESCRIPTION, "") ?: "",
            newEntryPosition = newEntryPosition,
            includeDirectionInCopy = preferences.getBoolean(KEY_INCLUDE_DIRECTION_IN_COPY, true),
            showPhotosInJournal = preferences.getBoolean(KEY_SHOW_PHOTOS_IN_JOURNAL, true),
            collectionLayoutMode = collectionLayoutMode,
            photoQualityJpeg = photoQualityJpeg,
            sharePhotoQualityJpeg = sharePhotoQualityJpeg,
        )
    }

    private companion object {
        const val PREFS_NAME = "xdw_preferences"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_APP_ICON_MODE = "app_icon_mode"
        const val KEY_NUMBER_FONT_SIZE = "number_font_size_sp"
        const val KEY_PRIMARY_LABEL = "primary_direction_label"
        const val KEY_SECONDARY_LABEL = "secondary_direction_label"
        const val KEY_PRIMARY_ON_TOP = "primary_direction_on_top"
        const val KEY_JOURNAL_DESCRIPTION = "journal_description"
        const val KEY_NEW_ENTRY_POSITION = "new_entry_position"
        const val KEY_INCLUDE_DIRECTION_IN_COPY = "include_direction_in_copy"
        const val KEY_SHOW_PHOTOS_IN_JOURNAL = "show_photos_in_journal"
        const val KEY_COLLECTION_LAYOUT_MODE = "collection_layout_mode"
        const val KEY_PHOTO_QUALITY_MODE = "photo_quality_mode"
        const val KEY_PHOTO_QUALITY_JPEG = "photo_quality_jpeg"
        const val KEY_SHARE_PHOTO_QUALITY_JPEG = "share_photo_quality_jpeg"

        const val DEFAULT_PRIMARY_LABEL = "ЗАПАД"
        const val DEFAULT_SECONDARY_LABEL = "ВОСТОК"
        const val MIN_NUMBER_SIZE = 18f
        const val MAX_NUMBER_SIZE = 27f
        const val MIN_PHOTO_QUALITY_JPEG = 60
        const val MAX_PHOTO_QUALITY_JPEG = 92
        const val DEFAULT_PHOTO_QUALITY_JPEG = 92
        const val DEFAULT_SHARE_PHOTO_QUALITY_JPEG = 72
    }
}
