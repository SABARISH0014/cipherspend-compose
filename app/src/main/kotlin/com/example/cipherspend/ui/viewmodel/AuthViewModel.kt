package com.example.cipherspend.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.cipherspend.data.db.DatabaseProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("cipher_auth_prefs", Context.MODE_PRIVATE)

    private val _isConfigured = MutableStateFlow(false)
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _phoneNumber = MutableStateFlow<String>("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val hasMpin = prefs.contains("user_mpin_hash")
        val bio = prefs.getBoolean("biometric_enabled", false)
        val phone = prefs.getString("user_phone", "") ?: ""

        _isConfigured.value = hasMpin
        _isBiometricEnabled.value = bio
        _phoneNumber.value = phone
    }

    fun setPhoneNumber(phone: String) {
        _phoneNumber.value = phone
        prefs.edit().putString("user_phone", phone).apply()

        val appContext = getApplication<Application>().applicationContext
        val generalPrefs = appContext.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE)

        var matchedSubId = -1
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.READ_PHONE_STATE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val subManager = appContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
                val subList = subManager?.activeSubscriptionInfoList
                val cleanInput = phone.filter { it.isDigit() }.takeLast(10)

                if (!subList.isNullOrEmpty() && cleanInput.isNotEmpty()) {
                    for (info in subList) {
                        @Suppress("DEPRECATION")
                        val simNumber = info.number?.filter { it.isDigit() }?.takeLast(10)
                        if (simNumber != null && simNumber == cleanInput) {
                            matchedSubId = info.subscriptionId
                            break
                        }
                    }
                }
            } catch (_: SecurityException) {
                matchedSubId = -1
            }
        }

        generalPrefs.edit().putInt("verified_sub_id", matchedSubId).apply()
    }

    fun setupMpinAndBiometric(mpin: String, enableBiometric: Boolean): Boolean {
        if (mpin.length != 4 || !mpin.all { it.isDigit() }) return false

        val hash = hashString(mpin)
        prefs.edit()
            .putString("user_mpin_hash", hash)
            .putBoolean("biometric_enabled", enableBiometric)
            .apply()

        _isConfigured.value = true
        _isBiometricEnabled.value = enableBiometric
        _isAuthenticated.value = true
        return true
    }

    fun verifyMpin(mpin: String): Boolean {
        val storedHash = prefs.getString("user_mpin_hash", null) ?: return false
        val matches = hashString(mpin) == storedHash
        if (matches) {
            _isAuthenticated.value = true
        }
        return matches
    }

    fun onBiometricSuccess() {
        _isAuthenticated.value = true
    }

    fun changeMpin(newMpin: String): Boolean {
        if (newMpin.length != 4 || !newMpin.all { it.isDigit() }) return false
        val hash = hashString(newMpin)
        prefs.edit().putString("user_mpin_hash", hash).apply()
        return true
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun lockVault() {
        _isAuthenticated.value = false
    }

    fun nuclearWipe() {
        val appContext = getApplication<Application>().applicationContext

        // 1. Completely delete Room database file and encrypted vault keys
        DatabaseProvider.nuclearWipe(appContext)

        // 2. Clear Auth SharedPreferences (MPIN, Phone, Biometrics)
        prefs.edit().clear().commit()

        // 3. Clear General SharedPreferences (Name, Budget, Cycle, Alerts, Demo mode)
        val generalPrefs = appContext.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE)
        generalPrefs.edit().clear().commit()

        // 4. Reset in-memory StateFlows
        _isConfigured.value = false
        _isAuthenticated.value = false
        _isBiometricEnabled.value = false
        _phoneNumber.value = ""
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
