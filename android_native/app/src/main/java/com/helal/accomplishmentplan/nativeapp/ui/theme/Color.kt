package com.helal.accomplishmentplan.nativeapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core Modern Blue-Gradient Brand Palette
val BrandDeepNavy = Color(0xFF0B2559)
val BrandRoyalBlue = Color(0xFF1D50D0)
val BrandPrimaryBlue = Color(0xFF2563EB)
val BrandBrightAzure = Color(0xFF3B82F6)
val BrandSkyAccent = Color(0xFF38BDF8)
val BrandSoftIce = Color(0xFFEFF6FF)
val BrandSurfaceTint = Color(0xFFF4F7FC)
val BrandCardBorder = Color(0xFFE2E8F0)

// Neutral Scale
val NeutralSlate900 = Color(0xFF0F172A)
val NeutralGray900 = Color(0xFF111827)
val NeutralGray700 = Color(0xFF374151)
val NeutralGray500 = Color(0xFF6B7280)
val NeutralGray300 = Color(0xFFD1D5DB)
val NeutralGray100 = Color(0xFFF3F4F6)
val PureWhite = Color(0xFFFFFFFF)

// Dark Surface Scale
val DarkBackground = Color(0xFF0A1120)
val DarkSurface = Color(0xFF111B2E)
val DarkSurfaceVariant = Color(0xFF1A2740)
val DarkOutline = Color(0xFF2D3E5E)

// Semantic Priority Colors
val PriorityHighBg = Color(0xFFFEE2E2)
val PriorityHighText = Color(0xFFB91C1C)
val PriorityHighDot = Color(0xFFEF4444)

val PriorityMedBg = Color(0xFFFEF3C7)
val PriorityMedText = Color(0xFFB45309)
val PriorityMedDot = Color(0xFFF59E0B)

val PriorityLowBg = Color(0xFFDCFCE7)
val PriorityLowText = Color(0xFF15803D)
val PriorityLowDot = Color(0xFF22C55E)

// Semantic Feedback Colors
val FeedbackError = Color(0xFFDC2626)
val FeedbackErrorContainer = Color(0xFFFEE2E2)
val FeedbackSuccess = Color(0xFF15803D)
val FeedbackSuccessContainer = Color(0xFFDCFCE7)
val FeedbackWarning = Color(0xFFB45309)
val FeedbackWarningContainer = Color(0xFFFEF3C7)

@Immutable
data class StatusVisualStyle(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val dotColor: Color
)

/**
 * Matches the Accomplishment Plan DB Web App status color system:
 * - Explicit styles for standard statuses
 * - Deterministic FNV-1a hashed palette for custom statuses
 * - Dynamic count-aware helper: count == 0 renders plain white/neutral with #111827 text & #D1D5DB border
 */
object StatusColorPalette {

    val zeroCountNeutralStyle = StatusVisualStyle(
        containerColor = PureWhite,
        contentColor = NeutralGray900,
        borderColor = NeutralGray300,
        dotColor = NeutralGray500
    )

    val totalItemsStyle = StatusVisualStyle(
        containerColor = Color(0xFFDBEAFE),
        contentColor = Color(0xFF1E3A8A),
        borderColor = Color(0xFF93C5FD),
        dotColor = BrandPrimaryBlue
    )

