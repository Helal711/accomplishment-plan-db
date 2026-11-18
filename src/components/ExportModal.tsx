import React, { useState } from 'react';
import { Download, FileSpreadsheet, FileText, Printer, X, Check, Copy, Table } from 'lucide-react';
import * as XLSX from 'xlsx';
import { TaskItem } from '../types';

interface ExportModalProps {
  isOpen: boolean;
  currentMonth: string;
  tasks: TaskItem[];
  onClose: () => void;
}

export const ExportModal: React.FC<ExportModalProps> = ({
  isOpen,
  currentMonth,
  tasks,
  onClose,
}) => {
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  // Helper to parse numeric amount for Excel and calculations
  const parseNumericAmount = (amt: string | number | undefined): number => {
    if (typeof amt === 'number') return Number.isFinite(amt) ? amt : 0;
    if (!amt) return 0;
    const cleaned = String(amt).replace(/[^0-9.-]+/g, '');
    const parsed = parseFloat(cleaned);
    return isNaN(parsed) ? 0 : parsed;
  };

  // 1. Export real Microsoft Excel (.xlsx) workbook using SheetJS
  const handleExportExcel = () => {
    try {
      const excelRows = tasks.map((t, idx) => {
        const taskNum = typeof t.taskNumber === 'number' && Number.isFinite(t.taskNumber) && t.taskNumber > 0
          ? t.taskNumber
          : idx + 1;
        const numericAmount = parseNumericAmount(t.amount);

        return {
          'Task No': taskNum,
          'Description': t.description || '',
          'Amount (TK)': numericAmount,
          'Payment Mode': t.paymentMode || '',
          'Unit': t.unit || '',
          'Priority': t.priority || '',
          'Status': t.status || '',
          'Expected Date': t.expectedDate || '',
          'Account Name': t.accountName || '',
          'Account Code': t.accountCode || '',
          'Category': t.category || '',
          'Remarks': t.remarks || '',
          'Month/Year': t.monthYear || currentMonth,
        };
      });

      const ws = XLSX.utils.json_to_sheet(excelRows);

      // Explicit column widths for professional readability
      ws['!cols'] = [
        { wch: 10 }, // Task No
        { wch: 45 }, // Description
        { wch: 15 }, // Amount (TK)
        { wch: 15 }, // Payment Mode
        { wch: 12 }, // Unit
        { wch: 12 }, // Priority
        { wch: 15 }, // Status
        { wch: 15 }, // Expected Date
        { wch: 32 }, // Account Name
        { wch: 15 }, // Account Code
        { wch: 18 }, // Category
        { wch: 45 }, // Remarks
        { wch: 15 }, // Month/Year
      ];

      // Freeze top header row so it stays visible while scrolling in Microsoft Excel
      ws['!views'] = [{ state: 'frozen', ySplit: 1 }];

      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, 'Accomplishment Plan');

      // Generate binary array and trigger download with proper MIME type
      const excelBuffer = XLSX.write(wb, { bookType: 'xlsx', type: 'array' });
      const blob = new Blob([excelBuffer], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `Accomplishment_Plan_Tasks_${currentMonth}.xlsx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      onClose();
    } catch (err) {
      console.error('Error generating Excel file:', err);
      alert('Failed to generate Excel file. Please try again.');
    }
  };

  // 2. Export UTF-8 BOM encoded CSV with Windows-compatible CRLF line endings
  const handleExportCSV = () => {
    try {
      const headers = [
        'Task No',
        'Description',
        'Amount (TK)',
        'Payment Mode',
        'Unit',
        'Priority',
        'Status',
        'Expected Date',
        'Account Name',
        'Account Code',
        'Category',
        'Remarks',
        'Month/Year',
      ];

      const escapeCsv = (val: string | number | undefined | null) => {
        if (val === null || val === undefined) return '""';
        const str = String(val).replace(/"/g, '""');
        return `"${str}"`;
      };

      const rows = tasks.map((t, idx) => {
        const taskNum = typeof t.taskNumber === 'number' && Number.isFinite(t.taskNumber) && t.taskNumber > 0
          ? t.taskNumber
          : idx + 1;
        const numericAmount = parseNumericAmount(t.amount);

        return [
          taskNum,
          escapeCsv(t.description),
          numericAmount,
          escapeCsv(t.paymentMode),
          escapeCsv(t.unit),
          escapeCsv(t.priority),
          escapeCsv(t.status),
          escapeCsv(t.expectedDate),
          escapeCsv(t.accountName),
          escapeCsv(t.accountCode),
          escapeCsv(t.category),
          escapeCsv(t.remarks),
          escapeCsv(t.monthYear || currentMonth),
        ].join(',');
      });

      // Include UTF-8 BOM (\uFEFF) and Windows CRLF (\r\n) for perfect Microsoft Excel compatibility
      const csvContent = '\uFEFF' + [headers.join(','), ...rows].join('\r\n');
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `Accomplishment_Plan_Tasks_${currentMonth}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      onClose();
    } catch (err) {
      console.error('Error exporting CSV:', err);
      alert('Failed to export CSV. Please try again.');
    }
  };

  // Generate formatted summary text matching Android
  const generateSummaryText = () => {
    let text = `Accomplishment Plan DB - Export (Summary)\n`;
    text += `Month: ${currentMonth} | Total Items: ${tasks.length}\n`;
    text += `--------------------------------------------\n`;
    tasks.forEach((task, idx) => {
      text += `#${idx + 1}. ${task.description}\n`;
      text += `Amount: ${task.amount} TK | ${task.paymentMode} | ${task.unit}\n`;
      text += `Priority: ${task.priority} | Status: ${task.status}\n`;
      text += `Account: ${task.accountName} (${task.accountCode})\n`;
      if (task.expectedDate) text += `Expected Date: ${task.expectedDate}\n`;
      if (task.remarks) text += `Remarks: ${task.remarks}\n`;
      text += `\n`;
    });
    text += `Developer by Md Al Helal Sarkar\n`;
    return text;
  };

  const handleCopySummary = () => {
    const text = generateSummaryText();
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownloadText = () => {
    const text = generateSummaryText();
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `Accomplishment_Plan_${currentMonth}_Report.txt`;
    link.click();
    URL.revokeObjectURL(url);
    onClose();
  };

  const handleExportPDF = () => {
    try {
      const itemsPerPage = 18;
      const totalPages = Math.max(1, Math.ceil(tasks.length / itemsPerPage));
      const pageWidth = 842;  // A4 Landscape width in points
      const pageHeight = 595; // A4 Landscape height in points

      const escapePdf = (str: string) => {
        if (!str) return '';
        return str
          .replace(/\\/g, '\\\\')
          .replace(/\(/g, '\\(')
          .replace(/\)/g, '\\)');
      };

      const fitPdfText = (str: string | undefined | null, maxChars: number) => {
        const s = (str || '').trim();
        if (s.length <= maxChars) return s;
        return s.slice(0, maxChars - 1) + '...';
      };

      const pageObjectIds: number[] = [];
      const contentObjectIds: number[] = [];

      for (let p = 0; p < totalPages; p++) {
        pageObjectIds.push(5 + p * 2);
        contentObjectIds.push(6 + p * 2);
      }

      // 11 Columns matching Android and Web:
      // #, Description, Amount, Mode, Unit, Priority, Status, Expected Date, Account, Code, Category
      const colPositions = [25, 48, 220, 280, 335, 380, 425, 490, 560, 670, 740];
      const colTitles = ['#', 'Description', 'Amount', 'Mode', 'Unit', 'Priority', 'Status', 'Expected Date', 'Account', 'Code', 'Category'];

      const pagesContents: string[] = [];

      for (let p = 0; p < totalPages; p++) {
        const startIdx = p * itemsPerPage;
        const endIdx = Math.min(startIdx + itemsPerPage, tasks.length);
        const pageTasks = tasks.slice(startIdx, endIdx);

        let stream = '';

        // Header Title (repeated on every page)
        stream += '0.117 0.533 0.898 rg\n'; // Corporate blue #1E88E5
        stream += 'BT /F2 15 Tf 25 562 Td (' + escapePdf('Accomplishment Plan Report') + ') Tj ET\n';

        stream += '0.420 0.447 0.502 rg\n'; // Gray #6B7280
        stream += 'BT /F1 9.5 Tf 25 548 Td (' + escapePdf('Master Task Accomplishments & Operational Expenses') + ') Tj ET\n';

        const printDate = new Date().toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
        const metaStr = 'Month/Year: ' + currentMonth + '  |  Total Tasks: ' + tasks.length + '  |  Date: ' + printDate;
        stream += 'BT /F1 9.5 Tf 460 556 Td (' + escapePdf(metaStr) + ') Tj ET\n';

        // Divider line below header
        stream += '0.898 0.906 0.922 RG 1 w 25 538 m 817 538 l S\n';

        // Column Headers (repeated on every page)
        let currentY = 520;
        stream += '0.122 0.161 0.216 rg\n'; // #1F2937
        colTitles.forEach((title, idx) => {
          const x = colPositions[idx];
          stream += 'BT /F2 8.5 Tf ' + x + ' ' + currentY + ' Td (' + escapePdf(title) + ') Tj ET\n';
        });

        currentY -= 6;
        stream += '0.898 0.906 0.922 RG 1 w 25 ' + currentY + ' m 817 ' + currentY + ' l S\n';
        currentY -= 16;

        // Data Rows for this page
        if (pageTasks.length === 0) {
          stream += '0.420 0.447 0.502 rg\n';
          stream += 'BT /F1 9 Tf 25 ' + (currentY - 20) + ' Td (' + escapePdf('No tasks recorded for this month.') + ') Tj ET\n';
        } else {
          pageTasks.forEach((task, idx) => {
            const rowNum = (startIdx + idx + 1).toString();
            const y = currentY;

            stream += '0.067 0.094 0.153 rg\n'; // Dark #111827
            // #
            stream += 'BT /F1 8 Tf ' + colPositions[0] + ' ' + y + ' Td (' + escapePdf(rowNum) + ') Tj ET\n';
            // Description
            stream += 'BT /F1 8 Tf ' + colPositions[1] + ' ' + y + ' Td (' + escapePdf(fitPdfText(task.description, 32)) + ') Tj ET\n';
            // Amount
            stream += 'BT /F1 8 Tf ' + colPositions[2] + ' ' + y + ' Td (' + escapePdf(fitPdfText(task.amount, 12)) + ') Tj ET\n';
            // Payment Mode
            stream += 'BT /F1 8 Tf ' + colPositions[3] + ' ' + y + ' Td (' + escapePdf(fitPdfText(task.paymentMode, 10)) + ') Tj ET\n';
            // Unit
            stream += 'BT /F1 8 Tf ' + colPositions[4] + ' ' + y + ' Td (' + escapePdf(fitPdfText(task.unit, 9)) + ') Tj ET\n';

            // Priority with color
            if (task.priority === 'High') {
              stream += '0.863 0.149 0.149 rg\n';
            } else if (task.priority === 'Med') {
              stream += '0.851 0.467 0.024 rg\n';
            } else {
              stream += '0.086 0.639 0.290 rg\n';
            }
            stream += 'BT /F2 8 Tf ' + colPositions[5] + ' ' + y + ' Td (' + escapePdf(fitPdfText(task.priority, 8)) + ') Tj ET\n';

            // Status
            stream += '0.118 0.251 0.686 rg\n';
            stream += 'BT /F2 8 Tf ' + colPositions[6] + ' ' + y + ' Td (' + escapePdf(fitPdfText(task.status, 12)) + ') Tj ET\n';

            // Expected Date, Account, Code, Category
            stream += '0.067 0.094 0.153 rg\n';
            stream += 'BT /F1 8 Tf ' + colPositions[7] + ' ' + y + ' Td (' + escapePdf(fitPdfText(task.expectedDate, 12)) + ') Tj ET\n';
            stream += 'BT /F1 8 Tf ' + colPositions[8] + ' ' + y + ' Td (' + escapePdf(fitPdfText(task.accountName, 20)) + ') Tj ET\n';
            stream += 'BT /F1 8 Tf ' + colPositions[9] + ' ' + y + ' Td (' + escapePdf(fitPdfText(task.accountCode, 12)) + ') Tj ET\n';
            stream += 'BT /F1 8 Tf ' + colPositions[10] + ' ' + y + ' Td (' + escapePdf(fitPdfText(task.category, 15)) + ') Tj ET\n';

            // Row divider line
            stream += '0.941 0.945 0.957 RG 0.5 w 25 ' + (y - 5) + ' m 817 ' + (y - 5) + ' l S\n';

            currentY -= 22;
          });
        }

        // Page Footer (repeated on every page with Page X of Y)
        stream += '0.898 0.906 0.922 RG 1 w 25 35 m 817 35 l S\n';
        stream += '0.420 0.447 0.502 rg\n';
        stream += 'BT /F1 8.5 Tf 25 20 Td (' + escapePdf('Accomplishment Plan DB • Confidential Internal Report') + ') Tj ET\n';
        const pageNumberStr = 'Page ' + (p + 1) + ' of ' + totalPages;
        stream += 'BT /F1 8.5 Tf 395 20 Td (' + escapePdf(pageNumberStr) + ') Tj ET\n';
        stream += 'BT /F1 8.5 Tf 640 20 Td (' + escapePdf('Developer by Md Al Helal Sarkar') + ') Tj ET\n';

        pagesContents.push(stream);
      }

      // Assemble binary chunks with exact UTF-8 byte offset tracking
      const encoder = new TextEncoder();
      const chunks: Uint8Array[] = [];
      let currentByteOffset = 0;
      const xrefOffsets: number[] = [];

      const appendChunk = (str: string) => {
        const bytes = encoder.encode(str + '\n');
        chunks.push(bytes);
        currentByteOffset += bytes.length;
      };

      appendChunk('%PDF-1.4');
      appendChunk('%\xE2\xE3\xCF\xD3');

      // Object 1: Catalog
      xrefOffsets[1] = currentByteOffset;
      appendChunk('1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj');

      // Object 2: Pages
      xrefOffsets[2] = currentByteOffset;
      const kidsStr = pageObjectIds.map((id) => id + ' 0 R').join(' ');
      appendChunk('2 0 obj\n<< /Type /Pages /Kids [' + kidsStr + '] /Count ' + totalPages + ' >>\nendobj');

      // Object 3: Font Regular (Helvetica)
      xrefOffsets[3] = currentByteOffset;
      appendChunk('3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj');

      // Object 4: Font Bold (Helvetica-Bold)
      xrefOffsets[4] = currentByteOffset;
      appendChunk('4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj');

      // Page objects & Content objects
      for (let p = 0; p < totalPages; p++) {
        const pageId = pageObjectIds[p];
        const contentId = contentObjectIds[p];
        const contentStr = pagesContents[p];

        // Page Object
        xrefOffsets[pageId] = currentByteOffset;
        appendChunk(
          pageId +
            ' 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ' +
            pageWidth +
            ' ' +
            pageHeight +
            '] /Contents ' +
            contentId +
            ' 0 R /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> >>\nendobj'
        );

        // Content Object
        xrefOffsets[contentId] = currentByteOffset;
        const streamBytes = encoder.encode(contentStr);
        appendChunk(
          contentId +
            ' 0 obj\n<< /Length ' +
            streamBytes.length +
            ' >>\nstream\n' +
            contentStr +
            'endstream\nendobj'
        );
      }

      // Cross-reference table
      const startXref = currentByteOffset;
      const totalObjectsCount = 4 + totalPages * 2;
      let xrefStr = 'xref\n0 ' + (totalObjectsCount + 1) + '\n0000000000 65535 f \n';
      for (let i = 1; i <= totalObjectsCount; i++) {
        const off = (xrefOffsets[i] || 0).toString().padStart(10, '0');
        xrefStr += off + ' 00000 n \n';
      }
      xrefStr += 'trailer\n<< /Size ' + (totalObjectsCount + 1) + ' /Root 1 0 R >>\nstartxref\n' + startXref + '\n%%EOF';
      appendChunk(xrefStr);

      const blob = new Blob(chunks as BlobPart[], { type: 'application/pdf' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `Accomplishment_Plan_Tasks_${currentMonth}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      onClose();
    } catch (err) {
      console.error('Error generating PDF:', err);
      alert('Failed to generate PDF. Please try again.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fadeIn">
      <div className="bg-white rounded-xl max-w-md w-full p-5 shadow-2xl border border-gray-100">
        <div className="flex items-center justify-between pb-3 border-b border-gray-100">
          <div className="flex items-center space-x-2 text-app-primary">
            <Download className="w-5 h-5" />
            <h3 className="font-semibold text-gray-900 text-base">
              Export Tasks Data ({currentMonth})
            </h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-xs text-gray-500 my-3">
          Export {tasks.length} task items for <strong>{currentMonth}</strong> in your preferred format:
        </p>

        <div className="space-y-2.5 my-3">
          {/* Real Microsoft Excel (.xlsx) */}
          <button
            type="button"
            onClick={handleExportExcel}
            className="w-full flex items-center p-3 rounded-lg border border-gray-200 hover:border-emerald-500 hover:bg-emerald-50/40 text-left transition-all group"
          >
            <div className="w-10 h-10 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center mr-3 flex-shrink-0 group-hover:scale-105 transition-transform">
              <FileSpreadsheet className="w-5 h-5" />
            </div>
            <div>
              <div className="text-sm font-semibold text-gray-900">Export Excel (.xlsx)</div>
              <div className="text-xs text-gray-500">Native Excel workbook with formatted columns, numbers & frozen header</div>
            </div>
          </button>

          {/* UTF-8 BOM CSV (.csv) */}
          <button
            type="button"
            onClick={handleExportCSV}
            className="w-full flex items-center p-3 rounded-lg border border-gray-200 hover:border-teal-500 hover:bg-teal-50/40 text-left transition-all group"
          >
            <div className="w-10 h-10 rounded-lg bg-teal-100 text-teal-700 flex items-center justify-center mr-3 flex-shrink-0 group-hover:scale-105 transition-transform">
              <Table className="w-5 h-5" />
            </div>
            <div>
              <div className="text-sm font-semibold text-gray-900">Export CSV (.csv)</div>
              <div className="text-xs text-gray-500">UTF-8 BOM encoded with CRLF for Microsoft Excel & Google Sheets</div>
            </div>
          </button>

          {/* Dedicated PDF Document */}
          <button
            type="button"
            onClick={handleExportPDF}
            className="w-full flex items-center p-3 rounded-lg border border-gray-200 hover:border-purple-500 hover:bg-purple-50/40 text-left transition-all group"
          >
            <div className="w-10 h-10 rounded-lg bg-purple-100 text-purple-700 flex items-center justify-center mr-3 flex-shrink-0 group-hover:scale-105 transition-transform">
              <Printer className="w-5 h-5" />
            </div>
            <div>
              <div className="text-sm font-semibold text-gray-900">Export PDF Document (Print/PDF)</div>
              <div className="text-xs text-gray-500">Generates clean report table without dialog/popup UI</div>
            </div>
          </button>

          {/* Text Report */}
          <button
            type="button"
            onClick={handleDownloadText}
            className="w-full flex items-center p-3 rounded-lg border border-gray-200 hover:border-blue-500 hover:bg-blue-50/40 text-left transition-all group"
          >
            <div className="w-10 h-10 rounded-lg bg-blue-100 text-app-primary flex items-center justify-center mr-3 flex-shrink-0 group-hover:scale-105 transition-transform">
              <FileText className="w-5 h-5" />
            </div>
            <div>
              <div className="text-sm font-semibold text-gray-900">Formatted Text Report (.txt)</div>
              <div className="text-xs text-gray-500">Structured narrative summary with all task notes & remarks</div>
            </div>
          </button>
        </div>

        <div className="pt-3 border-t border-gray-100 flex items-center justify-between">
          <button
            type="button"
            onClick={handleCopySummary}
            className="flex items-center space-x-1.5 text-xs font-medium text-gray-600 hover:text-app-primary py-1 px-2 rounded hover:bg-gray-100 transition-colors"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-green-600" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{copied ? 'Summary Copied!' : 'Copy Summary to Clipboard'}</span>
          </button>

          <button
            type="button"
            onClick={onClose}
            className="px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-100 rounded-lg"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
