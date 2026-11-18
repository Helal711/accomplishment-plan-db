import React, { useState } from 'react';
import { Download, FileSpreadsheet, FileText, Printer, X, Check, Copy, Table } from 'lucide-react';
import * as XLSX from 'xlsx';
import { TaskItem } from '../types';
import { downloadTasksPdf } from '../utils/pdfExport';

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

  const handleExportPDF = async () => {
    try {
      await downloadTasksPdf(currentMonth, tasks);
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
