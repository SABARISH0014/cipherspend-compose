package com.example.cipherspend.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipherspend.ui.theme.AmbientGlowBackdrop
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DarkBackground
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.TextMuted
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun StartupScreen(
    authViewModel: AuthViewModel,
    onNavigateToUnlock: () -> Unit,
    onNavigateToVerification: () -> Unit
) {
    // 2500ms boot delay before routing
    LaunchedEffect(Unit) {
        delay(2500)
        if (authViewModel.isConfigured.value) {
            onNavigateToUnlock()
        } else {
            onNavigateToVerification()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val progressOffset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 110f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progressOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        AmbientGlowBackdrop()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Center Glowing Orb Icon
            Box(
                modifier = Modifier
                    .size(120.dp * pulseScale)
                    .shadow(
                        elevation = 32.dp,
                        shape = CircleShape,
                        spotColor = CyberCyan.copy(alpha = 0.6f),
                        ambientColor = NeonEmerald.copy(alpha = 0.4f)
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CyberCyan.copy(alpha = 0.25f),
                                Color(0xFF0F1E28).copy(alpha = 0.8f)
                            )
                        )
                    )
                    .border(2.dp, CyberCyan.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A121D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "CipherSpend Core",
                        tint = CyberCyan,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // CIPHER SPEND Text
            Text(
                text = "CIPHER SPEND",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    fontSize = 28.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Your private offline expense vault",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    letterSpacing = 1.5.sp
                )
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Animated Terminal Scan Indicator Bar
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = progressOffset.dp)
                        .width(42.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(CyberCyan, NeonEmerald)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Terminal status
            Text(
                text = "INITIALIZING ZERO-KNOWLEDGE VAULT...",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CyberCyan.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}
