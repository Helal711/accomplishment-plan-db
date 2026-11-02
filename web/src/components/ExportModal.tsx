import React, { useState } from 'react';
import { Download, FileSpreadsheet, FileText, Printer, X, Check, Copy } from 'lucide-react';
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

  // 1. Export CSV
  const handleExportCSV = () => {
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
      'Remarks',
      'Month/Year',
    ];

    const rows = tasks.map((t) => [
      t.taskNumber,
      `"${(t.description || '').replace(/"/g, '""')}"`,
      `"${t.amount || ''}"`,
      `"${t.paymentMode || ''}"`,
      `"${t.unit || ''}"`,
      `"${t.priority || ''}"`,
      `"${t.status || ''}"`,
      `"${t.expectedDate || ''}"`,
      `"${(t.accountName || '').replace(/"/g, '""')}"`,
      `"${t.accountCode || ''}"`,
      `"${(t.remarks || '').replace(/"/g, '""')}"`,
      `"${t.monthYear || currentMonth}"`,
    ]);

    const csvContent =
      'data:text/csv;charset=utf-8,\uFEFF' +
      [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `Accomplishment_Plan_${currentMonth}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    onClose();
  };

  // Generate formatted summary text matching Android
  const generateSummaryText = () => {
    let text = `Accomplishment Plan DB - Export (Summary)\n`;
    text += `Month: ${currentMonth} | Total Items: ${tasks.length}\n`;
    text += `--------------------------------------------\n`;
    tasks.forEach((task) => {
      text += `#${task.taskNumber}. ${task.description}\n`;
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

  const handlePrint = () => {
    window.print();
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
          {/* CSV Spreadsheet */}
          <button
            type="button"
            onClick={handleExportCSV}
            className="w-full flex items-center p-3 rounded-lg border border-gray-200 hover:border-green-500 hover:bg-green-50/40 text-left transition-all group"
          >
            <div className="w-10 h-10 rounded-lg bg-green-100 text-green-700 flex items-center justify-center mr-3 flex-shrink-0 group-hover:scale-105 transition-transform">
              <FileSpreadsheet className="w-5 h-5" />
            </div>
            <div>
              <div className="text-sm font-semibold text-gray-900">Excel / CSV Spreadsheet (.csv)</div>
              <div className="text-xs text-gray-500">Universal table format compatible with MS Excel & Sheets</div>
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

          {/* Print / Save PDF */}
          <button
            type="button"
            onClick={handlePrint}
            className="w-full flex items-center p-3 rounded-lg border border-gray-200 hover:border-purple-500 hover:bg-purple-50/40 text-left transition-all group"
          >
            <div className="w-10 h-10 rounded-lg bg-purple-100 text-purple-700 flex items-center justify-center mr-3 flex-shrink-0 group-hover:scale-105 transition-transform">
              <Printer className="w-5 h-5" />
            </div>
            <div>
              <div className="text-sm font-semibold text-gray-900">Print or Save as PDF</div>
              <div className="text-xs text-gray-500">Opens browser print dialog to print or save as PDF</div>
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
