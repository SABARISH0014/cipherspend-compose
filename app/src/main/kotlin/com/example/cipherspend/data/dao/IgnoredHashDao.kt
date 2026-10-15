package com.example.cipherspend.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cipherspend.data.model.IgnoredHashEntity

@Dao
interface IgnoredHashDao {
    @Query("SELECT hash FROM ignored_hashes")
    suspend fun getAllIgnoredHashes(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM ignored_hashes WHERE hash = :hash)")
    suspend fun isHashIgnored(hash: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoredHash(entity: IgnoredHashEntity)

    @Query("DELETE FROM ignored_hashes WHERE hash = :hash")
    suspend fun removeIgnoredHash(hash: String)
}
