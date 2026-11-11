dialog_content = """package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.TaskItem
import com.example.ui.theme.AppPrimaryBlue
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
fun ExportDialog(
    currentMonth: String,
    tasks: List<TaskItem>,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

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
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = AppPrimaryBlue,
                        modifier = Modifier.padding(end = 8.dp).size(22.dp)
                    )
                    Text(
                        text = "Export Tasks Data ($currentMonth)",
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
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Export ${tasks.size} task items for $currentMonth in your preferred format:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 1. Export Excel (.xlsx)
                ExportFormatOption(
                    title = "Export Excel (.xlsx)",
                    subtitle = "Native Excel workbook with formatted columns, numbers & frozen header",
                    icon = Icons.Default.TableChart,
                    iconColor = Color(0xFF047857), // emerald-700
                    bgColor = Color(0xFFD1FAE5), // emerald-100
                    onClick = {
                        exportExcelFile(context, currentMonth, tasks)
                        onDismissRequest()
                    }
                )

                // 2. Export CSV (.csv)
                ExportFormatOption(
                    title = "Export CSV (.csv)",
                    subtitle = "UTF-8 BOM encoded with CRLF for Microsoft Excel & Google Sheets",
                    icon = Icons.Default.TableView,
                    iconColor = Color(0xFF0F766E), // teal-700
                    bgColor = Color(0xFFCCFBF1), // teal-100
                    onClick = {
                        exportCsvFile(context, currentMonth, tasks)
                        onDismissRequest()
                    }
                )

                // 3. Export PDF Document (Print/PDF)
                ExportFormatOption(
                    title = "Export PDF Document (Print/PDF)",
                    subtitle = "Generates clean report table document with header metadata",
                    icon = Icons.Default.PictureAsPdf,
                    iconColor = Color(0xFF7E22CE), // purple-700
                    bgColor = Color(0xFFF3E8FF), // purple-100
                    onClick = {
                        exportPdfFile(context, currentMonth, tasks)
                        onDismissRequest()
                    }
                )

                // 4. Formatted Text Report (.txt)
                ExportFormatOption(
                    title = "Formatted Text Report (.txt)",
                    subtitle = "Structured narrative summary with all task notes & remarks",
                    icon = Icons.Default.Description,
                    iconColor = Color(0xFF1D4ED8), // blue-700
                    bgColor = Color(0xFFDBEAFE), // blue-100
                    onClick = {
                        exportTextFile(context, currentMonth, tasks)
                        onDismissRequest()
                    }
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        val text = generateSummaryText(currentMonth, tasks)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Accomplishment Plan Summary", text)
                        clipboard.setPrimaryClip(clip)
                        copied = true
                        Toast.makeText(context, "Summary Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = if (copied) Color(0xFF16A34A) else Color(0xFF4B5563),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = if (copied) "Summary Copied!" else "Copy Summary to Clipboard",
                        color = if (copied) Color(0xFF16A34A) else Color(0xFF4B5563),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                TextButton(onClick = onDismissRequest) {
                    Text("Close", color = Color(0xFF4B5563), fontWeight = FontWeight.SemiBold)
                }
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
    bgColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .size(38.dp)
                    .background(bgColor, RoundedCornerShape(8.dp)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827),
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        lineHeight = 14.sp
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Export File Helpers Matching Web Exactly
// -------------------------------------------------------------------------------------------------

private fun parseNumericAmount(amt: String): Double {
    if (amt.isBlank()) return 0.0
    val cleaned = amt.replace(Regex("[^0-9.-]"), "")
    return cleaned.toDoubleOrNull() ?: 0.0
}

private fun escapeXml(str: String): String {
    return str.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

private fun escapeCsv(str: String?): String {
    if (str == null) return "\"\""
    return "\"" + str.replace("\"", "\"\"") + "\""
}

private fun generateSummaryText(month: String, tasks: List<TaskItem>): String {
    val builder = StringBuilder()
    builder.appendLine("Accomplishment Plan DB - Export (Summary)")
    builder.appendLine("Month: $month | Total Items: ${tasks.size}")
    builder.appendLine("--------------------------------------------")
    tasks.forEachIndexed { index, task ->
        builder.appendLine("#${index + 1}. ${task.description}")
        builder.appendLine("Amount: ${task.formattedAmount} | ${task.paymentMode} | ${task.unit}")
        builder.appendLine("Priority: ${task.priority} | Status: ${task.status}")
        builder.appendLine("Account: ${task.accountName} (${task.accountCode})")
        if (task.expectedDate.isNotBlank()) builder.appendLine("Expected Date: ${task.expectedDate}")
        if (task.remarks.isNotBlank()) builder.appendLine("Remarks: ${task.remarks}")
        builder.appendLine()
    }
    builder.appendLine("Developer by Md Al Helal Sarkar")
    return builder.toString()
}

// 1. Native Microsoft Excel (.xlsx) generator using OpenXML zip structure
private fun exportExcelFile(context: Context, month: String, tasks: List<TaskItem>) {
    try {
        val fileName = "Accomplishment_Plan_Tasks_$month.xlsx"
        val cacheFile = File(context.cacheDir, fileName)

        val colWidths = listOf(10, 45, 15, 15, 12, 12, 15, 15, 32, 15, 18, 45, 15)
        val headers = listOf(
            "Task No", "Description", "Amount (TK)", "Payment Mode", "Unit",
            "Priority", "Status", "Expected Date", "Account Name", "Account Code",
            "Category", "Remarks", "Month/Year"
        )

        val sheetDataXml = StringBuilder()
        sheetDataXml.append("<sheetData>")

        // Header Row (Row 1)
        sheetDataXml.append("<row r=\"1\">")
        headers.forEachIndexed { colIdx, header ->
            val colLetter = getColumnLetter(colIdx + 1)
            sheetDataXml.append("<c r=\"${colLetter}1\" t=\"inlineStr\" s=\"1\"><is><t>${escapeXml(header)}</t></is></c>")
        }
        sheetDataXml.append("</row>")

        // Data Rows
        tasks.forEachIndexed { rowIdx, task ->
            val r = rowIdx + 2
            val taskNo = if (task.taskNumber > 0) task.taskNumber else rowIdx + 1
            val numAmount = parseNumericAmount(task.amount)

            sheetDataXml.append("<row r=\"$r\">")
            sheetDataXml.append("<c r=\"A$r\"><v>$taskNo</v></c>")
            sheetDataXml.append("<c r=\"B$r\" t=\"inlineStr\"><is><t>${escapeXml(task.description)}</t></is></c>")
            sheetDataXml.append("<c r=\"C$r\"><v>$numAmount</v></c>")
            sheetDataXml.append("<c r=\"D$r\" t=\"inlineStr\"><is><t>${escapeXml(task.paymentMode)}</t></is></c>")
            sheetDataXml.append("<c r=\"E$r\" t=\"inlineStr\"><is><t>${escapeXml(task.unit)}</t></is></c>")
            sheetDataXml.append("<c r=\"F$r\" t=\"inlineStr\"><is><t>${escapeXml(task.priority)}</t></is></c>")
            sheetDataXml.append("<c r=\"G$r\" t=\"inlineStr\"><is><t>${escapeXml(task.status)}</t></is></c>")
            sheetDataXml.append("<c r=\"H$r\" t=\"inlineStr\"><is><t>${escapeXml(task.expectedDate)}</t></is></c>")
            sheetDataXml.append("<c r=\"I$r\" t=\"inlineStr\"><is><t>${escapeXml(task.accountName)}</t></is></c>")
            sheetDataXml.append("<c r=\"J$r\" t=\"inlineStr\"><is><t>${escapeXml(task.accountCode)}</t></is></c>")
            sheetDataXml.append("<c r=\"K$r\" t=\"inlineStr\"><is><t>${escapeXml(task.category)}</t></is></c>")
            sheetDataXml.append("<c r=\"L$r\" t=\"inlineStr\"><is><t>${escapeXml(task.remarks)}</t></is></c>")
            sheetDataXml.append("<c r=\"M$r\" t=\"inlineStr\"><is><t>${escapeXml(task.monthYear.ifBlank { month })}</t></is></c>")
            sheetDataXml.append("</row>")
        }
        sheetDataXml.append("</sheetData>")

        val colsXml = StringBuilder("<cols>")
        colWidths.forEachIndexed { i, w ->
            colsXml.append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$w\" customWidth=\"1\"/>")
        }
        colsXml.append("</cols>")

        val worksheetXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
$colsXml
<sheetViews><sheetView tabSelected="1" workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>
$sheetDataXml
</worksheet>"""

        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val wbRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

        val wbXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Accomplishment Plan" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

        val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="2">
