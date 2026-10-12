package com.example.cipherspend.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.cipherspend.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class CategorySpend(
    val category: String,
    val total: Double
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp ORDER BY timestamp DESC")
    fun getTransactionsInRangeFlow(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Query("SELECT * FROM transactions WHERE hash = :hash LIMIT 1")
    suspend fun getTransactionByHash(hash: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp")
    suspend fun getMonthlySpend(startTimestamp: Long, endTimestamp: Long): Double?

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp GROUP BY category ORDER BY total DESC")
    suspend fun getCategorySpends(startTimestamp: Long, endTimestamp: Long): List<CategorySpend>

    @Query("""
        SELECT * FROM transactions 
        WHERE body LIKE '%' || :query || '%' 
           OR merchant LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%' 
           OR sender LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>
}
