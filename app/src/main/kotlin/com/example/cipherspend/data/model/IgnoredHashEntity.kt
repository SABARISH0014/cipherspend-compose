package com.example.cipherspend.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ignored_hashes")
data class IgnoredHashEntity(
    @PrimaryKey
    val hash: String
)
