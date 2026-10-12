package com.example.cipherspend.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["hash"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hash: String,
    val sender: String,
    val body: String,
    val amount: Double,
    val category: String,
    val type: String = "UPI",
    val merchant: String = "Unknown",
    val timestamp: Long
)
