package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_trash_items")
data class HiddenTrashItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalId: Long,
    val name: String,
    val itemType: String, // "PHOTO", "VIDEO", "DOCUMENT", "PDF", "SCAN"
    val encryptedPath: String,
    val metadataJson: String = "",
    val deletedAt: Long = System.currentTimeMillis()
)
