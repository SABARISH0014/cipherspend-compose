package com.example.cipherspend.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DangerRed
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.glassEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SnackbarType {
    SUCCESS,
    ERROR,
    INFO
}

data class CipherSnackbarData(
    val message: String,
    val type: SnackbarType = SnackbarType.SUCCESS,
    val durationMillis: Long = 2500L,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val id: Long = System.currentTimeMillis()
)

/**
 * Global manager for app-wide glassmorphic notification pills.
 * Replaces native Android Toasts with smooth, modern in-app snackbars.
 */
object CipherSnackbarManager {
    private val _currentSnackbar = MutableStateFlow<CipherSnackbarData?>(null)
    val currentSnackbar: StateFlow<CipherSnackbarData?> = _currentSnackbar.asStateFlow()

    private var autoDismissJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun showMessage(
        message: String,
        isError: Boolean = false,
        durationMillis: Long = 2500L,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        val type = if (isError) SnackbarType.ERROR else SnackbarType.SUCCESS
        show(
            CipherSnackbarData(
                message = message,
                type = type,
                durationMillis = durationMillis,
                actionLabel = actionLabel,
                onAction = onAction,
                id = System.currentTimeMillis()
            )
        )
    }

    fun show(data: CipherSnackbarData) {
        autoDismissJob?.cancel()
        _currentSnackbar.value = data
        autoDismissJob = scope.launch {
            delay(data.durationMillis)
            if (_currentSnackbar.value?.id == data.id) {
                _currentSnackbar.value = null
            }
        }
    }

    fun dismiss() {
        autoDismissJob?.cancel()
        _currentSnackbar.value = null
    }
}

/**
 * Top-aligned glassmorphic snackbar host that animates in from the top
 * and auto-dismisses with a smooth fade.
 */
@Composable
fun CipherSnackbarHost(
    modifier: Modifier = Modifier
) {
    val snackbarData by CipherSnackbarManager.currentSnackbar.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 16.dp, start = 20.dp, end = 20.dp)
            .zIndex(100f),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = snackbarData != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 350)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut(animationSpec = tween(durationMillis = 250))
        ) {
            val current = snackbarData
            if (current != null) {
                val icon = when (current.type) {
                    SnackbarType.SUCCESS -> Icons.Default.CheckCircle
                    SnackbarType.ERROR -> Icons.Default.Error
                    SnackbarType.INFO -> Icons.Default.Info
                }
                val tint = when (current.type) {
                    SnackbarType.SUCCESS -> NeonEmerald
                    SnackbarType.ERROR -> DangerRed
                    SnackbarType.INFO -> CyberCyan
                }

                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .glassEffect(
                            shape = RoundedCornerShape(24.dp),
                            backgroundColor = Color(0xFF0F1726).copy(alpha = 0.94f),
                            borderColor = tint.copy(alpha = 0.45f),
                            borderWidth = 1.dp
                        )
                        .padding(horizontal = 18.dp, vertical = 11.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = current.message,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                        )
                        if (!current.actionLabel.isNullOrBlank() && current.onAction != null) {
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = current.actionLabel,
                                modifier = Modifier
                                    .clickable {
                                        val action = current.onAction
                                        CipherSnackbarManager.dismiss()
                                        action()
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = CyberCyan,
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
}
