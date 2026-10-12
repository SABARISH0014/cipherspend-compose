package com.example.cipherspend.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cipherspend.data.model.CustomRuleEntity

@Dao
interface CustomRuleDao {
    @Query("SELECT * FROM custom_rules WHERE sender = :sender")
    suspend fun getRulesForSender(sender: String): List<CustomRuleEntity>

    @Query("SELECT * FROM custom_rules")
    suspend fun getAllRules(): List<CustomRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CustomRuleEntity): Long
}
