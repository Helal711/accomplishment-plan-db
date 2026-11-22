/**
 * Centralized helpers for filtering selectable Task Status options and
 * Dashboard status filter chips without deleting or altering stored Firestore records.
 */

export function normalizeStatusKey(statusName: string | undefined | null): string {
  return (statusName || '').trim().replace(/\s+/g, ' ').toLowerCase();
}

/**
 * Statuses removed from all Task Status dropdowns, quick-update status menus,
 * status filters, and selectable status lists across Desktop & Mobile views.
 */
export const REMOVED_TASK_STATUSES: ReadonlySet<string> = new Set([
  'hold from md team',
  'hold from mr team',
  'cancel',
  'settlement',
  'cs ready',
  'cs in accounts',
]);

/**
 * Statuses hidden from the Dashboard status summary/filter bar across Desktop & Mobile views.
 */
export const HIDDEN_DASHBOARD_SUMMARY_STATUSES: ReadonlySet<string> = new Set([
  'hold from md team',
  'hold from mr team',
  'cancel',
  'settlement',
  'cs in accounts',
  'cs ready',
]);

export function isRemovedTaskStatus(statusName: string | undefined | null): boolean {
  return REMOVED_TASK_STATUSES.has(normalizeStatusKey(statusName));
}

export function isHiddenDashboardSummaryStatus(statusName: string | undefined | null): boolean {
  return HIDDEN_DASHBOARD_SUMMARY_STATUSES.has(normalizeStatusKey(statusName));
}

/**
 * Combines status candidates in order with case-insensitive deduplication,
 * excluding the four removed Task Status options.
 */
export function buildSelectableTaskStatuses(candidates: readonly (string | undefined | null)[]): string[] {
  const seen = new Set<string>();
  const result: string[] = [];

  for (const raw of candidates) {
    const trimmed = (raw || '').trim();
    if (!trimmed) continue;
    const key = normalizeStatusKey(trimmed);
    if (REMOVED_TASK_STATUSES.has(key) || seen.has(key)) continue;
    seen.add(key);
    result.push(trimmed);
  }

  return result;
}

export const DONE_STATUS_CHANGE_CONFIRM_TITLE = 'Need Confirmation:';

export const DONE_STATUS_CHANGE_CONFIRM_BODY =
  'This task is already marked as Done. Are you sure you want to change its status?';

export const DONE_TASK_UPDATE_CONFIRM_BODY =
  'This task is already marked as Done. Are you sure you want to update this task?';

export interface EditableTaskSnapshot {
  description: string;
  amount: string;
  unitPrice: string;
  totalQuantity: string;
  purchasedAmount: string;
  supplierName: string;
  priority: string;
  unit: string;
  paymentMode: string;
  expectedDate: string;
  accountName: string;
  accountCode: string;
  category: string;
  status: string;
  remarks: string;
}

export function normalizeEditableTaskSnapshot(
  input: Partial<EditableTaskSnapshot>
): EditableTaskSnapshot {
  return {
    description: String(input.description ?? '').trim(),
    amount: String(input.amount ?? '').replace(/\s*TK$/i, '').trim(),
    unitPrice: String(input.unitPrice ?? '').replace(/\s*TK$/i, '').trim(),
    totalQuantity: String(input.totalQuantity ?? '').replace(/\s*TK$/i, '').trim(),
    purchasedAmount: String(input.purchasedAmount ?? '').replace(/\s*TK$/i, '').trim(),
    supplierName: String(input.supplierName ?? '').trim(),
    priority: String(input.priority ?? '').trim(),
    unit: String(input.unit ?? '').trim(),
    paymentMode: String(input.paymentMode ?? '').trim(),
    expectedDate: String(input.expectedDate ?? '').trim(),
    accountName: String(input.accountName ?? '').trim(),
    accountCode: String(input.accountCode ?? '').trim(),
    category: String(input.category ?? '').trim(),
    status: String(input.status ?? '').trim(),
    remarks: String(input.remarks ?? '').trim(),
  };
}

export function hasEditableTaskChanges(
  original: EditableTaskSnapshot,
  edited: EditableTaskSnapshot
): boolean {
  const normalizedOriginal = normalizeEditableTaskSnapshot(original);
  const normalizedEdited = normalizeEditableTaskSnapshot(edited);
  return (Object.keys(normalizedOriginal) as Array<keyof EditableTaskSnapshot>).some(
    (key) => normalizedOriginal[key] !== normalizedEdited[key]
  );
}

export function isDoneStatus(statusName: string | undefined | null): boolean {
  return normalizeStatusKey(statusName) === 'done';
}

export function requiresDoneStatusChangeConfirmation(
  currentStatus: string | undefined | null,
  nextStatus: string | undefined | null
): boolean {
  if (!isDoneStatus(currentStatus)) return false;
  const nextKey = normalizeStatusKey(nextStatus);
  return nextKey.length > 0 && nextKey !== 'done';
}

export function requiresDoneTaskUpdateConfirmation(
  original: EditableTaskSnapshot,
  edited: EditableTaskSnapshot
): boolean {
  if (!isDoneStatus(original.status)) return false;
  return hasEditableTaskChanges(original, edited);
}



