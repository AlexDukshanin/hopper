package com.alex.hopper.data

import androidx.room.TypeConverter
import com.alex.hopper.settings.NewEntryPosition

class Converters {
    @TypeConverter
    fun fromCandidateNumbers(value: List<String>): String = value.joinToString(separator = "|")

    @TypeConverter
    fun toCandidateNumbers(value: String): List<String> =
        value.split('|').map(String::trim).filter(String::isNotEmpty)

    @TypeConverter
    fun fromNewEntryPosition(value: NewEntryPosition): String = value.name

    @TypeConverter
    fun toNewEntryPosition(value: String): NewEntryPosition =
        NewEntryPosition.entries.firstOrNull { it.name == value } ?: NewEntryPosition.Last
}