<font><sz val="11"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><name val="Calibri"/></font>
</fonts>
<fills count="2">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="gray125"/></fill>
</fills>
<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="2">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
</cellXfs>
</styleSheet>"""

        FileOutputStream(cacheFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                fun addEntry(name: String, data: String) {
                    zos.putNextEntry(ZipEntry(name))
                    zos.write(data.toByteArray(StandardCharsets.UTF_8))
                    zos.closeEntry()
                }
                addEntry("[Content_Types].xml", contentTypesXml)
                addEntry("_rels/.rels", relsXml)
                addEntry("xl/_rels/workbook.xml.rels", wbRelsXml)
                addEntry("xl/workbook.xml", wbXml)
                addEntry("xl/styles.xml", stylesXml)
                addEntry("xl/worksheets/sheet1.xml", worksheetXml)
            }
        }

        shareFile(context, cacheFile, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fileName)
    } catch (e: Exception) {
        Toast.makeText(context, "Error exporting Excel: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// 2. UTF-8 BOM CSV generator with Windows CRLF line endings
private fun exportCsvFile(context: Context, month: String, tasks: List<TaskItem>) {
    try {
        val fileName = "Accomplishment_Plan_Tasks_$month.csv"
        val cacheFile = File(context.cacheDir, fileName)

        val headers = listOf(
            "Task No", "Description", "Amount (TK)", "Payment Mode", "Unit",
            "Priority", "Status", "Expected Date", "Account Name", "Account Code",
            "Category", "Remarks", "Month/Year"
        )

        val builder = StringBuilder()
        // UTF-8 BOM
        builder.append("\uFEFF")
        builder.append(headers.joinToString(",") { escapeCsv(it) })
        builder.append("\r\n")

        tasks.forEachIndexed { idx, task ->
            val taskNum = if (task.taskNumber > 0) task.taskNumber else idx + 1
            val numAmount = parseNumericAmount(task.amount)

            val row = listOf(
                taskNum.toString(),
                escapeCsv(task.description),
                numAmount.toString(),
                escapeCsv(task.paymentMode),
                escapeCsv(task.unit),
                escapeCsv(task.priority),
                escapeCsv(task.status),
                escapeCsv(task.expectedDate),
                escapeCsv(task.accountName),
                escapeCsv(task.accountCode),
                escapeCsv(task.category),
                escapeCsv(task.remarks),
                escapeCsv(task.monthYear.ifBlank { month })
            )
            builder.append(row.joinToString(","))
            builder.append("\r\n")
        }

        FileOutputStream(cacheFile).use { fos ->
            fos.write(builder.toString().toByteArray(StandardCharsets.UTF_8))
        }

        shareFile(context, cacheFile, "text/csv", fileName)
    } catch (e: Exception) {
        Toast.makeText(context, "Error exporting CSV: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// 3. Native PDF Report generator
private fun exportPdfFile(context: Context, month: String, tasks: List<TaskItem>) {
    try {
        val fileName = "Accomplishment_Plan_Tasks_$month.pdf"
        val cacheFile = File(context.cacheDir, fileName)

        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create() // A4 Landscape
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = android.graphics.Color.rgb(30, 136, 229)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val metaPaint = Paint().apply {
            color = android.graphics.Color.rgb(107, 114, 128)
            textSize = 10f
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = android.graphics.Color.rgb(17, 24, 39)
            textSize = 9f
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = android.graphics.Color.rgb(31, 41, 55)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = android.graphics.Color.rgb(229, 231, 235)
            strokeWidth = 1f
        }

        // Draw Header
        canvas.drawText("Accomplishment Plan Report", 30f, 35f, titlePaint)
        canvas.drawText("Master Task Accomplishments & Operational Expenses", 30f, 50f, metaPaint)

        val printDate = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        canvas.drawText("Month/Year: $month  |  Total Tasks: ${tasks.size}  |  Printed: $printDate", 450f, 40f, metaPaint)
        canvas.drawLine(30f, 62f, 812f, 62f, linePaint)

        // Draw Table Header
        var currentY = 80f
        val colPositions = floatArrayOf(30f, 55f, 220f, 280f, 335f, 380f, 425f, 490f, 560f, 670f, 740f)
        val colTitles = arrayOf("#", "Description", "Amount", "Mode", "Unit", "Priority", "Status", "Expected Date", "Account", "Code", "Category")

        colTitles.forEachIndexed { i, title ->
            canvas.drawText(title, colPositions[i], currentY, headerPaint)
        }
        currentY += 8f
        canvas.drawLine(30f, currentY, 812f, currentY, linePaint)
        currentY += 16f

        // Draw Rows (Up to page limit)
        tasks.take(22).forEachIndexed { index, task ->
            val num = if (task.taskNumber > 0) task.taskNumber.toString() else (index + 1).toString()
            canvas.drawText(num, colPositions[0], currentY, textPaint)
            canvas.drawText(task.description.take(28), colPositions[1], currentY, textPaint)
            canvas.drawText(task.amount.take(10), colPositions[2], currentY, textPaint)
            canvas.drawText(task.paymentMode.take(8), colPositions[3], currentY, textPaint)
            canvas.drawText(task.unit.take(8), colPositions[4], currentY, textPaint)
            canvas.drawText(task.priority, colPositions[5], currentY, textPaint)
            canvas.drawText(task.status.take(11), colPositions[6], currentY, textPaint)
            canvas.drawText(task.expectedDate.take(11), colPositions[7], currentY, textPaint)
            canvas.drawText(task.accountName.take(18), colPositions[8], currentY, textPaint)
            canvas.drawText(task.accountCode.take(10), colPositions[9], currentY, textPaint)
            canvas.drawText(task.category.take(12), colPositions[10], currentY, textPaint)

            currentY += 18f
        }

        // Draw Footer
        canvas.drawLine(30f, 565f, 812f, 565f, linePaint)
        canvas.drawText("Accomplishment Plan DB • Confidential Internal Report", 30f, 580f, metaPaint)
        canvas.drawText("Developer by Md Al Helal Sarkar", 640f, 580f, metaPaint)

        pdfDoc.finishPage(page)

        FileOutputStream(cacheFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()

        shareFile(context, cacheFile, "application/pdf", fileName)
    } catch (e: Exception) {
        Toast.makeText(context, "Error exporting PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// 4. Formatted Text Report (.txt) generator
private fun exportTextFile(context: Context, month: String, tasks: List<TaskItem>) {
    try {
        val fileName = "Accomplishment_Plan_Tasks_$month.txt"
        val cacheFile = File(context.cacheDir, fileName)
        val text = generateSummaryText(month, tasks)

        FileOutputStream(cacheFile).use { fos ->
            fos.write(text.toByteArray(StandardCharsets.UTF_8))
        }

        shareFile(context, cacheFile, "text/plain", fileName)
    } catch (e: Exception) {
        Toast.makeText(context, "Error exporting text report: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// Generic file sharing via FileProvider
private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(sendIntent, "Share $title").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

private fun getColumnLetter(colIndex: Int): String {
    var num = colIndex
    var str = ""
    while (num > 0) {
        val rem = (num - 1) % 26
        str = (('A'.code + rem).toChar()) + str
        num = (num - 1) / 26
    }
    return str
}
"""

with open('android_src/AccomplishmentPlanDB/app/src/main/java/com/example/ui/components/ExportDialog.kt', 'w') as f:
    f.write(dialog_content)

print("Created updated ExportDialog.kt successfully")
