package com.example.cipherspend.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.TextSecondary
import com.example.cipherspend.ui.theme.glassEffect
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthSelector(
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    }
    val formattedMonth = remember(selectedMonth) {
        selectedMonth.format(monthFormatter)
    }
    val isCurrentMonth = remember(selectedMonth) {
        selectedMonth == YearMonth.now()
    }

    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Previous Month Arrow
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = CyberCyan)
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPreviousMonth()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous Month",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Center Pill Display
        Box(
            modifier = Modifier
                .glassEffect(
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = Color(0xFF131B2A).copy(alpha = 0.85f),
                    borderColor = if (isCurrentMonth) CyberCyan.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f)
                )
                .clickable(enabled = !isCurrentMonth && onCurrentMonth != null) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCurrentMonth?.invoke()
                }
                .padding(horizontal = 22.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formattedMonth,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Next Month Arrow
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = CyberCyan)
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNextMonth()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next Month",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
