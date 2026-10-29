package com.example.cipherspend.ui.theme

import androidx.compose.ui.graphics.Color

val DarkBackground: Color
    get() = if (ThemeManager.isDark) Color(0xFF090D14) else Color(0xFFF3F4F6)

val SurfaceDark: Color
    get() = if (ThemeManager.isDark) Color(0xFF111827) else Color(0xFFFFFFFF)

val SurfaceCard: Color
    get() = if (ThemeManager.isDark) Color(0x1AFFFFFF) else Color(0x0D000000)

val CardSurface: Color
    get() = if (ThemeManager.isDark) Color(0xFF131B2A) else Color(0xFFFFFFFF)

val CardBorder: Color
    get() = if (ThemeManager.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

val CyberCyan: Color
    get() = if (ThemeManager.isDark) Color(0xFF00E5FF) else Color(0xFF007A87)

val NeonEmerald: Color
    get() = if (ThemeManager.isDark) Color(0xFF00E676) else Color(0xFF008744)

val PurpleGlow: Color
    get() = if (ThemeManager.isDark) Color(0xFF7C4DFF) else Color(0xFF5E35B1)

val AmberGlow: Color
    get() = if (ThemeManager.isDark) Color(0xFFFFB300) else Color(0xFFD97706)

val DangerRed: Color
    get() = if (ThemeManager.isDark) Color(0xFFFF5252) else Color(0xFFDC2626)

val TextPrimary: Color
    get() = if (ThemeManager.isDark) Color(0xFFF3F4F6) else Color(0xFF111827)

val TextSecondary: Color
    get() = if (ThemeManager.isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563)

val TextMuted: Color
    get() = if (ThemeManager.isDark) Color(0xFF6B7280) else Color(0xFF6B7280)

val GlassWhite: Color
    get() = if (ThemeManager.isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)

val GlassBorder: Color
    get() = if (ThemeManager.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f)
