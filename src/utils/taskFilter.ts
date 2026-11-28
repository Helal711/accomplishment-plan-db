import { TaskItem } from '../types';
import { normalizeBengaliDigits } from './purchaseAmount';

/**
 * Normalizes text for case-insensitive, whitespace-tolerant partial search.
 * Preserves Unicode (including Bengali) characters cleanly.
 */
export function normalizeSearchText(value: unknown): string {
  if (value === null || value === undefined) return '';
  return String(value).toLowerCase().replace(/\s+/g, ' ').trim();
}

/**
 * Normalizes amount strings so "12500", "12,500", and "12,500 TK" all produce comparable variants.
 */
export function normalizeAmountForSearch(amount: unknown): string[] {
  const raw = normalizeSearchText(amount);
  if (!raw) return [];

  const asciiRaw = normalizeBengaliDigits(raw);
  const noCommas = asciiRaw.replace(/,/g, '');
  const numericOnly = noCommas.replace(/\s*(tk|bdt|৳)\b/gi, '').replace(/৳/g, '').trim();
  const variants = new Set<string>([raw, asciiRaw, noCommas]);

  if (numericOnly) {
    variants.add(numericOnly);
    variants.add(`${numericOnly} tk`);
    const parsed = Number(numericOnly);
    if (Number.isFinite(parsed)) {
      const formatted = parsed.toLocaleString('en-US');
      variants.add(formatted.toLowerCase());
      variants.add(`${formatted.toLowerCase()} tk`);
    }
  }

  return Array.from(variants);
}

/**
 * Extracts all searchable task fields into normalized strings.
 * Explicitly excludes Month/Year (`monthYear`), which is controlled solely by the Month Dropdown.
 */
export function extractSearchableTaskFields(task: TaskItem): string[] {
  const taskWithOptionalPaymentType = task as TaskItem & { paymentType?: string };
  const amountVariants = normalizeAmountForSearch(task.amount);
  const purchasedAmountVariants = normalizeAmountForSearch(task.purchasedAmount);
  const unitPriceVariants = normalizeAmountForSearch(task.unitPrice);
  const totalQuantityVariants = normalizeAmountForSearch(task.totalQuantity);
  const taskNumStr =
    task.taskNumber !== undefined && task.taskNumber !== null ? String(task.taskNumber).trim() : '';

  const fields: string[] = [
    normalizeSearchText(task.description),
    ...amountVariants,
    ...purchasedAmountVariants,
    ...unitPriceVariants,
    ...totalQuantityVariants,
    normalizeSearchText(task.supplierName),
    normalizeSearchText(task.paymentMode),
    normalizeSearchText(taskWithOptionalPaymentType.paymentType),
    normalizeSearchText(task.accountName),
    normalizeSearchText(task.accountCode),
    normalizeSearchText(task.status),
    normalizeSearchText(task.category),
    normalizeSearchText(task.unit),
    normalizeSearchText(task.priority),
    normalizeSearchText(task.expectedDate),
    normalizeSearchText(task.remarks),
  ];

  if (taskNumStr) {
    fields.push(taskNumStr);
    fields.push(`#${taskNumStr}`);
  }

  return fields.filter((f) => f.length > 0);
}

/**
 * Checks whether a TaskItem matches the search query across relevant task fields
 * (never matching the Month/Year field).
 */
export function matchesSearchQuery(task: TaskItem, searchQuery: string): boolean {
  const normalizedQuery = normalizeSearchText(searchQuery);
  if (!normalizedQuery) return true;

  const searchableFields = extractSearchableTaskFields(task);
  if (searchableFields.some((field) => field.includes(normalizedQuery))) {
    return true;
  }

  // Also check comma-stripped / TK-stripped / Bengali-digit numeric query across amount & purchase fields
  const amountQuery = normalizeBengaliDigits(normalizedQuery)
    .replace(/,/g, '')
    .replace(/\s*(tk|bdt|৳)$/i, '')
    .replace(/^৳\s*/i, '')
    .trim();
  if (amountQuery && /\d/.test(amountQuery)) {
    const numericFields = [
      ...normalizeAmountForSearch(task.amount),
      ...normalizeAmountForSearch(task.purchasedAmount),
      ...normalizeAmountForSearch(task.unitPrice),
      ...normalizeAmountForSearch(task.totalQuantity),
    ];
    if (numericFields.some((field) => field.includes(amountQuery))) {
      return true;
    }
  }

  return false;
}

