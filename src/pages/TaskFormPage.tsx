import React, { useState, useEffect, useMemo, useRef } from 'react';
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
import { DoneStatusConfirmDialog } from '../components/DoneStatusConfirmDialog';
import { DeveloperFooter } from '../components/DeveloperFooter';
import { getSavedMonthForUser } from '../utils/monthUtils';
import { getStatusColor } from '../utils/statusColors';
import {
  buildSelectableTaskStatuses,
  DONE_TASK_UPDATE_CONFIRM_BODY,
  EditableTaskSnapshot,
  hasEditableTaskChanges,
  isRemovedTaskStatus,
  normalizeEditableTaskSnapshot,
  normalizeStatusKey,
  requiresDoneTaskUpdateConfirmation,
} from '../utils/statusOptions';
import {
  extractShortcutTarget,
  resolveTaskFormShortcut,
  UNSAVED_CHANGES_CONFIRM_BODY,
  UNSAVED_CHANGES_CONFIRM_TITLE,
} from '../utils/keyboardShortcuts';
import {
  initPurchaseAmountState,
  applyUnitPriceChange,
  applyTotalQuantityChange,
  applyManualPurchasedAmountChange,
  calculatePurchasedAmount,
} from '../utils/purchaseAmount';
import { formatIsoDateToDisplay, parseDisplayDateToIso } from '../utils/dateUtils';

