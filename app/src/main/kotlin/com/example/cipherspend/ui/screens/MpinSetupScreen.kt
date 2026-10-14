package com.example.cipherspend.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipherspend.ui.theme.AmbientGlowBackdrop
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DangerRed
import com.example.cipherspend.ui.theme.DarkBackground
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.SurfaceDark
import com.example.cipherspend.ui.theme.TextMuted
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.TextSecondary
import com.example.cipherspend.ui.theme.glassEffect
import com.example.cipherspend.ui.viewmodel.AuthViewModel

@Composable
fun MpinSetupScreen(
    authViewModel: AuthViewModel,
    onMpinConfigured: () -> Unit
) {
    val density = LocalDensity.current.density
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val headerAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "mpinHeaderAlpha"
    )
    val headerOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 28f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "mpinHeaderOffsetY"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "mpinContentAlpha"
    )
    val contentOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 32f,
        animationSpec = tween(durationMillis = 500, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "mpinContentOffsetY"
    )

    var enteredPin by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    fun handleDigitPress(digit: String) {
        when (digit) {
            "CLEAR" -> enteredPin = ""
            "DEL" -> {
                if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                errorMessage = null
            }
            else -> {
                if (enteredPin.length < 4) {
                    val nextPin = enteredPin + digit
                    enteredPin = nextPin
                    errorMessage = null
                    if (nextPin.length == 4) {
                        val success = authViewModel.setupMpinAndBiometric(nextPin, enableBiometric)
                        if (success) {
                            onMpinConfigured()
                        } else {
                            errorMessage = "Failed to configure MPIN. Please try again."
                            enteredPin = ""
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        AmbientGlowBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 48.dp)
                    .graphicsLayer {
                        alpha = headerAlpha
                        translationY = headerOffsetY * density
                    }
            ) {
                // Security Icon Box
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .glassEffect(shape = CircleShape, backgroundColor = Color(0xFF131B2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security Shield",
                        tint = CyberCyan,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Setup Master MPIN",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Set a 4-digit Master MPIN. Your financial vault is encrypted locally and never leaves this phone.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) CyberCyan else Color.White.copy(alpha = 0.1f))
                                .border(
                                    1.dp,
                                    if (isFilled) CyberCyan else Color.White.copy(alpha = 0.25f),
                                    CircleShape
                                )
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = DangerRed)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Biometrics Toggle Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = contentAlpha
                            translationY = contentOffsetY * density
                        }
                        .glassEffect(shape = RoundedCornerShape(18.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp)
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
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometrics",
                                tint = NeonEmerald,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Biometric Quick Unlock",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Unlock seamlessly via Fingerprint / Face",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                        Switch(
                            checked = enableBiometric,
                            onCheckedChange = { enableBiometric = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonEmerald,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceDark
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = contentAlpha
                        translationY = contentOffsetY * density
                    }
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val keypad = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("CLEAR", "0", "DEL")
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
                                    .clickable { handleDigitPress(digit) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (digit == "DEL") {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Text(
                                        text = digit,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = if (digit == "CLEAR") TextSecondary else TextPrimary,
                                            fontSize = if (digit == "CLEAR") 13.sp else 22.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
