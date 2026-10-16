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

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

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

    @Query("UPDATE transactions SET merchant = :merchant WHERE body = :body AND sender = :sender")
    suspend fun updateMerchantByBodyAndSender(body: String, sender: String, merchant: String): Int

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("DELETE FROM transactions WHERE sender = 'DEMO_MODE' OR hash LIKE 'DEMO_%'")
    suspend fun deleteDemoTransactions()

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp AND category != 'Refund'")
    suspend fun getMonthlySpend(startTimestamp: Long, endTimestamp: Long): Double?

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp AND category != 'Refund' GROUP BY category ORDER BY total DESC")
    suspend fun getCategorySpends(startTimestamp: Long, endTimestamp: Long): List<CategorySpend>

    @Query("UPDATE transactions SET merchant = :newMerchant WHERE LOWER(merchant) = LOWER(:oldMerchant)")
    suspend fun updateMerchantGlobally(oldMerchant: String, newMerchant: String): Int

    @Query("""
        SELECT * FROM transactions 
        WHERE body LIKE '%' || :query || '%' 
           OR merchant LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%' 
           OR sender LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE body = :body AND abs(timestamp - :timestamp) <= :windowMs)")
    suspend fun hasExactBodyMatch(body: String, timestamp: Long, windowMs: Long = 259_200_000L): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE amount = :amount AND merchant = :merchant AND merchant != 'Unknown' AND merchant != 'Unknown Merchant' AND abs(timestamp - :timestamp) <= :windowMs)")
    suspend fun hasMerchantAmountMatch(amount: Double, merchant: String, timestamp: Long, windowMs: Long = 259_200_000L): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE amount = :amount AND abs(timestamp - :timestamp) <= :windowMs)")
    suspend fun hasFuzzyAmountMatch(amount: Double, timestamp: Long, windowMs: Long = 180_000L): Boolean

    @Query("SELECT * FROM transactions WHERE category = :category")
    suspend fun getTransactionsByCategory(category: String): List<TransactionEntity>

    @Query("UPDATE transactions SET category = :category, type = :type WHERE hash = :hash")
    suspend fun updateCategoryAndTypeByHash(hash: String, category: String, type: String): Int

    suspend fun existsSimilarTransaction(
        body: String,
        amount: Double,
        merchant: String,
        timestamp: Long
    ): Boolean {
        if (hasExactBodyMatch(body, timestamp)) return true
        if (merchant.isNotBlank() && merchant != "Unknown" && merchant != "Unknown Merchant") {
            if (hasMerchantAmountMatch(amount, merchant, timestamp)) return true
        }
        if (hasFuzzyAmountMatch(amount, timestamp)) return true
        return false
    }
}