export const TaskFormPage: React.FC = () => {
  const { taskId } = useParams<{ taskId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { currentUser } = useAuth();

  const isNew = !taskId || taskId === 'new';
  const initialTask: TaskItem | undefined = location.state?.task;
  const initialMonth: string = location.state?.month || getSavedMonthForUser(currentUser?.uid);

  // Form State
  const [description, setDescription] = useState(initialTask?.description || '');
  const [amount, setAmount] = useState(
    initialTask?.amount ? initialTask.amount.replace(/\s*TK/i, '') : ''
  );
  const [purchaseState, setPurchaseState] = useState(() =>
    initPurchaseAmountState(initialTask)
  );
  const [supplierName, setSupplierName] = useState(initialTask?.supplierName || '');
  const [priority, setPriority] = useState<Priority>(initialTask?.priority || 'High');
  const [unit, setUnit] = useState(initialTask?.unit || 'Unit-1');
  const [paymentMode, setPaymentMode] = useState(initialTask?.paymentMode || 'Cash');
  const [expectedDate, setExpectedDate] = useState(initialTask?.expectedDate || '');
  const [accountName, setAccountName] = useState(
    initialTask?.accountName || 'M_Supply expenses(Other)'
  );
  const [accountCode, setAccountCode] = useState(
    initialTask?.accountCode || '4532154'
  );
  const [status, setStatus] = useState(initialTask?.status || 'Draft Ready');
  const [category, setCategory] = useState(initialTask?.category || 'Materials');
  const [remarks, setRemarks] = useState(initialTask?.remarks || '');
  const [monthYear] = useState(initialTask?.monthYear || initialMonth);
  const [taskNumber, setTaskNumber] = useState(initialTask?.taskNumber || 1);
  const [originalSnapshot, setOriginalSnapshot] = useState<EditableTaskSnapshot>(() => {
    const initPurchase = initPurchaseAmountState(initialTask);
    return normalizeEditableTaskSnapshot({
      description: initialTask?.description || '',
      amount: initialTask?.amount ? initialTask.amount.replace(/\s*TK/i, '') : '',
      unitPrice: initPurchase.unitPrice,
      totalQuantity: initPurchase.totalQuantity,
      purchasedAmount: initPurchase.purchasedAmount,
      supplierName: initialTask?.supplierName || '',
      priority: initialTask?.priority || 'High',
      unit: initialTask?.unit || 'Unit-1',
      paymentMode: initialTask?.paymentMode || 'Cash',
      expectedDate: initialTask?.expectedDate || '',
      accountName: initialTask?.accountName || 'M_Supply expenses(Other)',
      accountCode: initialTask?.accountCode || '4532154',
      category: initialTask?.category || 'Materials',
      status: initialTask?.status || 'Draft Ready',
      remarks: initialTask?.remarks || '',
    });
  });
  const [pendingDoneUpdate, setPendingDoneUpdate] = useState<{
    payload: Partial<TaskItem>;
    snapshot: EditableTaskSnapshot;
  } | null>(null);
  const [confirmingDoneUpdate, setConfirmingDoneUpdate] = useState(false);
  const [doneUpdateError, setDoneUpdateError] = useState<string | null>(null);

  // Subscriptions
  const [accounts, setAccounts] = useState<AccountItem[]>([]);
  const [statuses, setStatuses] = useState<StatusItem[]>([]);
  const [categories, setCategories] = useState<CategoryItem[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successToast, setSuccessToast] = useState<string | null>(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showAccountModal, setShowAccountModal] = useState(false);
  const [showDiscardDialog, setShowDiscardDialog] = useState(false);
  const [accountSearchQuery, setAccountSearchQuery] = useState('');
  const submitLockRef = useRef(false);

  // Auto-calculate next serial number for new task if not passed
  useEffect(() => {
    if (isNew && currentUser) {
      import('../firebase/db').then(({ getNextTaskNumber }) => {
        getNextTaskNumber(currentUser.uid, monthYear).then((nextNum) => {
          setTaskNumber(nextNum);
        });
      });
    }
  }, [isNew, currentUser, monthYear]);

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

  // Combine and sort selectable statuses A-Z (case-insensitive), excluding removed options
  const combinedStatusNames = useMemo(() => {
    const candidates = [
      ...DEFAULT_STATUSES,
      ...statuses.map((s) => s.statusName),
      ...(!isRemovedTaskStatus(status) ? [status] : []),
    ];
    return buildSelectableTaskStatuses(candidates).sort((a, b) =>
      a.localeCompare(b, undefined, { sensitivity: 'base' })
    );
  }, [statuses, status]);

  const isHistoricalRemovedStatus = !isNew && isRemovedTaskStatus(status);

  // Combine and sort categories A-Z (case-insensitive) for display
  const combinedCategoryNames = useMemo(() => {
    const custom = categories.map((c) => c.categoryName);
    const set = new Set([...DEFAULT_CATEGORIES, ...custom]);
    if (category && !set.has(category)) {
      set.add(category);
    }
    return Array.from(set).sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }));
  }, [categories, category]);

  const handleStatusDropdownChange = (nextVal: string) => {
    if (!nextVal || isRemovedTaskStatus(nextVal)) return;
    if (normalizeStatusKey(nextVal) === normalizeStatusKey(status)) return;
    setStatus(nextVal);
  };

  const handleCancelDoneUpdate = () => {
    if (confirmingDoneUpdate) return;
    setPendingDoneUpdate(null);
    setDoneUpdateError(null);
    setError(null);

    // Discard pending changes and restore all original saved task fields
    setDescription(originalSnapshot.description);
    setAmount(originalSnapshot.amount);
    setPurchaseState(
      initPurchaseAmountState({
        unitPrice: originalSnapshot.unitPrice,
        totalQuantity: originalSnapshot.totalQuantity,
        purchasedAmount: originalSnapshot.purchasedAmount,
      })
    );
    setSupplierName(originalSnapshot.supplierName);
    setPriority((originalSnapshot.priority || 'High') as Priority);
    setUnit(originalSnapshot.unit);
    setPaymentMode(originalSnapshot.paymentMode);
    setExpectedDate(originalSnapshot.expectedDate);
    setAccountName(originalSnapshot.accountName);
    setAccountCode(originalSnapshot.accountCode);
    setStatus(originalSnapshot.status);
    setCategory(originalSnapshot.category);
    setRemarks(originalSnapshot.remarks);
  };

  const handleConfirmDoneUpdate = async () => {
    if (submitLockRef.current || !pendingDoneUpdate || !taskId || confirmingDoneUpdate || saving) {
      return;
    }

    submitLockRef.current = true;
    try {
      setConfirmingDoneUpdate(true);
      setDoneUpdateError(null);
      setError(null);
      await updateTask(taskId, pendingDoneUpdate.payload);
      setOriginalSnapshot(pendingDoneUpdate.snapshot);
      setPendingDoneUpdate(null);
      setSuccessToast('Task Updated Successfully!');
      setTimeout(() => {
        navigate('/');
      }, 700);
    } catch (err: any) {
      console.error('Error saving task:', err);
      const message = err?.message || 'Failed to save task. Original data has been kept.';
      setDoneUpdateError(message);
      setError(message);
      submitLockRef.current = false;
    } finally {
      setConfirmingDoneUpdate(false);
    }
  };

  const buildCurrentSnapshot = (): EditableTaskSnapshot =>
    normalizeEditableTaskSnapshot({
      description,
      amount,
      unitPrice: purchaseState.unitPrice,
      totalQuantity: purchaseState.totalQuantity,
      purchasedAmount: purchaseState.purchasedAmount,
      supplierName,
      priority,
      unit,
      paymentMode,
      expectedDate,
      accountName,
      accountCode,
      category,
      status,
      remarks,
    });

  const submitTaskForm = async () => {
    if (submitLockRef.current || saving || confirmingDoneUpdate || pendingDoneUpdate) return;
    setError(null);

    if (!description.trim()) {
      setError('Please enter a task description.');
      return;
    }

    if (!currentUser) {
      setError('You must be signed in to save tasks.');
      return;
    }

    const cleanedAmount = amount.trim();
    const validatedCode = getCodeForAccount(accountName, accounts);
    const finalAccountCode = accountCode.trim() || validatedCode;

    // Validate account exists in user's account master list
    const accountExists = combinedAccounts.some(
      (a) => a.accountName.toLowerCase().trim() === accountName.toLowerCase().trim()
    );
    if (!accountExists && combinedAccounts.length > 0) {
      setError(`Selected account "${accountName}" does not exist in your Account List. Please select a valid account.`);
      return;
    }

    // Prevent selecting removed statuses for new tasks or changing an existing task into a removed status
    const originalStatusKey = normalizeStatusKey(originalSnapshot.status);
    const currentStatusKey = normalizeStatusKey(status);
    const isPreservingHistoricalStatus = !isNew && originalStatusKey === currentStatusKey;
    if (isRemovedTaskStatus(status) && !isPreservingHistoricalStatus) {
      setError('Selected task status is no longer available. Please choose another status.');
      return;
    }

    const cleanedUnitPrice = purchaseState.unitPrice.trim();
    const cleanedTotalQuantity = purchaseState.totalQuantity.trim();
    const cleanedPurchasedAmount = purchaseState.purchasedAmount.trim();
    const cleanedSupplierName = supplierName.trim();

    if (!isNew) {
      const updatePayload: Partial<TaskItem> = {
        description: description.trim(),
        amount: cleanedAmount,
        unitPrice: cleanedUnitPrice,
        totalQuantity: cleanedTotalQuantity,
        purchasedAmount: cleanedPurchasedAmount,
        supplierName: cleanedSupplierName,
        paymentMode,
        unit,
        priority,
        status,
        expectedDate,
        accountName,
        accountCode: finalAccountCode,
        category,
        remarks: remarks.trim(),
      };

      const editedSnapshot = normalizeEditableTaskSnapshot({
        description: updatePayload.description,
        amount: updatePayload.amount,
        unitPrice: updatePayload.unitPrice,
        totalQuantity: updatePayload.totalQuantity,
        purchasedAmount: updatePayload.purchasedAmount,
        supplierName: updatePayload.supplierName,
        priority: updatePayload.priority,
        unit: updatePayload.unit,
        paymentMode: updatePayload.paymentMode,
        expectedDate: updatePayload.expectedDate,
        accountName: updatePayload.accountName,
        accountCode: updatePayload.accountCode,
        category: updatePayload.category,
        status: updatePayload.status,
        remarks: updatePayload.remarks,
      });

      if (requiresDoneTaskUpdateConfirmation(originalSnapshot, editedSnapshot)) {
        setDoneUpdateError(null);
        setPendingDoneUpdate({
          payload: updatePayload,
          snapshot: editedSnapshot,
        });
        return;
      }

      submitLockRef.current = true;
      try {
        setSaving(true);
        await updateTask(taskId, updatePayload);
        setOriginalSnapshot(editedSnapshot);
        setSuccessToast('Task Updated Successfully!');
        setTimeout(() => {
          navigate('/');
        }, 700);
      } catch (err: any) {
        console.error('Error saving task:', err);
        setError(err.message || 'Failed to save task.');
        setSaving(false);
        submitLockRef.current = false;
      }
      return;
    }

    submitLockRef.current = true;
    try {
      setSaving(true);
      await addTask({
        ownerUid: currentUser.uid,
        taskNumber,
        description: description.trim(),
        amount: cleanedAmount,
        unitPrice: cleanedUnitPrice,
        totalQuantity: cleanedTotalQuantity,
        purchasedAmount: cleanedPurchasedAmount,
        supplierName: cleanedSupplierName,
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
      setTimeout(() => {
        navigate('/');
      }, 700);
    } catch (err: any) {
      console.error('Error saving task:', err);
      setError(err.message || 'Failed to save task.');
      setSaving(false);
      submitLockRef.current = false;
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await submitTaskForm();
  };

  const handleEscapeShortcut = () => {
    if (pendingDoneUpdate) {
      if (!confirmingDoneUpdate) {
        handleCancelDoneUpdate();
      }
      return;
    }
    if (showDiscardDialog) {
      setShowDiscardDialog(false);
      return;
    }
    if (showDeleteModal) {
      if (!saving) {
        setShowDeleteModal(false);
      }
      return;
    }
    if (showAccountModal) {
      setShowAccountModal(false);
      return;
    }
    if (saving || confirmingDoneUpdate || submitLockRef.current) {
      return;
    }

    const currentSnapshot = buildCurrentSnapshot();
    if (hasEditableTaskChanges(originalSnapshot, currentSnapshot)) {
      setShowDiscardDialog(true);
      return;
    }

    navigate('/');
  };

  const hasOpenOverlay = Boolean(
    pendingDoneUpdate || showDiscardDialog || showDeleteModal || showAccountModal
  );

  const shortcutHandlersRef = useRef({
    submitTaskForm,
    handleEscapeShortcut,
    hasOpenOverlay,
  });
  shortcutHandlersRef.current = {
    submitTaskForm,
    handleEscapeShortcut,
    hasOpenOverlay,
  };

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      const { submitTaskForm: submitFn, handleEscapeShortcut: escapeFn, hasOpenOverlay: overlayOpen } =
        shortcutHandlersRef.current;
      const action = resolveTaskFormShortcut(
        event,
        extractShortcutTarget(event.target),
        overlayOpen
      );

      if (action === 'prevent-only') {
        event.preventDefault();
        return;
      }

      if (action === 'submit') {
        event.preventDefault();
        void submitFn();
        return;
      }

      if (action === 'escape') {
        event.preventDefault();
        escapeFn();
      }
    };

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);

  const handleDelete = async () => {
    if (!taskId || isNew) return;
    try {
      setSaving(true);
      await deleteTask(taskId, monthYear, currentUser?.uid);
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
                    placeholder="00,000"
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

            {/* 2b. Optional Purchase Calculation Fields (Unit Price, Total Quantity, Purchased Amount) */}
            <div className="rounded-lg border border-gray-200 bg-gray-50/70 p-3.5 space-y-2.5">
              <div className="flex items-center justify-between gap-2 flex-wrap">
                <span className="text-xs font-bold text-gray-700 uppercase tracking-wide">
                  Purchase Details <span className="font-normal normal-case text-gray-400">(Optional)</span>
                </span>
                {purchaseState.purchasedAmount.trim() && (
                  <span
                    className={`text-[11px] font-medium px-2 py-0.5 rounded-full ${
                      purchaseState.isManualOverride
                        ? 'bg-amber-50 text-amber-700 border border-amber-200'
                        : calculatePurchasedAmount(purchaseState.unitPrice, purchaseState.totalQuantity) !== null
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : 'bg-gray-100 text-gray-600'
                    }`}
                  >
                    {purchaseState.isManualOverride
                      ? 'Manual amount'
                      : 'Auto: Unit Price × Qty'}
                  </span>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label
                    htmlFor="unit-price-input"
                    className="block text-[11px] font-semibold text-gray-700 mb-1"
                  >
                    Unit Price
                  </label>
                  <input
                    id="unit-price-input"
                    type="text"
                    inputMode="decimal"
                    value={purchaseState.unitPrice}
                    onChange={(e) =>
                      setPurchaseState((prev) => applyUnitPriceChange(prev, e.target.value))
                    }
                    placeholder="e.g. 500"
                    className="w-full px-3 py-2 text-sm bg-white border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent"
                  />
                </div>

                <div>
                  <label
                    htmlFor="total-quantity-input"
                    className="block text-[11px] font-semibold text-gray-700 mb-1"
                  >
                    Total Quantity
                  </label>
                  <input
                    id="total-quantity-input"
                    type="text"
                    inputMode="decimal"
                    value={purchaseState.totalQuantity}
                    onChange={(e) =>
                      setPurchaseState((prev) => applyTotalQuantityChange(prev, e.target.value))
                    }
                    placeholder="e.g. 10"
                    className="w-full px-3 py-2 text-sm bg-white border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent"
                  />
                </div>

                <div>
                  <label
                    htmlFor="purchased-amount-input"
                    className="block text-[11px] font-semibold text-gray-700 mb-1"
                  >
                    Purchased Amount (TK)
                  </label>
                  <div className="relative">
                    <input
                      id="purchased-amount-input"
                      type="text"
                      inputMode="decimal"
                      value={purchaseState.purchasedAmount}
                      onChange={(e) =>
                        setPurchaseState((prev) =>
                          applyManualPurchasedAmountChange(prev, e.target.value)
                        )
                      }
                      placeholder="Auto or manual"
                      className="w-full pl-3 pr-10 py-2 text-sm bg-white border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent font-medium text-emerald-800"
                    />
                    <span className="absolute right-3 top-2 text-[11px] font-bold text-gray-400">
                      TK
                    </span>
                  </div>
                </div>
              </div>

              <div>
                <label
                  htmlFor="supplier-name-input"
                  className="block text-[11px] font-semibold text-gray-700 mb-1"
                >
                  Supplier Name (Optional)
                </label>
                <input
                  id="supplier-name-input"
                  type="text"
                  value={supplierName}
                  onChange={(e) => setSupplierName(e.target.value)}
                  placeholder="e.g. ABC Hardware & Traders"
                  className="w-full px-3 py-2 text-sm bg-white border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent"
                />
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
                  let dotBg = '#FB8C00';

                  if (p === 'High') {
                    dotBg = '#E53935';
                    if (isSelected) colorClass = 'border-red-500 bg-red-50 text-red-700 font-bold shadow-sm';
                  } else if (p === 'Med') {
                    dotBg = '#FB8C00';
                    if (isSelected) colorClass = 'border-amber-500 bg-amber-50 text-amber-700 font-bold shadow-sm';
                  } else {
                    dotBg = '#43A047';
                    if (isSelected) colorClass = 'border-green-500 bg-green-50 text-green-700 font-bold shadow-sm';
                  }

                  return (
                    <button
                      key={p}
                      type="button"
                      onClick={() => setPriority(p)}
                      className={`flex items-center justify-center space-x-2 py-2.5 px-3 rounded-lg border text-xs sm:text-sm transition-all ${colorClass}`}
                    >
                      <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ backgroundColor: dotBg }}></span>
                      <span>{p}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* 4. Unit & Completion Date (2-col grid) */}
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
                <label
                  htmlFor="completion-date-input"
                  className="block text-xs font-bold text-gray-800 uppercase tracking-wide mb-1.5"
                >
                  Completion Date
                </label>
                <div className="relative flex items-center w-full border border-gray-300 rounded-lg bg-white focus-within:ring-2 focus-within:ring-app-primary focus-within:border-transparent transition-all">
                  <div className="relative flex-1 min-w-0 flex items-center">
                    <Calendar className="w-4 h-4 text-gray-400 absolute left-3.5 pointer-events-none" />
                    <div
                      data-testid="completion-date-display"
                      className={`w-full pl-10 pr-3 py-2.5 text-sm truncate pointer-events-none select-none ${
                        expectedDate ? 'text-gray-900 font-medium' : 'text-gray-400'
                      }`}
                    >
                      {expectedDate || 'Select completion date'}
                    </div>
                    <input
                      id="completion-date-input"
                      type="date"
                      aria-label="Completion Date"
                      value={parseDisplayDateToIso(expectedDate)}
                      onChange={(e) => {
                        const nextIso = e.target.value;
                        setExpectedDate(nextIso ? formatIsoDateToDisplay(nextIso) : '');
                      }}
                      onClick={(e) => {
                        const inputEl = e.currentTarget as HTMLInputElement & {
                          showPicker?: () => void;
                        };
                        if (typeof inputEl.showPicker === 'function') {
                          try {
                            inputEl.showPicker();
                          } catch {
                            // Native browser click behavior handles opening the picker
                          }
                        }
                      }}
                      className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                    />
                  </div>
                  {expectedDate && (
                    <button
                      type="button"
                      onClick={() => setExpectedDate('')}
                      aria-label="Clear completion date"
                      title="Clear date"
                      data-testid="clear-completion-date-btn"
                      className="relative z-10 mr-1.5 p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-md transition-colors flex-shrink-0"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  )}
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
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-bold text-gray-800 uppercase tracking-wide">
                  Task Status
                </label>
                {(() => {
                  const activeColors = getStatusColor(status);
                  return (
                    <span
                      style={{
                        backgroundColor: activeColors.bg,
                        color: activeColors.text,
                        borderColor: activeColors.border,
                      }}
                      className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-full text-[11px] font-semibold border"
                    >
                      <span
                        className="w-1.5 h-1.5 rounded-full flex-shrink-0"
                        style={{ backgroundColor: activeColors.dot }}
                        aria-hidden="true"
                      />
                      <span>{status}</span>
                    </span>
                  );
                })()}
              </div>
              <div className="relative">
                {(() => {
                  const activeColors = getStatusColor(status);
                  return (
                    <>
                      <Tag
                        className="w-4 h-4 absolute left-3.5 top-3 pointer-events-none"
                        style={{ color: activeColors.dot }}
                      />
                      <select
                        value={isHistoricalRemovedStatus ? '' : status}
                        disabled={confirmingDoneUpdate || saving}
                        onChange={(e) => {
                          handleStatusDropdownChange(e.target.value);
                        }}
                        style={{
                          backgroundColor: activeColors.bg,
                          color: activeColors.text,
                          borderColor: activeColors.border,
                        }}
                        className="w-full pl-10 pr-3.5 py-2.5 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent font-semibold transition-colors"
                      >
                        {isHistoricalRemovedStatus && (
                          <option value="" disabled hidden>
                            Keep existing status (select new status to change)
                          </option>
                        )}
                        {combinedStatusNames.map((st) => {
                          const optColors = getStatusColor(st);
                          return (
                            <option
                              key={st}
                              value={st}
                              style={{
                                backgroundColor: optColors.bg,
                                color: optColors.text,
                              }}
                            >
                              {st}
                            </option>
                          );
                        })}
                      </select>
                    </>
                  );
                })()}
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
                disabled={saving || confirmingDoneUpdate}
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

      {/* Compact Done Task Update Confirmation Alert */}
      <DoneStatusConfirmDialog
        isOpen={!!pendingDoneUpdate}
        message={DONE_TASK_UPDATE_CONFIRM_BODY}
        isSubmitting={confirmingDoneUpdate}
        errorMessage={doneUpdateError}
        onCancel={handleCancelDoneUpdate}
        onConfirm={handleConfirmDoneUpdate}
      />

      {/* Compact Unsaved Changes Confirmation Alert (Esc shortcut) */}
      <DoneStatusConfirmDialog
        isOpen={showDiscardDialog}
        title={UNSAVED_CHANGES_CONFIRM_TITLE}
        message={UNSAVED_CHANGES_CONFIRM_BODY}
        onCancel={() => setShowDiscardDialog(false)}
        onConfirm={() => {
          setShowDiscardDialog(false);
          navigate('/');
        }}
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
