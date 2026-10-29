package com.example.cipherspend.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {
    var isDark by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE)
        isDark = prefs.getBoolean("pref_dark_mode", true)
    }

    fun setDarkMode(context: Context, dark: Boolean) {
        isDark = dark
        val prefs = context.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("pref_dark_mode", dark).apply()
    }

    fun toggle(context: Context) {
        setDarkMode(context, !isDark)
    }
}

