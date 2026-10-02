package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_items")
data class TrashItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalId: Long,
    val name: String,
    val itemType: String, // "DOCUMENT" or "PRIVATE_MEDIA"
    val filePath: String,
    val metadataJson: String = "",
    val deletedAt: Long = System.currentTimeMillis()
)
