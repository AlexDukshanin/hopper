package com.alex.hopper.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [WagonEntry::class, WagonCollection::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wagonEntryDao(): WagonEntryDao

    abstract fun collectionDao(): CollectionDao
}
