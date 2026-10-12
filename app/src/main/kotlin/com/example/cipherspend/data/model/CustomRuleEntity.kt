package com.example.cipherspend.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_rules")
data class CustomRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val regexPattern: String,
    val merchantName: String
)
