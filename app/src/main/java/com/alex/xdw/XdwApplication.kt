package com.alex.xdw

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alex.xdw.data.AppDatabase
import com.alex.xdw.data.RailRepository
import com.alex.xdw.ocr.OcrEngine
import com.alex.xdw.ocr.WagonNumberExtractor
import com.alex.xdw.settings.AppIconManager
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
    ).addMigrations(MIGRATION_1_2).build()

    private val photoStorage = PhotoStorage(context)
    private val ocrEngine = OcrEngine(context)
    private val extractor = WagonNumberExtractor()
    val settingsRepository = UserSettingsRepository(context)
    val appIconManager = AppIconManager(context)

    val repository = RailRepository(
        dao = database.wagonEntryDao(),
        ocrEngine = ocrEngine,
        extractor = extractor,
        photoStorage = photoStorage,
    )

    private companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE wagon_entries ADD COLUMN positionIndex INTEGER NOT NULL DEFAULT 0",
                )

                val cursor = db.query(
                    "SELECT id FROM wagon_entries ORDER BY createdAt ASC",
                )
                cursor.use {
                    var index = 0L
                    while (it.moveToNext()) {
                        val id = it.getLong(0)
                        db.execSQL(
                            "UPDATE wagon_entries SET positionIndex = ? WHERE id = ?",
                            arrayOf(index, id),
                        )
                        index += 1L
                    }
                }
            }
        }
    }
}
