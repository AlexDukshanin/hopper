package com.alex.hopper.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alex.hopper.settings.NewEntryPosition

@Entity(tableName = "collections")
data class WagonCollection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val createdAt: Long,
    val primaryDirectionLabel: String = DEFAULT_PRIMARY_DIRECTION_LABEL,
    val secondaryDirectionLabel: String = DEFAULT_SECONDARY_DIRECTION_LABEL,
    val isPrimaryDirectionOnTop: Boolean = true,
    val newEntryPosition: NewEntryPosition = NewEntryPosition.Last,
    val includeDirectionInCopy: Boolean = true,
) {
    val topDirectionLabel: String
        get() = if (isPrimaryDirectionOnTop) primaryDirectionLabel else secondaryDirectionLabel

    val bottomDirectionLabel: String
        get() = if (isPrimaryDirectionOnTop) secondaryDirectionLabel else primaryDirectionLabel

    companion object {
        const val DEFAULT_PRIMARY_DIRECTION_LABEL = "ЗАПАД"
        const val DEFAULT_SECONDARY_DIRECTION_LABEL = "ВОСТОК"
    }
}

data class CollectionSummary(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val entryCount: Int,
)
