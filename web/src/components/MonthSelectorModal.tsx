import React, { useState } from 'react';
import { Calendar, Plus, X, Check } from 'lucide-react';

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
  const [newMonthInput, setNewMonthInput] = useState('');
  const [isAdding, setIsAdding] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleAdd = (e: React.FormEvent) => {
    e.preventDefault();
    const val = newMonthInput.trim();
    if (!val) return;

    // Pattern e.g. "Dec-2026" or "Jan-2027"
    if (availableMonths.map((m) => m.toLowerCase()).includes(val.toLowerCase())) {
      setError('Month already exists in the list');
      return;
    }

    onAddMonth(val);
    onSelectMonth(val);
    setNewMonthInput('');
    setIsAdding(false);
    setError('');
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fadeIn">
      <div className="bg-white rounded-xl max-w-sm w-full p-5 shadow-2xl border border-gray-100">
        <div className="flex items-center justify-between pb-3 border-b border-gray-100">
          <div className="flex items-center space-x-2 text-app-primary">
            <Calendar className="w-5 h-5" />
            <h3 className="font-semibold text-gray-900 text-base">Select Month & Year</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="my-4 max-h-64 overflow-y-auto space-y-1.5 pr-1">
          {availableMonths.map((month) => {
            const isSelected = month === selectedMonth;
            return (
              <button
                key={month}
                onClick={() => {
                  onSelectMonth(month);
                  onClose();
                }}
                className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all ${
                  isSelected
                    ? 'bg-blue-50 text-app-primary border border-blue-200'
                    : 'text-gray-700 hover:bg-gray-50 border border-transparent'
                }`}
              >
                <span>{month}</span>
                {isSelected && <Check className="w-4 h-4 text-app-primary" />}
              </button>
            );
          })}
        </div>

        {isAdding ? (
          <form onSubmit={handleAdd} className="pt-2 border-t border-gray-100">
            <label className="block text-xs font-medium text-gray-700 mb-1">
              Add New Month (e.g. Feb-2027)
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
