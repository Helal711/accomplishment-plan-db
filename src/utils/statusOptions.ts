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
