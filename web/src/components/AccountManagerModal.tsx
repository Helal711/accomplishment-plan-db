import React, { useState } from 'react';
import { BookOpen, Plus, Trash2, Edit2, X, Check } from 'lucide-react';
import { AccountItem } from '../types';

interface AccountManagerModalProps {
  isOpen: boolean;
  accounts: AccountItem[];
  onAddAccount: (name: string, code: string) => Promise<void>;
  onUpdateAccount: (id: string, name: string, code: string) => Promise<void>;
  onDeleteAccount: (id: string) => Promise<void>;
  onClose: () => void;
}

export const AccountManagerModal: React.FC<AccountManagerModalProps> = ({
  isOpen,
  accounts,
  onAddAccount,
  onUpdateAccount,
  onDeleteAccount,
  onClose,
}) => {
  const [accountName, setAccountName] = useState('');
  const [accountCode, setAccountCode] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState('');
  const [editCode, setEditCode] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!accountName.trim() || !accountCode.trim()) {
      setError('Please provide both Account Name and Account Code');
      return;
    }

    try {
      setIsSubmitting(true);
      setError('');
      await onAddAccount(accountName.trim(), accountCode.trim());
      setAccountName('');
      setAccountCode('');
    } catch (err: any) {
      setError(err.message || 'Failed to add account');
    } finally {
      setIsSubmitting(false);
    }
  };

  const startEdit = (acc: AccountItem) => {
    setEditingId(acc.id);
    setEditName(acc.accountName);
    setEditCode(acc.accountCode);
  };

  const saveEdit = async (id: string) => {
    if (!editName.trim() || !editCode.trim()) return;
    try {
      await onUpdateAccount(id, editName.trim(), editCode.trim());
      setEditingId(null);
    } catch (err: any) {
      setError(err.message || 'Failed to update account');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fadeIn">
      <div className="bg-white rounded-xl max-w-lg w-full p-5 shadow-2xl border border-gray-100 flex flex-col max-h-[85vh]">
        <div className="flex items-center justify-between pb-3 border-b border-gray-100 flex-shrink-0">
          <div className="flex items-center space-x-2 text-app-primary">
            <BookOpen className="w-5 h-5" />
            <h3 className="font-semibold text-gray-900 text-base">Account Master Management</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-xs text-gray-500 my-2">
          Manage chart of accounts. Each account is automatically matched to its Account Code during task creation.
        </p>

        {/* Account list */}
        <div className="flex-1 overflow-y-auto space-y-2 pr-1 my-2">
          {accounts.length === 0 ? (
            <p className="text-xs text-gray-400 italic py-4 text-center">No custom accounts found.</p>
          ) : (
            accounts.map((acc) => {
              const isEditing = editingId === acc.id;
              return (
                <div
                  key={acc.id}
                  className="flex items-center justify-between p-2.5 bg-gray-50 border border-gray-200 rounded-lg text-xs"
                >
                  {isEditing ? (
                    <div className="flex-1 flex items-center space-x-2 mr-2">
                      <input
                        type="text"
                        value={editName}
                        onChange={(e) => setEditName(e.target.value)}
                        placeholder="Account Name"
                        className="flex-1 px-2 py-1 text-xs border rounded focus:ring-1 focus:ring-app-primary"
                      />
                      <input
                        type="text"
                        value={editCode}
                        onChange={(e) => setEditCode(e.target.value)}
                        placeholder="ACC-XXX"
                        className="w-28 px-2 py-1 text-xs border rounded focus:ring-1 focus:ring-app-primary"
                      />
                      <button
                        onClick={() => saveEdit(acc.id)}
                        className="p-1 bg-green-600 text-white rounded hover:bg-green-700"
                      >
                        <Check className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => setEditingId(null)}
                        className="p-1 bg-gray-300 text-gray-700 rounded hover:bg-gray-400"
                      >
                        <X className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ) : (
                    <>
                      <div className="flex items-center space-x-2 truncate">
                        <span className="font-semibold text-gray-900 truncate">
                          {acc.accountName}
                        </span>
                        <span className="bg-blue-100 text-app-dark-blue font-mono px-2 py-0.5 rounded text-[11px]">
                          {acc.accountCode}
                        </span>
                      </div>
                      <div className="flex items-center space-x-1 flex-shrink-0">
                        <button
                          onClick={() => startEdit(acc)}
                          className="p-1 text-gray-500 hover:text-app-primary hover:bg-blue-50 rounded"
                          title="Edit"
                        >
                          <Edit2 className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => onDeleteAccount(acc.id)}
                          className="p-1 text-red-500 hover:text-red-700 hover:bg-red-50 rounded"
                          title="Delete"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </>
                  )}
                </div>
              );
            })
          )}
        </div>

        {/* Add Account Form */}
        <form onSubmit={handleAdd} className="pt-3 border-t border-gray-100 flex-shrink-0">
          <label className="block text-xs font-semibold text-gray-700 mb-1">
            Add New Account
          </label>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mb-2">
            <input
              type="text"
              value={accountName}
              onChange={(e) => {
                setAccountName(e.target.value);
                setError('');
                if (!accountCode) {
                  const prefix = e.target.value.replace(/[^a-zA-Z]/g, '').slice(0, 3).toUpperCase();
                  if (prefix) setAccountCode(`ACC-${prefix}101`);
                }
              }}
              placeholder="Account Name (e.g. Electrical Spares)"
              className="px-3 py-1.5 text-xs border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-app-primary"
            />
            <input
              type="text"
              value={accountCode}
              onChange={(e) => {
                setAccountCode(e.target.value);
                setError('');
              }}
              placeholder="Account Code (e.g. ACC-EL412)"
              className="px-3 py-1.5 text-xs border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-app-primary font-mono"
            />
          </div>
          <button
            type="submit"
            disabled={isSubmitting || !accountName.trim() || !accountCode.trim()}
            className="w-full flex items-center justify-center space-x-1.5 py-2 text-xs font-semibold text-white bg-app-primary hover:bg-app-dark-blue rounded-lg disabled:opacity-50 transition-colors"
          >
            <Plus className="w-4 h-4" />
            <span>Add Account to List</span>
          </button>
          {error && <p className="text-xs text-red-600 mt-1">{error}</p>}
        </form>
      </div>
    </div>
  );
};
