import pdfMake from 'pdfmake/build/pdfmake';
import notoSansBengaliAssetUrl from '../assets/fonts/NotoSansBengali-Regular.ttf';
import { TaskItem } from '../types';
import { getStatusColor } from './statusColors';
import { normalizeBengaliDigits } from './purchaseAmount';

const FONT_FILE_NAME = 'NotoSansBengali-Regular.ttf';
const FONT_FAMILY_NAME = 'NotoBengali';

export const PDF_TABLE_HEADERS = [
  'Task No',
  'Description',
  'Amount (TK)',
  'Payment Method',
  'Purchased Amount (TK)',
  'Supplier Name',
  'Unit',
  'Priority',
  'Status',
  'Expected Date',
  'Account Name',
  'Account Code',
  'Category',
  'Remarks',
  'Month/Year',
] as const;

let fontRegistered = false;

function getPdfMakeInstance() {
  const mod = pdfMake as unknown as {
    default?: typeof pdfMake;
  } & typeof pdfMake;
  return mod.default && typeof mod.default.createPdf === 'function' ? mod.default : mod;
}

function uint8ArrayToBase64(bytes: Uint8Array): string {
  let binary = '';
  const chunkSize = 0x8000;
  for (let i = 0; i < bytes.length; i += chunkSize) {
    const chunk = bytes.subarray(i, i + chunkSize);
    binary += String.fromCharCode(...chunk);
  }
  return btoa(binary);
}

async function resolveFontBase64(customBase64Font?: string): Promise<string> {
  if (customBase64Font) {
    const commaIdx = customBase64Font.indexOf(',');
    return commaIdx !== -1 ? customBase64Font.slice(commaIdx + 1) : customBase64Font;
  }

  if (notoSansBengaliAssetUrl.startsWith('data:')) {
    const commaIdx = notoSansBengaliAssetUrl.indexOf(',');
    return commaIdx !== -1 ? notoSansBengaliAssetUrl.slice(commaIdx + 1) : notoSansBengaliAssetUrl;
  }

  const response = await fetch(notoSansBengaliAssetUrl);
  if (!response.ok) {
    throw new Error(`Failed to load Unicode PDF font (${response.status})`);
  }
  const arrayBuffer = await response.arrayBuffer();
  return uint8ArrayToBase64(new Uint8Array(arrayBuffer));
}

export async function ensurePdfUnicodeFontRegistered(customBase64Font?: string): Promise<void> {
  if (fontRegistered && !customBase64Font) return;

  const instance = getPdfMakeInstance();
  const base64 = await resolveFontBase64(customBase64Font);

  instance.addVirtualFileSystem({
    [FONT_FILE_NAME]: base64,
  });

  instance.addFonts({
    [FONT_FAMILY_NAME]: {
      normal: FONT_FILE_NAME,
      bold: FONT_FILE_NAME,
      italics: FONT_FILE_NAME,
      bolditalics: FONT_FILE_NAME,
    },
  });

  fontRegistered = true;
}

export function resolvePdfTaskNumber(rawTaskNumber: unknown, fallbackIndex: number): number {
  if (typeof rawTaskNumber === 'number' && Number.isFinite(rawTaskNumber) && rawTaskNumber > 0) {
    return Math.floor(rawTaskNumber);
  }
  if (typeof rawTaskNumber === 'string') {
    const trimmed = normalizeBengaliDigits(rawTaskNumber).trim();
    if (trimmed) {
      const parsed = Number(trimmed);
      if (Number.isFinite(parsed) && parsed > 0) {
        return Math.floor(parsed);
      }
    }
  }
  return fallbackIndex + 1;
}

export function safePdfText(value: unknown, fallback = '-'): string {
  if (value === null || value === undefined) return fallback;
  const normalized = String(value).replace(/\r\n/g, '\n').replace(/\r/g, '\n').trim();
  return normalized.length > 0 ? normalized : fallback;
}

