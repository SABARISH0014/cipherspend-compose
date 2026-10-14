package com.example.cipherspend.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.cipherspend.data.dao.AppNotificationDao
import com.example.cipherspend.data.dao.IgnoredHashDao
import com.example.cipherspend.data.dao.TransactionDao
import com.example.cipherspend.data.model.AppNotificationEntity
import com.example.cipherspend.data.model.IgnoredHashEntity
import com.example.cipherspend.data.model.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        AppNotificationEntity::class,
        IgnoredHashEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun appNotificationDao(): AppNotificationDao
    abstract fun ignoredHashDao(): IgnoredHashDao
}
