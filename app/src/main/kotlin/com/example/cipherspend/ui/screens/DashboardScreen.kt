package com.example.cipherspend.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberCoroutineScope
import com.example.cipherspend.service.SmsSyncManager
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.cipherspend.data.model.TransactionEntity
import com.example.cipherspend.ui.components.CipherSnackbarManager
import com.example.cipherspend.ui.components.MonthSelector
import com.example.cipherspend.ui.components.getCategoryColor
import com.example.cipherspend.ui.theme.AmberGlow
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DarkBackground
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.TextMuted
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.TextSecondary
import com.example.cipherspend.ui.theme.glassEffect
import com.example.cipherspend.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: TransactionViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfileEdit: () -> Unit = {},
    onNavigateToManualEntry: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val transactions by viewModel.transactions.collectAsState()
    val forecast by viewModel.forecast.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val syncProgress by SmsSyncManager.syncState

    val prefs = remember { context.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE) }
    val profileImage = prefs.getString("pref_profile_image", "assets/avatars/avatar1.png")

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotifListener by remember {
        mutableStateOf(
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.contains(context.packageName) == true
        )
    }

    var showPrivacyDisclosure by remember { mutableStateOf(false) }
    var pendingPermissionAction by remember { mutableStateOf<String?>(null) }

    // Auto-sync whenever permission is granted
    androidx.compose.runtime.LaunchedEffect(hasSmsPermission) {
        if (hasSmsPermission) {
            SmsSyncManager.syncInbox(context)
        }
    }

    // Immediately refresh forecast and UI when sync completes
    androidx.compose.runtime.LaunchedEffect(syncProgress.isSyncing) {
        if (!syncProgress.isSyncing) {
            viewModel.syncDemoModeState()
            viewModel.refreshForecast()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.RECEIVE_SMS] == true &&
                perms[Manifest.permission.READ_SMS] == true
        hasSmsPermission = granted
        if (granted) {
            viewModel.exitDemoMode()
            coroutineScope.launch {
                SmsSyncManager.syncInbox(context, fullRescan = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "100% Offline Vault",
                        style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasSmsPermission) {
                        val infiniteTransition = rememberInfiniteTransition(label = "syncRotation")
                        val rotationAngle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )
                        Box(
                            modifier = Modifier
                                .glassEffect(shape = CircleShape, backgroundColor = Color(0xFF131B2A))
                                .clickable {
                                    coroutineScope.launch {
                                        SmsSyncManager.syncInbox(context, fullRescan = true)
                                        CipherSnackbarManager.showMessage("Inbox synced")
                                    }
                                }
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Inbox",
                                tint = if (syncProgress.isSyncing) CyberCyan else TextSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        if (syncProgress.isSyncing) {
                                            rotationZ = rotationAngle
                                        }
                                    }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .glassEffect(shape = CircleShape, backgroundColor = Color(0xFF131B2A))
                            .clickable { onNavigateToNotifications() }
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (notifications.isNotEmpty()) CyberCyan else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .glassEffect(shape = CircleShape, backgroundColor = Color(0xFF131B2A))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Encrypted",
                            tint = NeonEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, CyberCyan.copy(alpha = 0.6f), CircleShape)
                            .clickable { onNavigateToProfileEdit() },
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarDisplay(
                            imagePath = profileImage,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        // Demo Mode Active Banner
        if (isDemoMode) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = Color(0xFF2A1C0A),
                            borderColor = AmberGlow.copy(alpha = 0.6f)
                        )
                        .border(1.dp, AmberGlow.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(AmberGlow)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Demo Mode Active",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = AmberGlow,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Text(
                            text = "Showing sample transactions so you can explore Analytics, Search, and Receipts.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary.copy(alpha = 0.9f),
                                lineHeight = 18.sp
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    pendingPermissionAction = "SMS"
                                    showPrivacyDisclosure = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGlow)
                            ) {
                                Text(
                                    text = "Connect Real SMS",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.exitDemoMode()
                                    CipherSnackbarManager.showMessage("Demo data cleared")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AmberGlow.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGlow)
                            ) {
                                Text(
                                    text = "Exit Demo",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = AmberGlow,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Month-by-Month Selector
        item {
            MonthSelector(
                selectedMonth = selectedMonth,
                onPreviousMonth = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.selectPreviousMonth()
                },
                onNextMonth = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.selectNextMonth()
                }
            )
        }

        // Budget Summary Card
        item {
            val totalSpent = forecast?.spent ?: 0.0
            val budget = forecast?.budget ?: 25000.0
            val progress = forecast?.progress ?: (if (budget > 0) (totalSpent / budget).toFloat().coerceIn(0f, 1f) else 0f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(shape = RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Spending Flow",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextSecondary, fontSize = 14.sp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (progress >= 0.8f) Color(0xFFFF5252).copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${(progress * 100).toInt()}% Used",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (progress >= 0.8f) Color(0xFFFF5252) else CyberCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "₹${"%,.2f".format(totalSpent)}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Monthly Budget: ₹${"%,.0f".format(budget)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (progress >= 0.8f) Color(0xFFFF5252) else CyberCyan,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val currentYm = java.time.YearMonth.now()
                    val daysLeftInMonth = if (selectedMonth == currentYm) {
                        (selectedMonth.lengthOfMonth() - java.time.LocalDate.now().dayOfMonth + 1).coerceAtLeast(1)
                    } else if (selectedMonth.isAfter(currentYm)) {
                        selectedMonth.lengthOfMonth()
                    } else {
                        1
                    }
                    val safePerDay = (budget - totalSpent).coerceAtLeast(0.0) / daysLeftInMonth.coerceAtLeast(1)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Safe/Day
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glassEffect(shape = RoundedCornerShape(12.dp), backgroundColor = Color(0xFF131B2A))
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Safe/Day",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${"%,.0f".format(safePerDay)}/d",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }

                        // Avg Burn
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glassEffect(shape = RoundedCornerShape(12.dp), backgroundColor = Color(0xFF131B2A))
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Avg Burn",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${"%,.0f".format(forecast?.dailyAverage ?: 0.0)}/d",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = NeonEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }

                        // Top Spend
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glassEffect(shape = RoundedCornerShape(12.dp), backgroundColor = Color(0xFF131B2A))
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Top Spend",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = forecast?.topCategory ?: "None",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Contextual Permissions Banner (HCI Requirement)
        if (!hasSmsPermission || !hasNotifListener) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(
                            shape = RoundedCornerShape(18.dp),
                            backgroundColor = Color(0xFF0F1E33),
                            borderColor = CyberCyan.copy(alpha = 0.35f)
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = "Auto Tracking",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Enable Auto-Tracking",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Zero-Knowledge Guarantee: SMS and UPI payment notifications are parsed 100% on-device by our native neural model. Data never leaves your hardware.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (!hasSmsPermission) {
                                    pendingPermissionAction = "SMS"
                                    showPrivacyDisclosure = true
                                } else if (!hasNotifListener) {
                                    pendingPermissionAction = "NOTIF"
                                    showPrivacyDisclosure = true
                                } else {
                                    coroutineScope.launch {
                                        SmsSyncManager.syncInbox(context, fullRescan = true)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                        ) {
                            Text(
                                text = when {
                                    !hasSmsPermission -> "Enable Auto-Tracking (SMS)"
                                    !hasNotifListener -> "Grant Notification Listener"
                                    syncProgress.isSyncing -> "Scanning Messages..."
                                    else -> "Sync SMS Inbox Now"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                        }

                        if (!isDemoMode && !hasSmsPermission) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.loadDemoTransactions()
                                    CipherSnackbarManager.showMessage("Demo transactions loaded!")
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                            ) {
                                Text(
                                    text = "Try Demo Mode (No SMS Needed)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live SMS Sync Indicator Banner
        if (syncProgress.isSyncing) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(shape = RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Syncing Banking SMS...",
                                style = MaterialTheme.typography.titleSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${syncProgress.totalScanned} scanned",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = CyberCyan,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        Text(
                            text = "Extracted ${syncProgress.transactionsFound} financial records into encrypted vault",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (transactions.isEmpty() && !syncProgress.isSyncing) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(shape = RoundedCornerShape(18.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No Transactions Found",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (!hasSmsPermission)
                                "Tap 'Enable Auto-Tracking' above to automatically parse bank SMS messages, or tap '+' to add manually."
                            else
                                "No bank debit/UPI messages detected in your inbox. Tap 'Sync SMS Inbox Now' or use '+' to record manually.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (!isDemoMode) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.loadDemoTransactions()
                                    CipherSnackbarManager.showMessage("Demo transactions loaded!")
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AmberGlow.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGlow)
                            ) {
                                Text(
                                    text = "Try Demo Mode (No SMS Needed)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = AmberGlow,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                TransactionRowItem(
                    transaction = tx,
                    onClick = { onNavigateToDetail(tx.id) }
                )
            }
        }

        // Generous bottom clearance to prevent bottom bar overlap
        item {
            Spacer(modifier = Modifier.height(110.dp))
        }
    }

    // Google Play Prominent Disclosure Dialog (Step 5)
    if (showPrivacyDisclosure && pendingPermissionAction != null) {
        ProminentDisclosureDialog(
            action = pendingPermissionAction ?: "SMS",
            onConfirm = {
                val action = pendingPermissionAction
                showPrivacyDisclosure = false
                pendingPermissionAction = null
                if (action == "SMS") {
                    val permissions = mutableListOf(
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS
                    )
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                } else if (action == "NOTIF") {
                    try {
                        context.startActivity(
                            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    } catch (_: Exception) {}
                }
            },
            onDismiss = {
                showPrivacyDisclosure = false
                pendingPermissionAction = null
            }
        )
    }
}

@Composable
fun ProminentDisclosureDialog(
    action: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    text = "Google Play Prominent Disclosure",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "CipherSpend requires ${if (action == "SMS") "SMS permission" else "Notification Listener access"} to automate your offline personal expense tracking.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column {
                        Text(
                            text = "Data Accessed:",
                            style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Incoming & historical bank SMS messages and UPI app payment notifications.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 16.sp)
                        )
                    }

                    Column {
                        Text(
                            text = "Purpose:",
                            style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Solely to automatically extract transaction amounts, categories, and merchant names for your personal expense ledger.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 16.sp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(shape = RoundedCornerShape(12.dp), backgroundColor = Color(0xFF0F1E33))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Zero-Knowledge Privacy Guarantee",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "All parsing and AI inference happen 100% locally on your device. Zero financial or personal data is ever transmitted, shared, or uploaded to any external server.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "I Agree & Continue",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TextMuted)
            ) {
                Text(
                    text = "Decline",
                    color = TextSecondary
                )
            }
        },
        containerColor = Color(0xFF131B2A),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
    )
}

@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(transaction.timestamp) {
        dateFormat.format(Date(transaction.timestamp))
    }
    val catColor = getCategoryColor(transaction.category)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(shape = RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(catColor.copy(alpha = 0.15f))
                        .border(1.dp, catColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = transaction.merchant.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = catColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = catColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = " • $formattedDate",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${"%,.2f".format(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = transaction.type,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
            }
        }
    }
}
