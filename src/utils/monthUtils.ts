export const MONTH_ABBRS = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
] as const;

const MONTH_PATTERN = /^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)-(\d{4})$/i;
const STORAGE_PATTERN = /^(\d{4})-(\d{2})$/;

/**
 * Returns the real current month & year in Mmm-YYYY format (e.g. "Nov-2026").
 */
export function getCurrentMonthYear(now: Date = new Date()): string {
  const monthName = MONTH_ABBRS[now.getMonth()];
  const year = now.getFullYear();
  return `${monthName}-${year}`;
}

/**
 * Parses a "Mmm-YYYY" string into { monthIdx (0-11), year } or null if custom/non-standard.
 */
export function parseMonthYear(monthYear: string): { monthIdx: number; year: number } | null {
  const match = (monthYear || '').trim().match(MONTH_PATTERN);
  if (!match) return null;
  const abbr = match[1].charAt(0).toUpperCase() + match[1].slice(1).toLowerCase();
  const monthIdx = MONTH_ABBRS.indexOf(abbr as (typeof MONTH_ABBRS)[number]);
  const year = parseInt(match[2], 10);
  if (monthIdx === -1 || Number.isNaN(year)) return null;
  return { monthIdx, year };
}

/**
 * Converts UI/Firestore month format ("Mmm-YYYY") to storage format ("YYYY-MM").
 * Custom non-standard month labels are preserved as-is.
 */
export function toStorageMonth(monthYear: string): string {
  const parsed = parseMonthYear(monthYear);
  if (!parsed) return (monthYear || '').trim();
  const mm = String(parsed.monthIdx + 1).padStart(2, '0');
  return `${parsed.year}-${mm}`;
}

/**
 * Converts storage format ("YYYY-MM") back to UI/Firestore format ("Mmm-YYYY").
 * Also normalizes "Mmm-YYYY" or preserves custom strings.
 */
export function fromStorageMonth(stored: string): string {
  const trimmed = (stored || '').trim();
  if (!trimmed) return getCurrentMonthYear();

  const storageMatch = trimmed.match(STORAGE_PATTERN);
  if (storageMatch) {
    const year = parseInt(storageMatch[1], 10);
    const monthNum = parseInt(storageMatch[2], 10);
    if (monthNum >= 1 && monthNum <= 12 && !Number.isNaN(year)) {
      return `${MONTH_ABBRS[monthNum - 1]}-${year}`;
    }
  }

  const parsed = parseMonthYear(trimmed);
  if (parsed) {
    return `${MONTH_ABBRS[parsed.monthIdx]}-${parsed.year}`;
  }

  return trimmed;
}

export function getSelectedMonthStorageKey(uid: string): string {
  return `selectedMonth_${uid}`;
}

export function getCustomMonthsStorageKey(uid: string): string {
  return `customMonths_${uid}`;
}

/**
 * Restores the user's saved month preference from localStorage (scoped by Firebase UID).
 * Falls back to the real current month & year on first visit when no saved month exists.
 */
export function getSavedMonthForUser(uid?: string | null): string {
  if (!uid) return getCurrentMonthYear();
  try {
    const saved = window.localStorage.getItem(getSelectedMonthStorageKey(uid));
    if (saved && saved.trim()) {
      return fromStorageMonth(saved);
    }
  } catch (err) {
    console.warn('Unable to read saved month from localStorage:', err);
  }
  return getCurrentMonthYear();
}

/**
 * Persists the user's selected month preference to localStorage in YYYY-MM format under selectedMonth_${uid}.
 */
export function saveMonthForUser(uid: string | undefined | null, monthYear: string): void {
  if (!uid || !monthYear.trim()) return;
  try {
    window.localStorage.setItem(getSelectedMonthStorageKey(uid), toStorageMonth(monthYear));
  } catch (err) {
    console.warn('Unable to save selected month to localStorage:', err);
  }
}

/**
 * Loads user-scoped custom months from localStorage.
 */
export function getCustomMonthsForUser(uid?: string | null): string[] {
  if (!uid) return [];
  try {
    const raw = window.localStorage.getItem(getCustomMonthsStorageKey(uid));
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed.filter((m): m is string => typeof m === 'string' && m.trim().length > 0);
    }
  } catch (err) {
    console.warn('Unable to read custom months from localStorage:', err);
  }
  return [];
}

/**
 * Saves a custom month to the user's localStorage list.
 */
export function saveCustomMonthForUser(uid: string | undefined | null, newMonth: string): string[] {
  const trimmed = newMonth.trim();
  if (!uid || !trimmed) return [];
  const existing = getCustomMonthsForUser(uid);
  if (existing.some((m) => m.toLowerCase() === trimmed.toLowerCase())) {
    return existing;
  }
  const updated = [...existing, trimmed];
  try {
    window.localStorage.setItem(getCustomMonthsStorageKey(uid), JSON.stringify(updated));
  } catch (err) {
    console.warn('Unable to save custom months to localStorage:', err);
  }
  return updated;
}
