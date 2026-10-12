package com.example.cipherspend

import android.app.Application
import com.example.cipherspend.data.db.DatabaseProvider
import com.example.cipherspend.service.AlertNotificationManager
import com.example.cipherspend.service.PredictionService

class CipherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
        AlertNotificationManager.initChannels(this)
        // Eagerly warmup AI & Database in background
        Thread {
            try {
                DatabaseProvider.getDatabase(this)
                PredictionService.getInstance(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
