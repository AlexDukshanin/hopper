package com.alex.xdw

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.alex.xdw.data.AppDatabase
import com.alex.xdw.data.RailRepository
import com.alex.xdw.ocr.OcrEngine
import com.alex.xdw.ocr.WagonNumberExtractor
import com.alex.xdw.settings.UserSettingsRepository
import com.alex.xdw.storage.PhotoStorage

class XdwApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "xdw.db",
    ).build()

    private val photoStorage = PhotoStorage(context)
    private val ocrEngine = OcrEngine(context)
    private val extractor = WagonNumberExtractor()
    val settingsRepository = UserSettingsRepository(context)

    val repository = RailRepository(
        dao = database.wagonEntryDao(),
        ocrEngine = ocrEngine,
        extractor = extractor,
        photoStorage = photoStorage,
    )
}
