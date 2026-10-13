package com.example.cipherspend.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_notifications")
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String,
    val timestamp: Long,
    val severity: String // "CRITICAL", "WARNING", "INFO"
)
