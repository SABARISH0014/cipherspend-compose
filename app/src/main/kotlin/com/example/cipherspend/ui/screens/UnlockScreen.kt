package com.example.cipherspend.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipherspend.ui.theme.CardSurface
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DangerRed
import com.example.cipherspend.ui.theme.DarkBackground
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.TextSecondary
import com.example.cipherspend.ui.theme.glassEffect
import kotlinx.coroutines.delay

@Composable
fun UnlockScreen(
    onUnlockWithPin: (String) -> Boolean,
    onTriggerBiometric: () -> Unit,
    isBiometricAvailable: Boolean,
    lockoutSeconds: Int = 0
) {
    LaunchedEffect(isBiometricAvailable, lockoutSeconds) {
        if (isBiometricAvailable && lockoutSeconds == 0) {
            delay(200)
            onTriggerBiometric()
        }
    }

    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .glassEffect(shape = CircleShape, backgroundColor = CardSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Vault Locked",
                        tint = CyberCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "CipherSpend",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enter 4-digit MPIN to unlock offline vault",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // PIN indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> DangerRed
                                        isFilled -> CyberCyan
                                        else -> Color.White.copy(alpha = 0.1f)
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isError) DangerRed else if (isFilled) CyberCyan else Color.White.copy(alpha = 0.25f),
                                    CircleShape
                                )
                        )
                    }
                }

                if (lockoutSeconds > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Too many failed attempts. Try again in ${lockoutSeconds}s",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DangerRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                } else if (isError) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Incorrect MPIN. Please try again.",
                        style = MaterialTheme.typography.bodySmall.copy(color = DangerRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = if (lockoutSeconds > 0) 0.45f else 1f
                    }
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val keypad = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                keypad.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .glassEffect(shape = RoundedCornerShape(14.dp))
                                    .clickable {
                                        if (lockoutSeconds > 0) return@clickable
                                        when (digit) {
                                            "BIO" -> {
                                                if (isBiometricAvailable) onTriggerBiometric()
                                            }
                                            "DEL" -> {
                                                isError = false
                                                if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                            }
                                            else -> {
                                                if (enteredPin.length < 4) {
                                                    isError = false
                                                    val next = enteredPin + digit
                                                    enteredPin = next
                                                    if (next.length == 4) {
                                                        val success = onUnlockWithPin(next)
                                                        if (!success) {
                                                            isError = true
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (digit) {
                                    "BIO" -> {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Biometric Unlock",
                                            tint = if (isBiometricAvailable && lockoutSeconds <= 0) NeonEmerald else Color.White.copy(alpha = 0.15f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    "DEL" -> {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = digit,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                color = TextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
