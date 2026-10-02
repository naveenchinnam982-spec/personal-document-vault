package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_activity_logs")
data class SecurityActivityLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val action: String, // "VAULT_UNLOCKED", "HIDDEN_VAULT_UNLOCKED", "PIN_CHANGED", "FAILED_AUTH", "FILE_IMPORTED", "FILE_EXPORTED", "FILE_DELETED", "BACKUP_CREATED"
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
