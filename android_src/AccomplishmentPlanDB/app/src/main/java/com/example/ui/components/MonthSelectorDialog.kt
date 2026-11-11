package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.theme.AppPrimaryBlue

@Composable
fun MonthSelectorDialog(
    selectedMonth: String,
    availableMonths: List<String>,
    onMonthSelected: (String) -> Unit,
    onAddNewMonth: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var isAddingNewMonth by remember { mutableStateOf(false) }
    var newMonthText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = AppPrimaryBlue,
                        modifier = Modifier.padding(end = 8.dp).size(22.dp)
                    )
                    Text(
                        text = "Select Month & Year",
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
                    .heightIn(max = 420.dp)
            ) {
                // Month List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(availableMonths) { month ->
                        val isSelected = month.equals(selectedMonth, ignoreCase = true)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMonthSelected(month)
                                    onDismissRequest()
                                },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = month,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) AppPrimaryBlue else Color(0xFF374151),
                                        fontSize = 13.5.sp
                                    )
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = AppPrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isAddingNewMonth) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Add New Month (e.g. Feb-2027)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF374151),
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newMonthText,
                            onValueChange = {
                                newMonthText = it
                                errorMessage = null
                            },
                            placeholder = { Text("Mmm-YYYY", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_month_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val trimmed = newMonthText.trim()
                                    if (trimmed.isNotBlank()) {
                                        if (availableMonths.any { it.equals(trimmed, ignoreCase = true) }) {
                                            errorMessage = "Month already exists in the list"
                                        } else {
                                            onAddNewMonth(trimmed)
                                            onMonthSelected(trimmed)
                                            newMonthText = ""
                                            isAddingNewMonth = false
                                            onDismissRequest()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppPrimaryBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    isAddingNewMonth = false
                                    errorMessage = null
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel", fontSize = 12.sp, color = Color(0xFF4B5563))
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
                } else {
                    OutlinedButton(
                        onClick = { isAddingNewMonth = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = AppPrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = "Add Custom Month",
                            color = AppPrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
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
