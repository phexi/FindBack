package com.itemfinder.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "location", defaultValue = "")
    val location: String = "",
    val itemPhotoPath: String,
    @ColumnInfo(name = "locationPhotoPath", defaultValue = "")
    val locationPhotoPaths: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
