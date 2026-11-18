package com.helal.accomplishmentplan.webview.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandBlue = Color(0xFF1E88E5)
val BrandBlueDark = Color(0xFF1565C0)
val BrandBlueLight = Color(0xFFDBEAFE)
val SurfaceLight = Color(0xFFF8FAFC)
val TextPrimary = Color(0xFF111827)
val TextSecondary = Color(0xFF4B5563)
val ErrorRed = Color(0xFFDC2626)

private val AppColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueLight,
    onPrimaryContainer = BrandBlueDark,
    secondary = BrandBlueDark,
    onSecondary = Color.White,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun AccomplishmentPlanTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography(),
        content = content
    )
}
