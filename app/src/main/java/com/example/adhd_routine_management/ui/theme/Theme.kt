package com.example.adhd_routine_management.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

// ダークモード固定のカラースキームを定義
private val DarkColorScheme = darkColorScheme(
    primary          = PrimaryTeal,
    onPrimary        = DarkBackground,
    primaryContainer = PrimaryTealDark,
    secondary        = SecondaryBlue,
    background       = DarkBackground,
    surface          = DarkSurface,
    surfaceVariant   = DarkSurfaceVar,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error            = ErrorRed
)

@Composable
fun ADHDRoutineTheme(content: @Composable () -> Unit) {
    // forceDarkTheme: ライトモードも強制的にダークで表示
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
