package com.example.cipherspend

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.cipherspend.ui.components.CipherSnackbarHost
import com.example.cipherspend.ui.components.MainAppScaffold
import com.example.cipherspend.ui.screens.AppNotificationsScreen
import com.example.cipherspend.ui.screens.ManualEntryScreen
import com.example.cipherspend.ui.screens.MpinSetupScreen
import com.example.cipherspend.ui.screens.ProfileEditScreen
import com.example.cipherspend.ui.screens.ProfileSetupScreen
import com.example.cipherspend.ui.screens.StartupScreen
import com.example.cipherspend.ui.screens.TransactionDetailScreen
import com.example.cipherspend.ui.screens.UnlockScreen
import com.example.cipherspend.ui.theme.CipherSpendTheme
import com.example.cipherspend.ui.viewmodel.AuthViewModel
import com.example.cipherspend.ui.viewmodel.TransactionViewModel

class MainActivity : FragmentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val transactionViewModel: TransactionViewModel by viewModels()
    private var backgroundTimestamp: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            CipherSpendTheme {
                val navController = rememberNavController()

                val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
                val isConfigured by authViewModel.isConfigured.collectAsState()

                LaunchedEffect(isAuthenticated, isConfigured) {
                    if (isConfigured && !isAuthenticated) {
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        if (currentRoute != null && currentRoute != "startup" && currentRoute != "unlock" &&
                            currentRoute != "mpin_setup" && currentRoute != "profile_setup"
                        ) {
                            navController.navigate("unlock") {
                                popUpTo(0) { inclusive = false }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = "startup",
                        enterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300)
                            ) + fadeOut(animationSpec = tween(300))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300)
                            ) + fadeOut(animationSpec = tween(300))
                        }
                    ) {
                    // 1. Startup / Terminal Boot Screen
                    composable(
                        route = "startup",
                        exitTransition = {
                            fadeOut(animationSpec = tween(350))
                        }
                    ) {
                        StartupScreen(
                            authViewModel = authViewModel,
                            onNavigateToUnlock = {
                                navController.navigate("unlock") {
                                    popUpTo("startup") { inclusive = true }
                                }
                            },
                            onNavigateToProfileSetup = {
                                navController.navigate("profile_setup") {
                                    popUpTo("startup") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 2. MPIN Setup Screen (4-Digit MPIN & Biometrics Toggle)
                    composable("mpin_setup") {
                        MpinSetupScreen(
                            authViewModel = authViewModel,
                            onMpinConfigured = {
                                navController.navigate("main_scaffold") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    // 5. Profile Setup Screen (Name, Monthly Target Budget, Salary Cycle Date)
                    composable("profile_setup") {
                        ProfileSetupScreen(
                            transactionViewModel = transactionViewModel,
                            onProfileSaved = {
                                navController.navigate("mpin_setup")
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
                            onNavigateToProfileEdit = {
                                navController.navigate("profile_edit")
                            },
                            onNuclearWipeComplete = {
                                transactionViewModel.onDatabaseReset()
                                navController.navigate("profile_setup") {
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

                    // 11. Profile Edit Screen
                    composable("profile_edit") {
                        ProfileEditScreen(
                            authViewModel = authViewModel,
                            transactionViewModel = transactionViewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }

                // Global Glassmorphic In-App Snackbar Host
                CipherSnackbarHost()
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

    override fun onStop() {
        super.onStop()
        backgroundTimestamp = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundTimestamp > 0L) {
            val elapsed = System.currentTimeMillis() - backgroundTimestamp
            if (elapsed > 60_000L && authViewModel.isConfigured.value && authViewModel.isAuthenticated.value) {
                authViewModel.lockVault()
            }
        }
    }
}