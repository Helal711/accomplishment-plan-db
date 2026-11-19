package com.helal.accomplishmentplan.nativeapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandRoyalBlue,
    onPrimary = PureWhite,
    primaryContainer = BrandSoftIce,
    onPrimaryContainer = BrandDeepNavy,
    secondary = BrandPrimaryBlue,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = BrandSkyAccent,
    onTertiary = BrandDeepNavy,
    background = BrandSurfaceTint,
    onBackground = NeutralSlate900,
    surface = PureWhite,
    onSurface = NeutralSlate900,
    surfaceVariant = Color(0xFFEEF2F9),
    onSurfaceVariant = NeutralGray700,
    outline = BrandCardBorder,
    outlineVariant = Color(0xFFE5E7EB),
    error = FeedbackError,
    onError = PureWhite,
    errorContainer = FeedbackErrorContainer,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandBrightAzure,
    onPrimary = PureWhite,
    primaryContainer = BrandRoyalBlue,
    onPrimaryContainer = BrandSoftIce,
    secondary = BrandSkyAccent,
    onSecondary = BrandDeepNavy,
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = Color(0xFF7DD3FC),
    onTertiary = BrandDeepNavy,
    background = DarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = DarkOutline,
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

@Immutable
data class ExtendedThemeColors(
    val gradientStart: Color,
    val gradientMiddle: Color,
    val gradientEnd: Color,
    val cardBorder: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedThemeColors(
        gradientStart = BrandDeepNavy,
        gradientMiddle = BrandRoyalBlue,
        gradientEnd = BrandBrightAzure,
        cardBorder = BrandCardBorder,
        success = FeedbackSuccess,
        successContainer = FeedbackSuccessContainer,
        warning = FeedbackWarning,
        warningContainer = FeedbackWarningContainer
    )
}

@Composable
fun AccomplishmentPlanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        ExtendedThemeColors(
            gradientStart = Color(0xFF081B45),
            gradientMiddle = Color(0xFF153D9E),
            gradientEnd = Color(0xFF1E56C5),
            cardBorder = DarkOutline,
            success = Color(0xFF4ADE80),
            successContainer = Color(0xFF14532D),
            warning = Color(0xFFFBBF24),
            warningContainer = Color(0xFF78350F)
        )
    } else {
        ExtendedThemeColors(
            gradientStart = BrandDeepNavy,
            gradientMiddle = BrandRoyalBlue,
            gradientEnd = BrandBrightAzure,
            cardBorder = BrandCardBorder,
            success = FeedbackSuccess,
            successContainer = FeedbackSuccessContainer,
            warning = FeedbackWarning,
            warningContainer = FeedbackWarningContainer
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
