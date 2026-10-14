package com.example.cipherspend.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.cipherspend.data.dao.CategorySpend
import com.example.cipherspend.ui.theme.AmberGlow
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DangerRed
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.PurpleGlow

@Composable
fun SankeyChart(
    budget: Double,
    totalSpent: Double,
    categories: List<CategorySpend>,
    modifier: Modifier = Modifier,
    selectedCategory: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sankey_particles")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_flow"
    )

    Canvas(
        modifier = modifier
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        val leftX = 8f
        val barWidth = 14f
        val rightX = width - 8f - barWidth
        val totalFlowHeight = height * 0.82f
        val startY = (height - totalFlowHeight) / 2f

        // Draw Source Budget Node (Left)
        drawRoundRect(
            color = CyberCyan,
            topLeft = Offset(leftX, startY),
            size = Size(barWidth, totalFlowHeight),
            cornerRadius = CornerRadius(8f, 8f)
        )

        val activeCategories = if (categories.isEmpty()) {
            listOf(
                CategorySpend("Food & Dining", 4200.0),
                CategorySpend("Shopping", 2800.0),
                CategorySpend("Bills & Utilities", 1950.0),
                CategorySpend("Travel", 1200.0)
            )
        } else {
            categories.take(6) // Limit to top 6 to prevent visual clutter
        }

        val effectiveSpent = if (totalSpent > 0) totalSpent else activeCategories.sumOf { it.total }
        val count = activeCategories.size
        val interItemSpacing = 8f
        val totalSpacing = interItemSpacing * (count - 1).coerceAtLeast(0)
        val usableFlowHeight = (totalFlowHeight - totalSpacing).coerceAtLeast(40f)

        var currentRightY = startY
        var currentSourceY = startY

        val pathMeasure = PathMeasure()

        for (item in activeCategories) {
            val proportion = (item.total / (if (effectiveSpent > 0) effectiveSpent else 1.0)).toFloat()
            val catHeight = (usableFlowHeight * proportion).coerceAtLeast(14f)
            val sourceAnchorHeight = (usableFlowHeight * proportion).coerceAtLeast(8f)
            val color = getCategoryColor(item.category)
            val isSelected = selectedCategory != null && item.category.equals(selectedCategory, ignoreCase = true)

            // Right Category Node
            drawRoundRect(
                color = color,
                topLeft = Offset(rightX, currentRightY),
                size = Size(barWidth, catHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Bezier Flow Path
            val path = Path()
            val startPointX = leftX + barWidth
            val startPointY = currentSourceY + (sourceAnchorHeight / 2f)
            val endPointX = rightX
            val endPointY = currentRightY + (catHeight / 2f)
            val tension = (endPointX - startPointX) * 0.5f

            path.moveTo(startPointX, startPointY)
            path.cubicTo(
                startPointX + tension, startPointY,
                endPointX - tension, endPointY,
                endPointX, endPointY
            )

            // Stream Opacity based on selection
            val streamAlpha = when {
                selectedCategory == null -> 0.16f
                isSelected -> 0.45f
                else -> 0.06f
            }

            drawPath(
                path = path,
                color = color.copy(alpha = streamAlpha),
                style = Stroke(width = (catHeight * 0.75f).coerceIn(4f, 32f))
            )

            // Glowing Particles along path
            pathMeasure.setPath(path, false)
            val pathLength = pathMeasure.length
            val showParticles = selectedCategory == null || isSelected
            if (showParticles && pathLength > 0f) {
                val numParticles = 3
                for (i in 0 until numParticles) {
                    val phase = i.toFloat() / numParticles
                    val prog = (animationProgress + phase) % 1f
                    val pos = pathMeasure.getPosition(pathLength * prog)

                    // Outer halo glow
                    drawCircle(
                        color = color.copy(alpha = 0.35f),
                        radius = (catHeight * 0.22f).coerceIn(3.5f, 9f),
                        center = pos
                    )
                    // Inner bright nucleus
                    drawCircle(
                        color = Color.White.copy(alpha = 0.92f),
                        radius = (catHeight * 0.10f).coerceIn(2f, 4.5f),
                        center = pos
                    )
                }
            }

            currentRightY += catHeight + interItemSpacing
            currentSourceY += sourceAnchorHeight
        }
    }
}

fun getCategoryColor(category: String): Color {
    val lower = category.lowercase()
    return when {
        lower.contains("food") -> NeonEmerald
        lower.contains("travel") -> Color(0xFF38BDF8)
        lower.contains("shop") -> AmberGlow
        lower.contains("bill") -> DangerRed
        lower.contains("invest") -> PurpleGlow
        lower.contains("refund") -> Color(0xFF2DD4BF)
        lower.contains("cash") -> Color(0xFFFB923C)
        else -> CyberCyan
    }
}
