package com.example.cipherspend.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.cipherspend.data.dao.AppNotificationDao
import com.example.cipherspend.data.dao.CustomRuleDao
import com.example.cipherspend.data.dao.TransactionDao
import com.example.cipherspend.data.model.AppNotificationEntity
import com.example.cipherspend.data.model.CustomRuleEntity
import com.example.cipherspend.data.model.TransactionEntity

@Database(
    entities = [TransactionEntity::class, CustomRuleEntity::class, AppNotificationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun customRuleDao(): CustomRuleDao
    abstract fun appNotificationDao(): AppNotificationDao
}
