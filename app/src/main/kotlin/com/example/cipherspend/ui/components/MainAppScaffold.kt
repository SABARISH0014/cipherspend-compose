package com.example.cipherspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cipherspend.ui.screens.AnalyticsScreen
import com.example.cipherspend.ui.screens.DashboardScreen
import com.example.cipherspend.ui.screens.SearchScreen
import com.example.cipherspend.ui.screens.SettingsScreen
import com.example.cipherspend.ui.screens.TransactionDetailScreen
import com.example.cipherspend.ui.theme.AmbientGlowBackdrop
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DarkBackground
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.SurfaceDark
import com.example.cipherspend.ui.theme.TextMuted
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.glassEffect
import com.example.cipherspend.ui.viewmodel.AuthViewModel
import com.example.cipherspend.ui.viewmodel.TransactionViewModel

sealed class TabItem(val route: String, val title: String, val icon: ImageVector) {
    object Transactions : TabItem("transactions", "History", Icons.AutoMirrored.Filled.ReceiptLong)
    object Analytics : TabItem("analytics", "Analytics", Icons.Default.Analytics)
    object Search : TabItem("search", "Search", Icons.Default.Search)
    object Settings : TabItem("settings", "Vault", Icons.Default.Settings)
}

@Composable
fun MainAppScaffold(
    authViewModel: AuthViewModel,
    transactionViewModel: TransactionViewModel,
    onLockVault: () -> Unit
) {
    val navController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showManualEntryDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            // Symmetrical 5-Element Bottom Bar (Zero Overlap, Ergonomic HCI Standard)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(
                            shape = RoundedCornerShape(26.dp),
                            backgroundColor = Color(0xFF0F1726).copy(alpha = 0.92f),
                            borderColor = Color.White.copy(alpha = 0.15f)
                        )
                        .height(68.dp)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Transactions Tab
                        BottomNavTab(
                            item = TabItem.Transactions,
                            isSelected = selectedTab == 0,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedTab = 0
                                navController.navigate(TabItem.Transactions.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )

                        // 2. Analytics Tab
                        BottomNavTab(
                            item = TabItem.Analytics,
                            isSelected = selectedTab == 1,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedTab = 1
                                navController.navigate(TabItem.Analytics.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )

                        // 3. Central Manual Entry FAB (Integrated, zero-overlap)
                        Box(
                            modifier = Modifier
                                .weight(1.1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .shadow(elevation = 10.dp, shape = CircleShape, spotColor = CyberCyan)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            listOf(CyberCyan, NeonEmerald)
                                        )
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = Color.White)
                                    ) {
                                        showManualEntryDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Manual Entry",
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        // 4. Search Tab
                        BottomNavTab(
                            item = TabItem.Search,
                            isSelected = selectedTab == 2,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedTab = 2
                                navController.navigate(TabItem.Search.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )

                        // 5. Settings Tab
                        BottomNavTab(
                            item = TabItem.Settings,
                            isSelected = selectedTab == 3,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedTab = 3
                                navController.navigate(TabItem.Settings.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
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

            NavHost(
                navController = navController,
                startDestination = TabItem.Transactions.route
            ) {
                composable(TabItem.Transactions.route) {
                    selectedTab = 0
                    DashboardScreen(
                        viewModel = transactionViewModel,
                        onNavigateToDetail = { id ->
                            navController.navigate("detail/$id")
                        }
                    )
                }
                composable(TabItem.Analytics.route) {
                    selectedTab = 1
                    AnalyticsScreen(viewModel = transactionViewModel)
                }
                composable(TabItem.Search.route) {
                    selectedTab = 2
                    SearchScreen(
                        viewModel = transactionViewModel,
                        onNavigateToDetail = { id ->
                            navController.navigate("detail/$id")
                        }
                    )
                }
                composable(TabItem.Settings.route) {
                    selectedTab = 3
                    SettingsScreen(
                        authViewModel = authViewModel,
                        transactionViewModel = transactionViewModel,
                        onLockVault = onLockVault
                    )
                }
                composable(
                    route = "detail/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { backStackEntry ->
                    val txId = backStackEntry.arguments?.getLong("id") ?: 0L
                    TransactionDetailScreen(
                        transactionId = txId,
                        viewModel = transactionViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    if (showManualEntryDialog) {
        ManualEntryDialog(
            onDismiss = { showManualEntryDialog = false },
            onSave = { amount, merchant, category, type ->
                transactionViewModel.insertManualTransaction(
                    amount = amount,
                    merchant = merchant,
                    category = category,
                    type = type
                )
                showManualEntryDialog = false
            }
        )
    }
}

@Composable
private fun BottomNavTab(
    item: TabItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = CyberCyan.copy(alpha = 0.2f))
            ) { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (isSelected) CyberCyan else TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.title,
            color = if (isSelected) CyberCyan else TextMuted,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
