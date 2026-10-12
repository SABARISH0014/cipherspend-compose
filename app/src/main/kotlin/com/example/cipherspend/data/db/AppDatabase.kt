package com.example.cipherspend.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.cipherspend.data.dao.CustomRuleDao
import com.example.cipherspend.data.dao.TransactionDao
import com.example.cipherspend.data.model.CustomRuleEntity
import com.example.cipherspend.data.model.TransactionEntity

@Database(
    entities = [TransactionEntity::class, CustomRuleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun customRuleDao(): CustomRuleDao
}
