package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccountMaster
import com.example.model.StatusItem
import com.example.ui.theme.AppPrimaryBlue

@Composable
fun StatusManagerDialog(
    customStatuses: List<StatusItem>,
    onAddStatus: (String) -> Unit,
    onRemoveStatus: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var newStatusText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val defaultStatuses = AccountMaster.defaultStatuses
    val nonDefaultCustom = customStatuses.filter { !it.isDefault }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = AppPrimaryBlue,
                        modifier = Modifier.padding(end = 8.dp).size(22.dp)
                    )
                    Text(
                        text = "Dynamic Status Settings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    )
                }
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                Text(
                    text = "Manage task statuses. Default statuses are protected; custom statuses can be added or deleted.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Standard Statuses Header
                    item {
                        Text(
                            text = "STANDARD STATUSES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    // Standard statuses cards
                    items(defaultStatuses) { statusName ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Label,
                                        contentDescription = null,
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        text = statusName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 12.5.sp,
                                            color = Color(0xFF374151),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                                Text(
                                    text = "Default",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = Color(0xFF4B5563),
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier
                                        .background(Color(0xFFE5E7EB), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // Custom Statuses Header
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "CUSTOM STATUSES (${nonDefaultCustom.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    if (nonDefaultCustom.isEmpty()) {
                        item {
                            Text(
                                text = "No custom statuses yet.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = Color(0xFF9CA3AF),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    } else {
                        items(nonDefaultCustom, key = { it.id.ifBlank { it.statusName } }) { statusItem ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDBEAFE))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Label,
                                            contentDescription = null,
                                            tint = AppPrimaryBlue,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text(
                                            text = statusItem.statusName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 12.5.sp,
                                                color = Color(0xFF1E3A8A),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemoveStatus(statusItem.statusName) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove status",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Add Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Add New Status",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151),
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newStatusText,
                            onValueChange = {
                                newStatusText = it
                                errorMessage = null
                            },
                            placeholder = { Text("e.g. Under Review", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_status_input")
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Button(
                            onClick = {
                                val trimmed = newStatusText.trim()
                                if (trimmed.isNotBlank()) {
                                    val existsInDefault = defaultStatuses.any { it.equals(trimmed, ignoreCase = true) }
                                    val existsInCustom = customStatuses.any { it.statusName.equals(trimmed, ignoreCase = true) }
                                    if (existsInDefault || existsInCustom) {
                                        errorMessage = "Status already exists"
                                    } else {
                                        onAddStatus(trimmed)
                                        newStatusText = ""
                                        errorMessage = null
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppPrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("add_status_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFDC2626),
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close", color = AppPrimaryBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
