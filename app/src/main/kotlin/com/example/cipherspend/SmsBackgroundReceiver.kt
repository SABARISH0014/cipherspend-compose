package com.example.cipherspend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.cipherspend.data.db.DatabaseProvider
import com.example.cipherspend.service.PredictionService
import com.example.cipherspend.service.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBackgroundReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        var subscriptionId = intent.getIntExtra("subscription", -1)
        if (subscriptionId == -1) subscriptionId = intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1)
        if (subscriptionId == -1) subscriptionId = intent.getIntExtra("sub_id", -1)

        val prefs = context.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE)
        val verifiedSubId = prefs.getInt("verified_sub_id", -1)

        // Drop SMS if arrived on unverified SIM slot
        if (verifiedSubId != -1 && subscriptionId != -1 && subscriptionId != verifiedSubId) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DatabaseProvider.getDatabase(context)
                val predictionService = PredictionService.getInstance(context)

                val autoDropDuplicates = prefs.getString("dedupe_rule", "") == "auto_drop" || prefs.getBoolean("auto_drop_duplicates", false)

                val overridePrefs = context.getSharedPreferences("cipher_merchant_overrides", Context.MODE_PRIVATE)
                val merchantOverrides = overridePrefs.all.mapNotNull { (k, v) ->
                    if (v is String) k to v else null
                }.toMap()

                // Group multi-part SMS by sender to reconstruct full concatenated messages
                val messagesBySender = messages.groupBy { it.displayOriginatingAddress ?: it.originatingAddress ?: "Unknown" }

                for ((sender, parts) in messagesBySender) {
                    val fullBody = parts.joinToString("") { it.displayMessageBody ?: it.messageBody ?: "" }
                    val timestamp = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

                    val parsedTx = SmsParser.parseMessage(
                        sender = sender,
                        body = fullBody,
                        timestamp = timestamp,
                        predictionService = predictionService,
                        merchantOverrides = merchantOverrides
                    )

                    if (parsedTx != null) {
                        if (db.ignoredHashDao().isHashIgnored(parsedTx.hash)) {
                            continue
                        }
                        if (autoDropDuplicates && db.transactionDao().existsSimilarTransaction(
                            body = parsedTx.body,
                            amount = parsedTx.amount,
                            merchant = parsedTx.merchant,
                            timestamp = parsedTx.timestamp
                        )) {
                            continue
                        }
                        val rowId = db.transactionDao().insert(parsedTx)
                        if (rowId != -1L) {
                            Log.d("SmsBackgroundReceiver", "Saved transaction: ₹${parsedTx.amount} to ${parsedTx.merchant}")
                            db.transactionDao().deleteDemoTransactions()
                            prefs.edit().putBoolean("is_demo_mode", false).apply()
                            val lastSync = prefs.getLong("last_sms_sync_timestamp", 0L)
                            if (parsedTx.timestamp > lastSync) {
                                prefs.edit().putLong("last_sms_sync_timestamp", parsedTx.timestamp).apply()
                            }
                            predictionService.checkSpendingLimits()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsBackgroundReceiver", "Error processing SMS: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}