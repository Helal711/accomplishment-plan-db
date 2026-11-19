import { TaskItem } from '../types';

export interface PurchaseAmountState {
  unitPrice: string;
  totalQuantity: string;
  purchasedAmount: string;
  /**
   * True when purchasedAmount was entered/edited manually by the user (or loaded from
   * a saved task whose purchasedAmount does not match unitPrice * totalQuantity).
   */
  isManualOverride: boolean;
}

const BENGALI_DIGITS: Record<string, string> = {
  '০': '0',
  '১': '1',
  '২': '2',
  '৩': '3',
  '৪': '4',
  '৫': '5',
  '৬': '6',
  '৭': '7',
  '৮': '8',
  '৯': '9',
};

/**
 * Converts Bengali digits (০-৯) to standard ASCII digits (0-9) while preserving all other characters.
 */
export function normalizeBengaliDigits(input: string): string {
  return input.replace(/[০-৯]/g, (digit) => BENGALI_DIGITS[digit] ?? digit);
}

/**
 * Parses an optional numeric input (English or Bengali digits, optional commas, decimals, or TK/৳ suffix).
 * Returns null if empty or not a valid finite number — never coerces blank inputs to 0.
 */
export function parseOptionalNumber(raw: unknown): number | null {
  if (raw === null || raw === undefined) return null;
  if (typeof raw === 'number') {
    return Number.isFinite(raw) ? raw : null;
  }

  const normalized = normalizeBengaliDigits(String(raw))
    .replace(/,/g, '')
    .replace(/\s*(tk|bdt|৳)\s*$/i, '')
    .replace(/^৳\s*/i, '')
    .trim();

  if (!normalized) return null;
  if (!/^-?(\d+(\.\d+)?|\.\d+)$/.test(normalized)) return null;

  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : null;
}

/**
 * Strips trailing "TK" from a stored value for clean form input display.
 */
export function sanitizeOptionalFieldForInput(raw: unknown): string {
  if (raw === null || raw === undefined) return '';
  return String(raw).replace(/\s*tk$/i, '').trim();
}

/**
 * Formats a calculated numeric value cleanly with en-US thousand separators and up to 6 decimal digits,
 * avoiding IEEE-754 floating-point noise (e.g., 0.1 * 0.2 -> "0.02", 500 * 10 -> "5,000").
 */
export function formatCalculatedNumber(value: number): string {
  if (!Number.isFinite(value)) return '';
  const rounded = Number(value.toFixed(6));
  const safeValue = Object.is(rounded, -0) ? 0 : rounded;
  if (Number.isInteger(safeValue)) {
    return safeValue.toLocaleString('en-US');
  }
  return safeValue.toLocaleString('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 6,
  });
}

/**
 * Calculates Purchased Amount = Unit Price × Total Quantity ONLY when both inputs are valid numbers.
 * Returns null if either Unit Price or Total Quantity is missing or invalid (never produces a misleading 0).
 */
export function calculatePurchasedAmount(
  unitPriceRaw: unknown,
  totalQuantityRaw: unknown
): string | null {
  const unitPrice = parseOptionalNumber(unitPriceRaw);
  const totalQuantity = parseOptionalNumber(totalQuantityRaw);
  if (unitPrice === null || totalQuantity === null) return null;

  const product = unitPrice * totalQuantity;
  if (!Number.isFinite(product)) return null;

  return formatCalculatedNumber(product);
}

/**
 * Formats an optional Purchased Amount for UI/export display (e.g., "5,000 TK").
 * Returns null if empty/missing so callers can omit it cleanly instead of displaying "0 TK".
 */
export function formatPurchasedAmountDisplay(raw: unknown): string | null {
  if (raw === null || raw === undefined) return null;
  const trimmed = String(raw).trim();
  if (!trimmed) return null;

  const parsed = parseOptionalNumber(trimmed);
  if (parsed !== null) {
    return `${formatCalculatedNumber(parsed)} TK`;
  }

  return /\btk$/i.test(trimmed) ? trimmed : `${trimmed} TK`;
}