export function formatPdfAmount(rawAmount: unknown): string {
  if (rawAmount === null || rawAmount === undefined) return '-';

  if (typeof rawAmount === 'number') {
    if (!Number.isFinite(rawAmount)) return '-';
    const formatted = Number.isInteger(rawAmount)
      ? rawAmount.toLocaleString('en-US')
      : rawAmount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return `${formatted} TK`;
  }

  const trimmed = String(rawAmount).trim();
  if (!trimmed) return '-';

  const withoutTk = normalizeBengaliDigits(trimmed)
    .replace(/\s*(tk|bdt|৳)\s*$/i, '')
    .replace(/^৳\s*/i, '')
    .trim();
  const numericCandidate = withoutTk.replace(/,/g, '');
  if (/^-?\d+(\.\d+)?$/.test(numericCandidate)) {
    const parsed = Number(numericCandidate);
    if (Number.isFinite(parsed)) {
      const formatted = Number.isInteger(parsed)
        ? parsed.toLocaleString('en-US')
        : parsed.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
      return `${formatted} TK`;
    }
  }

  return /\btk$/i.test(trimmed) ? trimmed : `${trimmed} TK`;
}

export function formatPdfOptionalAmount(rawAmount: unknown): string {
  if (rawAmount === null || rawAmount === undefined) return '';
  if (typeof rawAmount === 'string') {
    const trimmed = rawAmount.trim();
    if (!trimmed || trimmed === '-') return '';
  }
  const formatted = formatPdfAmount(rawAmount);
  return formatted === '-' ? '' : formatted;
}

function getPriorityColor(priority: string): string {
  const norm = priority.trim().toLowerCase();
  if (norm === 'high') return '#DC2626';
  if (norm === 'med' || norm === 'medium') return '#D97706';
  if (norm === 'low') return '#16A34A';
  return '#111827';
}

function pickFirstNonEmptyValue(...candidates: unknown[]): unknown {
  for (const candidate of candidates) {
    if (candidate === null || candidate === undefined) continue;
    if (typeof candidate === 'number') {
      if (Number.isFinite(candidate)) return candidate;
      continue;
    }
    if (String(candidate).trim().length > 0) {
      return candidate;
    }
  }
  return undefined;
}

function resolvePdfOptionalSupplierName(raw: unknown): string {
  const text = safePdfText(raw, '');
  return text === '-' ? '' : text;
}

export interface PdfTaskRowData {
  taskNo: string;
  description: string;
  amount: string;
  paymentMode: string;
  purchasedAmount: string;
  supplierName: string;
  unit: string;
  priority: string;
  status: string;
  expectedDate: string;
  accountName: string;
  accountCode: string;
  category: string;
  remarks: string;
  monthYear: string;
}

export function mapTaskToPdfRow(
  task: TaskItem,
  index: number,
  currentMonth = ''
): PdfTaskRowData {
  const taskRecord = task as unknown as Record<string, unknown>;
  const formattedAmount = formatPdfAmount(taskRecord.amount);
  const paymentMode = safePdfText(
    pickFirstNonEmptyValue(taskRecord.paymentMode, taskRecord.payment_mode, taskRecord.paymentType)
  );
  const purchasedAmount = formatPdfOptionalAmount(
    pickFirstNonEmptyValue(
      taskRecord.purchasedAmount,
      taskRecord.purchased_amount,
      taskRecord.purchaseAmount,
      taskRecord.purchase_amount
    )
  );
  const supplierName = resolvePdfOptionalSupplierName(
    pickFirstNonEmptyValue(
      taskRecord.supplierName,
      taskRecord.supplier_name,
      taskRecord.supplier
    )
  );

  return {
    taskNo: String(
      resolvePdfTaskNumber(
        pickFirstNonEmptyValue(taskRecord.taskNumber, taskRecord.task_number),
        index
      )
    ),
    description: safePdfText(taskRecord.description),
    amount: formattedAmount,
    paymentMode,
    purchasedAmount,
    supplierName,
    unit: safePdfText(taskRecord.unit),
    priority: safePdfText(taskRecord.priority),
    status: safePdfText(taskRecord.status),
    expectedDate: safePdfText(
      pickFirstNonEmptyValue(taskRecord.expectedDate, taskRecord.expected_date)
    ),
    accountName: safePdfText(
      pickFirstNonEmptyValue(taskRecord.accountName, taskRecord.account_name)
    ),
    accountCode: safePdfText(
      pickFirstNonEmptyValue(taskRecord.accountCode, taskRecord.account_code)
    ),
    category: safePdfText(taskRecord.category),
    remarks: safePdfText(taskRecord.remarks),
    monthYear: safePdfText(
      pickFirstNonEmptyValue(taskRecord.monthYear, taskRecord.month_year, currentMonth)
    ),
  };
}

