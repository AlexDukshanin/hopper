package com.alex.hopper.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

enum class AppThemeMode {
    SupabaseDark,
    RaycastDark,
    ComposioDark,
    NvidiaDark,
    MongoDark,
    StripeStyle,
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

private const val DEFAULT_SCAN_FRAME_WIDTH = 1.0f
private const val DEFAULT_SCAN_FRAME_HEIGHT = 0.255f
private const val DEFAULT_SCAN_FRAME_TOP = 0.30f
private const val DEFAULT_SCAN_FRAME_LEFT = 0f

data class ScanFrameSettings(
    val leftFraction: Float = DEFAULT_SCAN_FRAME_LEFT,
    val widthFraction: Float = DEFAULT_SCAN_FRAME_WIDTH,
    val heightFraction: Float = DEFAULT_SCAN_FRAME_HEIGHT,
    val topFraction: Float = DEFAULT_SCAN_FRAME_TOP,
)

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.RaycastDark,
    val appIconMode: AppIconMode = AppIconMode.Yellow,
    val numberFontSizeSp: Float = 25f,
    val primaryDirectionLabel: String = "ЗАПАД",
    val secondaryDirectionLabel: String = "ВОСТОК",
    val isPrimaryDirectionOnTop: Boolean = true,
    val journalDescription: String = "",
    val newEntryPosition: NewEntryPosition = NewEntryPosition.Last,
    val includeDirectionInCopy: Boolean = true,
    val showPhotosInJournal: Boolean = true,
    val collectionLayoutMode: CollectionLayoutMode = CollectionLayoutMode.Grid,
    val photoQualityJpeg: Int = 72,
    val sharePhotoQualityJpeg: Int = 60,
    val scanFrameLeftFraction: Float = DEFAULT_SCAN_FRAME_LEFT,
    val scanFrameWidthFraction: Float = DEFAULT_SCAN_FRAME_WIDTH,
    val scanFrameHeightFraction: Float = DEFAULT_SCAN_FRAME_HEIGHT,
    val scanFrameTopFraction: Float = DEFAULT_SCAN_FRAME_TOP,
) {
    val topDirectionLabel: String
        get() = if (isPrimaryDirectionOnTop) primaryDirectionLabel else secondaryDirectionLabel

    val bottomDirectionLabel: String
        get() = if (isPrimaryDirectionOnTop) secondaryDirectionLabel else primaryDirectionLabel

    val scanFrameSettings: ScanFrameSettings
        get() = ScanFrameSettings(
            leftFraction = scanFrameLeftFraction,
            widthFraction = scanFrameWidthFraction,
            heightFraction = scanFrameHeightFraction,
            topFraction = scanFrameTopFraction,
        )
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

    fun setScanFrameWidthFraction(value: Float) {
        preferences.edit()
            .putFloat(KEY_SCAN_FRAME_WIDTH, value.coerceIn(MIN_SCAN_FRAME_WIDTH, MAX_SCAN_FRAME_WIDTH))
            .apply()
        _settings.value = loadSettings()
    }

    fun setScanFrameLeftFraction(value: Float) {
        val widthFraction = _settings.value.scanFrameWidthFraction
        preferences.edit()
            .putFloat(KEY_SCAN_FRAME_LEFT, value.coerceIn(0f, (1f - widthFraction).coerceAtLeast(0f)))
            .apply()
        _settings.value = loadSettings()
    }

    fun setScanFrameHeightFraction(value: Float) {
        preferences.edit()
            .putFloat(KEY_SCAN_FRAME_HEIGHT, value.coerceIn(MIN_SCAN_FRAME_HEIGHT, MAX_SCAN_FRAME_HEIGHT))
            .apply()
        _settings.value = loadSettings()
    }

    fun setScanFrameTopFraction(value: Float) {
        preferences.edit()
            .putFloat(KEY_SCAN_FRAME_TOP, value.coerceIn(MIN_SCAN_FRAME_TOP, MAX_SCAN_FRAME_TOP))
            .apply()
        _settings.value = loadSettings()
    }

    fun setScanFrameSettings(scanFrameSettings: ScanFrameSettings) {
        val widthFraction = scanFrameSettings.widthFraction.coerceIn(MIN_SCAN_FRAME_WIDTH, MAX_SCAN_FRAME_WIDTH)
        val heightFraction = scanFrameSettings.heightFraction.coerceIn(MIN_SCAN_FRAME_HEIGHT, MAX_SCAN_FRAME_HEIGHT)
        val leftFraction = scanFrameSettings.leftFraction.coerceIn(0f, (1f - widthFraction).coerceAtLeast(0f))
        val topFraction = scanFrameSettings.topFraction.coerceIn(MIN_SCAN_FRAME_TOP, (1f - heightFraction).coerceAtLeast(MIN_SCAN_FRAME_TOP))
        preferences.edit()
            .putFloat(KEY_SCAN_FRAME_LEFT, leftFraction)
            .putFloat(KEY_SCAN_FRAME_WIDTH, widthFraction)
            .putFloat(KEY_SCAN_FRAME_HEIGHT, heightFraction)
            .putFloat(KEY_SCAN_FRAME_TOP, topFraction)
            .apply()
        _settings.value = loadSettings()
    }

    fun resetScanFrameSettings() {
        setScanFrameSettings(ScanFrameSettings())
    }

    fun hasMigratedCollectionScopedJournalSettings(): Boolean =
        preferences.getBoolean(KEY_COLLECTION_SCOPED_JOURNAL_SETTINGS_MIGRATED, false)

    fun markCollectionScopedJournalSettingsMigrated() {
        preferences.edit()
            .putBoolean(KEY_COLLECTION_SCOPED_JOURNAL_SETTINGS_MIGRATED, true)
            .apply()
    }

    private fun loadSettings(): AppSettings {
        val themeMode = when (preferences.getString(KEY_THEME_MODE, AppThemeMode.RaycastDark.name)) {
            "VercelStyle",
            "AppleLight",
            "AirtableLight",
            "FigmaLight",
            "AirbnbStyle",
            null,
            -> AppThemeMode.RaycastDark
            "LinearDark" -> AppThemeMode.RaycastDark
            "SentryDark" -> AppThemeMode.ComposioDark
            "ClickHouseDark" -> AppThemeMode.NvidiaDark
            AppThemeMode.SupabaseDark.name -> AppThemeMode.SupabaseDark
            AppThemeMode.RaycastDark.name -> AppThemeMode.RaycastDark
            AppThemeMode.ComposioDark.name -> AppThemeMode.ComposioDark
            AppThemeMode.NvidiaDark.name -> AppThemeMode.NvidiaDark
            AppThemeMode.MongoDark.name -> AppThemeMode.MongoDark
            AppThemeMode.StripeStyle.name -> AppThemeMode.StripeStyle
            AppThemeMode.HybridClean.name -> AppThemeMode.HybridClean
            else -> AppThemeMode.RaycastDark
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

        val numberFontSizeSp = preferences.getFloat(KEY_NUMBER_FONT_SIZE, 25f)
            .coerceIn(MIN_NUMBER_SIZE, MAX_NUMBER_SIZE)
            .roundToInt()
            .toFloat()

        val scanFrameWidthFraction = preferences.getFloat(KEY_SCAN_FRAME_WIDTH, DEFAULT_SCAN_FRAME_WIDTH)
            .coerceIn(MIN_SCAN_FRAME_WIDTH, MAX_SCAN_FRAME_WIDTH)
        val scanFrameHeightFraction = preferences.getFloat(KEY_SCAN_FRAME_HEIGHT, DEFAULT_SCAN_FRAME_HEIGHT)
            .coerceIn(MIN_SCAN_FRAME_HEIGHT, MAX_SCAN_FRAME_HEIGHT)
        val scanFrameLeftFraction = preferences.getFloat(
            KEY_SCAN_FRAME_LEFT,
            ((1f - scanFrameWidthFraction) / 2f).coerceAtLeast(0f),
        ).coerceIn(0f, (1f - scanFrameWidthFraction).coerceAtLeast(0f))
        val scanFrameTopFraction = preferences.getFloat(KEY_SCAN_FRAME_TOP, DEFAULT_SCAN_FRAME_TOP)
            .coerceIn(MIN_SCAN_FRAME_TOP, (1f - scanFrameHeightFraction).coerceAtLeast(MIN_SCAN_FRAME_TOP))

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
            scanFrameLeftFraction = scanFrameLeftFraction,
            scanFrameWidthFraction = scanFrameWidthFraction,
            scanFrameHeightFraction = scanFrameHeightFraction,
            scanFrameTopFraction = scanFrameTopFraction,
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
        const val KEY_SCAN_FRAME_LEFT = "scan_frame_left_fraction"
        const val KEY_SCAN_FRAME_WIDTH = "scan_frame_width_fraction"
        const val KEY_SCAN_FRAME_HEIGHT = "scan_frame_height_fraction"
        const val KEY_SCAN_FRAME_TOP = "scan_frame_top_fraction"
        const val KEY_COLLECTION_SCOPED_JOURNAL_SETTINGS_MIGRATED =
            "collection_scoped_journal_settings_migrated"

        const val DEFAULT_PRIMARY_LABEL = "ЗАПАД"
        const val DEFAULT_SECONDARY_LABEL = "ВОСТОК"
        const val MIN_NUMBER_SIZE = 18f
        const val MAX_NUMBER_SIZE = 27f
        const val MIN_PHOTO_QUALITY_JPEG = 60
        const val MAX_PHOTO_QUALITY_JPEG = 92
        const val DEFAULT_PHOTO_QUALITY_JPEG = 72
        const val DEFAULT_SHARE_PHOTO_QUALITY_JPEG = 60
        const val MIN_SCAN_FRAME_WIDTH = 0.7f
        const val MAX_SCAN_FRAME_WIDTH = 1.0f
        const val MIN_SCAN_FRAME_HEIGHT = 0.16f
        const val MAX_SCAN_FRAME_HEIGHT = 0.42f
        const val MIN_SCAN_FRAME_TOP = 0.02f
        const val MAX_SCAN_FRAME_TOP = 0.5f
    }
}
