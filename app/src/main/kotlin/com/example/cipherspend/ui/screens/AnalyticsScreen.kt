package com.example.cipherspend.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipherspend.data.dao.CategorySpend
import com.example.cipherspend.ui.components.SankeyChart
import com.example.cipherspend.ui.components.getCategoryColor
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.TextMuted
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.TextSecondary
import com.example.cipherspend.ui.theme.glassEffect
import com.example.cipherspend.ui.viewmodel.TransactionViewModel

@Composable
fun AnalyticsScreen(
    viewModel: TransactionViewModel
) {
    val forecast by viewModel.forecast.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    val categorySpends = remember(allTransactions) {
        if (allTransactions.isEmpty()) {
            listOf(
                CategorySpend("Food & Dining", 4200.0),
                CategorySpend("Shopping", 2800.0),
                CategorySpend("Bills & Utilities", 1950.0),
                CategorySpend("Travel", 1200.0)
            )
        } else {
            allTransactions.groupBy { it.category }
                .map { (cat, list) -> CategorySpend(cat, list.sumOf { it.amount }) }
                .sortedByDescending { it.total }
        }
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    ) {
                        SankeyChart(
                            budget = budget,
                            totalSpent = totalSpent,
                            categories = categorySpends
                        )
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
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${"%,.0f".format(forecast?.projected ?: (totalSpent * 1.25))}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
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
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${"%,.0f".format(forecast?.dailyAverage ?: (totalSpent / 15))}/day",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = NeonEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(shape = RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                fontSize = 14.sp
                            )
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
            }
        }

        // Bottom clearance
        item {
            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}
