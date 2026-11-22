/**
 * Timezone-safe date formatting and parsing helpers for Completion Date.
 * Uses pure integer arithmetic and month lookup tables (never UTC-parsed Date objects)
 * so selected dates never shift across timezones or DST boundaries.
 */

const SHORT_MONTHS = [
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

const MONTH_NAME_TO_NUMBER: Record<string, number> = {
  jan: 1,
  january: 1,
  feb: 2,
  february: 2,
  mar: 3,
  march: 3,
  apr: 4,
  april: 4,
  may: 5,
  jun: 6,
  june: 6,
  jul: 7,
  july: 7,
  aug: 8,
  august: 8,
  sep: 9,
  sept: 9,
  september: 9,
  oct: 10,
  october: 10,
  nov: 11,
  november: 11,
  dec: 12,
  december: 12,
};

function isLeapYear(year: number): boolean {
  return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}

export function isValidCalendarDate(year: number, month: number, day: number): boolean {
  if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) {
    return false;
  }
  if (year < 1000 || year > 9999 || month < 1 || month > 12 || day < 1) {
    return false;
  }
  const daysInMonth = [
    31,
    isLeapYear(year) ? 29 : 28,
    31,
    30,
    31,
    30,
    31,
    31,
    30,
    31,
    30,
    31,
  ][month - 1];
  return day <= daysInMonth;
}

function pad2(n: number): string {
  return String(n).padStart(2, '0');
}

function toIsoString(year: number, month: number, day: number): string {
  if (!isValidCalendarDate(year, month, day)) return '';
  return `${year}-${pad2(month)}-${pad2(day)}`;
}

/**
 * Converts an HTML date input value (YYYY-MM-DD) into the app's display format (DD MMM YYYY).
 * Example: "2026-11-22" -> "22 Nov 2026", "2026-01-05" -> "05 Jan 2026".
 * Returns empty string for blank or invalid values.
 */
export function formatIsoDateToDisplay(isoDate: string): string {
  const trimmed = (isoDate || '').trim();
  if (!trimmed) return '';

  const match = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(trimmed);
  if (!match) return '';

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  if (!isValidCalendarDate(year, month, day)) return '';

  return `${pad2(day)} ${SHORT_MONTHS[month - 1]} ${year}`;
}

/**
 * Converts a stored date string (such as "17 Nov 2026", "8 Nov 2026", "2026-11-17",
 * "17-Nov-2026", "Nov 17, 2026", or "17/11/2026") into an HTML date input value (YYYY-MM-DD).
 * Returns empty string when blank or when the stored string is an unparseable historical note
 * (allowing the raw historical text to remain preserved untouched in state).
 */
export function parseDisplayDateToIso(storedDate: string | undefined | null): string {
  const trimmed = (storedDate || '').trim();
  if (!trimmed) return '';

  // 1. ISO format: YYYY-MM-DD (or YYYY/MM/DD)
  const isoMatch = /^(\d{4})[-/](\d{1,2})[-/](\d{1,2})$/.exec(trimmed);
  if (isoMatch) {
    return toIsoString(Number(isoMatch[1]), Number(isoMatch[2]), Number(isoMatch[3]));
  }

  // 2. App standard format: DD MMM YYYY / D MMM YYYY / DD-MMM-YYYY / DD Month YYYY
  const dmyTextMatch = /^(\d{1,2})[\s,-]+([A-Za-z]+)[,\s-]+(\d{4})$/.exec(trimmed);
  if (dmyTextMatch) {
    const day = Number(dmyTextMatch[1]);
    const month = MONTH_NAME_TO_NUMBER[dmyTextMatch[2].toLowerCase()];
    const year = Number(dmyTextMatch[3]);
    if (month) {
      return toIsoString(year, month, day);
    }
  }

  // 3. Text month-first format: MMM DD, YYYY / Month D YYYY
  const mdyTextMatch = /^([A-Za-z]+)[\s-]+(\d{1,2})[,\s-]+(\d{4})$/.exec(trimmed);
  if (mdyTextMatch) {
    const month = MONTH_NAME_TO_NUMBER[mdyTextMatch[1].toLowerCase()];
    const day = Number(mdyTextMatch[2]);
    const year = Number(mdyTextMatch[3]);
    if (month) {
      return toIsoString(year, month, day);
    }
  }

  // 4. Numeric DD/MM/YYYY or DD-MM-YYYY (falls back to MM/DD/YYYY only if 2nd number > 12)
  const numericMatch = /^(\d{1,2})[-/](\d{1,2})[-/](\d{4})$/.exec(trimmed);
  if (numericMatch) {
    const first = Number(numericMatch[1]);
    const second = Number(numericMatch[2]);
    const year = Number(numericMatch[3]);
    if (second <= 12) {
      return toIsoString(year, second, first);
    }
    if (first <= 12) {
      return toIsoString(year, first, second);
    }
  }

  return '';
}
