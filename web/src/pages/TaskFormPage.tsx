import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import {
  ArrowLeft,
  Calendar,
  Building2,
  CreditCard,
  Tag,
  BookOpen,
  FolderTree,
  Trash2,
  CheckCircle2,
  AlertCircle,
  Search,
  X,
  Check,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import {
  addTask,
  updateTask,
  deleteTask,
  subscribeToAccounts,
  subscribeToStatuses,
  subscribeToCategories,
} from '../firebase/db';
import {
  TaskItem,
  AccountItem,
  StatusItem,
  CategoryItem,
  Priority,
  DEFAULT_UNITS,
  DEFAULT_PAYMENTS,
  DEFAULT_STATUSES,
  DEFAULT_ACCOUNTS,
  DEFAULT_CATEGORIES,
  getCodeForAccount,
} from '../types';
import { ConfirmDeleteModal } from '../components/ConfirmDeleteModal';
import { DeveloperFooter } from '../components/DeveloperFooter';

export const TaskFormPage: React.FC = () => {
  const { taskId } = useParams<{ taskId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { currentUser } = useAuth();

  const isNew = !taskId || taskId === 'new';
  const initialTask: TaskItem | undefined = location.state?.task;
  const initialMonth: string = location.state?.month || 'Nov-2026';

  // Form State
  const [description, setDescription] = useState(initialTask?.description || '');
  const [amount, setAmount] = useState(
    initialTask?.amount ? initialTask.amount.replace(/\s*TK/i, '') : ''
  );
  const [priority, setPriority] = useState<Priority>(initialTask?.priority || 'High');
  const [unit, setUnit] = useState(initialTask?.unit || 'Unit-1');
  const [paymentMode, setPaymentMode] = useState(initialTask?.paymentMode || 'Cash');
  const [expectedDate, setExpectedDate] = useState(initialTask?.expectedDate || '17 Nov 2026');
  const [accountName, setAccountName] = useState(initialTask?.accountName || 'Civil Maintenance');
  const [accountCode, setAccountCode] = useState(
    initialTask?.accountCode || 'ACC-CM101'
  );
  const [status, setStatus] = useState(initialTask?.status || 'Draft Ready');
  const [category, setCategory] = useState(initialTask?.category || 'Materials');
  const [remarks, setRemarks] = useState(initialTask?.remarks || '');
  const [monthYear] = useState(initialTask?.monthYear || initialMonth);
  const [taskNumber] = useState(initialTask?.taskNumber || 1);

  // Subscriptions
  const [accounts, setAccounts] = useState<AccountItem[]>([]);
  const [statuses, setStatuses] = useState<StatusItem[]>([]);
  const [categories, setCategories] = useState<CategoryItem[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successToast, setSuccessToast] = useState<string | null>(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showAccountModal, setShowAccountModal] = useState(false);
  const [accountSearchQuery, setAccountSearchQuery] = useState('');

  useEffect(() => {
    if (!currentUser) return;
    const unsubAccounts = subscribeToAccounts(currentUser.uid, setAccounts);
    const unsubStatuses = subscribeToStatuses(currentUser.uid, setStatuses);
    const unsubCategories = subscribeToCategories(currentUser.uid, setCategories);
    return () => {
      unsubAccounts();
      unsubStatuses();
      unsubCategories();
    };
  }, [currentUser]);

  // Combine accounts
  const combinedAccounts = useMemo(() => {
    const customNames = new Set(accounts.map((a) => a.accountName.toLowerCase().trim()));
    const remainingPredefined = DEFAULT_ACCOUNTS.filter(
      (a) => !customNames.has(a.accountName.toLowerCase().trim())
    ).map((a, i) => ({
      id: `predef_${i}`,
      ownerUid: '',
      accountName: a.accountName,
      accountCode: a.accountCode,
    }));
    return [...accounts, ...remainingPredefined];
  }, [accounts]);

  // Filtered accounts for modal search
  const filteredAccounts = useMemo(() => {
    if (!accountSearchQuery.trim()) return combinedAccounts;
    const q = accountSearchQuery.toLowerCase().trim();
    return combinedAccounts.filter(
      (a) =>
        a.accountName.toLowerCase().includes(q) ||
        a.accountCode.toLowerCase().includes(q)
    );
  }, [combinedAccounts, accountSearchQuery]);

  // Combine statuses
  const combinedStatusNames = Array.from(
    new Set([...DEFAULT_STATUSES, ...statuses.map((s) => s.statusName)])
  );

  // Combine categories
  const combinedCategoryNames = useMemo(() => {
    const custom = categories.map((c) => c.categoryName);
    const set = new Set([...DEFAULT_CATEGORIES, ...custom]);
    if (category && !set.has(category)) {
      set.add(category);
    }
    return Array.from(set);
  }, [categories, category]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!description.trim()) {
      setError('Please enter a task description.');
      return;
    }

    if (!currentUser) {
      setError('You must be signed in to save tasks.');
      return;
    }

    try {
      setSaving(true);
      const cleanedAmount = amount.trim();
      const validatedCode = getCodeForAccount(accountName, accounts);
      const finalAccountCode = accountCode.trim() || validatedCode;

      if (isNew) {
        await addTask({
          ownerUid: currentUser.uid,
          taskNumber,
          description: description.trim(),
          amount: cleanedAmount,
          paymentMode,
          unit,
          priority,
          status,
          expectedDate,
          accountName,
          accountCode: finalAccountCode,
          category,
          remarks: remarks.trim(),
          monthYear,
        });
        setSuccessToast('Task Created Successfully!');
      } else {
        await updateTask(taskId, {
          description: description.trim(),
          amount: cleanedAmount,
          paymentMode,
          unit,
          priority,
          status,
          expectedDate,
          accountName,
          accountCode: finalAccountCode,
          category,
          remarks: remarks.trim(),
        });
        setSuccessToast('Task Updated Successfully!');
      }

      setTimeout(() => {
        navigate('/');
      }, 700);
    } catch (err: any) {
      console.error('Error saving task:', err);
      setError(err.message || 'Failed to save task.');
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!taskId || isNew) return;
    try {
      setSaving(true);
      await deleteTask(taskId);
      setShowDeleteModal(false);
      navigate('/');
    } catch (err: any) {
      setError(err.message || 'Failed to delete task.');
      setSaving(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-between">
      <div>
        {/* Top Header matching Android UpdateTaskScreen */}
        <header className="sticky top-0 z-40 bg-app-primary text-white shadow-sm">
          <div className="max-w-3xl mx-auto px-4 h-14 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <button
                type="button"
                onClick={() => navigate('/')}
                className="p-1.5 rounded-full hover:bg-white/10 transition-colors"
                title="Go Back"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
              <h1 className="text-base sm:text-lg font-semibold tracking-wide">
                {isNew ? 'Add New Task' : `Update Task #${taskNumber}`}
              </h1>
            </div>

            {!isNew && (
              <button
                type="button"
                onClick={() => setShowDeleteModal(true)}
                className="flex items-center space-x-1 px-2.5 py-1 text-xs font-semibold text-white/90 hover:text-white bg-red-600/80 hover:bg-red-600 rounded-md transition-colors"
                title="Delete this task"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Delete</span>
              </button>
            )}
          </div>
        </header>

        {/* Success / Error notification toasts */}
        {successToast && (
          <div className="max-w-3xl mx-auto px-4 mt-3">
            <div className="p-3 bg-green-50 border border-green-200 text-green-800 rounded-lg text-xs font-medium flex items-center space-x-2 animate-fadeIn">
              <CheckCircle2 className="w-4 h-4 text-green-600 flex-shrink-0" />
              <span>{successToast}</span>
            </div>
          </div>
        )}

        {error && (
          <div className="max-w-3xl mx-auto px-4 mt-3">
            <div className="p-3 bg-red-50 border border-red-200 text-red-800 rounded-lg text-xs font-medium flex items-center space-x-2 animate-fadeIn">
              <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
              <span>{error}</span>
            </div>
          </div>
        )}

        {/* Form Container */}
        <main className="max-w-3xl mx-auto px-4 py-5 pb-28">
          <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border border-gray-200 p-5 sm:p-7 space-y-5">
            {/* 1. Description */}
            <div>
              <label className="block text-xs font-bold text-gray-800 uppercase tracking-wide mb-1.5">
                Description <span className="text-red-500">*</span>
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="e.g. Civil Maintenance material purchase for factory renovation..."
                rows={2}
                required
                className="w-full px-3.5 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent transition-all"
              />
            </div>

            {/* 2. Amount & Payment Mode (2-col grid) */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-gray-800 uppercase tracking-wide mb-1.5">
                  Amount (TK)
                </label>
                <div className="relative">
                  <input
                    type="text"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="e.g. 13,000"
                    className="w-full pl-3.5 pr-12 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent font-medium"
                  />
                  <span className="absolute right-3.5 top-2.5 text-xs font-bold text-gray-400">
                    TK
                  </span>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-800 uppercase tracking-wide mb-1.5">
                  Payment Mode
                </label>
                <div className="relative">
                  <CreditCard className="w-4 h-4 text-gray-400 absolute left-3.5 top-3 pointer-events-none" />
                  <select
                    value={paymentMode}
                    onChange={(e) => setPaymentMode(e.target.value)}
                    className="w-full pl-10 pr-3.5 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent bg-white"
                  >
                    {DEFAULT_PAYMENTS.map((pm) => (
                      <option key={pm} value={pm}>
                        {pm}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            {/* 3. Priority Selection (High, Med, Low pills) */}
            <div>
              <label className="block text-xs font-bold text-gray-800 uppercase tracking-wide mb-2">
                Priority
              </label>
              <div className="grid grid-cols-3 gap-2.5">
                {(['High', 'Med', 'Low'] as Priority[]).map((p) => {
                  const isSelected = priority === p;
                  let colorClass = 'border-gray-200 text-gray-700 bg-gray-50';
                  let dotColor = 'bg-gray-400';

                  if (p === 'High') {
                    dotColor = 'bg-priority-high';
                    if (isSelected) colorClass = 'border-red-500 bg-red-50 text-red-700 font-bold shadow-sm';
                  } else if (p === 'Med') {
                    dotColor = 'bg-priority-med';
                    if (isSelected) colorClass = 'border-amber-500 bg-amber-50 text-amber-700 font-bold shadow-sm';
                  } else {
                    dotColor = 'bg-priority-low';
                    if (isSelected) colorClass = 'border-green-500 bg-green-50 text-green-700 font-bold shadow-sm';
                  }

                  return (
                    <button
                      key={p}
                      type="button"
                      onClick={() => setPriority(p)}
                      className={`flex items-center justify-center space-x-2 py-2.5 px-3 rounded-lg border text-xs sm:text-sm transition-all ${colorClass}`}
                    >
                      <span className={`w-2.5 h-2.5 rounded-full ${dotColor}`}></span>
                      <span>{p}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* 4. Unit & Expected Date (2-col grid) */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-gray-800 uppercase tracking-wide mb-1.5">
                  Factory Unit
                </label>
                <div className="relative">
                  <Building2 className="w-4 h-4 text-gray-400 absolute left-3.5 top-3 pointer-events-none" />
                  <select
                    value={unit}
                    onChange={(e) => setUnit(e.target.value)}
                    className="w-full pl-10 pr-3.5 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent bg-white"
                  >
                    {DEFAULT_UNITS.map((u) => (
                      <option key={u} value={u}>
                        {u}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-800 uppercase tracking-wide mb-1.5">
                  Expected Date
                </label>
                <div className="relative">
                  <Calendar className="w-4 h-4 text-gray-400 absolute left-3.5 top-3 pointer-events-none" />
                  <input
                    type="text"
                    value={expectedDate}
                    onChange={(e) => setExpectedDate(e.target.value)}
                    placeholder="e.g. 17 Nov 2026"
                    className="w-full pl-10 pr-3.5 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent"
                  />
                </div>
              </div>
            </div>

            {/* 5. Account Name & Account Code (2-col grid) */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-gray-800 uppercase tracking-wide mb-1.5">
                  Account Name
                </label>
                <div
                  onClick={() => setShowAccountModal(true)}
                  className="relative flex items-center justify-between w-full pl-10 pr-3.5 py-2.5 text-sm border border-gray-300 rounded-lg cursor-pointer bg-white hover:border-app-primary transition-colors"
                >
                  <BookOpen className="w-4 h-4 text-gray-400 absolute left-3.5 pointer-events-none" />
                  <span className="truncate font-medium text-gray-900">
                    {accountName || 'Select Account'}
                  </span>
                  <span className="text-xs text-app-primary font-semibold ml-2 flex-shrink-0">
                    Browse
                  </span>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-800 uppercase tracking-wide mb-1.5">
                  Account Code
                </label>
                <input
                  type="text"
                  value={accountCode}
                  onChange={(e) => setAccountCode(e.target.value)}
                  placeholder="e.g. ACC-CM101"
                  className="w-full px-3.5 py-2.5 text-sm font-mono border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent bg-gray-50"
                />
              </div>
            </div>

            {/* 6. Category Selection */}
            <div>
              <label className="block text-xs font-bold text-gray-800 uppercase tracking-wide mb-1.5">
                Category
              </label>
              <div className="relative">
                <FolderTree className="w-4 h-4 text-gray-400 absolute left-3.5 top-3 pointer-events-none" />
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full pl-10 pr-3.5 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent bg-white font-medium text-gray-900"
                >
                  {combinedCategoryNames.map((cat) => (
                    <option key={cat} value={cat}>
                      {cat}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* 7. Status Selection */}
            <div>
              <label className="block text-xs font-bold text-gray-800 uppercase tracking-wide mb-1.5">
                Task Status
              </label>
              <div className="relative">
                <Tag className="w-4 h-4 text-gray-400 absolute left-3.5 top-3 pointer-events-none" />
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                  className="w-full pl-10 pr-3.5 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent bg-white font-medium"
                >
                  {combinedStatusNames.map((st) => (
                    <option key={st} value={st}>
                      {st}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* 7. Remarks */}
            <div>
              <label className="block text-xs font-bold text-gray-800 uppercase tracking-wide mb-1.5">
                Remarks / Notes
              </label>
              <textarea
                value={remarks}
                onChange={(e) => setRemarks(e.target.value)}
                placeholder="Add any specific purchase order notes, reference voucher numbers, supplier or technician details..."
                rows={3}
                className="w-full px-3.5 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent transition-all"
              />
            </div>

            {/* Submit Action Button matching Android */}
            <div className="pt-2">
              <button
                type="submit"
                disabled={saving}
                className="w-full h-12 bg-app-primary hover:bg-app-dark-blue text-white font-semibold rounded-md shadow-md transition-all duration-200 text-sm sm:text-base flex items-center justify-center space-x-2 disabled:opacity-50"
              >
                {saving ? (
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                ) : (
                  <span>{isNew ? 'Save Task' : 'Update Data'}</span>
                )}
              </button>
            </div>
          </form>
        </main>
      </div>

      {/* Footer */}
      <DeveloperFooter />

      {/* Confirm Delete Modal */}
      <ConfirmDeleteModal
        isOpen={showDeleteModal}
        title="Delete Task"
        message={`Are you sure you want to delete Task #${taskNumber}: "${description}"? This action cannot be reversed.`}
        confirmText="Confirm Delete"
        onConfirm={handleDelete}
        onCancel={() => setShowDeleteModal(false)}
      />

      {/* Searchable Account Selection Modal */}
      {showAccountModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fadeIn">
          <div className="bg-white rounded-xl max-w-lg w-full p-5 shadow-2xl border border-gray-100 flex flex-col max-h-[85vh]">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100 flex-shrink-0">
              <div className="flex items-center space-x-2 text-app-primary">
                <BookOpen className="w-5 h-5" />
                <h3 className="font-semibold text-gray-900 text-base">
                  Select Account ({filteredAccounts.length})
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowAccountModal(false)}
                className="p-1 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="relative my-3 flex-shrink-0">
              <Search className="w-4 h-4 text-gray-400 absolute left-3 top-3 pointer-events-none" />
              <input
                type="text"
                value={accountSearchQuery}
                onChange={(e) => setAccountSearchQuery(e.target.value)}
                placeholder="Search account name or code..."
                autoFocus
                className="w-full pl-9 pr-8 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary"
              />
              {accountSearchQuery && (
                <button
                  type="button"
                  onClick={() => setAccountSearchQuery('')}
                  className="absolute right-2.5 top-2.5 text-gray-400 hover:text-gray-600"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>

            <div className="flex-1 overflow-y-auto space-y-1 pr-1">
              {filteredAccounts.length === 0 ? (
                <p className="text-xs text-gray-500 py-8 text-center">
                  No accounts match "{accountSearchQuery}"
                </p>
              ) : (
                filteredAccounts.map((acc) => {
                  const isSelected =
                    acc.accountName.toLowerCase().trim() === accountName.toLowerCase().trim();
                  return (
                    <button
                      key={acc.id || acc.accountName}
                      type="button"
                      onClick={() => {
                        setAccountName(acc.accountName);
                        setAccountCode(acc.accountCode);
                        setShowAccountModal(false);
                      }}
                      className={`w-full flex items-center justify-between p-2.5 rounded-lg text-left text-xs transition-colors ${
                        isSelected
                          ? 'bg-blue-50 border border-blue-200 text-app-primary'
                          : 'hover:bg-gray-50 text-gray-800 border border-transparent'
                      }`}
                    >
                      <div className="truncate pr-2">
                        <div className={`font-medium truncate ${isSelected ? 'font-bold' : ''}`}>
                          {acc.accountName}
                        </div>
                        <div className="text-[11px] text-gray-500 font-mono">
                          Code: {acc.accountCode}
                        </div>
                      </div>
                      {isSelected && <Check className="w-4 h-4 text-app-primary flex-shrink-0" />}
                    </button>
                  );
                })
              )}
            </div>

            <div className="pt-3 border-t border-gray-100 flex justify-end flex-shrink-0">
              <button
                type="button"
                onClick={() => setShowAccountModal(false)}
                className="px-4 py-2 text-xs font-semibold text-gray-600 hover:bg-gray-100 rounded-lg"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
