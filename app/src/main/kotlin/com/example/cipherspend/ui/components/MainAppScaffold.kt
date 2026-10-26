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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.cipherspend.ui.screens.AnalyticsScreen
import com.example.cipherspend.ui.screens.DashboardScreen
import com.example.cipherspend.ui.screens.SearchScreen
import com.example.cipherspend.ui.screens.SettingsScreen
import com.example.cipherspend.ui.theme.AmbientGlowBackdrop
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DarkBackground
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.SurfaceDark
import com.example.cipherspend.ui.theme.TextMuted
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.bounceClick
import com.example.cipherspend.ui.theme.glassEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.cipherspend.ui.viewmodel.AuthViewModel
import com.example.cipherspend.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch

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
    onLockVault: () -> Unit,
    onNavigateToManualEntry: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfileEdit: () -> Unit = {},
    onNuclearWipeComplete: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

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
                            isSelected = pagerState.currentPage == 0,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )

                        // 2. Analytics Tab
                        BottomNavTab(
                            item = TabItem.Analytics,
                            isSelected = pagerState.currentPage == 1,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
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
                                    .bounceClick()
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
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onNavigateToManualEntry()
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
                            isSelected = pagerState.currentPage == 2,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(2)
                                }
                            }
                        )

                        // 5. Settings Tab
                        BottomNavTab(
                            item = TabItem.Settings,
                            isSelected = pagerState.currentPage == 3,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(3)
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
                .statusBarsPadding()
        ) {
            AmbientGlowBackdrop()

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> DashboardScreen(
                        viewModel = transactionViewModel,
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onNavigateToProfileEdit = onNavigateToProfileEdit,
                        onNavigateToManualEntry = onNavigateToManualEntry
                    )
                    1 -> AnalyticsScreen(
                        viewModel = transactionViewModel,
                        onNavigateToDetail = onNavigateToDetail
                    )
                    2 -> SearchScreen(
                        viewModel = transactionViewModel,
                        onNavigateToDetail = onNavigateToDetail
                    )
                    3 -> SettingsScreen(
                        authViewModel = authViewModel,
                        transactionViewModel = transactionViewModel,
                        onLockVault = onLockVault,
                        onNuclearWipeComplete = onNuclearWipeComplete
                    )
                }
            }
        }
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
