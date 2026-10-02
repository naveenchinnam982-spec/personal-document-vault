package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "private_media")
data class PrivateMedia(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val encryptedPath: String,
    val mediaType: String, // "PHOTO", "VIDEO"
    val albumId: Long? = null,
    val isFavorite: Boolean = false,
    val fileSizeBytes: Long = 0L,
    val fileHash: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
