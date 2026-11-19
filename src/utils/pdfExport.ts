import pdfMake from 'pdfmake/build/pdfmake';
import notoSansBengaliAssetUrl from '../assets/fonts/NotoSansBengali-Regular.ttf';
import { TaskItem } from '../types';
import { getStatusColor } from './statusColors';
import { formatPurchasedAmountDisplay } from './purchaseAmount';

const FONT_FILE_NAME = 'NotoSansBengali-Regular.ttf';
const FONT_FAMILY_NAME = 'NotoBengali';

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
    const trimmed = rawTaskNumber.trim();
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

  const withoutTk = trimmed.replace(/\s*tk$/i, '').trim();
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

function getPriorityColor(priority: string): string {
  const norm = priority.trim().toLowerCase();
  if (norm === 'high') return '#DC2626';
  if (norm === 'med' || norm === 'medium') return '#D97706';
  if (norm === 'low') return '#16A34A';
  return '#111827';
}

export interface PdfTaskRowData {
  taskNo: string;
  description: string;
  amount: string;
  purchasedAmount: string;
  supplierName: string;
  unit: string;
  priority: string;
  paymentMode: string;
  accountName: string;
  accountCode: string;
  category: string;
  status: string;
  expectedDate: string;
  remarks: string;
}

export function mapTaskToPdfRow(task: TaskItem, index: number): PdfTaskRowData {
  const taskRecord = task as unknown as Record<string, unknown>;
  const purchasedDisplay = formatPurchasedAmountDisplay(taskRecord.purchasedAmount);
  return {
    taskNo: String(resolvePdfTaskNumber(taskRecord.taskNumber, index)),
    description: safePdfText(taskRecord.description),
    amount: formatPdfAmount(taskRecord.amount),
    purchasedAmount: purchasedDisplay ?? '-',
    supplierName: safePdfText(taskRecord.supplierName),
    unit: safePdfText(taskRecord.unit),
    priority: safePdfText(taskRecord.priority),
    paymentMode: safePdfText(taskRecord.paymentMode ?? taskRecord.paymentType),
    accountName: safePdfText(taskRecord.accountName),
    accountCode: safePdfText(taskRecord.accountCode),
    category: safePdfText(taskRecord.category),
    status: safePdfText(taskRecord.status),
    expectedDate: safePdfText(taskRecord.expectedDate),
    remarks: safePdfText(taskRecord.remarks),
  };
}

