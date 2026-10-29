package com.example.cipherspend.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipherspend.data.dao.CategorySpend
import com.example.cipherspend.ui.components.MonthSelector
import com.example.cipherspend.ui.components.SankeyChart
import com.example.cipherspend.ui.components.getCategoryColor
import com.example.cipherspend.ui.theme.CardBorder
import com.example.cipherspend.ui.theme.CardSurface
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.TextMuted
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.TextSecondary
import com.example.cipherspend.ui.theme.ThemeManager
import com.example.cipherspend.ui.theme.glassEffect
import com.example.cipherspend.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: TransactionViewModel,
    onNavigateToDetail: (Long) -> Unit = {}
) {
    val forecast by viewModel.forecast.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categorySpends = remember(transactions) {
        transactions.filter { !it.category.equals("Refund", ignoreCase = true) }
            .groupBy { it.category }
            .map { (cat, list) -> CategorySpend(cat, list.sumOf { it.amount }) }
            .sortedByDescending { it.total }
    }

    val totalSpent = remember(categorySpends) {
        categorySpends.sumOf { it.total }
    }
    val budget = forecast?.budget ?: 25000.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Visual Intelligence",
                        style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Spending Analytics",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Icon(
                    imageVector = Icons.Default.AutoGraph,
                    contentDescription = "Analytics",
                    tint = CyberCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Month-by-Month Selector
        item {
            MonthSelector(
                selectedMonth = selectedMonth,
                onPreviousMonth = { viewModel.selectPreviousMonth() },
                onNextMonth = { viewModel.selectNextMonth() },
                onCurrentMonth = { viewModel.selectCurrentMonth() }
            )
        }

        // Sankey Flowchart Card (Phase 2 Requirement)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(shape = RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Interactive Sankey Flow",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Source → Outflows",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (categorySpends.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "No Outflow Data Yet",
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Enable SMS tracking or add transactions to visualize your cashflow.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                        ) {
                            SankeyChart(
                                budget = budget,
                                totalSpent = totalSpent,
                                categories = categorySpends,
                                selectedCategory = selectedCategory
                            )
                        }
                    }
                }
            }
        }

        // Predictive Insights Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .glassEffect(shape = RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Projected Month-End",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${"%,.0f".format(forecast?.projected ?: (totalSpent * 1.25))}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .glassEffect(shape = RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Daily Spend Velocity",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${"%,.0f".format(forecast?.dailyAverage ?: (totalSpent / 15))}/day",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = NeonEmerald,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (categorySpends.isNotEmpty()) {
            item {
                Text(
                    text = "Category Breakdown",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(categorySpends.size) { index ->
                val cat = categorySpends[index]
                val catColor = getCategoryColor(cat.category)
                val percent = if (totalSpent > 0) (cat.total / totalSpent * 100).toInt() else 0
                val isSelected = selectedCategory == cat.category
                val catTransactions = remember(transactions, cat.category) {
                    transactions.filter { it.category.equals(cat.category, ignoreCase = true) }
                }
                val topMerchants = remember(catTransactions) {
                    catTransactions.groupBy { it.merchant }
                        .mapValues { it.value.sumOf { tx -> tx.amount } }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(3)
                }
                val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

                val itemBg = if (ThemeManager.isDark) {
                    if (isSelected) Color(0xFF1E2B40) else Color(0xFF172234)
                } else {
                    if (isSelected) catColor.copy(alpha = 0.14f) else Color.White
                }
                val itemBorder = if (isSelected) {
                    catColor.copy(alpha = 0.90f)
                } else {
                    if (ThemeManager.isDark) catColor.copy(alpha = 0.35f) else catColor.copy(alpha = 0.28f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(itemBg)
                        .border(1.dp, itemBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            selectedCategory = if (selectedCategory == cat.category) null else cat.category
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(catColor, RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                                Text(
                                    text = cat.category,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "₹${"%,.2f".format(cat.total)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                )
                                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                Text(
                                    text = "($percent%)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { (percent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = catColor,
                            trackColor = if (ThemeManager.isDark) Color.White.copy(alpha = 0.12f) else catColor.copy(alpha = 0.15f)
                        )

                        AnimatedVisibility(
                            visible = isSelected,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                if (topMerchants.isNotEmpty()) {
                                    Text(
                                        text = "TOP MERCHANTS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CyberCyan,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        topMerchants.forEach { (merchant, total) ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (ThemeManager.isDark) catColor.copy(alpha = 0.15f) else catColor.copy(alpha = 0.08f)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        catColor.copy(alpha = if (ThemeManager.isDark) 0.35f else 0.25f),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Column {
                                                    Text(
                                                        text = merchant,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.SemiBold
                                                        ),
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "₹${"%,.0f".format(total)}",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = catColor,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                Text(
                                    text = "RECENT TRANSACTIONS (${catTransactions.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontSize = 10.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    catTransactions.take(6).forEach { tx ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (ThemeManager.isDark) Color(0xFF1E2C42) else Color(0xFFF8FAFC)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (ThemeManager.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { onNavigateToDetail(tx.id) }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tx.merchant,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = dateFormat.format(Date(tx.timestamp)),
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = TextMuted,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                            Text(
                                                text = "₹${"%,.2f".format(tx.amount)}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold
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

        // Bottom clearance
        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
