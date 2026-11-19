import { TaskItem } from '../types';
import {
  parseOptionalNumber,
  formatPurchasedAmountDisplay,
} from './purchaseAmount';

export function parseRequiredNumericAmount(amt: string | number | undefined): number {
  const parsed = parseOptionalNumber(amt);
  return parsed !== null ? parsed : 0;
}

export function resolveOptionalExcelNumber(raw: string | number | undefined): number | string {
  if (raw === null || raw === undefined) return '';
  const trimmed = String(raw).trim();
  if (!trimmed) return '';
  const parsed = parseOptionalNumber(raw);
  return parsed !== null ? parsed : trimmed;
}

export interface ExcelTaskRow {
  'Task No': number;
  Description: string;
  'Amount (TK)': number;
  'Unit Price': number | string;
  'Total Quantity': number | string;
  'Purchased Amount (TK)': number | string;
  'Supplier Name': string;
  'Payment Mode': string;
  Unit: string;
  Priority: string;
  Status: string;
  'Expected Date': string;
  'Account Name': string;
  'Account Code': string;
  Category: string;
  Remarks: string;
  'Month/Year': string;
}

export function buildExcelRows(currentMonth: string, tasks: TaskItem[]): ExcelTaskRow[] {
  return tasks.map((t, idx) => {
    const taskNum =
      typeof t.taskNumber === 'number' && Number.isFinite(t.taskNumber) && t.taskNumber > 0
        ? t.taskNumber
        : idx + 1;

    return {
      'Task No': taskNum,
      Description: t.description || '',
      'Amount (TK)': parseRequiredNumericAmount(t.amount),
      'Unit Price': resolveOptionalExcelNumber(t.unitPrice),
      'Total Quantity': resolveOptionalExcelNumber(t.totalQuantity),
      'Purchased Amount (TK)': resolveOptionalExcelNumber(t.purchasedAmount),
      'Supplier Name': (t.supplierName || '').trim(),
      'Payment Mode': t.paymentMode || '',
      Unit: t.unit || '',
      Priority: t.priority || '',
      Status: t.status || '',
      'Expected Date': t.expectedDate || '',
      'Account Name': t.accountName || '',
      'Account Code': t.accountCode || '',
      Category: t.category || '',
      Remarks: t.remarks || '',
      'Month/Year': t.monthYear || currentMonth,
    };
  });
}

export function buildCsvContent(currentMonth: string, tasks: TaskItem[]): string {
  const headers = [
    'Task No',
    'Description',
    'Amount (TK)',
    'Unit Price',
    'Total Quantity',
    'Purchased Amount (TK)',
    'Supplier Name',
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
    const taskNum =
      typeof t.taskNumber === 'number' && Number.isFinite(t.taskNumber) && t.taskNumber > 0
        ? t.taskNumber
        : idx + 1;
    const numericAmount = parseRequiredNumericAmount(t.amount);
    const unitPriceVal = resolveOptionalExcelNumber(t.unitPrice);
    const totalQtyVal = resolveOptionalExcelNumber(t.totalQuantity);
    const purchasedVal = resolveOptionalExcelNumber(t.purchasedAmount);

    return [
      taskNum,
      escapeCsv(t.description),
      numericAmount,
      unitPriceVal === '' ? '""' : typeof unitPriceVal === 'number' ? unitPriceVal : escapeCsv(unitPriceVal),
      totalQtyVal === '' ? '""' : typeof totalQtyVal === 'number' ? totalQtyVal : escapeCsv(totalQtyVal),
      purchasedVal === '' ? '""' : typeof purchasedVal === 'number' ? purchasedVal : escapeCsv(purchasedVal),
      escapeCsv((t.supplierName || '').trim()),
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

  // Include UTF-8 BOM (\uFEFF) and Windows CRLF (\r\n) for Microsoft Excel compatibility
  return '\uFEFF' + [headers.join(','), ...rows].join('\r\n');
}

export function buildSummaryText(currentMonth: string, tasks: TaskItem[]): string {
  let text = `Accomplishment Plan DB - Export (Summary)\n`;
  text += `Month: ${currentMonth} | Total Items: ${tasks.length}\n`;
  text += `--------------------------------------------\n`;
  tasks.forEach((task, idx) => {
    text += `#${idx + 1}. ${task.description}\n`;
    text += `Amount: ${task.amount} TK | ${task.paymentMode} | ${task.unit}\n`;

    const purchasedDisplay = formatPurchasedAmountDisplay(task.purchasedAmount);
    const supplierDisplay = (task.supplierName || '').trim();

    if (purchasedDisplay) {
      text += `Purchased Amount: ${purchasedDisplay}\n`;
    }
    if (supplierDisplay) {
      text += `Supplier Name: ${supplierDisplay}\n`;
    }

    text += `Priority: ${task.priority} | Status: ${task.status}\n`;
    text += `Account: ${task.accountName} (${task.accountCode})\n`;
    if (task.expectedDate) text += `Expected Date: ${task.expectedDate}\n`;
    if (task.remarks) text += `Remarks: ${task.remarks}\n`;
    text += `\n`;
  });
  text += `Developer by Md Al Helal Sarkar\n`;
  return text;
}
