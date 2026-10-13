package com.example.cipherspend

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cipherspend.ui.components.MainAppScaffold
import com.example.cipherspend.ui.screens.AppNotificationsScreen
import com.example.cipherspend.ui.screens.InteractiveTrainingScreen
import com.example.cipherspend.ui.screens.ManualEntryScreen
import com.example.cipherspend.ui.screens.MpinSetupScreen
import com.example.cipherspend.ui.screens.ProfileSetupScreen
import com.example.cipherspend.ui.screens.StartupScreen
import com.example.cipherspend.ui.screens.TransactionDetailScreen
import com.example.cipherspend.ui.screens.UnlockScreen
import com.example.cipherspend.ui.screens.VerificationScreen
import com.example.cipherspend.ui.screens.VerifySetupScreen
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
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "startup"
                ) {
                    // 1. Startup / Terminal Boot Screen
                    composable("startup") {
                        StartupScreen(
                            authViewModel = authViewModel,
                            onNavigateToUnlock = {
                                navController.navigate("unlock") {
                                    popUpTo("startup") { inclusive = true }
                                }
                            },
                            onNavigateToVerification = {
                                navController.navigate("verification") {
                                    popUpTo("startup") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 2. Verification Screen (Phone Number Collection & TOTP Secret Gen)
                    composable("verification") {
                        VerificationScreen(
                            authViewModel = authViewModel,
                            onNavigateToVerifySetup = {
                                navController.navigate("verify_setup")
                            }
                        )
                    }

                    // 3. Verify Setup Screen (TOTP Secret Display & 6-Digit Code Input)
                    composable("verify_setup") {
                        VerifySetupScreen(
                            authViewModel = authViewModel,
                            onVerificationSuccess = {
                                navController.navigate("mpin_setup")
                            }
                        )
                    }

                    // 4. MPIN Setup Screen (4-Digit MPIN & Biometrics Toggle)
                    composable("mpin_setup") {
                        MpinSetupScreen(
                            authViewModel = authViewModel,
                            onMpinConfigured = {
                                navController.navigate("profile_setup")
                            }
                        )
                    }

                    // 5. Profile Setup Screen (Name, Monthly Target Budget, Salary Cycle Date)
                    composable("profile_setup") {
                        ProfileSetupScreen(
                            transactionViewModel = transactionViewModel,
                            onProfileSaved = {
                                navController.navigate("main_scaffold") {
                                    popUpTo("verification") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 6. Unlock Screen (MPIN Login & Biometrics)
                    composable("unlock") {
                        val isBiometricEnabled = authViewModel.isBiometricEnabled.value
                        UnlockScreen(
                            onUnlockWithPin = { pin ->
                                val success = authViewModel.verifyMpin(pin)
                                if (success) {
                                    navController.navigate("main_scaffold") {
                                        popUpTo("unlock") { inclusive = true }
                                    }
                                }
                                success
                            },
                            onTriggerBiometric = {
                                launchBiometricPrompt {
                                    navController.navigate("main_scaffold") {
                                        popUpTo("unlock") { inclusive = true }
                                    }
                                }
                            },
                            isBiometricAvailable = isBiometricEnabled
                        )
                    }

                    // 7. Main App Scaffold (Nested Navigation for 4 bottom tabs)
                    composable("main_scaffold") {
                        MainAppScaffold(
                            authViewModel = authViewModel,
                            transactionViewModel = transactionViewModel,
                            onLockVault = {
                                authViewModel.lockVault()
                                navController.navigate("unlock") {
                                    popUpTo("main_scaffold") { inclusive = true }
                                }
                            },
                            onNavigateToManualEntry = {
                                navController.navigate("manual_entry")
                            },
                            onNavigateToDetail = { id ->
                                navController.navigate("detail/$id")
                            },
                            onNavigateToNotifications = {
                                navController.navigate("notifications")
                            },
                            onNuclearWipeComplete = {
                                navController.navigate("verification") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    // 8. Manual Entry Screen (Full Scaffold pushed over bottom bar)
                    composable("manual_entry") {
                        ManualEntryScreen(
                            viewModel = transactionViewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // 9. Interactive Training Screen (NER Regex Match Editor)
                    composable(
                        route = "interactive_training?body={body}&sender={sender}",
                        arguments = listOf(
                            navArgument("body") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("sender") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val body = backStackEntry.arguments?.getString("body") ?: ""
                        val sender = backStackEntry.arguments?.getString("sender") ?: "HDFC-BANK"
                        InteractiveTrainingScreen(
                            smsBody = body,
                            sender = sender,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // Pushed over bottom bar: Transaction Detail Screen
                    composable(
                        route = "detail/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val txId = backStackEntry.arguments?.getLong("id") ?: 0L
                        TransactionDetailScreen(
                            transactionId = txId,
                            viewModel = transactionViewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToTraining = { body, sender ->
                                val encodedBody = Uri.encode(body)
                                val encodedSender = Uri.encode(sender)
                                navController.navigate("interactive_training?body=$encodedBody&sender=$encodedSender")
                            }
                        )
                    }

                    // 10. App Notifications / Insights Screen
                    composable("notifications") {
                        AppNotificationsScreen(
                            viewModel = transactionViewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun launchBiometricPrompt(onSuccess: (() -> Unit)? = null) {
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
                    onSuccess?.invoke()
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