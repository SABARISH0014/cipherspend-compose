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
            val customRules = db.customRuleDao().getAllRules()
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
            val candidateTransactions = mutableListOf<TransactionEntity>()
            var latestTimestamp = lastSyncTime

            cursor?.use { c ->
                val addressIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)
                val subIdIdx = c.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)

                while (c.moveToNext()) {
                    if (scanned >= 1000) break
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
                            customRules = customRules,
                            predictionService = predictionService
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
                                    candidateTransactions.add(tx)
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Error parsing message #$scanned: ${e.message}")
                    }

                    if (scanned % 50 == 0) {
                        _syncState.value = SyncProgress(
                            isSyncing = true,
                            totalScanned = scanned,
                            transactionsFound = candidateTransactions.size
                        )
                    }
                }
            }

            var insertedCount = 0
            if (candidateTransactions.isNotEmpty()) {
                // Chunk insertions to prevent SQLite bind variable limit exceptions
                candidateTransactions.chunked(100).forEach { batch ->
                    val insertedIds = db.transactionDao().insertAll(batch)
                    insertedCount += insertedIds.count { it != -1L }
                }
                Log.d(TAG, "Inbox sync finished: Scanned $scanned SMS, found ${candidateTransactions.size} transactions, inserted $insertedCount new rows.")
            } else {
                Log.d(TAG, "Inbox sync finished: Scanned $scanned SMS, found 0 new transactions.")
            }

            if (latestTimestamp > 0) {
                prefs.edit().putLong("last_sms_sync_timestamp", latestTimestamp).apply()
            }

            predictionService.checkSpendingLimits()

            _syncState.value = SyncProgress(
                isSyncing = false,
                totalScanned = scanned,
                transactionsFound = candidateTransactions.size
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
}
