package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(12.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = AppPrimaryBlue,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Select Month Sheet",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                availableMonths.forEach { month ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMonthSelected(month)
                                onDismissRequest()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (month == selectedMonth),
                            onClick = {
                                onMonthSelected(month)
                                onDismissRequest()
                            }
                        )
                        Text(
                            text = month,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (month == selectedMonth) FontWeight.Bold else FontWeight.Normal,
                                color = if (month == selectedMonth) AppPrimaryBlue else Color(0xFF374151)
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isAddingNewMonth) {
                    OutlinedTextField(
                        value = newMonthText,
                        onValueChange = { newMonthText = it },
                        label = { Text("New Month (e.g. Feb-2027)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_month_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newMonthText.isNotBlank()) {
                                onAddNewMonth(newMonthText.trim())
                                onDismissRequest()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Blank Sheet")
                    }
                } else {
                    TextButton(
                        onClick = { isAddingNewMonth = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = AppPrimaryBlue)
                        Text(
                            text = "Add Next Month Sheet",
                            color = AppPrimaryBlue,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close", color = AppPrimaryBlue)
            }
        }
    )
}
