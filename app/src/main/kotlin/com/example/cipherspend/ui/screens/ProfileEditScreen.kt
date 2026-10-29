package com.example.cipherspend.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipherspend.ui.components.CipherSnackbarManager
import com.example.cipherspend.ui.components.HighRiskGuardDialog
import com.example.cipherspend.ui.theme.AmbientGlowBackdrop
import com.example.cipherspend.ui.theme.CardBorder
import com.example.cipherspend.ui.theme.CardSurface
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DangerRed
import com.example.cipherspend.ui.theme.DarkBackground
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.PurpleGlow
import com.example.cipherspend.ui.theme.TextMuted
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.TextSecondary
import com.example.cipherspend.ui.theme.ThemeManager
import com.example.cipherspend.ui.theme.glassEffect
import com.example.cipherspend.ui.viewmodel.AuthViewModel
import com.example.cipherspend.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    authViewModel: AuthViewModel,
    transactionViewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val prefs = remember { context.getSharedPreferences("CipherSpendPrefs", Context.MODE_PRIVATE) }
    var userName by remember { mutableStateOf(prefs.getString("user_name", "") ?: "") }
    var monthlyBudget by remember { mutableStateOf((prefs.getFloat("monthly_budget", 25000f)).toInt().toString()) }
    var salaryCycleDay by remember { mutableStateOf(prefs.getInt("salary_cycle_day", 1).toString()) }
    var profileImage by remember { mutableStateOf(prefs.getString("pref_profile_image", "assets/avatars/avatar1.png") ?: "assets/avatars/avatar1.png") }

    var showAvatarSheet by remember { mutableStateOf(false) }
    var showTotpGuardForMpin by remember { mutableStateOf(false) }
    var showChangeMpinDialog by remember { mutableStateOf(false) }

    var newMpin by remember { mutableStateOf("") }
    var confirmMpin by remember { mutableStateOf("") }
    var mpinError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(selectedUri)
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (originalBitmap != null) {
                        val maxDim = 512
                        val width = originalBitmap.width
                        val height = originalBitmap.height
                        val (newWidth, newHeight) = if (width > maxDim || height > maxDim) {
                            if (width > height) {
                                maxDim to ((height.toFloat() / width) * maxDim).toInt()
                            } else {
                                (((width.toFloat() / height) * maxDim).toInt()) to maxDim
                            }
                        } else {
                            width to height
                        }

                        val scaledBitmap = if (newWidth != width || newHeight != height) {
                            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                        } else {
                            originalBitmap
                        }

                        // Delete previous user_avatar_*.png files in filesDir
                        context.filesDir.listFiles { file ->
                            file.name.startsWith("user_avatar_") && file.name.endsWith(".png")
                        }?.forEach { it.delete() }

                        // Save as new PNG to persist across app restarts
                        val savedFile = File(context.filesDir, "user_avatar_${System.currentTimeMillis()}.png")
                        FileOutputStream(savedFile).use { out ->
                            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }

                        withContext(Dispatchers.Main) {
                            val newPath = "file://${savedFile.absolutePath}"
                            profileImage = newPath
                            prefs.edit().putString("pref_profile_image", newPath).apply()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        CipherSnackbarManager.showMessage("Failed to load avatar", isError = true)
                    }
                }
            }
            showAvatarSheet = false
        }
    }

    val avatarList = remember {
        (1..10).map { "assets/avatars/avatar$it.png" }
    }

    fun saveProfile() {
        if (userName.trim().isEmpty()) {
            formError = "Please enter your name"
            return
        }
        val budgetVal = monthlyBudget.toFloatOrNull()
        if (budgetVal == null || budgetVal <= 0f) {
            formError = "Please enter a valid monthly budget"
            return
        }
        val dayVal = salaryCycleDay.toIntOrNull()
        if (dayVal == null || dayVal !in 1..31) {
            formError = "Salary cycle date must be between 1 and 31"
            return
        }

        prefs.edit()
            .putString("user_name", userName.trim())
            .putFloat("monthly_budget", budgetVal)
            .putInt("salary_cycle_day", dayVal)
            .putString("pref_profile_image", profileImage)
            .apply()

        transactionViewModel.setMonthlyBudget(budgetVal)
        CipherSnackbarManager.showMessage("Profile updated successfully")
        onNavigateBack()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EDIT PROFILE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    )
                }

                TextButton(onClick = { saveProfile() }) {
                    Text(
                        text = "SAVE",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
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
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Avatar Display with Edit Overlay
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clickable { showAvatarSheet = true },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    AvatarDisplay(
                        imagePath = profileImage,
                        modifier = Modifier
                            .size(110.dp)
                            .border(2.dp, CyberCyan, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CyberCyan)
                            .border(2.dp, DarkBackground, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change Avatar",
                            tint = if (ThemeManager.isDark) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Tap avatar to change",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )

                // Personal Details Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(shape = RoundedCornerShape(22.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Account Information",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        OutlinedTextField(
                            value = userName,
                            onValueChange = {
                                userName = it
                                formError = null
                            },
                            label = { Text("Your Name") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = CyberCyan)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = CardSurface.copy(alpha = 0.6f),
                                unfocusedContainerColor = CardSurface.copy(alpha = 0.4f),
                                focusedLabelColor = CyberCyan,
                                unfocusedLabelColor = TextMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = monthlyBudget,
                            onValueChange = {
                                monthlyBudget = it.filter { ch -> ch.isDigit() || ch == '.' }
                                formError = null
                            },
                            label = { Text("Monthly Budget (₹)") },
                            leadingIcon = {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = NeonEmerald)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonEmerald,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = CardSurface.copy(alpha = 0.6f),
                                unfocusedContainerColor = CardSurface.copy(alpha = 0.4f),
                                focusedLabelColor = NeonEmerald,
                                unfocusedLabelColor = TextMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = salaryCycleDay,
                            onValueChange = {
                                salaryCycleDay = it.filter { ch -> ch.isDigit() }.take(2)
                                formError = null
                            },
                            label = { Text("Salary Cycle Day (1-31)") },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CyberCyan)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = CardSurface.copy(alpha = 0.6f),
                                unfocusedContainerColor = CardSurface.copy(alpha = 0.4f),
                                focusedLabelColor = CyberCyan,
                                unfocusedLabelColor = TextMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }

                if (formError != null) {
                    Text(
                        text = formError ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = DangerRed)
                    )
                }

                // Security & MPIN Section Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(shape = RoundedCornerShape(22.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Security & Encryption",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (ThemeManager.isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f))
                                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                                .clickable { showTotpGuardForMpin = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PurpleGlow.copy(alpha = if (ThemeManager.isDark) 0.18f else 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VpnKey,
                                        contentDescription = "MPIN",
                                        tint = PurpleGlow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Change MPIN",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Update your 4-digit Master Vault PIN",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Open",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Save Profile Button
                Button(
                    onClick = { saveProfile() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = CyberCyan.copy(alpha = if (ThemeManager.isDark) 0.4f else 0.2f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = if (ThemeManager.isDark) Color.Black else Color.White
                    )
                ) {
                    Text(
                        text = "Save Changes",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Avatar Picker Bottom Sheet
    if (showAvatarSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAvatarSheet = false },
            sheetState = sheetState,
            containerColor = CardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Select Profile Avatar",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                // 10 Preloaded Avatars
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(avatarList) { assetPath ->
                        val isSelected = profileImage == assetPath
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) CyberCyan else CardBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    profileImage = assetPath
                                    prefs.edit().putString("pref_profile_image", assetPath).apply()
                                    showAvatarSheet = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AvatarDisplay(
                                imagePath = assetPath,
                                modifier = Modifier.size(68.dp)
                            )
                        }
                    }
                }

                // Pick From Gallery Button
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleGlow.copy(alpha = if (ThemeManager.isDark) 0.25f else 0.12f),
                        contentColor = PurpleGlow
                    )
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = "Gallery")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PICK FROM GALLERY", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // MPIN Security Guard for Changing MPIN
    if (showTotpGuardForMpin) {
        HighRiskGuardDialog(
            title = "Authorize MPIN Change",
            message = "Enter your current 4-digit Master MPIN to proceed with setting a new MPIN.",
            onVerify = { pin -> authViewModel.verifyMpin(pin) },
            onDismiss = { showTotpGuardForMpin = false },
            onAuthorized = {
                showTotpGuardForMpin = false
                newMpin = ""
                confirmMpin = ""
                mpinError = null
                showChangeMpinDialog = true
            }
        )
    }

    // Change MPIN Dialog
    if (showChangeMpinDialog) {
        AlertDialog(
            onDismissRequest = { showChangeMpinDialog = false },
            title = {
                Text(
                    text = "Set New 4-Digit MPIN",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter and confirm your new 4-digit Master Vault PIN.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )

                    OutlinedTextField(
                        value = newMpin,
                        onValueChange = {
                            newMpin = it.filter { ch -> ch.isDigit() }.take(4)
                            mpinError = null
                        },
                        label = { Text("New 4-Digit MPIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = CardSurface.copy(alpha = 0.6f),
                            unfocusedContainerColor = CardSurface.copy(alpha = 0.4f),
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmMpin,
                        onValueChange = {
                            confirmMpin = it.filter { ch -> ch.isDigit() }.take(4)
                            mpinError = null
                        },
                        label = { Text("Confirm 4-Digit MPIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = CardSurface.copy(alpha = 0.6f),
                            unfocusedContainerColor = CardSurface.copy(alpha = 0.4f),
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (mpinError != null) {
                        Text(
                            text = mpinError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = DangerRed)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMpin.length != 4) {
                            mpinError = "MPIN must be exactly 4 digits"
                            return@Button
                        }
                        if (newMpin != confirmMpin) {
                            mpinError = "MPINs do not match"
                            return@Button
                        }
                        val success = authViewModel.changeMpin(newMpin)
                        if (success) {
                            showChangeMpinDialog = false
                            CipherSnackbarManager.showMessage("MPIN updated successfully")
                        } else {
                            mpinError = "Failed to update MPIN"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Update MPIN", color = if (ThemeManager.isDark) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeMpinDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardSurface
        )
    }
}

@Composable
fun AvatarDisplay(
    imagePath: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(imagePath) {
        try {
            when {
                imagePath.isNullOrBlank() -> {
                    context.assets.open("avatars/avatar1.png").use { BitmapFactory.decodeStream(it) }
                }
                imagePath.startsWith("assets/") -> {
                    val relative = imagePath.removePrefix("assets/")
                    context.assets.open(relative).use { BitmapFactory.decodeStream(it) }
                }
                imagePath.startsWith("file://") -> {
                    val filePath = Uri.parse(imagePath).path
                    if (filePath != null) {
                        BitmapFactory.decodeFile(filePath)
                    } else null
                }
                imagePath.startsWith("content://") -> {
                    context.contentResolver.openInputStream(Uri.parse(imagePath)).use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                else -> {
                    try {
                        context.assets.open(imagePath).use { BitmapFactory.decodeStream(it) }
                    } catch (e: Exception) {
                        context.assets.open("avatars/avatar1.png").use { BitmapFactory.decodeStream(it) }
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(CardSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Avatar",
                tint = CyberCyan,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
