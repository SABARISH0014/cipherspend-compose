package com.example.cipherspend.ui.screens

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipherspend.ui.theme.AmbientGlowBackdrop
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DangerRed
import com.example.cipherspend.ui.theme.DarkBackground
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.TextMuted
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.TextSecondary
import com.example.cipherspend.ui.theme.glassEffect
import com.example.cipherspend.ui.viewmodel.TransactionViewModel

@Composable
fun ProfileSetupScreen(
    transactionViewModel: TransactionViewModel,
    onProfileSaved: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val scrollState = rememberScrollState()

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val headerAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "headerAlpha"
    )
    val headerOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 28f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "headerOffsetY"
    )

    val formAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "formAlpha"
    )
    val formOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 32f,
        animationSpec = tween(durationMillis = 500, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "formOffsetY"
    )

    var userName by remember { mutableStateOf("") }
    var targetBudget by remember { mutableStateOf("25000") }
    var salaryCycleDay by remember { mutableStateOf("1") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun saveProfile() {
        if (userName.trim().isEmpty()) {
            errorMessage = "Please enter your name"
            return
        }

        val budgetVal = targetBudget.toFloatOrNull()
        if (budgetVal == null || budgetVal <= 0f) {
            errorMessage = "Please enter a valid target budget"
            return
        }

        val dayVal = salaryCycleDay.toIntOrNull()
        if (dayVal == null || dayVal !in 1..31) {
            errorMessage = "Salary cycle date must be between 1 and 31"
            return
        }

        val prefs = context.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("user_name", userName.trim())
            .putFloat("monthly_budget", budgetVal)
            .putInt("salary_cycle_day", dayVal)
            .apply()

        transactionViewModel.setMonthlyBudget(budgetVal)
        onProfileSaved()
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
                .imePadding()
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
                // Header Profile Orb Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .glassEffect(shape = CircleShape, backgroundColor = Color(0xFF131B2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = CyberCyan,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Profile & Target Budget",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Personalize your monthly targets and tracking cadence to activate zero-knowledge insights.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Card container with fields
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = formAlpha
                            translationY = formOffsetY * density
                        }
                        .glassEffect(shape = RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        // Your Name Field
                        OutlinedTextField(
                            value = userName,
                            onValueChange = {
                                userName = it
                                errorMessage = null
                            },
                            label = { Text("Your Name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = CyberCyan
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = CyberCyan,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = Color(0xFF131B2A).copy(alpha = 0.6f),
                                unfocusedContainerColor = Color(0xFF131B2A).copy(alpha = 0.4f)
                            )
                        )

                        // Monthly Target Budget Field
                        OutlinedTextField(
                            value = targetBudget,
                            onValueChange = {
                                targetBudget = it.filter { ch -> ch.isDigit() || ch == '.' }
                                errorMessage = null
                            },
                            label = { Text("Monthly Target Budget (₹)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = NeonEmerald
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonEmerald,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = NeonEmerald,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = Color(0xFF131B2A).copy(alpha = 0.6f),
                                unfocusedContainerColor = Color(0xFF131B2A).copy(alpha = 0.4f)
                            )
                        )

                        // Salary Cycle Start Date Field
                        OutlinedTextField(
                            value = salaryCycleDay,
                            onValueChange = {
                                salaryCycleDay = it.filter { ch -> ch.isDigit() }.take(2)
                                errorMessage = null
                            },
                            label = { Text("Salary Cycle Start Date (Day 1 - 31)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = CyberCyan
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = CyberCyan,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = Color(0xFF131B2A).copy(alpha = 0.6f),
                                unfocusedContainerColor = Color(0xFF131B2A).copy(alpha = 0.4f)
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
            }

            // Bottom Save Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = formAlpha
                        translationY = formOffsetY * density
                    }
                    .padding(bottom = 36.dp, top = 24.dp)
            ) {
                Button(
                    onClick = { saveProfile() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
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
                        text = "Save & Continue",
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
