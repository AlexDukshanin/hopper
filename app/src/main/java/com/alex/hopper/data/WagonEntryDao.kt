package com.alex.hopper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WagonEntryDao {
    @Query("SELECT * FROM wagon_entries ORDER BY createdAt DESC")
    fun observeAllEntries(): Flow<List<WagonEntry>>

    @Query("SELECT * FROM wagon_entries WHERE collectionId = :collectionId ORDER BY positionIndex ASC")
    fun observeEntries(collectionId: Long): Flow<List<WagonEntry>>

    @Query("SELECT * FROM wagon_entries WHERE collectionId = :collectionId ORDER BY positionIndex ASC")
    suspend fun getEntriesOrdered(collectionId: Long): List<WagonEntry>

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

    @Query("UPDATE wagon_entries SET condition = :condition WHERE id = :id")
    suspend fun updateCondition(id: Long, condition: WagonCondition)

    @Query("UPDATE wagon_entries SET positionIndex = :positionIndex WHERE id = :id")
    suspend fun updatePositionIndex(id: Long, positionIndex: Long)

    @Query(
        """
        UPDATE wagon_entries
        SET positionIndex = positionIndex + 1
        WHERE collectionId = :collectionId AND positionIndex >= :fromIndex
        """,
    )
    suspend fun shiftPositionIndexes(
        collectionId: Long,
        fromIndex: Long,
    )

    @Query("SELECT MAX(positionIndex) FROM wagon_entries WHERE collectionId = :collectionId")
    suspend fun getMaxPositionIndex(collectionId: Long): Long?

    @Query("SELECT * FROM wagon_entries WHERE collectionId = :collectionId ORDER BY positionIndex ASC")
    suspend fun getEntriesByCollectionId(collectionId: Long): List<WagonEntry>

    @androidx.room.Transaction
    suspend fun replaceOrder(entries: List<WagonEntry>) {
        entries.forEachIndexed { index, entry ->
            updatePositionIndex(entry.id, index.toLong())
        }
    }

    @Query("DELETE FROM wagon_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM wagon_entries WHERE collectionId = :collectionId")
    suspend fun deleteByCollectionId(collectionId: Long)
}