/**
 * Checks whether a TaskItem belongs to the selected Month/Year.
 */
export function matchesMonthFilter(task: TaskItem, selectedMonth?: string | null): boolean {
  const normalizedSelected = normalizeSearchText(selectedMonth);
  if (!normalizedSelected) return true;

  const normalizedTaskMonth = normalizeSearchText(task.monthYear);
  if (!normalizedTaskMonth) return true;

  return normalizedTaskMonth === normalizedSelected;
}

/**
 * Checks whether a TaskItem matches the selected status filter (null = Total Items / All).
 */
export function matchesStatusFilter(task: TaskItem, selectedStatus: string | null): boolean {
  if (!selectedStatus) return true;
  return normalizeSearchText(task.status) === normalizeSearchText(selectedStatus);
}

/**
 * Combines Month/Year scoping, Status filtering, and multi-field Search filtering simultaneously.
 * Search never matches the Month/Year field.
 */
export function filterTasks(
  tasks: TaskItem[],
  searchQuery: string,
  selectedStatus: string | null,
  selectedMonth?: string | null
): TaskItem[] {
  const hasSearch = normalizeSearchText(searchQuery).length > 0;
  const hasMonth = normalizeSearchText(selectedMonth).length > 0;
  if (!hasSearch && !selectedStatus && !hasMonth) return tasks;

  return tasks.filter(
    (task) =>
      matchesMonthFilter(task, selectedMonth) &&
      matchesStatusFilter(task, selectedStatus) &&
      matchesSearchQuery(task, searchQuery)
  );
}

export interface TaskFilterState {
  searchQuery: string;
  selectedStatus: string | null;
  selectedMonth: string;
}

/**
 * Clears only Search and Status filters while strictly preserving the currently selected Month/Year.
 */
export function clearSearchAndStatusFilters(state: TaskFilterState): TaskFilterState {
  return {
    searchQuery: '',
    selectedStatus: null,
    selectedMonth: state.selectedMonth,
  };
}

/**
 * Determines whether the Month/Year filter is actively narrowing tasks away from the default month.
 * If defaultMonth is omitted, any non-empty selectedMonth is treated as an active month filter.
 */
export function isMonthFilterActive(
  selectedMonth?: string | null,
  defaultMonth?: string | null
): boolean {
  const normalizedSelected = normalizeSearchText(selectedMonth);
  if (!normalizedSelected) return false;

  const normalizedDefault = normalizeSearchText(defaultMonth);
  if (!normalizedDefault) return true;

  return normalizedSelected !== normalizedDefault;
}

/**
 * Formats a concise, user-facing task result count label derived strictly from
 * the final filtered task array length and all active filters (Search, Status, Month/Year).
 */
export function formatTaskResultCountLabel(
  visibleCount: number,
  searchQuery: string,
  selectedStatus: string | null,
  selectedMonth?: string | null,
  defaultMonth?: string | null
): string {
  const safeCount = Number.isFinite(visibleCount) && visibleCount > 0 ? Math.floor(visibleCount) : 0;
  if (safeCount === 0) {
    return 'No Tasks Found (0)';
  }

  const hasSearch = normalizeSearchText(searchQuery).length > 0;
  const hasStatusFilter = normalizeSearchText(selectedStatus).length > 0;
  const hasMonthFilter = isMonthFilterActive(selectedMonth, defaultMonth);
  const taskNoun = safeCount === 1 ? 'Task' : 'Tasks';

  if (hasStatusFilter || hasMonthFilter) {
    return `Filtered Results: ${safeCount} ${taskNoun}`;
  }
  if (hasSearch) {
    return `Search Results: ${safeCount} ${taskNoun}`;
  }
  return `Total Tasks: ${safeCount}`;
}
