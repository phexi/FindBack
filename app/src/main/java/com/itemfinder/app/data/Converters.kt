package com.itemfinder.app.data

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return JSONArray(value).toString()
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            val jsonArray = JSONArray(value)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            // Legacy format: single plain path
            if (value.isNotBlank()) listOf(value) else emptyList()
        }
    }
}
