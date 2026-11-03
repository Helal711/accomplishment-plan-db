package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TaskItem
import com.example.ui.theme.AppPrimaryBlue

@Composable
fun ExportDialog(
    currentMonth: String,
    tasks: List<TaskItem>,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(12.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = AppPrimaryBlue,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Export Tasks Data ($currentMonth)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choose format to export all items in this month:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF4B5563))
                )

                Spacer(modifier = Modifier.height(4.dp))

                ExportFormatOption(
                    title = "PDF Document (.pdf)",
                    subtitle = "Complete formatted table with status colors",
                    icon = Icons.Default.PictureAsPdf,
                    iconColor = Color(0xFFD32F2F),
                    onClick = {
                        shareExportSummary(context, "PDF", currentMonth, tasks)
                        onDismissRequest()
                    }
                )

                ExportFormatOption(
                    title = "Excel Spreadsheet (.xlsx)",
                    subtitle = "Master sheet layout compatible with Excel",
                    icon = Icons.Default.TableChart,
                    iconColor = Color(0xFF2E7D32),
                    onClick = {
                        shareExportSummary(context, "Excel", currentMonth, tasks)
                        onDismissRequest()
                    }
                )

                ExportFormatOption(
                    title = "Word Document (.docx)",
                    subtitle = "Accomplishment report with notes & remarks",
                    icon = Icons.Default.Description,
                    iconColor = Color(0xFF1976D2),
                    onClick = {
                        shareExportSummary(context, "Word", currentMonth, tasks)
                        onDismissRequest()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = AppPrimaryBlue)
            }
        }
    )
}

@Composable
private fun ExportFormatOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }
    }
}

private fun shareExportSummary(context: Context, format: String, month: String, tasks: List<TaskItem>) {
    val builder = StringBuilder()
    builder.appendLine("Accomplishment Plan DB - Export ($format)")
    builder.appendLine("Month: $month | Total Items: ${tasks.size}")
    builder.appendLine("--------------------------------------------")
    tasks.forEachIndexed { index, task ->
        builder.appendLine("#${index + 1}. ${task.description}")
        builder.appendLine("Amount: ${task.formattedAmount} | ${task.paymentMode} | ${task.unit}")
        builder.appendLine("Priority: ${task.priority} | Status: ${task.status}")
        builder.appendLine("Account: ${task.accountName} (${task.accountCode})")
        if (task.remarks.isNotBlank()) {
            builder.appendLine("Remarks: ${task.remarks}")
        }
        builder.appendLine()
    }
    builder.appendLine("Developer by Md Al Helal Sarkar")

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, builder.toString())
        putExtra(Intent.EXTRA_TITLE, "Accomplishment Plan DB $month.$format")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Export $format - Accomplishment Plan")
    context.startActivity(shareIntent)
}
