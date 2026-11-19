package com.helal.accomplishmentplan.nativeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.helal.accomplishmentplan.nativeapp.ui.theme.LocalExtendedColors
import com.helal.accomplishmentplan.nativeapp.ui.theme.PureWhite
import com.helal.accomplishmentplan.nativeapp.ui.theme.StatusColorPalette

/**
 * Reusable modern blue-gradient hero/dashboard header inspired by the Accomplishment Plan DB
 * visual direction, with full status-bar edge-to-edge inset support.
 */
@Composable
fun BlueGradientHeader(
    modifier: Modifier = Modifier,
    bottomRoundedCorners: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val extended = LocalExtendedColors.current
    val shape = if (bottomRoundedCorners) {
        RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    } else {
        RoundedCornerShape(0.dp)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        extended.gradientStart,
                        extended.gradientMiddle,
                        extended.gradientEnd
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Compact glassmorphic metric pill for header KPIs and status indicators.
 */
@Composable
fun SummaryGlassChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = PureWhite.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PureWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Dynamic status filter pill with Web App count-based color parity:
 * - count == 0: neutral white surface + dark text (#111827) + subtle border (#D1D5DB)
 * - count >= 1: status-specific fill, border, dot, and text colors
 * - isTotal == true: always retains Total Items primary blue identity
 * Enforces M3 48dp minimum interactive touch target while keeping the visible pill compact.
 */
@Composable
fun DynamicStatusFilterPill(
    label: String,
    count: Int,
    selected: Boolean,
    isTotal: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = when {
        isTotal -> StatusColorPalette.totalItemsStyle
        else -> StatusColorPalette.summaryPillStyle(label, count)
    }
    val borderWidth = if (selected) 2.dp else 1.dp
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else style.borderColor

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = style.containerColor,
            border = BorderStroke(borderWidth, borderColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(style.dotColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = style.contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = style.contentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = style.contentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GradientPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val extended = LocalExtendedColors.current
    val gradient = Brush.horizontalGradient(
        colors = if (enabled && !loading) {
            listOf(extended.gradientStart, extended.gradientMiddle, extended.gradientEnd)
        } else {
            listOf(Color(0xFF94A3B8), Color(0xFF94A3B8))
        }
    )

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = PureWhite,
            disabledContentColor = PureWhite.copy(alpha = 0.85f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(gradient, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = PureWhite,
                    strokeWidth = 2.4.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )
                }
            }
        }
    }
}

@Composable
fun InfoBannerCard(
    title: String,
    message: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}
