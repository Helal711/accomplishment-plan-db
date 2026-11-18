export interface StatusColorStyle {
  bg: string;
  text: string;
  border: string;
  accent: string;
  dot: string;
}

export const STANDARD_STATUS_CARDS = [
  'Done',
  'WO Done',
  'Pending',
  'In Progress',
  'Cancelled',
  'Draft Ready',
  'Advance Ready',
  'Hold from MR Team',
] as const;

/**
 * Single reusable status color mapping used consistently across:
 * - Compact Status Color Cards
 * - Task Card Status Badges
 * - Status Dropdown Options
 * - Status Summary Bar indicators
 * - Dynamic Status Settings
 */
export function getStatusColor(status: string): StatusColorStyle {
  const s = (status || '').trim().toLowerCase().replace(/\s+/g, ' ');

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

  // Custom / fallback status -> Neutral Grey
  return {
    bg: '#F3F4F6',
    text: '#4B5563',
    border: '#D1D5DB',
    accent: '#9CA3AF',
    dot: '#9CA3AF',
  };
}
