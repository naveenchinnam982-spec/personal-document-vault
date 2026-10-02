package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_hidden_items")
data class HiddenHiddenItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val itemType: String, // "PHOTO" or "VIDEO"
    val encryptedPath: String,
    val fileSizeBytes: Long = 0L,
    val fileHash: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
