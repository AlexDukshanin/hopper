package com.alex.hopper

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alex.hopper.data.AppDatabase
import com.alex.hopper.data.RailRepository
import com.alex.hopper.exchange.CollectionExchangeManager
import com.alex.hopper.ocr.OcrEngine
import com.alex.hopper.ocr.WagonNumberExtractor
import com.alex.hopper.settings.AppIconManager
import com.alex.hopper.settings.UserSettingsRepository
import com.alex.hopper.storage.PhotoStorage

class XdwApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "xdw.db",
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()

    private val photoStorage = PhotoStorage(context)
    private val ocrEngine = OcrEngine(context)
    private val extractor = WagonNumberExtractor()
    val settingsRepository = UserSettingsRepository(context)
    val appIconManager = AppIconManager(context)

    val repository = RailRepository(
        wagonEntryDao = database.wagonEntryDao(),
        collectionDao = database.collectionDao(),
        ocrEngine = ocrEngine,
        extractor = extractor,
        photoStorage = photoStorage,
    )
    val collectionExchangeManager = CollectionExchangeManager(
        context = context,
        repository = repository,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS collections (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "ALTER TABLE wagon_entries ADD COLUMN collectionId INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_wagon_entries_collectionId ON wagon_entries(collectionId)",
                )

                val countCursor = db.query("SELECT COUNT(*) FROM wagon_entries")
                val hasEntries = countCursor.use {
                    it.moveToFirst()
                    it.getLong(0) > 0L
                }

                if (hasEntries) {
                    db.execSQL(
                        "INSERT INTO collections (id, name, description, createdAt) VALUES (?, ?, ?, ?)",
                        arrayOf<Any>(1L, "Подборка 1", "", System.currentTimeMillis()),
                    )
                    db.execSQL(
                        "UPDATE wagon_entries SET collectionId = 1 WHERE collectionId = 0",
                    )
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE wagon_entries ADD COLUMN isLoaded INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE collections
                    ADD COLUMN primaryDirectionLabel TEXT NOT NULL DEFAULT 'ЗАПАД'
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    ALTER TABLE collections
                    ADD COLUMN secondaryDirectionLabel TEXT NOT NULL DEFAULT 'ВОСТОК'
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    ALTER TABLE collections
                    ADD COLUMN isPrimaryDirectionOnTop INTEGER NOT NULL DEFAULT 1
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    ALTER TABLE collections
                    ADD COLUMN newEntryPosition TEXT NOT NULL DEFAULT 'Last'
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    ALTER TABLE collections
                    ADD COLUMN includeDirectionInCopy INTEGER NOT NULL DEFAULT 1
                    """.trimIndent(),
                )
            }
        }
    }
}
