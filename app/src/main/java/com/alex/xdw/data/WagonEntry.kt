package com.alex.xdw.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wagon_entries")
data class WagonEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAt: Long,
    val imagePath: String,
    val primaryNumber: String?,
    val candidateNumbers: List<String>,
    val recognizedText: String,
    val note: String,
)