    private val customPalette: List<StatusVisualStyle> = listOf(
        StatusVisualStyle(Color(0xFFE0E7FF), Color(0xFF3730A3), Color(0xFFA5B4FC), Color(0xFF4F46E5)),
        StatusVisualStyle(Color(0xFFFCE7F3), Color(0xFF9D174D), Color(0xFFF9A8D4), Color(0xFFDB2777)),
        StatusVisualStyle(Color(0xFFCCFBF1), Color(0xFF115E59), Color(0xFF5EEAD4), Color(0xFF0D9488)),
        StatusVisualStyle(Color(0xFFF3E8FF), Color(0xFF6B21A8), Color(0xFFD8B4FE), Color(0xFF9333EA)),
        StatusVisualStyle(Color(0xFFECFCCB), Color(0xFF3F6212), Color(0xFFBEF264), Color(0xFF65A30D)),
        StatusVisualStyle(Color(0xFFCFFAFE), Color(0xFF155E75), Color(0xFF67E8F9), Color(0xFF0891B2)),
        StatusVisualStyle(Color(0xFFFFE4E6), Color(0xFF9F1239), Color(0xFFFDA4AF), Color(0xFFE11D48)),
        StatusVisualStyle(Color(0xFFFEF3C7), Color(0xFF92400E), Color(0xFFFCD34D), Color(0xFFD97706)),
        StatusVisualStyle(Color(0xFFE0F2FE), Color(0xFF075985), Color(0xFF7DD3FC), Color(0xFF0284C7)),
        StatusVisualStyle(Color(0xFFF5F3FF), Color(0xFF5B21B6), Color(0xFFC4B5FD), Color(0xFF7C3AED))
    )

    fun styleForStatus(statusName: String): StatusVisualStyle {
        val normalized = statusName.trim().lowercase()
        return when (normalized) {
            "done", "done from hod", "advance done", "wo done" -> StatusVisualStyle(
                containerColor = Color(0xFFDCFCE7),
                contentColor = Color(0xFF166534),
                borderColor = Color(0xFF86EFAC),
                dotColor = Color(0xFF16A34A)
            )
            "draft ready" -> StatusVisualStyle(
                containerColor = Color(0xFFFEF9C3),
                contentColor = Color(0xFF854D0E),
                borderColor = Color(0xFFFDE047),
                dotColor = Color(0xFFCA8A04)
            )
            "advance ready" -> StatusVisualStyle(
                containerColor = Color(0xFFFEF3C7),
                contentColor = Color(0xFFB45309),
                borderColor = Color(0xFFFCD34D),
                dotColor = Color(0xFFD97706)
            )
            "pending", "payment pending" -> StatusVisualStyle(
                containerColor = Color(0xFFFFEDD5),
                contentColor = Color(0xFFC2410C),
                borderColor = Color(0xFFFDBA74),
                dotColor = Color(0xFFEA580C)
            )
            "in progress" -> StatusVisualStyle(
                containerColor = Color(0xFFDBEAFE),
                contentColor = Color(0xFF1D4ED8),
                borderColor = Color(0xFF93C5FD),
                dotColor = Color(0xFF2563EB)
            )
            "cancelled", "cancel", "no need", "unavailable" -> StatusVisualStyle(
                containerColor = Color(0xFFFEE2E2),
                contentColor = Color(0xFFB91C1C),
                borderColor = Color(0xFFFCA5A5),
                dotColor = Color(0xFFDC2626)
            )
            "hold from mr team", "hold" -> StatusVisualStyle(
                containerColor = Color(0xFFF3E8FF),
                contentColor = Color(0xFF6B21A8),
                borderColor = Color(0xFFD8B4FE),
                dotColor = Color(0xFF9333EA)
            )
            else -> {
                val hash = fnv1a32(normalized)
                val index = ((hash and 0x7FFFFFFF) % customPalette.size)
                customPalette[index]
            }
        }
    }

    /**
     * Dynamic count-aware status summary pill color:
     * - count == 0: neutral white background, dark text (#111827), subtle border (#D1D5DB)
     * - count >= 1: original status-specific colors
     */
    fun summaryPillStyle(statusName: String, count: Int): StatusVisualStyle {
        return if (count <= 0) zeroCountNeutralStyle else styleForStatus(statusName)
    }

    internal fun fnv1a32(input: String): Int {
        var hash = 0x811c9dc5.toInt()
        for (ch in input) {
            hash = hash xor ch.code
            hash *= 0x01000193
        }
        return hash
    }
}

object BrandGradients {
    val primaryHeaderBrush: Brush
        get() = Brush.linearGradient(
            colors = listOf(BrandDeepNavy, BrandRoyalBlue, BrandBrightAzure)
        )

    val subtleAccentBrush: Brush
        get() = Brush.horizontalGradient(
            colors = listOf(BrandRoyalBlue, BrandPrimaryBlue, BrandSkyAccent)
        )
}
