package com.example.cipherspend

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.cipherspend.data.db.DatabaseProvider
import com.example.cipherspend.service.PredictionService
import com.example.cipherspend.service.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationService : NotificationListenerService() {

    private val upiApps = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "GPay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "in.org.npci.upiapp" to "BHIM",
        "com.sbi.SBIFreedomPlus" to "SBI YONO",
        "com.snapwork.hdfc" to "HDFC Bank",
        "com.csam.icici.bank.imobile" to "ICICI iMobile",
        "com.dreamplug.androidapp" to "Cred"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName
        if (!upiApps.containsKey(packageName)) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        var text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        if (bigText.isNotEmpty() && bigText.length > text.length) {
            text = bigText
        }

        val senderName = upiApps[packageName] ?: "UPI"
        val fullBody = "$title $text".trim()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DatabaseProvider.getDatabase(applicationContext)
                val predictionService = PredictionService.getInstance(applicationContext)

                val prefs = applicationContext.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE)
                val autoDropDuplicates = prefs.getString("dedupe_rule", "") == "auto_drop" || prefs.getBoolean("auto_drop_duplicates", false)

                val overridePrefs = applicationContext.getSharedPreferences("cipher_merchant_overrides", Context.MODE_PRIVATE)
                val merchantOverrides = overridePrefs.all.mapNotNull { (k, v) ->
                    if (v is String) k to v else null
                }.toMap()

                val parsedTx = SmsParser.parseMessage(
                    sender = senderName,
                    body = fullBody,
                    timestamp = sbn.postTime,
                    predictionService = predictionService,
                    merchantOverrides = merchantOverrides
                )

                if (parsedTx != null) {
                    if (db.ignoredHashDao().isHashIgnored(parsedTx.hash)) {
                        return@launch
                    }
                    if (autoDropDuplicates && db.transactionDao().existsSimilarTransaction(
                        body = parsedTx.body,
                        amount = parsedTx.amount,
                        merchant = parsedTx.merchant,
                        timestamp = parsedTx.timestamp
                    )) {
                        return@launch
                    }
                    val rowId = db.transactionDao().insert(parsedTx)
                    if (rowId != -1L) {
                        Log.d("NotificationService", "Saved notification transaction: ₹${parsedTx.amount} to ${parsedTx.merchant}")
                        db.transactionDao().deleteDemoTransactions()
                        prefs.edit().putBoolean("is_demo_mode", false).apply()
                        predictionService.checkSpendingLimits()
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationService", "Error parsing notification: ${e.message}", e)
            }
        }
    }
}