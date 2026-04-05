package com.alex.hopper.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wagon_entries",
    indices = [Index("collectionId")],
)
data class WagonEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val collectionId: Long,
    val positionIndex: Long,
    val createdAt: Long,
    val imagePath: String,
    val primaryNumber: String?,
    val candidateNumbers: List<String>,
    val recognizedText: String,
    val note: String,
)
