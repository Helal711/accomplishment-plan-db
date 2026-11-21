import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Inbox } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import {
  subscribeToTasks,
  subscribeToAccounts,
  subscribeToStatuses,
  subscribeToCategories,
  quickUpdateTaskStatus,
  deleteTask,
  addAccount,
  updateAccount,
  deleteAccount,
  addStatus,
  deleteStatus,
  addCategory,
  updateCategory,
  deleteCategory,
} from '../firebase/db';
import { TaskItem, AccountItem, StatusItem, CategoryItem, DEFAULT_STATUSES, DEFAULT_MONTHS } from '../types';
import { AppHeader, filterVisibleDashboardStatusCounts } from '../components/AppHeader';
import { TaskCard } from '../components/TaskCard';
import { MonthSelectorModal } from '../components/MonthSelectorModal';
import { StatusManagerModal } from '../components/StatusManagerModal';
import { AccountManagerModal } from '../components/AccountManagerModal';
import { CategoryManagerModal } from '../components/CategoryManagerModal';
import { ExportModal } from '../components/ExportModal';
import { ConfirmDeleteModal } from '../components/ConfirmDeleteModal';
import { DeveloperFooter } from '../components/DeveloperFooter';
import {
  getCurrentMonthYear,
  getSavedMonthForUser,
  saveMonthForUser,
  getCustomMonthsForUser,
  saveCustomMonthForUser,
} from '../utils/monthUtils';
import { filterTasks } from '../utils/taskFilter';
import {
  buildSelectableTaskStatuses,
  confirmDoneStatusChange,
  isRemovedTaskStatus,
  normalizeStatusKey,
} from '../utils/statusOptions';

function buildAvailableMonths(uid: string | undefined | null, activeMonth: string): string[] {
  const custom = getCustomMonthsForUser(uid);
  const current = getCurrentMonthYear();
  const seen = new Set<string>();
  const list: string[] = [];
  for (const m of [activeMonth, current, ...DEFAULT_MONTHS, ...custom]) {
    const trimmed = (m || '').trim();
    if (!trimmed) continue;
    const key = trimmed.toLowerCase();
    if (!seen.has(key)) {
      seen.add(key);
      list.push(trimmed);
    }
  }
  return list;
}

