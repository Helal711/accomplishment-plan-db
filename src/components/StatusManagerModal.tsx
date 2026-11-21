import React, { useState } from 'react';
import { Sliders, Plus, Trash2, X } from 'lucide-react';
import { StatusItem, DEFAULT_STATUSES } from '../types';
import { getStatusColor } from '../utils/statusColors';
import { isRemovedTaskStatus } from '../utils/statusOptions';

interface StatusManagerModalProps {
  isOpen: boolean;
  statuses: StatusItem[];
  onAddStatus: (statusName: string) => Promise<void>;
  onRemoveStatus: (statusId: string) => Promise<void>;
  onClose: () => void;
}

export const StatusManagerModal: React.FC<StatusManagerModalProps> = ({
  isOpen,
  statuses,
  onAddStatus,
  onRemoveStatus,
  onClose,
}) => {
  const [newStatus, setNewStatus] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const visibleCustomStatuses = statuses.filter(
    (s) => !s.isDefault && !isRemovedTaskStatus(s.statusName)
  );

  // Merge defaults and visible custom statuses for duplicate validation
  const allStatusNames = [
    ...DEFAULT_STATUSES,
    ...visibleCustomStatuses
      .filter((s) => !DEFAULT_STATUSES.map((d) => d.toLowerCase()).includes(s.statusName.toLowerCase()))
      .map((s) => s.statusName),
  ];

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    const val = newStatus.trim();
    if (!val) return;

    if (isRemovedTaskStatus(val)) {
      setError('This status option has been removed and cannot be added.');
      return;
    }

    if (allStatusNames.map((s) => s.toLowerCase()).includes(val.toLowerCase())) {
      setError('Status already exists');
      return;
    }

    try {
      setIsSubmitting(true);
      setError('');
      await onAddStatus(val);
      setNewStatus('');
    } catch (err: any) {
      setError(err.message || 'Failed to add status');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fadeIn">
      <div className="bg-white rounded-xl max-w-md w-full p-5 shadow-2xl border border-gray-100 flex flex-col max-h-[85vh]">
        <div className="flex items-center justify-between pb-3 border-b border-gray-100 flex-shrink-0">
          <div className="flex items-center space-x-2 text-app-primary">
            <Sliders className="w-5 h-5" />
            <h3 className="font-semibold text-gray-900 text-base">Dynamic Status Settings</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-xs text-gray-500 my-3">
          Manage task statuses. Default statuses are protected; custom statuses can be added or deleted.
        </p>

        {/* Status list */}
        <div className="flex-1 overflow-y-auto space-y-2 pr-1 my-2">
          {/* Default Statuses */}
          <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider px-1">
            Standard Statuses
          </div>
          <div className="grid grid-cols-2 gap-2">
            {DEFAULT_STATUSES.map((name) => {
              const colors = getStatusColor(name);
              return (
                <div
                  key={name}
                  style={{
                    backgroundColor: colors.bg,
                    borderColor: colors.border,
                    color: colors.text,
                  }}
                  className="flex items-center justify-between px-3 py-2 border rounded-lg text-xs font-medium"
                >
                  <div className="flex items-center space-x-1.5 truncate">
                    <span
                      className="w-2 h-2 rounded-full flex-shrink-0"
                      style={{ backgroundColor: colors.dot }}
                      aria-hidden="true"
                    />
                    <span className="truncate font-semibold">{name}</span>
                  </div>
                  <span className="text-[10px] bg-white/75 text-gray-700 px-1.5 py-0.5 rounded font-medium">
                    Default
                  </span>
                </div>
              );
            })}
          </div>

          {/* Custom Statuses */}
          <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider px-1 pt-3">
            Custom Statuses ({visibleCustomStatuses.length})
          </div>

          {visibleCustomStatuses.length === 0 ? (
            <p className="text-xs text-gray-400 italic px-1">No custom statuses yet.</p>
          ) : (
            <div className="space-y-1.5">
              {visibleCustomStatuses.map((st) => {
                  const colors = getStatusColor(st.statusName);
                  return (
                    <div
                      key={st.id}
                      style={{
                        backgroundColor: colors.bg,
                        borderColor: colors.border,
                        color: colors.text,
                      }}
                      className="flex items-center justify-between px-3 py-2 border rounded-lg text-xs font-medium"
                    >
                      <div className="flex items-center space-x-2">
                        <span
                          className="w-2 h-2 rounded-full flex-shrink-0"
                          style={{ backgroundColor: colors.dot }}
                          aria-hidden="true"
                        />
                        <span className="font-semibold">{st.statusName}</span>
                      </div>
                      <button
                        type="button"
                        onClick={() => onRemoveStatus(st.id)}
                        title="Remove status"
                        className="p-1 text-red-500 hover:text-red-700 hover:bg-white/60 rounded"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  );
                })}
            </div>
          )}
        </div>

        {/* Add status form */}
        <form onSubmit={handleAdd} className="pt-3 border-t border-gray-100 flex-shrink-0">
          <label className="block text-xs font-medium text-gray-700 mb-1">
            Add New Status
          </label>
          <div className="flex items-center space-x-2">
            <input
              type="text"
              value={newStatus}
              onChange={(e) => {
                setNewStatus(e.target.value);
                setError('');
              }}
              placeholder="e.g. Under Review"
              className="flex-1 px-3 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary"
            />
            <button
              type="submit"
              disabled={isSubmitting || !newStatus.trim()}
              className="flex items-center space-x-1 px-3 py-2 text-xs font-semibold text-white bg-app-primary hover:bg-app-dark-blue rounded-lg disabled:opacity-50 transition-colors"
            >
              <Plus className="w-4 h-4" />
              <span>Add</span>
            </button>
          </div>
          {error && <p className="text-xs text-red-600 mt-1">{error}</p>}
        </form>
      </div>
    </div>
  );
};
