import { TaskItem } from '../types';

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

  const noCommas = raw.replace(/,/g, '');
  const numericOnly = noCommas.replace(/\s*tk\b/gi, '').trim();
  const variants = new Set<string>([raw, noCommas]);

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
 * Extracts all searchable fields from a TaskItem into normalized strings.
 */
export function extractSearchableTaskFields(task: TaskItem, fallbackMonth?: string): string[] {
  const taskWithOptionalPaymentType = task as TaskItem & { paymentType?: string };
  const amountVariants = normalizeAmountForSearch(task.amount);
  const taskNumStr =
    task.taskNumber !== undefined && task.taskNumber !== null ? String(task.taskNumber).trim() : '';

  const fields: string[] = [
    normalizeSearchText(task.description),
    ...amountVariants,
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
    normalizeSearchText(task.monthYear || fallbackMonth),
  ];

  if (taskNumStr) {
    fields.push(taskNumStr);
    fields.push(`#${taskNumStr}`);
  }

  return fields.filter((f) => f.length > 0);
}

/**
 * Checks whether a TaskItem matches the search query across all relevant task fields.
 */
export function matchesSearchQuery(
  task: TaskItem,
  searchQuery: string,
  fallbackMonth?: string
): boolean {
  const normalizedQuery = normalizeSearchText(searchQuery);
  if (!normalizedQuery) return true;

  const searchableFields = extractSearchableTaskFields(task, fallbackMonth);
  if (searchableFields.some((field) => field.includes(normalizedQuery))) {
    return true;
  }

  // Also check comma-stripped / TK-stripped amount query (e.g. searching "12,500 TK" or "12,500")
  const amountQuery = normalizedQuery.replace(/,/g, '').replace(/\s*tk$/i, '').trim();
  if (amountQuery && /\d/.test(amountQuery)) {
    const amountFields = normalizeAmountForSearch(task.amount);
    if (amountFields.some((field) => field.includes(amountQuery))) {
      return true;
    }
  }

  return false;
}

/**
 * Checks whether a TaskItem matches the selected status filter (null = Total Items / All).
 */
export function matchesStatusFilter(task: TaskItem, selectedStatus: string | null): boolean {
  if (!selectedStatus) return true;
  return normalizeSearchText(task.status) === normalizeSearchText(selectedStatus);
}

/**
 * Combines status filtering and multi-field search filtering simultaneously.
 */
export function filterTasks(
  tasks: TaskItem[],
  searchQuery: string,
  selectedStatus: string | null,
  fallbackMonth?: string
): TaskItem[] {
  const hasSearch = normalizeSearchText(searchQuery).length > 0;
  if (!hasSearch && !selectedStatus) return tasks;

  return tasks.filter(
    (task) =>
      matchesStatusFilter(task, selectedStatus) &&
      matchesSearchQuery(task, searchQuery, fallbackMonth)
  );
}
