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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipherspend.ui.theme.AmbientGlowBackdrop
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DangerRed
import com.example.cipherspend.ui.theme.DarkBackground
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.TextSecondary
import com.example.cipherspend.ui.theme.glassEffect
import com.example.cipherspend.ui.viewmodel.TransactionViewModel

@Composable
fun ManualEntryScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food & Dining") }
    var selectedType by remember { mutableStateOf("UPI") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Food & Dining", "Shopping", "Bills & Utilities", "Travel", "Investment", "Others")
    val types = listOf("UPI", "Card", "Cash", "Bank Transfer")

    fun saveTransaction() {
        val amt = amountText.toDoubleOrNull()
        if (amt == null || amt <= 0.0) {
            errorMessage = "Please enter a valid amount greater than 0"
            return
        }

        val cleanMerchant = merchantText.trim().ifBlank { "Manual Entry" }
        viewModel.insertManualTransaction(
            amount = amt,
            merchant = cleanMerchant,
            category = selectedCategory,
            type = selectedType
        )
        Toast.makeText(context, "Transaction saved to offline vault", Toast.LENGTH_SHORT).show()
        onNavigateBack()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MANUAL ENTRY",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AmbientGlowBackdrop()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Main Form Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassEffect(shape = RoundedCornerShape(22.dp), backgroundColor = Color(0xFF0F1726).copy(alpha = 0.88f))
                            .padding(22.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            // Amount Field
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = {
                                    amountText = it.filter { ch -> ch.isDigit() || ch == '.' }
                                    errorMessage = null
                                },
                                label = { Text("Amount (₹)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CurrencyRupee,
                                        contentDescription = null,
                                        tint = CyberCyan
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                                    focusedLabelColor = CyberCyan,
                                    unfocusedLabelColor = TextSecondary,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = Color(0xFF131B2A).copy(alpha = 0.6f),
                                    unfocusedContainerColor = Color(0xFF131B2A).copy(alpha = 0.4f)
                                )
                            )

                            // Merchant Field
                            OutlinedTextField(
                                value = merchantText,
                                onValueChange = {
                                    merchantText = it
                                    errorMessage = null
                                },
                                label = { Text("Merchant / Store Name") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Store,
                                        contentDescription = null,
                                        tint = CyberCyan
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                                    focusedLabelColor = CyberCyan,
                                    unfocusedLabelColor = TextSecondary,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = Color(0xFF131B2A).copy(alpha = 0.6f),
                                    unfocusedContainerColor = Color(0xFF131B2A).copy(alpha = 0.4f)
                                )
                            )

                            // Category Selector
                            Column {
                                Text(
                                    text = "Category",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(categories) { cat ->
                                        val isSelected = cat == selectedCategory
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) CyberCyan.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                                                .border(
                                                    1.dp,
                                                    if (isSelected) CyberCyan else Color.White.copy(alpha = 0.12f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedCategory = cat }
                                                .padding(horizontal = 14.dp, vertical = 9.dp)
                                        ) {
                                            Text(
                                                text = cat,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = if (isSelected) CyberCyan else TextSecondary,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Payment Method Selector
                            Column {
                                Text(
                                    text = "Payment Method",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(types) { t ->
                                        val isSelected = t == selectedType
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) NeonEmerald.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                                                .border(
                                                    1.dp,
                                                    if (isSelected) NeonEmerald else Color.White.copy(alpha = 0.12f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedType = t }
                                                .padding(horizontal = 14.dp, vertical = 9.dp)
                                        ) {
                                            Text(
                                                text = t,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = if (isSelected) NeonEmerald else TextSecondary,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = DangerRed)
                        )
                    }
                }

                // Bottom Save Transaction Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 32.dp)
                ) {
                    Button(
                        onClick = { saveTransaction() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = CyberCyan.copy(alpha = 0.4f)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Save Transaction",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
