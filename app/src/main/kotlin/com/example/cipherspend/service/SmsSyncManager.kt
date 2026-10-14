package com.example.cipherspend.service

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.example.cipherspend.data.db.DatabaseProvider
import com.example.cipherspend.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.util.concurrent.atomic.AtomicBoolean

data class SyncProgress(
    val isSyncing: Boolean = false,
    val totalScanned: Int = 0,
    val transactionsFound: Int = 0,
    val error: String? = null
)

object SmsSyncManager {
    private const val TAG = "SmsSyncManager"
    private val isSyncRunning = AtomicBoolean(false)

    private val _syncState = mutableStateOf(SyncProgress())
    val syncState: State<SyncProgress> = _syncState

    suspend fun syncInbox(context: Context, fullRescan: Boolean = false): Int = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_SMS permission not granted, skipping inbox sync.")
            return@withContext 0
        }

        // Concurrency guard: Prevent parallel duplicate syncs
        if (!isSyncRunning.compareAndSet(false, true)) {
            Log.d(TAG, "Sync is already running, skipping parallel execution.")
            return@withContext 0
        }

        _syncState.value = SyncProgress(isSyncing = true)

        try {
            val db = DatabaseProvider.getDatabase(context)
            val predictionService = PredictionService.getInstance(context)

            val prefs = context.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE)
            val autoDropDuplicates = prefs.getString("dedupe_rule", "") == "auto_drop" || prefs.getBoolean("auto_drop_duplicates", false)
            val verifiedSubId = prefs.getInt("verified_sub_id", -1)
            val currentTxCount = db.transactionDao().getTransactionCount()
            val lastSyncTime = if (fullRescan || currentTxCount == 0) 0L else prefs.getLong("last_sms_sync_timestamp", 0L)

            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.SUBSCRIPTION_ID
            )

            val selection = if (lastSyncTime > 0) "${Telephony.Sms.DATE} > ?" else null
            val selectionArgs = if (lastSyncTime > 0) arrayOf(lastSyncTime.toString()) else null
            val sortOrder = "${Telephony.Sms.DATE} DESC"

            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            var scanned = 0
            var totalFound = 0
            var insertedCount = 0
            val batchBuffer = mutableListOf<TransactionEntity>()
            var latestTimestamp = lastSyncTime

            // Self-healing: Reclassify any existing transactions falsely marked as "Cash"
            reclassifyLegacyCashTransactions(context)

            val overridePrefs = context.getSharedPreferences("cipher_merchant_overrides", Context.MODE_PRIVATE)
            val merchantOverrides = overridePrefs.all.mapNotNull { (k, v) ->
                if (v is String) k to v else null
            }.toMap()

            cursor?.use { c ->
                val addressIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)
                val subIdIdx = c.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)

                while (c.moveToNext()) {
                    scanned++
                    val sender = if (addressIdx != -1) c.getString(addressIdx) ?: "Unknown" else "Unknown"
                    val body = if (bodyIdx != -1) c.getString(bodyIdx) ?: "" else ""
                    val timestamp = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()
                    val subId = if (subIdIdx != -1) c.getInt(subIdIdx) else -1

                    if (timestamp > latestTimestamp) {
                        latestTimestamp = timestamp
                    }

                    // Multi-SIM filter if specified
                    if (verifiedSubId != -1 && subId != -1 && subId != verifiedSubId) {
                        continue
                    }

                    // Parse bank/UPI SMS with per-message fault isolation
                    try {
                        val tx = SmsParser.parseMessage(
                            sender = sender,
                            body = body,
                            timestamp = timestamp,
                            predictionService = predictionService,
                            merchantOverrides = merchantOverrides
                        )

                        if (tx != null) {
                            if (!db.ignoredHashDao().isHashIgnored(tx.hash)) {
                                val isDup = autoDropDuplicates && db.transactionDao().existsSimilarTransaction(
                                    body = tx.body,
                                    amount = tx.amount,
                                    merchant = tx.merchant,
                                    timestamp = tx.timestamp
                                )
                                if (!isDup) {
                                    batchBuffer.add(tx)
                                    totalFound++

                                    if (fullRescan) {
                                        db.transactionDao().updateCategoryAndTypeByHash(tx.hash, tx.category, tx.type)
                                    }

                                    // Batch insert into Room in chunks of 50 to maintain constant memory and update UI progressively
                                    if (batchBuffer.size >= 50) {
                                        val insertedIds = db.transactionDao().insertAll(batchBuffer)
                                        insertedCount += insertedIds.count { it != -1L }
                                        batchBuffer.clear()
                                    }
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Error parsing message #$scanned: ${e.message}")
                    }

                    if (scanned % 25 == 0) {
                        _syncState.value = SyncProgress(
                            isSyncing = true,
                            totalScanned = scanned,
                            transactionsFound = totalFound
                        )
                    }
                }
            }

            // Flush remaining transactions in buffer
            if (batchBuffer.isNotEmpty()) {
                val insertedIds = db.transactionDao().insertAll(batchBuffer)
                insertedCount += insertedIds.count { it != -1L }
                batchBuffer.clear()
            }

            // Automatically clear Demo Mode transactions when real SMS transactions are imported
            if (insertedCount > 0) {
                db.transactionDao().deleteDemoTransactions()
                prefs.edit().putBoolean("is_demo_mode", false).apply()
            }

            Log.d(TAG, "Inbox sync finished: Scanned $scanned SMS, found $totalFound transactions, inserted $insertedCount new rows.")

            if (latestTimestamp > 0) {
                prefs.edit().putLong("last_sms_sync_timestamp", latestTimestamp).apply()
            }

            predictionService.checkSpendingLimits()

            _syncState.value = SyncProgress(
                isSyncing = false,
                totalScanned = scanned,
                transactionsFound = totalFound
            )

            return@withContext insertedCount
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync SMS inbox: ${e.message}", e)
            _syncState.value = SyncProgress(
                isSyncing = false,
                error = e.message
            )
            return@withContext 0
        } finally {
            isSyncRunning.set(false)
        }
    }

    /**
     * Self-healing migration for previously scanned messages that were erroneously
     * tagged as "Cash" category due to unanchored substring matching.
     */
    suspend fun reclassifyLegacyCashTransactions(context: Context) {
        try {
            val db = DatabaseProvider.getDatabase(context)
            val predictionService = PredictionService.getInstance(context)
            val cashTxList = db.transactionDao().getTransactionsByCategory("Cash")
            for (tx in cashTxList) {
                val lower = tx.body.lowercase()
                val isGenuineCash = (lower.contains("atm") ||
                        lower.contains("cash withdrawal") ||
                        lower.contains("withdrawn cash") ||
                        lower.contains("cash w/d") ||
                        Regex("""\b(withdrew|withdrawn)\s+(?:rs\.?|inr|₹)?\s*\d+""").containsMatchIn(lower)) &&
                        !lower.contains("cash limit") &&
                        !lower.contains("cashback") &&
                        !lower.contains("cash back") &&
                        !lower.contains("available cash")

                if (!isGenuineCash) {
                    val newCategory = predictionService.predictCategory(tx.body)
                    val newType = SmsParser.determineType(lower)
                    db.transactionDao().update(tx.copy(category = newCategory, type = newType))
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to reclassify legacy cash transactions: ${e.message}")
        }
    }
}