export const DashboardPage: React.FC = () => {
  const { currentUser } = useAuth();
  const navigate = useNavigate();

  const [selectedMonth, setSelectedMonth] = useState<string>(() =>
    getSavedMonthForUser(currentUser?.uid)
  );
  const [availableMonths, setAvailableMonths] = useState<string[]>(() =>
    buildAvailableMonths(currentUser?.uid, getSavedMonthForUser(currentUser?.uid))
  );
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [accounts, setAccounts] = useState<AccountItem[]>([]);
  const [statuses, setStatuses] = useState<StatusItem[]>([]);
  const [categories, setCategories] = useState<CategoryItem[]>([]);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedStatus, setSelectedStatus] = useState<string | null>(null);
  const [loadingTasks, setLoadingTasks] = useState<boolean>(true);

  // Modals state
  const [showMonthModal, setShowMonthModal] = useState<boolean>(false);
  const [showStatusModal, setShowStatusModal] = useState<boolean>(false);
  const [showAccountModal, setShowAccountModal] = useState<boolean>(false);
  const [showCategoryModal, setShowCategoryModal] = useState<boolean>(false);
  const [showExportModal, setShowExportModal] = useState<boolean>(false);
  const [taskToDelete, setTaskToDelete] = useState<TaskItem | null>(null);

  // Restore user-scoped month preference or reset when logged out/switched
  useEffect(() => {
    if (!currentUser) {
      setTasks([]);
      setAccounts([]);
      setStatuses([]);
      setCategories([]);
      setSearchQuery('');
      setSelectedStatus(null);
      setLoadingTasks(false);
      const fallbackMonth = getCurrentMonthYear();
      setSelectedMonth(fallbackMonth);
      setAvailableMonths(buildAvailableMonths(null, fallbackMonth));
      return;
    }

    const restoredMonth = getSavedMonthForUser(currentUser.uid);
    setSelectedMonth(restoredMonth);
    setSelectedStatus(null);
    setAvailableMonths(buildAvailableMonths(currentUser.uid, restoredMonth));
  }, [currentUser?.uid]);

  // Subscribe to tasks for current user and month
  useEffect(() => {
    if (!currentUser) return;
    setLoadingTasks(true);
    const unsubscribe = subscribeToTasks(
      currentUser.uid,
      selectedMonth,
      (fetchedTasks) => {
        setTasks(fetchedTasks);
        setLoadingTasks(false);
      },
      (err) => {
        console.error('Failed to subscribe to tasks:', err);
        setLoadingTasks(false);
      }
    );
    return () => unsubscribe();
  }, [currentUser, selectedMonth]);

  // Subscribe to accounts
  useEffect(() => {
    if (!currentUser) return;
    const unsubscribe = subscribeToAccounts(currentUser.uid, (items) => {
      setAccounts(items);
    });
    return () => unsubscribe();
  }, [currentUser]);

  // Subscribe to custom statuses
  useEffect(() => {
    if (!currentUser) return;
    const unsubscribe = subscribeToStatuses(currentUser.uid, (items) => {
      setStatuses(items);
    });
    return () => unsubscribe();
  }, [currentUser]);

  // Subscribe to categories
  useEffect(() => {
    if (!currentUser) return;
    const unsubscribe = subscribeToCategories(currentUser.uid, (items) => {
      setCategories(items);
    });
    return () => unsubscribe();
  }, [currentUser]);

  // Combined selectable status names (preserving order, case-insensitive deduplication, excluding removed options)
  const availableStatusNames = useMemo(() => {
    const custom = statuses.map((s) => s.statusName);
    return buildSelectableTaskStatuses([...DEFAULT_STATUSES, ...custom]);
  }, [statuses]);

  // Filter tasks by both status filter and multi-field search query
  const filteredTasks = useMemo(() => {
    return filterTasks(tasks, searchQuery, selectedStatus, selectedMonth);
  }, [tasks, searchQuery, selectedStatus, selectedMonth]);

  const totalCount = tasks.length;
  const pendingCount = tasks.filter((t) => (t.status || '').toLowerCase() !== 'done').length;

  // Dynamically compute counts for all visible dashboard summary statuses in the selected month
  const statusCounts = useMemo(() => {
    const allStatusCounts = availableStatusNames.map((st) => {
      const count = tasks.filter(
        (t) => (t.status || '').toLowerCase().trim() === st.toLowerCase().trim()
      ).length;
      return { name: st, count };
    });
    return filterVisibleDashboardStatusCounts(allStatusCounts);
  }, [availableStatusNames, tasks]);

  const handleStatusChange = async (
    taskId: string,
    currentStatus: string,
    newStatus: string
  ) => {
    if (isRemovedTaskStatus(newStatus)) return;
    if (normalizeStatusKey(currentStatus) === normalizeStatusKey(newStatus)) return;
    if (!confirmDoneStatusChange(currentStatus, newStatus)) return;

    try {
      await quickUpdateTaskStatus(taskId, newStatus);
    } catch (err: any) {
      console.error('Failed to change status:', err);
      window.alert(err?.message || 'Failed to update task status. Please try again.');
    }
  };

  const handleConfirmDelete = async () => {
    if (!taskToDelete) return;
    try {
      await deleteTask(taskToDelete.id, selectedMonth, currentUser?.uid);
      setTaskToDelete(null);
    } catch (err) {
      console.error('Failed to delete task:', err);
    }
  };

  const handleSelectMonth = (month: string) => {
    setSelectedMonth(month);
    setSelectedStatus(null);
    saveMonthForUser(currentUser?.uid, month);
    setAvailableMonths((prev) => {
      if (prev.some((m) => m.toLowerCase() === month.toLowerCase())) return prev;
      return [month, ...prev];
    });
  };

  const handleAddMonth = (newMonth: string) => {
    saveCustomMonthForUser(currentUser?.uid, newMonth);
    handleSelectMonth(newMonth);
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-between">
      <div>
        {/* Header matching corporate blue Android bar */}
        <AppHeader
          selectedMonth={selectedMonth}
          totalCount={totalCount}
          pendingCount={pendingCount}
          statusCounts={statusCounts}
          selectedStatus={selectedStatus}
          onSelectStatus={setSelectedStatus}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
          onOpenMonthSelector={() => setShowMonthModal(true)}
          onOpenExport={() => setShowExportModal(true)}
          onOpenStatusManager={() => setShowStatusModal(true)}
          onOpenAccountManager={() => setShowAccountModal(true)}
          onOpenCategoryManager={() => setShowCategoryModal(true)}
        />

        {/* Content Container with bottom padding for fixed footer & FAB */}
        <main className="max-w-4xl mx-auto px-1.5 sm:px-4 pt-1 pb-20 sm:py-4 sm:pb-28">
          {loadingTasks ? (
            <div className="py-20 text-center">
              <div className="w-8 h-8 border-3 border-app-primary border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
              <p className="text-xs text-gray-500">Loading tasks for {selectedMonth}...</p>
            </div>
          ) : filteredTasks.length === 0 ? (
            <div className="bg-white rounded-xl border border-gray-200 p-10 text-center shadow-sm my-6">
              <div className="w-12 h-12 rounded-full bg-blue-50 text-app-primary flex items-center justify-center mx-auto mb-3">
                <Inbox className="w-6 h-6" />
              </div>
              <h3 className="text-base font-semibold text-gray-800">
                {searchQuery.trim() || selectedStatus
                  ? 'No matching tasks found'
                  : `No tasks found for ${selectedMonth}`}
              </h3>
              <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
                {searchQuery.trim() || selectedStatus
                  ? 'Try clearing the active status filter or adjusting your search terms.'
                  : 'Get started by creating a new task using the blue "+" button.'}
              </p>
              {searchQuery.trim() || selectedStatus ? (
                <button
                  type="button"
                  onClick={() => {
                    setSelectedStatus(null);
                    setSearchQuery('');
                  }}
                  className="mt-4 inline-flex items-center space-x-1.5 px-4 py-2 bg-blue-50 hover:bg-blue-100 text-app-primary text-xs font-semibold rounded-lg border border-blue-200 transition-colors"
                >
                  <span>Show All Tasks</span>
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => navigate('/tasks/new', { state: { month: selectedMonth } })}
                  className="mt-4 inline-flex items-center space-x-1.5 px-4 py-2 bg-app-primary hover:bg-app-dark-blue text-white text-xs font-semibold rounded-lg shadow-sm transition-colors"
                >
                  <Plus className="w-4 h-4" />
                  <span>Add First Task</span>
                </button>
              )}
            </div>
          ) : (
            <div className="space-y-1.5 sm:space-y-2.5">
              {filteredTasks.map((task, index) => (
                <TaskCard
                  key={task.id || `${task.taskNumber}-${index}`}
                  task={task}
                  serialNumber={task.taskNumber}
                  availableStatuses={availableStatusNames}
                  onCardClick={() => navigate(`/tasks/${task.id}`, { state: { task } })}
                  onStatusChange={(newStatus) =>
                    handleStatusChange(task.id, task.status, newStatus)
                  }
                  onDeleteClick={() => setTaskToDelete(task)}
                />
              ))}
            </div>
          )}
        </main>
      </div>

      {/* Floating Add Task Button (FAB) matching Android, raised above fixed footer */}
      <div className="fixed right-5 bottom-14 z-30">
        <button
          type="button"
          onClick={() => navigate('/tasks/new', { state: { month: selectedMonth } })}
          className="w-14 h-14 rounded-full bg-app-primary hover:bg-app-dark-blue text-white shadow-xl flex items-center justify-center transition-transform hover:scale-105 active:scale-95"
          title="Add New Task"
        >
          <Plus className="w-7 h-7" />
        </button>
      </div>

      {/* Permanently fixed Footer */}
      <DeveloperFooter />

      {/* Modals */}
      <MonthSelectorModal
        isOpen={showMonthModal}
        selectedMonth={selectedMonth}
        availableMonths={availableMonths}
        onSelectMonth={handleSelectMonth}
        onAddMonth={handleAddMonth}
        onClose={() => setShowMonthModal(false)}
      />

      <StatusManagerModal
        isOpen={showStatusModal}
        statuses={statuses}
        onAddStatus={async (name) => {
          if (currentUser) await addStatus(currentUser.uid, name, false);
        }}
        onRemoveStatus={async (id) => {
          await deleteStatus(id);
        }}
        onClose={() => setShowStatusModal(false)}
      />

      <AccountManagerModal
        isOpen={showAccountModal}
        accounts={accounts}
        onAddAccount={async (name, code) => {
          if (currentUser) await addAccount(currentUser.uid, name, code);
        }}
        onUpdateAccount={async (id, name, code) => {
          await updateAccount(id, { accountName: name, accountCode: code });
        }}
        onDeleteAccount={async (id) => {
          await deleteAccount(id);
        }}
        onClose={() => setShowAccountModal(false)}
      />

      <CategoryManagerModal
        isOpen={showCategoryModal}
        categories={categories}
        onAddCategory={async (name) => {
          if (currentUser) await addCategory(currentUser.uid, name);
        }}
        onEditCategory={async (id, name) => {
          await updateCategory(id, name);
        }}
        onDeleteCategory={async (id) => {
          await deleteCategory(id);
        }}
        onClose={() => setShowCategoryModal(false)}
      />

      <ExportModal
        isOpen={showExportModal}
        currentMonth={selectedMonth}
        tasks={filteredTasks}
        onClose={() => setShowExportModal(false)}
      />

      <ConfirmDeleteModal
        isOpen={!!taskToDelete}
        title="Delete Task"
        message={`Are you sure you want to delete Task #${taskToDelete?.taskNumber}: "${taskToDelete?.description}"? This action cannot be undone.`}
        confirmText="Delete Task"
        onConfirm={handleConfirmDelete}
        onCancel={() => setTaskToDelete(null)}
      />
    </div>
  );
};
