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
  syncOfficialAccountList,
  syncOfficialStatusList,
} from '../firebase/db';
import { TaskItem, AccountItem, StatusItem, CategoryItem, DEFAULT_STATUSES, DEFAULT_MONTHS } from '../types';
import { AppHeader } from '../components/AppHeader';
import { TaskCard } from '../components/TaskCard';
import { MonthSelectorModal } from '../components/MonthSelectorModal';
import { StatusManagerModal } from '../components/StatusManagerModal';
import { AccountManagerModal } from '../components/AccountManagerModal';
import { CategoryManagerModal } from '../components/CategoryManagerModal';
import { ExportModal } from '../components/ExportModal';
import { ConfirmDeleteModal } from '../components/ConfirmDeleteModal';
import { DeveloperFooter } from '../components/DeveloperFooter';

export const DashboardPage: React.FC = () => {
  const { currentUser } = useAuth();
  const navigate = useNavigate();

  const [selectedMonth, setSelectedMonth] = useState<string>('Nov-2026');
  const [availableMonths, setAvailableMonths] = useState<string[]>(DEFAULT_MONTHS);
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [accounts, setAccounts] = useState<AccountItem[]>([]);
  const [statuses, setStatuses] = useState<StatusItem[]>([]);
  const [categories, setCategories] = useState<CategoryItem[]>([]);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [loadingTasks, setLoadingTasks] = useState<boolean>(true);

  // Modals state
  const [showMonthModal, setShowMonthModal] = useState<boolean>(false);
  const [showStatusModal, setShowStatusModal] = useState<boolean>(false);
  const [showAccountModal, setShowAccountModal] = useState<boolean>(false);
  const [showCategoryModal, setShowCategoryModal] = useState<boolean>(false);
  const [showExportModal, setShowExportModal] = useState<boolean>(false);
  const [taskToDelete, setTaskToDelete] = useState<TaskItem | null>(null);
  const [isDeletingTask, setIsDeletingTask] = useState<boolean>(false);
  const [deleteFeedback, setDeleteFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

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

  // Combined status names
  const availableStatusNames = useMemo(() => {
    const custom = statuses.map((s) => s.statusName);
    const combined = Array.from(new Set([...DEFAULT_STATUSES, ...custom]));
    return combined;
  }, [statuses]);

  // Filter tasks by search query
  const filteredTasks = useMemo(() => {
    if (!searchQuery.trim()) return tasks;
    const q = searchQuery.toLowerCase().trim();
    return tasks.filter((t) => {
      return (
        t.description.toLowerCase().includes(q) ||
        t.unit.toLowerCase().includes(q) ||
        t.status.toLowerCase().includes(q) ||
        t.accountName.toLowerCase().includes(q) ||
        t.accountCode.toLowerCase().includes(q) ||
        (t.category || '').toLowerCase().includes(q) ||
        t.remarks.toLowerCase().includes(q) ||
        t.taskNumber.toString().includes(q)
      );
    });
  }, [tasks, searchQuery]);

  const totalCount = tasks.length;
  const pendingCount = tasks.filter((t) => (t.status || '').toLowerCase() !== 'done').length;

  const handleStatusChange = async (taskId: string, newStatus: string) => {
    try {
      await quickUpdateTaskStatus(taskId, newStatus);
    } catch (err) {
      console.error('Failed to change status:', err);
    }
  };

  const handleConfirmDelete = async () => {
    if (!taskToDelete || isDeletingTask) return;
    try {
      setIsDeletingTask(true);
      await deleteTask(taskToDelete.id, currentUser?.uid);
      const deletedNumber = taskToDelete.taskNumber;
      setTaskToDelete(null);
      setDeleteFeedback({
        type: 'success',
        message: `Task #${deletedNumber} deleted successfully.`,
      });
      setTimeout(() => setDeleteFeedback(null), 4000);
    } catch (err: any) {
      console.error('Failed to delete task:', err);
      setDeleteFeedback({
        type: 'error',
        message: err.message || 'Failed to delete task.',
      });
      setTimeout(() => setDeleteFeedback(null), 4000);
    } finally {
      setIsDeletingTask(false);
    }
  };

  const handleAddMonth = (newMonth: string) => {
    if (!availableMonths.includes(newMonth)) {
      setAvailableMonths((prev) => [...prev, newMonth]);
    }
    setSelectedMonth(newMonth);
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-between">
      <div>
        {/* Header matching corporate blue Android bar */}
        <AppHeader
          selectedMonth={selectedMonth}
          totalCount={totalCount}
          pendingCount={pendingCount}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
          onOpenMonthSelector={() => setShowMonthModal(true)}
          onOpenExport={() => setShowExportModal(true)}
          onOpenStatusManager={() => setShowStatusModal(true)}
          onOpenAccountManager={() => setShowAccountModal(true)}
          onOpenCategoryManager={() => setShowCategoryModal(true)}
        />

        {/* Content Container */}
        <main className="max-w-4xl mx-auto px-3 sm:px-4 py-4 pb-24">
          {deleteFeedback && (
            <div
              className={`mb-3 p-3 rounded-lg text-xs font-semibold flex items-center space-x-2 transition-all ${
                deleteFeedback.type === 'success'
                  ? 'bg-green-50 text-green-800 border border-green-200'
                  : 'bg-red-50 text-red-800 border border-red-200'
              }`}
            >
              <span>{deleteFeedback.message}</span>
            </div>
          )}
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
                {searchQuery ? 'No matching tasks found' : `No tasks found for ${selectedMonth}`}
              </h3>
              <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
                {searchQuery
                  ? 'Try adjusting your search terms.'
                  : 'Get started by creating a new task using the blue "+" button.'}
              </p>
              {!searchQuery && (
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
            <div className="space-y-2.5">
              {filteredTasks.map((task) => (
                <TaskCard
                  key={task.id || task.taskNumber}
                  task={task}
                  availableStatuses={availableStatusNames}
                  onCardClick={() => navigate(`/tasks/${task.id}`, { state: { task } })}
                  onStatusChange={(newStatus) => handleStatusChange(task.id, newStatus)}
                  onDeleteClick={() => setTaskToDelete(task)}
                />
              ))}
            </div>
          )}
        </main>
      </div>

      {/* Floating Add Task Button (FAB) matching Android */}
      <div className="fixed right-5 bottom-12 z-30">
        <button
          type="button"
          onClick={() => navigate('/tasks/new', { state: { month: selectedMonth } })}
          className="w-14 h-14 rounded-full bg-app-primary hover:bg-app-dark-blue text-white shadow-xl flex items-center justify-center transition-transform hover:scale-105 active:scale-95"
          title="Add New Task"
        >
          <Plus className="w-7 h-7" />
        </button>
      </div>

      {/* Footer */}
      <DeveloperFooter />

      {/* Modals */}
      <MonthSelectorModal
        isOpen={showMonthModal}
        selectedMonth={selectedMonth}
        availableMonths={availableMonths}
        onSelectMonth={setSelectedMonth}
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
        onSyncOfficialStatuses={async () => {
          if (currentUser) {
            return await syncOfficialStatusList(currentUser.uid, true);
          }
        }}
        onClose={() => setShowStatusModal(false)}
      />

      <AccountManagerModal
        isOpen={showAccountModal}
        accounts={accounts}
        onSyncOfficialList={async () => {
          if (currentUser) {
            return await syncOfficialAccountList(currentUser.uid);
          }
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
        tasks={tasks}
        onClose={() => setShowExportModal(false)}
      />

      <ConfirmDeleteModal
        isOpen={!!taskToDelete}
        title="Delete Task"
        message="Are you sure you want to delete this task? This action cannot be undone."
        confirmText="Delete"
        onConfirm={handleConfirmDelete}
        onCancel={() => setTaskToDelete(null)}
      />
    </div>
  );
};
