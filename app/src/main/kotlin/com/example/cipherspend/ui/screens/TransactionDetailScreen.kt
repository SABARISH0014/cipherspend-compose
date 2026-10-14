package com.example.cipherspend.ui.screens

import android.content.Context
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.cipherspend.ui.components.CipherSnackbarManager
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipherspend.data.model.TransactionEntity
import com.example.cipherspend.ui.components.getCategoryColor
import com.example.cipherspend.ui.theme.AmbientGlowBackdrop
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DangerRed
import com.example.cipherspend.ui.theme.DarkBackground
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.PurpleGlow
import com.example.cipherspend.ui.theme.TextMuted
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.TextSecondary
import com.example.cipherspend.ui.theme.glassEffect
import com.example.cipherspend.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom TicketShape with jagged 6dp sawtooth top and bottom edges
 * matching Flutter's TicketClipper for authentic receipt visuals.
 */
class TicketShape(
    private val toothWidth: Dp = 12.dp,
    private val toothHeight: Dp = 6.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val twPx = with(density) { toothWidth.toPx() }
        val thPx = with(density) { toothHeight.toPx() }

        path.moveTo(0f, thPx)

        // Top sawtooth edge
        var curX = 0f
        var up = false
        while (curX < size.width) {
            val nextX = (curX + twPx / 2f).coerceAtMost(size.width)
            val nextY = if (up) thPx else 0f
            path.lineTo(nextX, nextY)
            curX = nextX
            up = !up
        }
        path.lineTo(size.width, thPx)

        // Right side
        path.lineTo(size.width, size.height - thPx)

        // Bottom sawtooth edge
        curX = size.width
        up = false
        while (curX > 0f) {
            val nextX = (curX - twPx / 2f).coerceAtLeast(0f)
            val nextY = if (up) size.height - thPx else size.height
            path.lineTo(nextX, nextY)
            curX = nextX
            up = !up
        }
        path.lineTo(0f, size.height - thPx)

        // Left side
        path.lineTo(0f, thPx)
        path.close()

        return Outline.Generic(path)
    }
}

@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    var transaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var isEditingMerchant by remember { mutableStateOf(false) }
    var editedMerchantName by remember { mutableStateOf("") }

    val categories = listOf("Food & Dining", "Shopping", "Bills & Utilities", "Travel", "Investment", "Cash", "Transaction", "Others")

    LaunchedEffect(transactionId) {
        val found = viewModel.getTransactionById(transactionId)
        if (found != null) {
            transaction = found
            editedMerchantName = found.merchant
        }
    }

    val currentTx = transaction
    if (currentTx == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Transaction Record Not Found", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = { onNavigateBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Go Back", color = Color.Black)
                }
            }
        }
        return
    }

    // Typewriter effect state for raw SMS body
    var displayedLength by remember(currentTx.body) { mutableIntStateOf(0) }
    var isTypewriterFinished by remember(currentTx.body) { mutableStateOf(false) }

    LaunchedEffect(currentTx.body) {
        val totalLength = currentTx.body.length
        if (totalLength == 0) {
            isTypewriterFinished = true
            return@LaunchedEffect
        }
        displayedLength = 0
        isTypewriterFinished = false
        val stepDelay = (1000L / totalLength).coerceIn(8L, 45L)
        for (i in 1..totalLength) {
            displayedLength = i
            delay(stepDelay)
        }
        isTypewriterFinished = true
    }

    val catColor = getCategoryColor(currentTx.category)
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a", Locale.getDefault()) }
    val formattedDate = remember(currentTx.timestamp) { dateFormat.format(Date(currentTx.timestamp)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        AmbientGlowBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onNavigateBack() }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }

                Text(
                    text = "Transaction Details",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteTransaction(currentTx)
                        CipherSnackbarManager.showMessage("Transaction deleted and blacklisted")
                        onNavigateBack()
                    }
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed)
                }
            }

            // Receipt Block rendered inside custom TicketShape with jagged edges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TicketShape(toothWidth = 14.dp, toothHeight = 6.dp))
                    .background(Color(0xFF0F1726).copy(alpha = 0.95f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), TicketShape(toothWidth = 14.dp, toothHeight = 6.dp))
                    .padding(vertical = 24.dp, horizontal = 20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "TRANSACTION RECEIPT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "₹${"%,.2f".format(currentTx.amount)}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentTx.merchant,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(catColor.copy(alpha = 0.15f))
                                .border(1.dp, catColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentTx.category,
                                style = MaterialTheme.typography.labelSmall.copy(color = catColor, fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentTx.type,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                    )
                }
            }

            // Category Change Row Selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(shape = RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Text(
                            text = currentTx.category,
                            style = MaterialTheme.typography.labelSmall.copy(color = catColor, fontWeight = FontWeight.Bold)
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            val isSelected = cat == currentTx.category
                            val chipColor = getCategoryColor(cat)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) chipColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                                    .border(
                                        1.dp,
                                        if (isSelected) chipColor else Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        if (cat != currentTx.category) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.updateTransactionCategory(currentTx.id, cat)
                                            transaction = currentTx.copy(category = cat)
                                            CipherSnackbarManager.showMessage("Category changed to $cat")
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSelected) chipColor else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // One-Tap Merchant Correction Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(shape = RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = "Merchant",
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Merchant (Tap to Correct)",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }

                        if (!isEditingMerchant) {
                            IconButton(onClick = { isEditingMerchant = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Merchant",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isEditingMerchant) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = editedMerchantName,
                                onValueChange = { editedMerchantName = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.updateMerchantCorrection(currentTx.id, editedMerchantName)
                                    transaction = currentTx.copy(merchant = editedMerchantName)
                                    isEditingMerchant = false
                                    CipherSnackbarManager.showMessage("Merchant updated & pattern rule saved")
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Save", tint = Color.Black)
                            }
                        }
                    } else {
                        Text(
                            text = currentTx.merchant,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Error Prevention: Editing this merchant saves silently and creates an automated local rule for future SMS from ${currentTx.sender}.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                    )
                }
            }

            // Raw Source Body with Typewriter Animation & █ Cursor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(shape = RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Parsed Source (${currentTx.sender})",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(currentTx.body))
                                CipherSnackbarManager.showMessage("Copied raw body")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    val typewriterDisplay = currentTx.body.take(displayedLength) + (if (!isTypewriterFinished) "█" else "")

                    Text(
                        text = typewriterDisplay,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Timestamp: $formattedDate",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Return Button
            Button(
                onClick = { onNavigateBack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131B2A))
            ) {
                Text("Return to Vault", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
