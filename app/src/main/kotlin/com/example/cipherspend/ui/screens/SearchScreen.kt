package com.example.cipherspend.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cipherspend.data.model.TransactionEntity
import com.example.cipherspend.ui.components.CipherSnackbarManager
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.PurpleGlow
import com.example.cipherspend.ui.theme.TextMuted
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.TextSecondary
import com.example.cipherspend.ui.theme.bounceClick
import com.example.cipherspend.ui.theme.glassEffect
import com.example.cipherspend.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: TransactionViewModel,
    onNavigateToDetail: (Long) -> Unit
) {
    val context = LocalContext.current

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCat by viewModel.selectedCategoryFilter.collectAsState()
    val selectedType by viewModel.selectedTypeFilter.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val filterChips = listOf("All", "Food & Dining", "Shopping", "Bills & Utilities", "Travel", "Investment", "Cash", "Transaction", "Others")
    val typeChips = listOf("All", "UPI", "Card", "Cash", "Bank Transfer")

    var activeFilterCount = 0
    if (selectedCat != null && selectedCat != "All") activeFilterCount++
    if (selectedType != null && selectedType != "All") activeFilterCount++
    if (startDate != null || endDate != null) activeFilterCount++

    fun escapeCsv(s: String): String {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"${s.replace("\"", "\"\"")}\""
        }
        return s
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = outputStream.bufferedWriter()
                writer.write("Date,Merchant,Amount,Category,Type,Hash,Sender\n")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                searchResults.forEach { tx ->
                    val dateStr = dateFormat.format(Date(tx.timestamp))
                    val merchantClean = escapeCsv(tx.merchant)
                    val categoryClean = escapeCsv(tx.category)
                    val typeClean = escapeCsv(tx.type)
                    val hashClean = escapeCsv(tx.hash)
                    val senderClean = escapeCsv(tx.sender)
                    writer.write("$dateStr,$merchantClean,${tx.amount},$categoryClean,$typeClean,$hashClean,$senderClean\n")
                }
                writer.flush()
            }
            CipherSnackbarManager.showMessage("Exported ${searchResults.size} transactions to CSV")
        } catch (e: Exception) {
            CipherSnackbarManager.showMessage("Export failed: ${e.message}", isError = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Header Row with Export CSV Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Zero-Trace Search",
                    style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Search & Filter",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Export CSV Button
            Box(
                modifier = Modifier
                    .glassEffect(shape = CircleShape, backgroundColor = Color(0xFF131B2A))
                    .clickable {
                        if (searchResults.isEmpty()) {
                            CipherSnackbarManager.showMessage("No transactions found to export", isError = true)
                        } else {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            exportLauncher.launch("CipherSpend_Export_$timestamp.csv")
                        }
                    }
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Export CSV",
                    tint = CyberCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar & Filter Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search merchants, notes, banks...", color = TextMuted) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyberCyan)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = Color(0xFF131B2A).copy(alpha = 0.8f),
                    unfocusedContainerColor = Color(0xFF131B2A).copy(alpha = 0.6f)
                )
            )

            // Filter Button with Active Filter Count Badge
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .glassEffect(shape = RoundedCornerShape(16.dp), backgroundColor = Color(0xFF131B2A))
                    .clickable { showFilterDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = if (activeFilterCount > 0) CyberCyan else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )

                if (activeFilterCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(CyberCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeFilterCount.toString(),
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Category Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filterChips) { chip ->
                val isSelected = (selectedCat == null && chip == "All") || (selectedCat == chip)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (isSelected) CyberCyan else Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            viewModel.onCategoryFilterChanged(if (chip == "All") null else chip)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isSelected) CyberCyan else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        val hasTypeFilter = !selectedType.isNullOrBlank() && selectedType != "All"
        val hasDateFilter = startDate != null || endDate != null

        if (hasTypeFilter || hasDateFilter) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasTypeFilter) {
                    Box(
                        modifier = Modifier
                            .glassEffect(shape = RoundedCornerShape(10.dp), backgroundColor = Color(0xFF131B2A))
                            .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable { viewModel.setTypeFilter(null) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Method: $selectedType ✕",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                if (hasDateFilter) {
                    Box(
                        modifier = Modifier
                            .glassEffect(shape = RoundedCornerShape(10.dp), backgroundColor = Color(0xFF131B2A))
                            .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable { viewModel.setDateRange(null, null) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Date Range Active ✕",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Results Section
        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank() || activeFilterCount > 0) {
                        "No matching encrypted transactions found."
                    } else {
                        "Type or apply filters to search encrypted records."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${searchResults.size} transaction(s)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "Total: ₹${"%,.2f".format(searchResults.sumOf { it.amount })}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(searchResults, key = { it.id }) { tx ->
                    TransactionRowItem(
                        transaction = tx,
                        onClick = { onNavigateToDetail(tx.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(110.dp))
                }
            }
        }
    }

    // Glassmorphic Filter Dialog
    if (showFilterDialog) {
        Dialog(
            onDismissRequest = { showFilterDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .glassEffect(
                        shape = RoundedCornerShape(24.dp),
                        backgroundColor = Color(0xFF0F1726).copy(alpha = 0.96f),
                        borderColor = CyberCyan.copy(alpha = 0.35f),
                        borderWidth = 1.2.dp
                    )
                    .padding(22.dp)
            ) {
                val dialogScrollState = rememberScrollState()
                Column(
                    modifier = Modifier.verticalScroll(dialogScrollState),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter Transactions",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        TextButton(onClick = {
                            viewModel.clearFilters()
                        }) {
                            Text("Reset", color = CyberCyan, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Date Range Filter Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "DATE RANGE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        val dateDisplay = remember(startDate, endDate) {
                            if (startDate != null && endDate != null) {
                                val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                "${df.format(Date(startDate!!))} - ${df.format(Date(endDate!!))}"
                            } else {
                                "All Dates"
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showDateRangePicker = true }
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dateDisplay,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                                )
                            }

                            if (startDate != null || endDate != null) {
                                TextButton(onClick = { viewModel.setDateRange(null, null) }) {
                                    Text("Clear Dates", color = NeonEmerald, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Payment Type Filter Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PAYMENT METHOD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(typeChips) { type ->
                                val isSelected = (selectedType == null && type == "All") || (selectedType == type)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) PurpleGlow.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                                        .border(
                                            1.dp,
                                            if (isSelected) PurpleGlow else Color.White.copy(alpha = 0.12f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            viewModel.setTypeFilter(if (type == "All") null else type)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = type,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isSelected) PurpleGlow else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Category Filter Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "CATEGORY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filterChips) { cat ->
                                val isSelected = (selectedCat == null && cat == "All") || (selectedCat == cat)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                        .border(
                                            1.dp,
                                            if (isSelected) CyberCyan else Color.White.copy(alpha = 0.12f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            viewModel.onCategoryFilterChanged(if (cat == "All") null else cat)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
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

                    // Apply Button
                    Button(
                        onClick = { showFilterDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bounceClick(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                    ) {
                        Text("Apply Filters", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Material3 Date Range Picker Dialog
    if (showDateRangePicker) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate,
            initialSelectedEndDateMillis = endDate
        )

        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setDateRange(
                            state.selectedStartDateMillis,
                            state.selectedEndDateMillis
                        )
                        showDateRangePicker = false
                    }
                ) {
                    Text("Confirm", color = CyberCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFF0F1726)
            )
        ) {
            DateRangePicker(
                state = state,
                colors = DatePickerDefaults.colors(
                    containerColor = Color(0xFF0F1726),
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextSecondary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = CyberCyan,
                    selectedYearContentColor = Color.Black,
                    selectedYearContainerColor = CyberCyan,
                    dayContentColor = TextPrimary,
                    selectedDayContentColor = Color.Black,
                    selectedDayContainerColor = CyberCyan,
                    todayContentColor = CyberCyan,
                    todayDateBorderColor = CyberCyan
                )
            )
        }
    }
}
