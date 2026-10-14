package com.example.cipherspend.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipherspend.ui.components.CipherSnackbarManager
import com.example.cipherspend.ui.theme.AmbientGlowBackdrop
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DangerRed
import com.example.cipherspend.ui.theme.DarkBackground
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.TextMuted
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.TextSecondary
import com.example.cipherspend.ui.theme.glassEffect
import com.example.cipherspend.ui.viewmodel.AuthViewModel

@Composable
fun VerifySetupScreen(
    authViewModel: AuthViewModel,
    onVerificationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val totpSecret by authViewModel.totpSecret.collectAsState()
    val phoneNumber by authViewModel.phoneNumber.collectAsState()

    val secret = totpSecret ?: remember { authViewModel.generateNewTotpSecret() }

    var enteredCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun verifyCode() {
        val cleanCode = enteredCode.filter { it.isDigit() }
        if (cleanCode.length != 6) {
            errorMessage = "Please enter all 6 digits of the TOTP code"
            return
        }

        val isValid = authViewModel.verifyAndEnableTotp(secret, cleanCode)
        if (isValid) {
            errorMessage = null
            focusManager.clearFocus()
            CipherSnackbarManager.showMessage("Authenticator verified successfully")
            onVerificationSuccess()
        } else {
            errorMessage = "Invalid verification code. Please check your authenticator and try again."
        }
    }

    // Format secret into two visually readable chunks
    val formattedSecret = remember(secret) {
        if (secret.length >= 16) {
            val part1 = secret.substring(0, secret.length / 2).chunked(1).joinToString(" ")
            val part2 = secret.substring(secret.length / 2).chunked(1).joinToString(" ")
            "$part1\n$part2"
        } else {
            secret.chunked(1).joinToString(" ")
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
                modifier = Modifier.padding(top = 56.dp)
            ) {
                // Shield Glowing Orb Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(
                            elevation = 28.dp,
                            shape = CircleShape,
                            spotColor = NeonEmerald.copy(alpha = 0.5f),
                            ambientColor = CyberCyan.copy(alpha = 0.3f)
                        )
                        .clip(CircleShape)
                        .background(NeonEmerald.copy(alpha = 0.15f))
                        .border(1.5.dp, NeonEmerald.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F1E1B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield Security",
                            tint = NeonEmerald,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Title
                Text(
                    text = "Link Your Authenticator",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subtitle
                Text(
                    text = "Add the following setup key to Google Authenticator\nor Authy to protect your account.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Secret Key Display Box with Copy Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(
                            shape = RoundedCornerShape(18.dp),
                            backgroundColor = Color(0xFF0C1420).copy(alpha = 0.88f),
                            borderColor = NeonEmerald.copy(alpha = 0.25f)
                        )
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedSecret,
                            style = TextStyle(
                                color = NeonEmerald,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 24.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(secret))
                                CipherSnackbarManager.showMessage("Key copied to clipboard")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Secret",
                                tint = NeonEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Open Authenticator Button
                OutlinedButton(
                    onClick = {
                        try {
                            val account = if (phoneNumber.isNotBlank()) phoneNumber else "CipherSpend"
                            val uri = Uri.parse("otpauth://totp/CipherSpend:$account?secret=$secret&issuer=CipherSpend")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            CipherSnackbarManager.showMessage(
                                "Copy the secret key and open your Authenticator app manually",
                                isError = true
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonEmerald
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Open Authenticator",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = NeonEmerald
                        )
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Verify 6-digit Code Label & Input
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Verify 6-Digit Code",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = NeonEmerald.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .glassEffect(
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = Color(0xFF0C1420).copy(alpha = 0.85f),
                                borderColor = if (errorMessage != null) DangerRed.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = enteredCode,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(6)
                                enteredCode = filtered
                                if (errorMessage != null && filtered.length == 6) {
                                    errorMessage = null
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { verifyCode() }
                            ),
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(CyberCyan),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (enteredCode.isEmpty()) {
                                    Text(
                                        text = "• • • • • •",
                                        style = TextStyle(
                                            color = TextMuted.copy(alpha = 0.45f),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 8.sp,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DangerRed,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Bottom Verify Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp, top = 20.dp)
            ) {
                Button(
                    onClick = { verifyCode() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = NeonEmerald.copy(alpha = 0.45f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonEmerald,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verify Code",
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
