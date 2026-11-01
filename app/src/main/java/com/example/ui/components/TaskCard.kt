package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Priority
import com.example.model.TaskItem
import com.example.ui.theme.AppCardBorder
import com.example.ui.theme.PriorityHighRed
import com.example.ui.theme.PriorityLowGreen
import com.example.ui.theme.PriorityMedYellow
import com.example.ui.theme.UnitChipBg
import com.example.ui.theme.UnitChipText

@Composable
fun TaskCard(
    task: TaskItem,
    availableStatuses: List<String>,
    onCardClick: () -> Unit,
    onStatusChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Row 1: Title and Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${task.taskNumber}. ${task.description}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color(0xFF111827)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Priority Badge (e.g. 🔴 High / 🟢 Low / 🟡 Med)
                PriorityIndicator(priority = task.priorityEnum)
            }

            // Row 2: Amount | Payment Mode
            Text(
                text = "Amount: ${task.formattedAmount} | ${task.paymentMode}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = Color(0xFF4B5563)
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
            )

            // Row 3: Factory Unit Chip & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Factory Unit Badge: [🏢 Unit-1]
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = UnitChipBg,
                    modifier = Modifier.testTag("unit_badge_${task.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = "Unit",
                            tint = UnitChipText,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.unit,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp,
                                color = UnitChipText
                            )
                        )
                    }
                }

                // Status Pill with Dropdown
                Box {
                    val (bgColor, textColor, borderColor) = getStatusColorTriple(task.status)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .clickable { statusMenuExpanded = true }
                            .testTag("status_pill_${task.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.status,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = textColor
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change Status",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        availableStatuses.forEach { statusOption ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = statusOption,
                                        fontWeight = if (statusOption == task.status) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    statusMenuExpanded = false
                                    onStatusChange(statusOption)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityIndicator(priority: Priority) {
    val (dotColor, label) = when (priority) {
        Priority.HIGH -> PriorityHighRed to "High"
        Priority.MED -> PriorityMedYellow to "Med"
        Priority.LOW -> PriorityLowGreen to "Low"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color = dotColor, shape = CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = Color(0xFF374151)
            )
        )
    }
}

fun getStatusColorTriple(status: String): Triple<Color, Color, Color> {
    return when (status.lowercase()) {
        "done" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFFA5D6A7))
        "advance ready" -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Color(0xFFFDE68A))
        "draft ready" -> Triple(Color(0xFFFEF9C3), Color(0xFF854D0E), Color(0xFFFDE047))
        "in progress" -> Triple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), Color(0xFF93C5FD))
        else -> Triple(Color(0xFFF3F4F6), Color(0xFF4B5563), Color(0xFFD1D5DB))
    }
}
