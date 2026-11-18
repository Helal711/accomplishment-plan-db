export interface StatusColorStyle {
  bg: string;
  text: string;
  border: string;
  accent: string;
  dot: string;
}

/**
 * Palette of high-contrast, light-tinted accessible colors for custom statuses.
 * Assigned deterministically based on normalized status name so every custom status
 * receives a consistent, visually distinct color everywhere in the app.
 */
const CUSTOM_STATUS_PALETTE: StatusColorStyle[] = [
  // Indigo
  { bg: '#E0E7FF', text: '#3730A3', border: '#A5B4FC', accent: '#4F46E5', dot: '#4F46E5' },
  // Sky
  { bg: '#E0F2FE', text: '#075985', border: '#7DD3FC', accent: '#0284C7', dot: '#0284C7' },
  // Rose
  { bg: '#FCE7F3', text: '#9D174D', border: '#F9A8D4', accent: '#DB2777', dot: '#DB2777' },
  // Lime
  { bg: '#ECFCCB', text: '#3F6212', border: '#BEF264', accent: '#65A30D', dot: '#65A30D' },
  // Fuchsia
  { bg: '#FAE8FF', text: '#86198F', border: '#F0ABFC', accent: '#C026D3', dot: '#C026D3' },
  // Cyan
  { bg: '#CFFAFE', text: '#155E75', border: '#67E8F9', accent: '#0891B2', dot: '#0891B2' },
  // Emerald
  { bg: '#D1FAE5', text: '#065F46', border: '#6EE7B7', accent: '#059669', dot: '#059669' },
  // Violet
  { bg: '#EDE9FE', text: '#5B21B6', border: '#C4B5FD', accent: '#7C3AED', dot: '#7C3AED' },
  // Coral / Warm Rose
  { bg: '#FFE4E6', text: '#9F1239', border: '#FDA4AF', accent: '#E11D48', dot: '#E11D48' },
  // Warm Yellow
  { bg: '#FEF9C3', text: '#854D0E', border: '#FDE047', accent: '#CA8A04', dot: '#CA8A04' },
];

function hashStatusName(normalized: string): number {
  let hash = 2166136261;
  for (let i = 0; i < normalized.length; i++) {
    hash ^= normalized.charCodeAt(i);
    hash = Math.imul(hash, 16777619);
  }
  return hash >>> 0;
}

/**
 * Single reusable status color mapping used consistently across:
 * - Top Status Summary filter buttons
 * - Task Card status badges
 * - Quick status update dropdown
 * - Task Status dropdown options
 * - Dynamic Status Settings modal
 */
export function getStatusColor(status: string): StatusColorStyle {
  const s = (status || '').trim().toLowerCase().replace(/\s+/g, ' ');

  if (!s) {
    return {
      bg: '#F1F5F9',
      text: '#334155',
      border: '#CBD5E1',
      accent: '#64748B',
      dot: '#64748B',
    };
  }

  // Done -> Green
  if (s === 'done') {
    return {
      bg: '#DCFCE7',
      text: '#166534',
      border: '#86EFAC',
      accent: '#16A34A',
      dot: '#16A34A',
    };
  }

  // WO Done / Work Order Done -> Blue
  if (s === 'wo done' || s === 'work order done') {
    return {
      bg: '#DBEAFE',
      text: '#1E40AF',
      border: '#93C5FD',
      accent: '#2563EB',
      dot: '#2563EB',
    };
  }

  // Pending -> Orange
  if (s === 'pending') {
    return {
      bg: '#FFEDD5',
      text: '#9A3412',
      border: '#FDBA74',
      accent: '#EA580C',
      dot: '#EA580C',
    };
  }

  // In Progress -> Purple
  if (s === 'in progress' || s === 'in-progress') {
    return {
      bg: '#F3E8FF',
      text: '#6B21A8',
      border: '#D8B4FE',
      accent: '#9333EA',
      dot: '#9333EA',
    };
  }

  // Cancelled / Cancel -> Red
  if (s === 'cancelled' || s === 'canceled' || s === 'cancel') {
    return {
      bg: '#FEE2E2',
      text: '#991B1B',
      border: '#FCA5A5',
      accent: '#DC2626',
      dot: '#DC2626',
    };
  }

  // Draft Ready -> Grey
  if (s === 'draft ready') {
    return {
      bg: '#F1F5F9',
      text: '#334155',
      border: '#CBD5E1',
      accent: '#64748B',
      dot: '#64748B',
    };
  }

  // Advance Ready -> Teal
  if (s === 'advance ready') {
    return {
      bg: '#CCFBF1',
      text: '#115E59',
      border: '#5EEAD4',
      accent: '#0D9488',
      dot: '#0D9488',
    };
  }

  // Hold from MR Team -> Amber
  if (s === 'hold from mr team') {
    return {
      bg: '#FEF3C7',
      text: '#92400E',
      border: '#FCD34D',
      accent: '#D97706',
      dot: '#D97706',
    };
  }

  // Semantic aliases for related workflow statuses
  if (s.startsWith('cancel')) {
    return {
      bg: '#FEE2E2',
      text: '#991B1B',
      border: '#FCA5A5',
      accent: '#DC2626',
      dot: '#DC2626',
    };
  }

  if (s.startsWith('hold')) {
    return {
      bg: '#FEF3C7',
      text: '#92400E',
      border: '#FCD34D',
      accent: '#D97706',
      dot: '#D97706',
    };
  }

  // Deterministic color assignment for all custom statuses
  const paletteIdx = hashStatusName(s) % CUSTOM_STATUS_PALETTE.length;
  return CUSTOM_STATUS_PALETTE[paletteIdx];
}
