import React from 'react';
import { getStatusColor, STANDARD_STATUS_CARDS } from '../utils/statusColors';

export const StatusColorCards: React.FC = () => {
  return (
    <section
      aria-label="Status color reference"
      className="mb-3"
      data-testid="status-color-cards"
    >
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-1.5 sm:gap-2">
        {STANDARD_STATUS_CARDS.map((statusName) => {
          const colors = getStatusColor(statusName);
          return (
            <div
              key={statusName}
              style={{
                backgroundColor: colors.bg,
                borderColor: colors.border,
                borderLeftColor: colors.accent,
                color: colors.text,
              }}
              className="flex items-center space-x-1.5 px-2.5 py-1.5 rounded border border-l-[3px] min-w-0"
            >
              <span
                className="w-2 h-2 rounded-full flex-shrink-0"
                style={{ backgroundColor: colors.dot }}
                aria-hidden="true"
              />
              <span className="text-[11px] sm:text-xs font-semibold truncate leading-tight">
                {statusName}
              </span>
            </div>
          );
        })}
      </div>
    </section>
  );
};
