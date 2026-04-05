package com.alex.hopper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class WagonCollection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val createdAt: Long,
)

data class CollectionSummary(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val entryCount: Int,
)
