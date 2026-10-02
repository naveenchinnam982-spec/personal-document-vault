package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_albums")
data class HiddenAlbum(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "Personal", // "Personal", "Family", "Important", "Travel", "Certificates", "Custom"
    val createdAt: Long = System.currentTimeMillis()
)
