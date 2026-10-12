package com.example.cipherspend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import com.example.cipherspend.data.db.DatabaseProvider
import com.example.cipherspend.service.PredictionService
import com.example.cipherspend.service.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBackgroundReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        var subscriptionId = intent.getIntExtra("subscription", -1)
        if (subscriptionId == -1) subscriptionId = intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1)
        if (subscriptionId == -1) subscriptionId = intent.getIntExtra("sub_id", -1)

        val prefs = context.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE)
        val verifiedSubId = prefs.getInt("verified_sub_id", -1)

        // Drop SMS if arrived on unverified SIM slot
        if (verifiedSubId != -1 && subscriptionId != -1 && subscriptionId != verifiedSubId) {
            return
        }

        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DatabaseProvider.getDatabase(context)
                val customRules = db.customRuleDao().getAllRules()
                val predictionService = PredictionService.getInstance(context)

                pdus.forEach { pdu ->
                    val msg = SmsMessage.createFromPdu(pdu as ByteArray, format)
                    val sender = msg.originatingAddress ?: "Unknown"
                    val body = msg.messageBody ?: ""
                    val timestamp = msg.timestampMillis

                    val parsedTx = SmsParser.parseMessage(
                        sender = sender,
                        body = body,
                        timestamp = timestamp,
                        customRules = customRules,
                        predictionService = predictionService
                    )

                    if (parsedTx != null) {
                        val rowId = db.transactionDao().insert(parsedTx)
                        if (rowId != -1L) {
                            Log.d("SmsBackgroundReceiver", "Saved transaction: ₹${parsedTx.amount} to ${parsedTx.merchant}")
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