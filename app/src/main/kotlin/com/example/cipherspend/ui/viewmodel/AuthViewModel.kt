package com.example.cipherspend.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.cipherspend.data.db.DatabaseProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

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

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _lockoutSecondsRemaining = MutableStateFlow(0)
    val lockoutSecondsRemaining: StateFlow<Int> = _lockoutSecondsRemaining.asStateFlow()

    private var lockoutJob: Job? = null

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

    private fun getOrCreateSalt(): String {
        var salt = prefs.getString("user_mpin_salt", null)
        if (salt == null) {
            val randomBytes = ByteArray(16)
            SecureRandom().nextBytes(randomBytes)
            salt = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
            prefs.edit().putString("user_mpin_salt", salt).apply()
        }
        return salt
    }

    private fun hashWithSalt(input: String, salt: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("$salt$input".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun setupMpinAndBiometric(mpin: String, enableBiometric: Boolean): Boolean {
        if (mpin.length != 4 || !mpin.all { it.isDigit() }) return false

        val salt = getOrCreateSalt()
        val hash = hashWithSalt(mpin, salt)
        prefs.edit()
            .putString("user_mpin_hash", hash)
            .putString("user_mpin_salt", salt)
            .putBoolean("biometric_enabled", enableBiometric)
            .apply()

        _isConfigured.value = true
        _isBiometricEnabled.value = enableBiometric
        _isAuthenticated.value = true
        _failedAttempts.value = 0
        _lockoutSecondsRemaining.value = 0
        lockoutJob?.cancel()
        return true
    }

    fun verifyMpin(mpin: String): Boolean {
        if (_lockoutSecondsRemaining.value > 0) return false

        val storedHash = prefs.getString("user_mpin_hash", null) ?: return false
        val salt = prefs.getString("user_mpin_salt", null)

        val matches = if (salt != null) {
            hashWithSalt(mpin, salt) == storedHash
        } else {
            // Check legacy unsalted hash for backwards compatibility
            val legacyMatch = hashString(mpin) == storedHash
            if (legacyMatch) {
                // Transparently migrate to salted hash
                val newSalt = getOrCreateSalt()
                val newHash = hashWithSalt(mpin, newSalt)
                prefs.edit().putString("user_mpin_hash", newHash).apply()
            }
            legacyMatch
        }

        if (matches) {
            _isAuthenticated.value = true
            _failedAttempts.value = 0
            _lockoutSecondsRemaining.value = 0
            lockoutJob?.cancel()
        } else {
            val attempts = _failedAttempts.value + 1
            _failedAttempts.value = attempts
            if (attempts >= 5) {
                startLockout(30)
            }
        }
        return matches
    }

    private fun startLockout(seconds: Int) {
        lockoutJob?.cancel()
        _lockoutSecondsRemaining.value = seconds
        lockoutJob = viewModelScope.launch {
            for (sec in seconds downTo 1) {
                _lockoutSecondsRemaining.value = sec
                delay(1000L)
            }
            _lockoutSecondsRemaining.value = 0
            _failedAttempts.value = 0
        }
    }

    fun onBiometricSuccess() {
        _isAuthenticated.value = true
        _failedAttempts.value = 0
        _lockoutSecondsRemaining.value = 0
        lockoutJob?.cancel()
    }

    fun changeMpin(newMpin: String): Boolean {
        if (newMpin.length != 4 || !newMpin.all { it.isDigit() }) return false
        val salt = getOrCreateSalt()
        val hash = hashWithSalt(newMpin, salt)
        prefs.edit()
            .putString("user_mpin_hash", hash)
            .putString("user_mpin_salt", salt)
            .apply()
        _failedAttempts.value = 0
        _lockoutSecondsRemaining.value = 0
        lockoutJob?.cancel()
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

        // 4. Reset in-memory StateFlows and cancel lockouts
        lockoutJob?.cancel()
        _isConfigured.value = false
        _isAuthenticated.value = false
        _isBiometricEnabled.value = false
        _phoneNumber.value = ""
        _failedAttempts.value = 0
        _lockoutSecondsRemaining.value = 0
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
