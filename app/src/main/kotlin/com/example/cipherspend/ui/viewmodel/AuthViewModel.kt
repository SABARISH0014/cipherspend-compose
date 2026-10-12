package com.example.cipherspend.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("cipher_auth_prefs", Context.MODE_PRIVATE)

    private val _isConfigured = MutableStateFlow(false)
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isTotpEnabled = MutableStateFlow(false)
    val isTotpEnabled: StateFlow<Boolean> = _isTotpEnabled.asStateFlow()

    private val _totpSecret = MutableStateFlow<String?>(null)
    val totpSecret: StateFlow<String?> = _totpSecret.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val hasMpin = prefs.contains("user_mpin_hash")
        val bio = prefs.getBoolean("biometric_enabled", false)
        val totp = prefs.getBoolean("totp_enabled", false)
        val secret = prefs.getString("totp_secret", null)

        _isConfigured.value = hasMpin
        _isBiometricEnabled.value = bio
        _isTotpEnabled.value = totp
        _totpSecret.value = secret
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

    fun generateNewTotpSecret(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val random = SecureRandom()
        val secret = (1..16).map { allowedChars[random.nextInt(allowedChars.length)] }.joinToString("")
        _totpSecret.value = secret
        return secret
    }

    fun verifyAndEnableTotp(secret: String, enteredCode: String): Boolean {
        val valid = verifyTotpCode(secret, enteredCode)
        if (valid) {
            prefs.edit()
                .putString("totp_secret", secret)
                .putBoolean("totp_enabled", true)
                .apply()
            _isTotpEnabled.value = true
            _totpSecret.value = secret
        }
        return valid
    }

    fun disableTotp() {
        prefs.edit()
            .remove("totp_secret")
            .putBoolean("totp_enabled", false)
            .apply()
        _isTotpEnabled.value = false
        _totpSecret.value = null
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun lockVault() {
        _isAuthenticated.value = false
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun verifyTotpCode(secret: String, code: String): Boolean {
        if (code.length != 6) return false
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val timeStep = 30L

        // Check current, previous, and next window to accommodate clock drift
        for (i in -1..1) {
            val step = (currentTimeSeconds / timeStep) + i
            if (generateTotpForStep(secret, step) == code) {
                return true
            }
        }
        return false
    }

    private fun generateTotpForStep(base32Secret: String, step: Long): String {
        val key = decodeBase32(base32Secret)
        val data = ByteBuffer.allocate(8).putLong(step).array()
        val mac = Mac.getInstance("HmacSHA1").apply {
            init(SecretKeySpec(key, "HmacSHA1"))
        }
        val hash = mac.doFinal(data)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)
        val otp = binary % (10.0.pow(6).toInt())
        return "%06d".format(otp)
    }

    private fun decodeBase32(secret: String): ByteArray {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleanSecret = secret.trim().uppercase().replace("=", "")
        var buffer = 0
        var bitsLeft = 0
        val result = mutableListOf<Byte>()

        for (c in cleanSecret) {
            val valIndex = base32Chars.indexOf(c)
            if (valIndex < 0) continue
            buffer = (buffer shl 5) or valIndex
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                result.add((buffer shr bitsLeft).toByte())
            }
        }
        return result.toByteArray()
    }
}
