package com.example.cipherspend

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import com.example.cipherspend.ui.components.MainAppScaffold
import com.example.cipherspend.ui.screens.SetupScreen
import com.example.cipherspend.ui.screens.UnlockScreen
import com.example.cipherspend.ui.theme.CipherSpendTheme
import com.example.cipherspend.ui.viewmodel.AuthViewModel
import com.example.cipherspend.ui.viewmodel.TransactionViewModel

class MainActivity : FragmentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val transactionViewModel: TransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CipherSpendTheme {
                val isConfigured by authViewModel.isConfigured.collectAsState()
                val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
                val isBiometricEnabled by authViewModel.isBiometricEnabled.collectAsState()

                when {
                    !isConfigured -> {
                        SetupScreen(
                            onSetupComplete = { mpin, enableBio ->
                                val success = authViewModel.setupMpinAndBiometric(mpin, enableBio)
                                if (success) {
                                    Toast.makeText(this@MainActivity, "Vault configured successfully", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    !isAuthenticated -> {
                        UnlockScreen(
                            onUnlockWithPin = { pin ->
                                authViewModel.verifyMpin(pin)
                            },
                            onTriggerBiometric = {
                                launchBiometricPrompt()
                            },
                            isBiometricAvailable = isBiometricEnabled
                        )
                    }

                    else -> {
                        MainAppScaffold(
                            authViewModel = authViewModel,
                            transactionViewModel = transactionViewModel,
                            onLockVault = {
                                authViewModel.lockVault()
                            }
                        )
                    }
                }
            }
        }

        // Auto trigger biometric prompt if configured and enabled
        if (authViewModel.isConfigured.value && authViewModel.isBiometricEnabled.value && !authViewModel.isAuthenticated.value) {
            launchBiometricPrompt()
        }
    }

    private fun launchBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    authViewModel.onBiometricSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Fallback to MPIN silently
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock CipherSpend Vault")
            .setSubtitle("Biometric zero-knowledge verification")
            .setNegativeButtonText("Use MPIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}