/**
 * Formats an optional unit price or quantity cleanly for display/export (returns '' when absent).
 */
export function formatOptionalNumericText(raw: unknown): string {
  if (raw === null || raw === undefined) return '';
  const trimmed = String(raw).trim();
  if (!trimmed) return '';

  const parsed = parseOptionalNumber(trimmed);
  if (parsed !== null) {
    return formatCalculatedNumber(parsed);
  }
  return trimmed;
}

/**
 * Initializes the three optional purchase fields from an existing TaskItem (or empty for a new task).
 */
export function initPurchaseAmountState(task?: Partial<TaskItem>): PurchaseAmountState {
  const unitPrice = sanitizeOptionalFieldForInput(task?.unitPrice);
  const totalQuantity = sanitizeOptionalFieldForInput(task?.totalQuantity);
  const purchasedAmount = sanitizeOptionalFieldForInput(task?.purchasedAmount);

  const computed = calculatePurchasedAmount(unitPrice, totalQuantity);
  const parsedPurchased = parseOptionalNumber(purchasedAmount);
  const parsedComputed = computed !== null ? parseOptionalNumber(computed) : null;

  const matchesAuto =
    purchasedAmount.length > 0 &&
    parsedPurchased !== null &&
    parsedComputed !== null &&
    Math.abs(parsedPurchased - parsedComputed) < 1e-6;

  return {
    unitPrice,
    totalQuantity,
    purchasedAmount,
    isManualOverride: purchasedAmount.length > 0 && !matchesAuto,
  };
}

/**
 * Pure non-circular transition when Unit Price changes:
 * - If both Unit Price and Total Quantity are valid numbers, updates Purchased Amount automatically.
 * - If either input is missing/invalid:
 *   - Clears auto-calculated Purchased Amount (avoiding misleading 0 or stale auto totals).
 *   - Preserves a manually entered Purchased Amount.
 */
export function applyUnitPriceChange(
  state: PurchaseAmountState,
  nextUnitPrice: string
): PurchaseAmountState {
  const calculated = calculatePurchasedAmount(nextUnitPrice, state.totalQuantity);
  if (calculated !== null) {
    return {
      unitPrice: nextUnitPrice,
      totalQuantity: state.totalQuantity,
      purchasedAmount: calculated,
      isManualOverride: false,
    };
  }

  return {
    unitPrice: nextUnitPrice,
    totalQuantity: state.totalQuantity,
    purchasedAmount: state.isManualOverride ? state.purchasedAmount : '',
    isManualOverride: state.isManualOverride,
  };
}

/**
 * Pure non-circular transition when Total Quantity changes.
 */
export function applyTotalQuantityChange(
  state: PurchaseAmountState,
  nextTotalQuantity: string
): PurchaseAmountState {
  const calculated = calculatePurchasedAmount(state.unitPrice, nextTotalQuantity);
  if (calculated !== null) {
    return {
      unitPrice: state.unitPrice,
      totalQuantity: nextTotalQuantity,
      purchasedAmount: calculated,
      isManualOverride: false,
    };
  }

  return {
    unitPrice: state.unitPrice,
    totalQuantity: nextTotalQuantity,
    purchasedAmount: state.isManualOverride ? state.purchasedAmount : '',
    isManualOverride: state.isManualOverride,
  };
}

/**
 * Pure non-circular transition when the user directly types/edits Purchased Amount.
 * Always accepts manual input and never mutates Unit Price or Total Quantity.
 */
export function applyManualPurchasedAmountChange(
  state: PurchaseAmountState,
  nextPurchasedAmount: string
): PurchaseAmountState {
  const trimmed = nextPurchasedAmount.trim();
  return {
    ...state,
    purchasedAmount: nextPurchasedAmount,
    isManualOverride: trimmed.length > 0,
  };
}
