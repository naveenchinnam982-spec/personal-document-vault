package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val description: String = "",
    val filePath: String,
    val fileType: String, // "PDF", "IMAGE", "SCAN"
    val tags: String = "", // Comma separated
    val expiryDate: Long? = null,
    val isFavorite: Boolean = false,
    val ocrText: String = "",
    val fileSizeBytes: Long = 0L,
    val fileHash: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val tagList: List<String>
        get() = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun daysUntilExpiry(): Long? {
        if (expiryDate == null) return null
        val diff = expiryDate - System.currentTimeMillis()
        return diff / (1000 * 60 * 60 * 24)
    }

    val isExpiringSoon: Boolean
        get() {
            val days = daysUntilExpiry() ?: return false
            return days in 0..30
        }

    val isExpired: Boolean
        get() {
            val days = daysUntilExpiry() ?: return false
            return days < 0
        }
}