export function buildPdfDocumentDefinition(currentMonth: string, tasks: TaskItem[]): Record<string, unknown> {
  const printDate = new Date().toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });

  // A3 landscape: 1190.55pt wide - 40pt horizontal margins (20pt each) = 1150.55pt usable width.
  // 15 column widths sum to 1038pt + 105pt horizontal padding (3.5pt * 2 * 15) + 5.6pt borders (16 * 0.35pt) = 1148.6pt.
  const columnWidths = [32, 152, 64, 50, 68, 112, 38, 42, 62, 56, 90, 54, 56, 110, 52];

  const headerRow = [
    { text: PDF_TABLE_HEADERS[0], style: 'tableHeader', alignment: 'center' },
    { text: PDF_TABLE_HEADERS[1], style: 'tableHeader' },
    { text: PDF_TABLE_HEADERS[2], style: 'tableHeader', alignment: 'right' },
    { text: PDF_TABLE_HEADERS[3], style: 'tableHeader' },
    { text: PDF_TABLE_HEADERS[4], style: 'tableHeader', alignment: 'right' },
    { text: PDF_TABLE_HEADERS[5], style: 'tableHeader' },
    { text: PDF_TABLE_HEADERS[6], style: 'tableHeader' },
    { text: PDF_TABLE_HEADERS[7], style: 'tableHeader', alignment: 'center' },
    { text: PDF_TABLE_HEADERS[8], style: 'tableHeader' },
    { text: PDF_TABLE_HEADERS[9], style: 'tableHeader' },
    { text: PDF_TABLE_HEADERS[10], style: 'tableHeader' },
    { text: PDF_TABLE_HEADERS[11], style: 'tableHeader' },
    { text: PDF_TABLE_HEADERS[12], style: 'tableHeader' },
    { text: PDF_TABLE_HEADERS[13], style: 'tableHeader' },
    { text: PDF_TABLE_HEADERS[14], style: 'tableHeader', alignment: 'center' },
  ];

  const bodyRows: unknown[][] = [headerRow];

  if (tasks.length === 0) {
    bodyRows.push([
      {
        text: `No tasks recorded for ${currentMonth}.`,
        colSpan: 15,
        alignment: 'center',
        color: '#6B7280',
        fontSize: 9.5,
        margin: [0, 16, 0, 16],
      },
      ...Array.from({ length: 14 }, () => ({})),
    ]);
  } else {
    tasks.forEach((task, index) => {
      const mapped = mapTaskToPdfRow(task, index, currentMonth);
      const statusColor = mapped.status !== '-' ? getStatusColor(mapped.status).text : '#111827';
      const rowFill = index % 2 === 1 ? '#F8FAFC' : undefined;

      bodyRows.push([
        { text: mapped.taskNo, style: 'cellText', alignment: 'center', fillColor: rowFill },
        { text: mapped.description, style: 'cellText', fillColor: rowFill },
        { text: mapped.amount, style: 'cellAmount', alignment: 'right', fillColor: rowFill },
        { text: mapped.paymentMode, style: 'cellText', fillColor: rowFill },
        { text: mapped.purchasedAmount, style: 'cellAmount', alignment: 'right', fillColor: rowFill },
        { text: mapped.supplierName, style: 'cellText', fillColor: rowFill },
        { text: mapped.unit, style: 'cellText', fillColor: rowFill },
        {
          text: mapped.priority,
          style: 'cellText',
          alignment: 'center',
          color: getPriorityColor(mapped.priority),
          fillColor: rowFill,
        },
        { text: mapped.status, style: 'cellText', color: statusColor, fillColor: rowFill },
        { text: mapped.expectedDate, style: 'cellText', fillColor: rowFill },
        { text: mapped.accountName, style: 'cellText', fillColor: rowFill },
        { text: mapped.accountCode, style: 'cellText', fillColor: rowFill },
        { text: mapped.category, style: 'cellText', fillColor: rowFill },
        { text: mapped.remarks, style: 'cellText', fillColor: rowFill },
        { text: mapped.monthYear, style: 'cellText', alignment: 'center', fillColor: rowFill },
      ]);
    });
  }

  return {
    pageSize: 'A3',
    pageOrientation: 'landscape',
    pageMargins: [20, 62, 20, 38],
    defaultStyle: {
      font: FONT_FAMILY_NAME,
      fontSize: 8,
      color: '#111827',
      lineHeight: 1.22,
    },
    styles: {
      tableHeader: {
        fontSize: 8,
        color: '#FFFFFF',
        fillColor: '#1E3A8A',
      },
      cellText: {
        fontSize: 7.8,
        color: '#111827',
      },
      cellAmount: {
        fontSize: 7.8,
        color: '#0F172A',
      },
    },
    header: () => ({
      margin: [20, 16, 20, 0],
      stack: [
        {
          columns: [
            {
              width: '*',
              stack: [
                {
                  text: 'Accomplishment Plan Report',
                  fontSize: 15,
                  color: '#1E88E5',
                },
                {
                  text: 'Master Task Accomplishments & Operational Expenses',
                  fontSize: 9.5,
                  color: '#6B7280',
                  margin: [0, 1, 0, 0],
                },
              ],
            },
            {
              width: 'auto',
              alignment: 'right',
              text: `Month/Year: ${currentMonth}  |  Total Tasks: ${tasks.length}  |  Date: ${printDate}`,
              fontSize: 9.5,
              color: '#4B5563',
              margin: [0, 7, 0, 0],
            },
          ],
        },
        {
          canvas: [
            {
              type: 'line',
              x1: 0,
              y1: 6,
              x2: 1150,
              y2: 6,
              lineWidth: 0.75,
              lineColor: '#CBD5E1',
            },
          ],
        },
      ],
    }),
    footer: (currentPage: number, pageCount: number) => ({
      margin: [20, 6, 20, 0],
      stack: [
        {
          canvas: [
            {
              type: 'line',
              x1: 0,
              y1: 0,
              x2: 1150,
              y2: 0,
              lineWidth: 0.75,
              lineColor: '#CBD5E1',
            },
          ],
        },
        {
          margin: [0, 6, 0, 0],
          columns: [
            {
              width: '*',
              text: 'Accomplishment Plan DB • Confidential Internal Report',
              fontSize: 8.5,
              color: '#6B7280',
            },
            {
              width: 'auto',
              alignment: 'center',
              text: `Page ${currentPage} of ${pageCount}`,
              fontSize: 8.5,
              color: '#374151',
            },
            {
              width: '*',
              alignment: 'right',
              text: 'Developer by Md Al Helal Sarkar',
              fontSize: 8.5,
              color: '#6B7280',
            },
          ],
        },
      ],
    }),
    content: [
      {
        table: {
          headerRows: 1,
          dontBreakRows: true,
          widths: columnWidths,
          body: bodyRows,
        },
        layout: {
          hLineWidth: (i: number, node: { table: { body: unknown[] } }) =>
            i === 0 || i === 1 || i === node.table.body.length ? 0.8 : 0.4,
          vLineWidth: () => 0.35,
          hLineColor: (i: number) => (i <= 1 ? '#1E3A8A' : '#E2E8F0'),
          vLineColor: () => '#E2E8F0',
          paddingLeft: () => 3.5,
          paddingRight: () => 3.5,
          paddingTop: (rowIndex: number) => (rowIndex === 0 ? 5 : 4),
          paddingBottom: (rowIndex: number) => (rowIndex === 0 ? 5 : 4),
        },
      },
    ],
  };
}

export async function generateTasksPdfBuffer(
  currentMonth: string,
  tasks: TaskItem[],
  customBase64Font?: string
): Promise<Uint8Array> {
  await ensurePdfUnicodeFontRegistered(customBase64Font);
  const instance = getPdfMakeInstance();
  const docDefinition = buildPdfDocumentDefinition(currentMonth, tasks);
  const pdfDoc = instance.createPdf(docDefinition);
  return pdfDoc.getBuffer();
}

export async function downloadTasksPdf(currentMonth: string, tasks: TaskItem[]): Promise<void> {
  const buffer = await generateTasksPdfBuffer(currentMonth, tasks);
  const blob = new Blob([buffer as BlobPart], { type: 'application/pdf' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `Accomplishment_Plan_Tasks_${currentMonth}.pdf`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
