package com.alex.hopper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query(
        """
        SELECT
            c.id AS id,
            c.name AS name,
            c.description AS description,
            c.createdAt AS createdAt,
            COUNT(e.id) AS entryCount
        FROM collections c
        LEFT JOIN wagon_entries e ON e.collectionId = c.id
        GROUP BY c.id
        ORDER BY c.createdAt DESC, c.id DESC
        """,
    )
    fun observeCollections(): Flow<List<CollectionSummary>>

    @Query("SELECT * FROM collections WHERE id = :id LIMIT 1")
    fun observeCollection(id: Long): Flow<WagonCollection?>

    @Query("SELECT * FROM collections WHERE id = :id LIMIT 1")
    suspend fun getCollectionById(id: Long): WagonCollection?

    @Query("SELECT * FROM collections ORDER BY createdAt DESC, id DESC")
    suspend fun getCollections(): List<WagonCollection>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: WagonCollection): Long

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun updateName(id: Long, name: String)

    @Query("UPDATE collections SET description = :description WHERE id = :id")
    suspend fun updateDescription(id: Long, description: String)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteById(id: Long)
}
