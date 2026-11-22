import { TaskItem } from '../types';
import {
  parseOptionalNumber,
  formatOptionalNumericText,
  sanitizeOptionalFieldForInput,
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

export function formatInlineAmountLabel(rawAmount: string | number | undefined): string {
  if (rawAmount === null || rawAmount === undefined) return '0 TK';
  const trimmed = String(rawAmount).trim();
  if (!trimmed) return '0 TK';
  return trimmed.toLowerCase().endsWith('tk') ? trimmed : `${trimmed} TK`;
}

/**
 * Formats the Task Card inline metadata row in exact order:
 * Amount: [Amount] TK | [Payment Method] | Purchased Amount (TK): [Purchased Amount] | Supplier Name: [Supplier Name]
 * Omits empty optional fields cleanly without zero or placeholder values.
 */
export function buildTaskInlineMetadata(task: Partial<TaskItem> & { paymentType?: string }): string {
  const amountText = formatInlineAmountLabel(task.amount);
  const paymentMethod = String(task.paymentMode ?? task.paymentType ?? 'Cash').trim() || 'Cash';
  const purchasedAmountValue = formatOptionalNumericText(
    sanitizeOptionalFieldForInput(task.purchasedAmount)
  );
  const supplierName = String(task.supplierName ?? '').trim();

  const parts: string[] = [`Amount: ${amountText}`, paymentMethod];
  if (purchasedAmountValue) {
    parts.push(`Purchased Amount (TK): ${purchasedAmountValue}`);
  }
  if (supplierName) {
    parts.push(`Supplier Name: ${supplierName}`);
  }

  return parts.join(' | ');
}

export interface ExcelTaskRow {
  'Task No': number;
  Description: string;
  'Amount (TK)': number;
  'Payment Method': string;
  'Purchased Amount (TK)': number | string;
  'Supplier Name': string;
  Unit: string;
  Priority: string;
  Status: string;
  'Completion Date': string;
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
      'Payment Method': t.paymentMode || '',
      'Purchased Amount (TK)': resolveOptionalExcelNumber(t.purchasedAmount),
      'Supplier Name': (t.supplierName || '').trim(),
      Unit: t.unit || '',
      Priority: t.priority || '',
      Status: t.status || '',
      'Completion Date': t.expectedDate || '',
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
    'Payment Method',
    'Purchased Amount (TK)',
    'Supplier Name',
    'Unit',
    'Priority',
    'Status',
    'Completion Date',
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
    const purchasedVal = resolveOptionalExcelNumber(t.purchasedAmount);

    return [
      taskNum,
      escapeCsv(t.description),
      numericAmount,
      escapeCsv(t.paymentMode),
      purchasedVal === '' ? '""' : typeof purchasedVal === 'number' ? purchasedVal : escapeCsv(purchasedVal),
      escapeCsv((t.supplierName || '').trim()),
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
    text += `${buildTaskInlineMetadata(task)}\n`;
    text += `Unit: ${task.unit} | Priority: ${task.priority} | Status: ${task.status}\n`;
    text += `Account: ${task.accountName} (${task.accountCode})\n`;
    if (task.expectedDate) text += `Completion Date: ${task.expectedDate}\n`;
    if (task.remarks) text += `Remarks: ${task.remarks}\n`;
    text += `\n`;
  });
  text += `Developer by Md Al Helal Sarkar\n`;
  return text;
}
