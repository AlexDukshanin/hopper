package com.alex.hopper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WagonEntryDao {
    @Query("SELECT * FROM wagon_entries ORDER BY positionIndex ASC")
    fun observeEntries(): Flow<List<WagonEntry>>

    @Query("SELECT * FROM wagon_entries ORDER BY positionIndex ASC")
    suspend fun getEntriesOrdered(): List<WagonEntry>

    @Query("SELECT * FROM wagon_entries WHERE id = :id LIMIT 1")
    fun observeEntry(id: Long): Flow<WagonEntry?>

    @Query("SELECT * FROM wagon_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): WagonEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WagonEntry): Long

    @Query("UPDATE wagon_entries SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)

    @Query("UPDATE wagon_entries SET primaryNumber = :number WHERE id = :id")
    suspend fun updatePrimaryNumber(id: Long, number: String?)

    @Query("UPDATE wagon_entries SET imagePath = :imagePath WHERE id = :id")
    suspend fun updateImagePath(id: Long, imagePath: String)

    @Query("UPDATE wagon_entries SET positionIndex = :positionIndex WHERE id = :id")
    suspend fun updatePositionIndex(id: Long, positionIndex: Long)

    @Query("SELECT MAX(positionIndex) FROM wagon_entries")
    suspend fun getMaxPositionIndex(): Long?

    @androidx.room.Transaction
    suspend fun replaceOrder(entries: List<WagonEntry>) {
        entries.forEachIndexed { index, entry ->
            updatePositionIndex(entry.id, index.toLong())
        }
    }

    @Query("DELETE FROM wagon_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
