import React, { useState, useEffect, useMemo } from 'react';
import { Calendar, Plus, X, Check, ChevronLeft, ChevronRight } from 'lucide-react';
import { MONTH_ABBRS, getCurrentMonthYear, parseMonthYear } from '../utils/monthUtils';

interface MonthSelectorModalProps {
  isOpen: boolean;
  selectedMonth: string;
  availableMonths: string[];
  onSelectMonth: (month: string) => void;
  onAddMonth: (month: string) => void;
  onClose: () => void;
}

export const MonthSelectorModal: React.FC<MonthSelectorModalProps> = ({
  isOpen,
  selectedMonth,
  availableMonths,
  onSelectMonth,
  onAddMonth,
  onClose,
}) => {
  const realCurrentMonth = useMemo(() => getCurrentMonthYear(), []);
  const realCurrentYear = new Date().getFullYear();

  const [pickerYear, setPickerYear] = useState<number>(() => {
    return parseMonthYear(selectedMonth)?.year ?? realCurrentYear;
  });
  const [newMonthInput, setNewMonthInput] = useState('');
  const [isAdding, setIsAdding] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      const parsed = parseMonthYear(selectedMonth);
      setPickerYear(parsed?.year ?? realCurrentYear);
      setIsAdding(false);
      setError('');
      setNewMonthInput('');
    }
  }, [isOpen, selectedMonth, realCurrentYear]);

  if (!isOpen) return null;

  const yearOptions = (() => {
    const minYear = Math.min(pickerYear - 2, realCurrentYear - 5);
    const maxYear = Math.max(pickerYear + 2, realCurrentYear + 5);
    const list: number[] = [];
    for (let y = minYear; y <= maxYear; y++) {
      list.push(y);
    }
    return list;
  })();

  // Non-standard custom months added by user (standard Mmm-YYYY months are directly selectable in the grid)
  const customOnlyMonths = availableMonths.filter((m) => !parseMonthYear(m));

  const handleAdd = (e: React.FormEvent) => {
    e.preventDefault();
    const val = newMonthInput.trim();
    if (!val) return;

    // Normalize standard Mmm-YYYY input if matched
    const parsed = parseMonthYear(val);
    const normalizedVal = parsed ? `${MONTH_ABBRS[parsed.monthIdx]}-${parsed.year}` : val;

    if (!parsed && availableMonths.map((m) => m.toLowerCase()).includes(normalizedVal.toLowerCase())) {
      setError('Month already exists in the list');
      return;
    }

    onAddMonth(normalizedVal);
    onSelectMonth(normalizedVal);
    setNewMonthInput('');
    setIsAdding(false);
    setError('');
    onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fadeIn"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-xl max-w-sm w-full p-5 shadow-2xl border border-gray-100"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between pb-3 border-b border-gray-100">
          <div className="flex items-center space-x-2 text-app-primary">
            <Calendar className="w-5 h-5" />
            <h3 className="font-semibold text-gray-900 text-base">Select Month & Year</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close month selector"
            className="p-1 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Year Navigation Bar + Quick Current Month Button */}
        <div className="flex items-center justify-between mt-3 mb-3 bg-gray-50 border border-gray-200 rounded-lg p-1.5">
          <div className="flex items-center space-x-1">
            <button
              type="button"
              onClick={() => setPickerYear((y) => y - 1)}
              aria-label="Previous year"
              className="p-1.5 rounded-md text-gray-600 hover:text-app-primary hover:bg-white transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>

            <select
              aria-label="Select year"
              value={pickerYear}
              onChange={(e) => setPickerYear(parseInt(e.target.value, 10))}
              className="bg-white border border-gray-200 rounded-md px-2.5 py-1 text-sm font-bold text-gray-800 focus:outline-none focus:ring-2 focus:ring-app-primary cursor-pointer"
            >
              {yearOptions.map((yr) => (
                <option key={yr} value={yr}>
                  {yr}
                </option>
              ))}
            </select>

            <button
              type="button"
              onClick={() => setPickerYear((y) => y + 1)}
              aria-label="Next year"
              className="p-1.5 rounded-md text-gray-600 hover:text-app-primary hover:bg-white transition-colors"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          <button
            type="button"
            onClick={() => {
              onSelectMonth(realCurrentMonth);
              onClose();
            }}
            className="px-2.5 py-1 text-xs font-semibold text-app-primary bg-blue-50 hover:bg-blue-100 border border-blue-200 rounded-md transition-colors"
            title={`Jump to ${realCurrentMonth}`}
          >
            Current
          </button>
        </div>

        {/* 12-Month Grid Picker */}
        <div className="grid grid-cols-3 sm:grid-cols-4 gap-2 mb-4">
          {MONTH_ABBRS.map((abbr) => {
            const monthVal = `${abbr}-${pickerYear}`;
            const isSelected = monthVal.toLowerCase() === selectedMonth.toLowerCase();
            const isCurrentReal = monthVal.toLowerCase() === realCurrentMonth.toLowerCase();

            return (
              <button
                key={monthVal}
                type="button"
                onClick={() => {
                  onSelectMonth(monthVal);
                  onClose();
                }}
                className={`relative flex items-center justify-center py-2 px-2 rounded-lg text-xs sm:text-sm font-medium border transition-all ${
                  isSelected
                    ? 'bg-app-primary text-white border-app-primary shadow-sm font-bold'
                    : isCurrentReal
                    ? 'bg-blue-50/70 text-app-primary border-blue-300 font-semibold hover:bg-blue-100/70'
                    : 'bg-white text-gray-700 border-gray-200 hover:border-blue-300 hover:bg-gray-50'
                }`}
              >
                <span>{abbr}</span>
                {isCurrentReal && !isSelected && (
                  <span
                    className="absolute top-1 right-1 w-1.5 h-1.5 rounded-full bg-app-primary"
                    title="Current month"
                  />
                )}
              </button>
            );
          })}
        </div>

        {/* Custom Non-Standard Months (if any exist) */}
        {customOnlyMonths.length > 0 && (
          <div className="mb-3 pt-2 border-t border-gray-100">
            <p className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider mb-1.5">
              Custom Months
            </p>
            <div className="max-h-28 overflow-y-auto space-y-1 pr-1">
              {customOnlyMonths.map((month) => {
                const isSelected = month === selectedMonth;
                return (
                  <button
                    key={month}
                    type="button"
                    onClick={() => {
                      onSelectMonth(month);
                      onClose();
                    }}
                    className={`w-full flex items-center justify-between px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                      isSelected
                        ? 'bg-blue-50 text-app-primary border border-blue-200'
                        : 'text-gray-700 hover:bg-gray-50 border border-transparent'
                    }`}
                  >
                    <span>{month}</span>
                    {isSelected && <Check className="w-3.5 h-3.5 text-app-primary" />}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* Preserved Custom Month Management */}
        {isAdding ? (
          <form onSubmit={handleAdd} className="pt-2 border-t border-gray-100">
            <label className="block text-xs font-medium text-gray-700 mb-1">
              Add Custom Month (e.g. Feb-2027)
            </label>
            <div className="flex items-center space-x-2">
              <input
                type="text"
                value={newMonthInput}
                onChange={(e) => {
                  setNewMonthInput(e.target.value);
                  setError('');
                }}
                placeholder="Mmm-YYYY"
                autoFocus
                className="flex-1 px-3 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent"
              />
              <button
                type="submit"
                className="px-3 py-1.5 text-xs font-semibold text-white bg-app-primary hover:bg-app-dark-blue rounded-lg transition-colors"
              >
                Save
              </button>
              <button
                type="button"
                onClick={() => setIsAdding(false)}
                className="px-2 py-1.5 text-xs text-gray-500 hover:bg-gray-100 rounded-lg"
              >
                Cancel
              </button>
            </div>
            {error && <p className="text-xs text-red-600 mt-1">{error}</p>}
          </form>
        ) : (
          <button
            type="button"
            onClick={() => setIsAdding(true)}
            className="w-full flex items-center justify-center space-x-1.5 py-2 text-xs font-semibold text-app-primary hover:bg-blue-50 rounded-lg border border-dashed border-blue-300 transition-colors"
          >
            <Plus className="w-4 h-4" />
            <span>Add Custom Month</span>
          </button>
        )}
      </div>
    </div>
  );
};
