package com.example.cipherspend.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates a premium dark glassmorphic frosted surface.
 * IMPORTANT HCI FIX: We do NOT apply blur to the content container itself.
 * Applying blur to child text/composables causes text smearing and unreadable UI.
 * Instead, glassEffect produces a frosted surface with multi-stop specular gradients
 * and delicate luminous borders, keeping all text and icons 100% crisp and readable.
 */
fun Modifier.glassEffect(
    shape: Shape = RoundedCornerShape(18.dp),
    backgroundColor: Color = Color(0xFF131B2A).copy(alpha = 0.72f),
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                backgroundColor,
                Color(0xFF0D1420).copy(alpha = 0.85f)
            )
        ),
        shape = shape
    )
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                borderColor,
                Color.White.copy(alpha = 0.02f),
                borderColor.copy(alpha = 0.06f)
            )
        ),
        shape = shape
    )

/**
 * Ambient glow backdrop placed BEHIND content.
 * Blurs glowing light in the background canvas so translucent cards floating
 * over it reveal authentic frosted refraction without blurring their text.
 */
@Composable
fun AmbientGlowBackdrop(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Cyan ambient light orb (top-left)
        Box(
            modifier = Modifier
                .offset(x = (-40).dp, y = (-20).dp)
                .size(240.dp)
                .clip(CircleShape)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(80.dp)
                    } else {
                        Modifier
                    }
                )
                .background(CyberCyan.copy(alpha = 0.10f), CircleShape)
        )

        // Purple ambient light orb (bottom-right)
        Box(
            modifier = Modifier
                .offset(x = 180.dp, y = 340.dp)
                .size(280.dp)
                .clip(CircleShape)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(90.dp)
                    } else {
                        Modifier
                    }
                )
                .background(PurpleGlow.copy(alpha = 0.08f), CircleShape)
        )
    }
}