export function buildPdfDocumentDefinition(currentMonth: string, tasks: TaskItem[]): Record<string, unknown> {
  const printDate = new Date().toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });

  // 12 columns inner text widths summing to 742pt + 60pt horizontal padding (2.5pt*2*12) + ~4.5pt borders = 806.5pt inside 810pt (842 - 16*2)
  const columnWidths = [20, 132, 56, 36, 34, 38, 96, 50, 62, 60, 54, 104];

  const headerRow = [
    { text: '#', style: 'tableHeader', alignment: 'center' },
    { text: 'Description', style: 'tableHeader' },
    { text: 'Amount', style: 'tableHeader', alignment: 'right' },
    { text: 'Unit', style: 'tableHeader' },
    { text: 'Priority', style: 'tableHeader', alignment: 'center' },
    { text: 'Mode', style: 'tableHeader' },
    { text: 'Account Name', style: 'tableHeader' },
    { text: 'Code', style: 'tableHeader' },
    { text: 'Category', style: 'tableHeader' },
    { text: 'Status', style: 'tableHeader' },
    { text: 'Exp. Date', style: 'tableHeader' },
    { text: 'Remarks', style: 'tableHeader' },
  ];

  const bodyRows: unknown[][] = [headerRow];

  if (tasks.length === 0) {
    bodyRows.push([
      {
        text: `No tasks recorded for ${currentMonth}.`,
        colSpan: 12,
        alignment: 'center',
        color: '#6B7280',
        fontSize: 9,
        margin: [0, 14, 0, 14],
      },
      ...Array(11).fill({}),
    ]);
  } else {
    tasks.forEach((task, index) => {
      const mapped = mapTaskToPdfRow(task, index);
      const statusColor = mapped.status !== '-' ? getStatusColor(mapped.status).text : '#111827';
      const rowFill = index % 2 === 1 ? '#F8FAFC' : undefined;

      const amountCell =
        mapped.purchasedAmount !== '-'
          ? {
              stack: [
                { text: mapped.amount, style: 'cellAmount', alignment: 'right' },
                {
                  text: `Pur: ${mapped.purchasedAmount}`,
                  fontSize: 6.4,
                  color: '#047857',
                  alignment: 'right',
                  margin: [0, 1, 0, 0],
                },
              ],
              fillColor: rowFill,
            }
          : { text: mapped.amount, style: 'cellAmount', alignment: 'right', fillColor: rowFill };

      const descriptionCell =
        mapped.supplierName !== '-'
          ? {
              stack: [
                { text: mapped.description, style: 'cellText' },
                {
                  text: `Supplier: ${mapped.supplierName}`,
                  fontSize: 6.4,
                  color: '#374151',
                  margin: [0, 1.5, 0, 0],
                },
              ],
              fillColor: rowFill,
            }
          : { text: mapped.description, style: 'cellText', fillColor: rowFill };

      bodyRows.push([
        { text: mapped.taskNo, style: 'cellText', alignment: 'center', fillColor: rowFill },
        descriptionCell,
        amountCell,
        { text: mapped.unit, style: 'cellText', fillColor: rowFill },
        {
          text: mapped.priority,
          style: 'cellText',
          alignment: 'center',
          color: getPriorityColor(mapped.priority),
          fillColor: rowFill,
        },
        { text: mapped.paymentMode, style: 'cellText', fillColor: rowFill },
        { text: mapped.accountName, style: 'cellText', fillColor: rowFill },
        { text: mapped.accountCode, style: 'cellText', fillColor: rowFill },
        { text: mapped.category, style: 'cellText', fillColor: rowFill },
        { text: mapped.status, style: 'cellText', color: statusColor, fillColor: rowFill },
        { text: mapped.expectedDate, style: 'cellText', fillColor: rowFill },
        { text: mapped.remarks, style: 'cellText', fillColor: rowFill },
      ]);
    });
  }

  return {
    pageSize: 'A4',
    pageOrientation: 'landscape',
    pageMargins: [16, 56, 16, 34],
    defaultStyle: {
      font: FONT_FAMILY_NAME,
      fontSize: 7.5,
      color: '#111827',
      lineHeight: 1.2,
    },
    styles: {
      tableHeader: {
        fontSize: 7.5,
        color: '#FFFFFF',
        fillColor: '#1E3A8A',
      },
      cellText: {
        fontSize: 7.2,
        color: '#111827',
      },
      cellAmount: {
        fontSize: 7.2,
        color: '#0F172A',
      },
    },
    header: () => ({
      margin: [16, 14, 16, 0],
      stack: [
        {
          columns: [
            {
              width: '*',
              stack: [
                {
                  text: 'Accomplishment Plan Report',
                  fontSize: 13.5,
                  color: '#1E88E5',
                },
                {
                  text: 'Master Task Accomplishments & Operational Expenses',
                  fontSize: 8.5,
                  color: '#6B7280',
                  margin: [0, 1, 0, 0],
                },
              ],
            },
            {
              width: 'auto',
              alignment: 'right',
              text: `Month/Year: ${currentMonth}  |  Total Tasks: ${tasks.length}  |  Date: ${printDate}`,
              fontSize: 8.5,
              color: '#4B5563',
              margin: [0, 6, 0, 0],
            },
          ],
        },
        {
          canvas: [
            {
              type: 'line',
              x1: 0,
              y1: 5,
              x2: 810,
              y2: 5,
              lineWidth: 0.75,
              lineColor: '#CBD5E1',
            },
          ],
        },
      ],
    }),
    footer: (currentPage: number, pageCount: number) => ({
      margin: [16, 4, 16, 0],
      stack: [
        {
          canvas: [
            {
              type: 'line',
              x1: 0,
              y1: 0,
              x2: 810,
              y2: 0,
              lineWidth: 0.75,
              lineColor: '#CBD5E1',
            },
          ],
        },
        {
          margin: [0, 5, 0, 0],
          columns: [
            {
              width: '*',
              text: 'Accomplishment Plan DB • Confidential Internal Report',
              fontSize: 7.5,
              color: '#6B7280',
            },
            {
              width: 'auto',
              alignment: 'center',
              text: `Page ${currentPage} of ${pageCount}`,
              fontSize: 7.5,
              color: '#374151',
            },
            {
              width: '*',
              alignment: 'right',
              text: 'Developer by Md Al Helal Sarkar',
              fontSize: 7.5,
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
          paddingLeft: () => 2.5,
          paddingRight: () => 2.5,
          paddingTop: () => 3.5,
          paddingBottom: () => 3.5,
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
