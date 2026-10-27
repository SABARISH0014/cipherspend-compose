package com.example.cipherspend.ui.screens

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.example.cipherspend.NotificationService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.cipherspend.ui.components.CipherSnackbarManager
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.core.app.NotificationManagerCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipherspend.ui.components.HighRiskGuardDialog
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DangerRed
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.PurpleGlow
import com.example.cipherspend.ui.theme.SurfaceDark
import com.example.cipherspend.ui.theme.TextMuted
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.TextSecondary
import com.example.cipherspend.ui.theme.glassEffect
import com.example.cipherspend.ui.viewmodel.AuthViewModel
import com.example.cipherspend.ui.viewmodel.TransactionViewModel

private const val PRIVACY_POLICY_URL = "https://sabarish0014.github.io/cipherspend-compose/"

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    transactionViewModel: TransactionViewModel,
    onLockVault: () -> Unit,
    onNuclearWipeComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current

    val prefs = remember { context.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE) }

    val isBiometricEnabled by authViewModel.isBiometricEnabled.collectAsState()
    val forecast by transactionViewModel.forecast.collectAsState()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showPurgeDialog by remember { mutableStateOf(false) }
    var showMpinGuardForPurge by remember { mutableStateOf(false) }

    var budgetInput by remember { mutableStateOf((forecast?.budget ?: 25000.0).toInt().toString()) }

    var autoDropDuplicates by remember {
        mutableStateOf(
            prefs.getString("dedupe_rule", "") == "auto_drop" || prefs.getBoolean("auto_drop_duplicates", false)
        )
    }

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var isIgnoringBattery by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true)
    }

    fun checkNotifListener(): Boolean {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val cn = ComponentName(context, NotificationService::class.java)
            if (nm != null && nm.isNotificationListenerAccessGranted(cn)) {
                return true
            }
        } catch (_: Exception) {}

        try {
            if (NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) {
                return true
            }
        } catch (_: Exception) {}

        try {
            val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
            if (enabled.contains(context.packageName) || enabled.contains("com.example.cipherspend") || enabled.contains("NotificationService")) {
                return true
            }
        } catch (_: Exception) {}

        return false
    }

    var isNotifListenerActive by remember {
        mutableStateOf(checkNotifListener())
    }

    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isIgnoringBattery = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
                isNotifListenerActive = checkNotifListener()
                coroutineScope.launch {
                    delay(200)
                    isIgnoringBattery = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
                    isNotifListenerActive = checkNotifListener()
                    delay(400)
                    isNotifListenerActive = checkNotifListener()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val json = transactionViewModel.exportVaultJson()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.bufferedWriter().use { writer ->
                        writer.write(json)
                    }
                }
                withContext(Dispatchers.Main) {
                    CipherSnackbarManager.showMessage("Vault backup saved")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    CipherSnackbarManager.showMessage("Backup failed: ${e.message}", isError = true)
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                } ?: ""
                val count = transactionViewModel.importVaultJson(json)
                withContext(Dispatchers.Main) {
                    CipherSnackbarManager.showMessage("Restored $count transactions")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    CipherSnackbarManager.showMessage("Restore failed: ${e.message}", isError = true)
                }
            }
        }
    }

    LaunchedEffect(isNotifListenerActive) {
        if (!isNotifListenerActive) {
            repeat(4) {
                delay(400)
                isNotifListenerActive = checkNotifListener()
            }
        }
    }



    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                Text(
                    text = "System & Security",
                    style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Section: Progressive Security & Advanced Vault Mode
        item {
            Text(
                text = "Progressive Security",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(shape = RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Biometric Unlock Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric",
                                tint = NeonEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Biometric Quick Unlock",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = "Fingerprint / Face ID unlock",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                            }
                        }

                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { authViewModel.setBiometricEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonEmerald,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceDark
                            )
                        )
                    }

                    // Immediate Vault Lockdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLockVault()
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Lock Vault Now",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = "Require MPIN / Biometric to access",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = CyberCyan)
                    }
                }
            }
        }

        // Section: Automation & Rules
        item {
            Text(
                text = "Automation & Rules",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(shape = RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 1. Auto-Drop Duplicates Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CopyAll,
                                contentDescription = "Duplicates",
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Auto-Drop Duplicates",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = "Discard exact/similar transactions automatically",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                            }
                        }

                        Switch(
                            checked = autoDropDuplicates,
                            onCheckedChange = {
                                autoDropDuplicates = it
                                prefs.edit()
                                    .putString("dedupe_rule", if (it) "auto_drop" else "ask")
                                    .putBoolean("auto_drop_duplicates", it)
                                    .apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = CyberCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceDark
                            )
                        )
                    }

                    // 2. UPI Notification Tracker Status Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                                } catch (_: Exception) {
                                    CipherSnackbarManager.showMessage("Could not open Notification Listener settings", isError = true)
                                }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "UPI Notification Tracker",
                                tint = if (isNotifListenerActive) NeonEmerald else CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "UPI Notification Tracker",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = if (isNotifListenerActive) "Active (Listening for GPay, PhonePe, Paytm)" else "Tap to Enable",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isNotifListenerActive) NeonEmerald else TextMuted
                                    )
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isNotifListenerActive) NeonEmerald.copy(alpha = 0.15f) else CyberCyan.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isNotifListenerActive) "Active" else "Enable",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isNotifListenerActive) NeonEmerald else CyberCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // 3. Unrestricted Battery (OEM Reliability)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        context.startActivity(fallbackIntent)
                                    } catch (_: Exception) {
                                        CipherSnackbarManager.showMessage("Could not open battery settings", isError = true)
                                    }
                                }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = "Unrestricted Battery",
                                tint = if (isIgnoringBattery) NeonEmerald else CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Background Reliability (Battery Optimization)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = if (isIgnoringBattery) "Unrestricted (Background parsing protected)" else "Optimized (Tap to protect against OEM background kill)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isIgnoringBattery) NeonEmerald else TextMuted
                                    )
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isIgnoringBattery) NeonEmerald.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isIgnoringBattery) "Active" else "Optimize",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isIgnoringBattery) NeonEmerald else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section: Spending Flow Budget & Proactive Alerts
        item {
            Text(
                text = "Budget & Predictive Alerts",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(shape = RoundedCornerShape(18.dp))
                    .clickable { showBudgetDialog = true }
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alerts",
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Monthly Budget Limit",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                            Text(
                                text = "Proactive alerts at 80% & 100%",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }
                    }

                    Text(
                        text = "₹${"%,.0f".format(forecast?.budget ?: 25000.0)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Section: 100% Offline Privacy Info
        item {
            Text(
                text = "100% Offline Privacy",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(shape = RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Hardware-Backed AES-256 Local Encryption",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = NeonEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "CipherSpend operates 100% offline with SQLCipher SQLite 256-bit encryption and on-device TensorFlow Lite inference. No servers, no tracking, zero data leakage.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                backupLauncher.launch("CipherSpend_Vault_$timestamp.json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan.copy(alpha = 0.2f),
                                contentColor = CyberCyan
                            ),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Backup",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Backup Vault",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Button(
                            onClick = {
                                restoreLauncher.launch(arrayOf("application/json"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurpleGlow.copy(alpha = 0.2f),
                                contentColor = PurpleGlow
                            ),
                            border = BorderStroke(1.dp, PurpleGlow.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = "Restore",
                                    tint = PurpleGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Restore Backup",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showPrivacyPolicyDialog = true },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = "Privacy Policy", tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Privacy Policy & Data Safety", color = CyberCyan, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                    }

                    Button(
                        onClick = { showPurgeDialog = true },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.15f))
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Kill Switch", tint = DangerRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Account & Data", color = DangerRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(110.dp))
        }
    }



    // Budget Configuration Dialog
    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Monthly Budget", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        text = "Native notifications will proactively alert you at 80% and 100% of this monthly threshold.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Budget (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = budgetInput.toFloatOrNull() ?: 25000f
                        transactionViewModel.setMonthlyBudget(amount)
                        showBudgetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Save", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF131B2A)
        )
    }

    // Kill Switch Initial Warning Dialog
    if (showPurgeDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeDialog = false },
            title = { Text("Delete Account & Data", color = DangerRed) },
            text = {
                Text(
                    text = "This will permanently destroy the encrypted database (cipherspend_encrypted.db), wipe encryption keys from hardware keystore, and erase your MPIN and user profile.\n\nYou will be prompted for your 4-digit Master MPIN next.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPurgeDialog = false
                        showMpinGuardForPurge = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Erase Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF131B2A)
        )
    }

    // High Risk Guard Dialog requiring 4-digit Master MPIN before Factory Reset executes
    if (showMpinGuardForPurge) {
        HighRiskGuardDialog(
            title = "Authorize Vault Destruction",
            message = "Enter your 4-digit Master MPIN to authorize permanent destruction of the encrypted database.",
            onVerify = { pin -> authViewModel.verifyMpin(pin) },
            onDismiss = { showMpinGuardForPurge = false },
            onAuthorized = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showMpinGuardForPurge = false
                authViewModel.nuclearWipe()
                CipherSnackbarManager.showMessage("Account & data erased. Vault destroyed.")
                onNuclearWipeComplete()
            }
        )
    }

    // In-App Privacy Policy & Data Safety Disclosure Dialog (Play Store Requirement)
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = NeonEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Privacy Policy & Data Safety",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "CipherSpend is engineered with a strict Zero-Knowledge Offline architecture for your financial privacy.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 17.sp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassEffect(shape = RoundedCornerShape(12.dp), backgroundColor = Color(0xFF0F1E33))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "1. Data Collection & Storage",
                                style = MaterialTheme.typography.labelMedium.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "100% of financial transactions, SMS bodies, and budgets are stored locally on your device inside an AES-256 SQLCipher database (cipherspend_encrypted.db). Data never touches any external cloud server.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassEffect(shape = RoundedCornerShape(12.dp), backgroundColor = Color(0xFF0F1E33))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "2. Zero Internet Access",
                                style = MaterialTheme.typography.labelMedium.copy(color = NeonEmerald, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "CipherSpend does not request the android.permission.INTERNET permission in production builds, making it mathematically and physically impossible for your data to leave your phone.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassEffect(shape = RoundedCornerShape(12.dp), backgroundColor = Color(0xFF0F1E33))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "3. Permissions Used",
                                style = MaterialTheme.typography.labelMedium.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "READ_SMS / RECEIVE_SMS & BIND_NOTIFICATION_LISTENER_SERVICE are used strictly on-device to automate expense logging. All NLP and neural parsing happen locally.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassEffect(shape = RoundedCornerShape(12.dp), backgroundColor = Color(0xFF0F1E33))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "4. Complete Data Deletion",
                                style = MaterialTheme.typography.labelMedium.copy(color = DangerRed, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Users can permanently wipe all local database records, cryptographic keys, and application preferences at any time using 'Delete Account & Data'.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyPolicyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            CipherSnackbarManager.showMessage("Could not open browser: ${e.message}", isError = true)
                        }
                    }
                ) {
                    Text(
                        text = "Open Web Policy (HTTPS)",
                        color = CyberCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = Color(0xFF131B2A),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
        )
    }
}
