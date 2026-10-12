package com.example.cipherspend.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    transactionViewModel: TransactionViewModel,
    onLockVault: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val isBiometricEnabled by authViewModel.isBiometricEnabled.collectAsState()
    val isTotpEnabled by authViewModel.isTotpEnabled.collectAsState()
    val totpSecret by authViewModel.totpSecret.collectAsState()
    val forecast by transactionViewModel.forecast.collectAsState()

    var showTotpDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showPurgeDialog by remember { mutableStateOf(false) }

    var candidateTotpSecret by remember { mutableStateOf("") }
    var totpInputCode by remember { mutableStateOf("") }
    var budgetInput by remember { mutableStateOf((forecast?.budget ?: 25000.0).toInt().toString()) }

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

        // Section: Progressive Security & Advanced Vault Mode (Step 6.1)
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
                    // Advanced Vault Mode (TOTP)
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
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = "TOTP",
                                tint = PurpleGlow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Advanced Vault Mode (TOTP)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = if (isTotpEnabled) "Enabled (2FA Authenticator Paired)" else "Pair Authenticator App (RFC 6238)",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                            }
                        }

                        if (isTotpEnabled) {
                            TextButton(onClick = { authViewModel.disableTotp() }) {
                                Text("Disable", color = DangerRed)
                            }
                        } else {
                            Button(
                                onClick = {
                                    candidateTotpSecret = authViewModel.generateNewTotpSecret()
                                    showTotpDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PurpleGlow)
                            ) {
                                Text("Pair", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

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
                            .clickable { onLockVault() },
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

        // Section: Zero-Knowledge Architecture Info
        item {
            Text(
                text = "Zero-Knowledge Architecture",
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
                        text = "Military-Grade Local Encryption",
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

                    Button(
                        onClick = { showPurgeDialog = true },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.15f))
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Purge", tint = DangerRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Purge All Encrypted Data", color = DangerRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(110.dp))
        }
    }

    // TOTP Pairing Dialog
    if (showTotpDialog) {
        AlertDialog(
            onDismissRequest = { showTotpDialog = false },
            title = { Text("Pair Authenticator App (TOTP)", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter this secret key in Google Authenticator, Aegis, or 2FAS:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassEffect(shape = RoundedCornerShape(10.dp), backgroundColor = Color(0xFF131B2A))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(candidateTotpSecret))
                                Toast.makeText(context, "Secret copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = candidateTotpSecret,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = PurpleGlow,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMuted)
                        }
                    }

                    OutlinedTextField(
                        value = totpInputCode,
                        onValueChange = { if (it.length <= 6) totpInputCode = it },
                        label = { Text("6-Digit Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleGlow,
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
                        val verified = authViewModel.verifyAndEnableTotp(candidateTotpSecret, totpInputCode)
                        if (verified) {
                            Toast.makeText(context, "TOTP Advanced Vault Activated!", Toast.LENGTH_SHORT).show()
                            showTotpDialog = false
                            totpInputCode = ""
                        } else {
                            Toast.makeText(context, "Invalid OTP code", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleGlow)
                ) {
                    Text("Verify & Enable", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTotpDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF131B2A)
        )
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

    // Purge Confirmation Dialog
    if (showPurgeDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeDialog = false },
            title = { Text("Wipe All Transactions?", color = DangerRed) },
            text = {
                Text(
                    text = "This will permanently delete all encrypted transactions and rules from your local SQLCipher database. This action cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        transactionViewModel.clearAllData()
                        showPurgeDialog = false
                        Toast.makeText(context, "Database purged", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Wipe Data", color = Color.White)
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
}
