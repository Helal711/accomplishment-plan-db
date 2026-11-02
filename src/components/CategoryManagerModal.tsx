import React, { useState } from 'react';
import { FolderTree, Plus, Trash2, Edit2, Check, X } from 'lucide-react';
import { CategoryItem, DEFAULT_CATEGORIES } from '../types';

interface CategoryManagerModalProps {
  isOpen: boolean;
  categories: CategoryItem[];
  onAddCategory: (categoryName: string) => Promise<void>;
  onEditCategory: (categoryId: string, newName: string) => Promise<void>;
  onDeleteCategory: (categoryId: string) => Promise<void>;
  onClose: () => void;
}

export const CategoryManagerModal: React.FC<CategoryManagerModalProps> = ({
  isOpen,
  categories,
  onAddCategory,
  onEditCategory,
  onDeleteCategory,
  onClose,
}) => {
  const [newCategory, setNewCategory] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Editing state
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState('');
  const [editError, setEditError] = useState('');

  // Deleting confirmation state
  const [deletingCategory, setDeletingCategory] = useState<CategoryItem | null>(null);

  if (!isOpen) return null;

  // Use categories from props, fallback to default list if not yet loaded
  const displayCategories: CategoryItem[] = categories.length > 0
    ? [...categories].sort((a, b) => a.categoryName.localeCompare(b.categoryName))
    : DEFAULT_CATEGORIES.map((cat, idx) => ({
        id: `default_${idx}`,
        ownerUid: '',
        categoryName: cat,
      }));

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    const val = newCategory.trim();
    if (!val) {
      setError('Category name cannot be blank');
      return;
    }

    const isDuplicate = displayCategories.some(
      (c) => c.categoryName.trim().toLowerCase() === val.toLowerCase()
    );
    if (isDuplicate) {
      setError(`Category "${val}" already exists`);
      return;
    }

    try {
      setIsSubmitting(true);
      setError('');
      await onAddCategory(val);
      setNewCategory('');
    } catch (err: any) {
      setError(err.message || 'Failed to add category');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleStartEdit = (cat: CategoryItem) => {
    setEditingId(cat.id);
    setEditName(cat.categoryName);
    setEditError('');
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setEditName('');
    setEditError('');
  };

  const handleSaveEdit = async (cat: CategoryItem) => {
    const val = editName.trim();
    if (!val) {
      setEditError('Category name cannot be blank');
      return;
    }

    const isDuplicate = displayCategories.some(
      (c) => c.id !== cat.id && c.categoryName.trim().toLowerCase() === val.toLowerCase()
    );
    if (isDuplicate) {
      setEditError(`Category "${val}" already exists`);
      return;
    }

    try {
      setIsSubmitting(true);
      setEditError('');
      if (cat.id && !cat.id.startsWith('default_')) {
        await onEditCategory(cat.id, val);
      } else {
        await onAddCategory(val);
      }
      setEditingId(null);
      setEditName('');
    } catch (err: any) {
      setEditError(err.message || 'Failed to update category');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!deletingCategory) return;
    try {
      setIsSubmitting(true);
      if (deletingCategory.id && !deletingCategory.id.startsWith('default_')) {
        await onDeleteCategory(deletingCategory.id);
      }
      setDeletingCategory(null);
    } catch (err: any) {
      alert(err.message || 'Failed to delete category');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fadeIn">
      <div className="bg-white rounded-xl max-w-lg w-full p-5 shadow-2xl border border-gray-100 flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-gray-100 flex-shrink-0">
          <div className="flex items-center space-x-2 text-app-primary">
            <FolderTree className="w-5 h-5 text-blue-600" />
            <h3 className="font-semibold text-gray-900 text-base">Category Management</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-xs text-gray-500 my-2">
          Manage task categories ({displayCategories.length} available). You can add custom categories, rename, or delete them.
        </p>

        {/* Add Category Form */}
        <form onSubmit={handleAdd} className="my-2 flex-shrink-0">
          <div className="flex space-x-2">
            <input
              type="text"
              value={newCategory}
              onChange={(e) => {
                setNewCategory(e.target.value);
                setError('');
              }}
              placeholder="Enter new category name..."
              className="flex-1 px-3 py-2 text-xs border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              disabled={isSubmitting}
            />
            <button
              type="submit"
              disabled={isSubmitting || !newCategory.trim()}
              className="px-3 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-medium flex items-center space-x-1 disabled:opacity-50 transition"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Add</span>
            </button>
          </div>
          {error && <p className="text-[11px] text-red-600 mt-1">{error}</p>}
        </form>

        {/* Category List */}
        <div className="flex-1 overflow-y-auto space-y-1.5 pr-1 my-2">
          {displayCategories.map((cat) => {
            const isEditing = editingId === cat.id;

            return (
              <div
                key={cat.id || cat.categoryName}
                className="flex items-center justify-between p-2.5 bg-gray-50 hover:bg-gray-100/70 border border-gray-200 rounded-lg text-xs transition"
              >
                {isEditing ? (
                  <div className="flex-1 flex flex-col mr-2">
                    <div className="flex items-center space-x-2">
                      <input
                        type="text"
                        value={editName}
                        onChange={(e) => {
                          setEditName(e.target.value);
                          setEditError('');
                        }}
                        className="flex-1 px-2 py-1 text-xs border border-blue-400 rounded focus:outline-none"
                        autoFocus
                      />
                      <button
                        type="button"
                        onClick={() => handleSaveEdit(cat)}
                        disabled={isSubmitting}
                        className="p-1 text-green-600 hover:bg-green-50 rounded"
                        title="Save"
                      >
                        <Check className="w-4 h-4" />
                      </button>
                      <button
                        type="button"
                        onClick={handleCancelEdit}
                        className="p-1 text-gray-400 hover:bg-gray-200 rounded"
                        title="Cancel"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                    {editError && <p className="text-[10px] text-red-600 mt-1">{editError}</p>}
                  </div>
                ) : (
                  <>
                    <span className="font-medium text-gray-800 truncate mr-2">{cat.categoryName}</span>
                    <div className="flex items-center space-x-1 flex-shrink-0">
                      <button
                        type="button"
                        onClick={() => handleStartEdit(cat)}
                        className="p-1 text-gray-500 hover:text-blue-600 hover:bg-white rounded"
                        title="Edit category name"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        type="button"
                        onClick={() => setDeletingCategory(cat)}
                        className="p-1 text-gray-400 hover:text-red-600 hover:bg-white rounded"
                        title="Delete category"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </>
                )}
              </div>
            );
          })}
        </div>

        {/* Footer */}
        <div className="pt-3 border-t border-gray-100 flex justify-end flex-shrink-0">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-1.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-medium rounded-lg transition"
          >
            Close
          </button>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      {deletingCategory && (
        <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fadeIn">
          <div className="bg-white rounded-xl max-w-sm w-full p-5 shadow-2xl border border-gray-100">
            <h4 className="font-bold text-gray-900 text-sm mb-2">Delete Category</h4>
            <p className="text-xs text-gray-600 mb-4">
              Are you sure you want to delete category <strong>"{deletingCategory.categoryName}"</strong>? Existing tasks will keep their saved category.
            </p>
            <div className="flex justify-end space-x-2">
              <button
                type="button"
                onClick={() => setDeletingCategory(null)}
                disabled={isSubmitting}
                className="px-3 py-1.5 border border-gray-300 rounded-lg text-xs font-medium text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isSubmitting}
                className="px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white rounded-lg text-xs font-medium"
              >
                {isSubmitting ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
