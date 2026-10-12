package com.example.cipherspend.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.cipherspend.MainActivity
import java.util.Locale

object AlertNotificationManager {
    const val CHANNEL_ID = "cipherspend_spending_alerts"
    private const val CHANNEL_NAME = "Spending Limit Alerts"
    private const val CHANNEL_DESC = "Proactive alerts when spending approaches 80% or 100% of monthly budget"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendSpendingAlert(context: Context, percent: Int, spent: Double, budget: Double) {
        val title = if (percent >= 100) {
            "Budget Alert: 100% Limit Reached!"
        } else {
            "Spending Warning: $percent% of Budget Used"
        }

        val content = String.format(
            Locale.getDefault(),
            "You have spent ₹%.2f of your ₹%.2f monthly budget.",
            spent,
            budget
        )

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            percent,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(if (percent >= 100) 100 else 80, notification)
    }
}
