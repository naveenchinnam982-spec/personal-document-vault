package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_items")
data class HiddenItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val itemType: String, // "PHOTO", "VIDEO", "DOCUMENT", "PDF", "SCAN"
    val encryptedPath: String,
    val category: String = "Confidential", // "Personal", "Financial", "Legal", "Medical", "Confidential", "Other"
    val albumId: Long? = null,
    val description: String = "",
    val tags: String = "", // Comma separated
    val isFavorite: Boolean = false,
    val fileSizeBytes: Long = 0L,
    val fileHash: String? = null,
    val ocrText: String = "",
    val expiryDate: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null
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
