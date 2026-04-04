package com.alex.xdw.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromCandidateNumbers(value: List<String>): String = value.joinToString(separator = "|")

    @TypeConverter
    fun toCandidateNumbers(value: String): List<String> =
        value.split('|').map(String::trim).filter(String::isNotEmpty)
}
