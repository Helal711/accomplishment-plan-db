import React, { useEffect } from 'react';
import {
  DONE_STATUS_CHANGE_CONFIRM_BODY,
  DONE_STATUS_CHANGE_CONFIRM_TITLE,
} from '../utils/statusOptions';

interface DoneStatusConfirmDialogProps {
  isOpen: boolean;
  isSubmitting?: boolean;
  errorMessage?: string | null;
  onCancel: () => void;
  onConfirm: () => void;
}

export const DoneStatusConfirmDialog: React.FC<DoneStatusConfirmDialogProps> = ({
  isOpen,
  isSubmitting = false,
  errorMessage = null,
  onCancel,
  onConfirm,
}) => {
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !isSubmitting) {
        onCancel();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isSubmitting, onCancel]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 animate-fadeIn"
      onClick={() => {
        if (!isSubmitting) onCancel();
      }}
    >
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="done-status-confirm-title"
        aria-describedby="done-status-confirm-body"
        data-testid="done-status-confirm-dialog"
        className="w-full max-w-[340px] rounded-xl bg-white px-5 py-4 shadow-xl border border-gray-200"
        onClick={(e) => e.stopPropagation()}
      >
        <p
          id="done-status-confirm-title"
          className="text-sm font-bold text-gray-900 leading-snug"
        >
          {DONE_STATUS_CHANGE_CONFIRM_TITLE}
        </p>

        <p
          id="done-status-confirm-body"
          className="mt-2 text-[13px] text-gray-700 leading-relaxed"
        >
          {DONE_STATUS_CHANGE_CONFIRM_BODY}
        </p>

        {errorMessage && (
          <p
            role="alert"
            className="mt-2.5 text-xs text-red-600 bg-red-50 border border-red-200 rounded-md px-2.5 py-1.5"
          >
            {errorMessage}
          </p>
        )}

        <div className="mt-4 flex items-center justify-end gap-2">
          <button
            type="button"
            disabled={isSubmitting}
            onClick={onCancel}
            data-testid="done-status-cancel-btn"
            className="px-3.5 py-1.5 text-xs font-semibold tracking-wide text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md transition-colors disabled:opacity-50"
          >
            CANCEL
          </button>
          <button
            type="button"
            disabled={isSubmitting}
            onClick={onConfirm}
            data-testid="done-status-ok-btn"
            className="px-4 py-1.5 text-xs font-semibold tracking-wide text-white bg-app-primary hover:bg-app-dark-blue rounded-md shadow-sm transition-colors disabled:opacity-50"
          >
            OK
          </button>
        </div>
      </div>
    </div>
  );
};
