import React, { useState, useRef, useEffect } from 'react';
import {
  Settings,
  Calendar,
  Download,
  Search,
  X,
  Sliders,
  BookOpen,
  FolderTree,
  LogOut,
  ChevronDown,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

interface AppHeaderProps {
  selectedMonth: string;
  totalCount: number;
  pendingCount: number;
  searchQuery: string;
  onSearchChange: (query: string) => void;
  onOpenMonthSelector: () => void;
  onOpenExport: () => void;
  onOpenStatusManager: () => void;
  onOpenAccountManager: () => void;
  onOpenCategoryManager: () => void;
}

export const AppHeader: React.FC<AppHeaderProps> = ({
  selectedMonth,
  totalCount,
  pendingCount,
  searchQuery,
  onSearchChange,
  onOpenMonthSelector,
  onOpenExport,
  onOpenStatusManager,
  onOpenAccountManager,
  onOpenCategoryManager,
}) => {
  const { currentUser, logout } = useAuth();
  const [showSettingsMenu, setShowSettingsMenu] = useState(false);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const settingsMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (settingsMenuRef.current && !settingsMenuRef.current.contains(event.target as Node)) {
        setShowSettingsMenu(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <header className="sticky top-0 z-40 w-full bg-white shadow-sm">
      {/* Primary Corporate Blue Header */}
      <div className="bg-app-primary text-white">
        <div className="max-w-4xl mx-auto px-3 sm:px-4 h-14 flex items-center justify-between">
          {/* Left: Settings Menu */}
          <div className="relative" ref={settingsMenuRef}>
            <button
              type="button"
              onClick={() => setShowSettingsMenu(!showSettingsMenu)}
              className="w-10 h-10 rounded-full flex items-center justify-center text-white/90 hover:text-white hover:bg-white/10 transition-colors"
              title="Settings & Menu"
            >
              <Settings className="w-5 h-5" />
            </button>

            {showSettingsMenu && (
              <div className="absolute left-0 mt-2 w-64 bg-white rounded-xl shadow-2xl border border-gray-100 py-2 z-50 text-gray-800 animate-fadeIn">
                <div className="px-4 py-2 border-b border-gray-100">
                  <p className="text-[11px] font-medium text-gray-500 uppercase tracking-wider">
                    Signed in as
                  </p>
                  <p className="text-xs font-semibold text-gray-900 truncate">
                    {currentUser?.email || currentUser?.displayName || currentUser?.uid.slice(0, 12)}
                  </p>
                </div>

                <div className="py-1">
                  <button
                    type="button"
                    onClick={() => {
                      setShowSettingsMenu(false);
                      onOpenAccountManager();
                    }}
                    className="w-full flex items-center space-x-2.5 px-4 py-2 text-xs font-medium text-gray-700 hover:bg-gray-50 hover:text-app-primary transition-colors text-left"
                  >
                    <BookOpen className="w-4 h-4 text-app-primary" />
                    <span>Master Account List</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setShowSettingsMenu(false);
                      onOpenCategoryManager();
                    }}
                    className="w-full flex items-center space-x-2.5 px-4 py-2 text-xs font-medium text-gray-700 hover:bg-gray-50 hover:text-app-primary transition-colors text-left"
                  >
                    <FolderTree className="w-4 h-4 text-app-primary" />
                    <span>Category Management</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setShowSettingsMenu(false);
                      onOpenStatusManager();
                    }}
                    className="w-full flex items-center space-x-2.5 px-4 py-2 text-xs font-medium text-gray-700 hover:bg-gray-50 hover:text-app-primary transition-colors text-left"
                  >
                    <Sliders className="w-4 h-4 text-app-primary" />
                    <span>Dynamic Status Settings</span>
                  </button>
                </div>

                <div className="border-t border-gray-100 pt-1">
                  <button
                    type="button"
                    onClick={() => {
                      setShowSettingsMenu(false);
                      logout();
                    }}
                    className="w-full flex items-center space-x-2.5 px-4 py-2 text-xs font-medium text-red-600 hover:bg-red-50 transition-colors text-left"
                  >
                    <LogOut className="w-4 h-4" />
                    <span>Sign Out</span>
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Center: Month Selector matching Android wireframe 📅 Nov-2026 ▾ */}
          <button
            type="button"
            onClick={onOpenMonthSelector}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg hover:bg-white/10 transition-colors font-semibold text-sm sm:text-base tracking-wide"
          >
            <Calendar className="w-4 h-4 text-white/90" />
            <span>{selectedMonth}</span>
            <ChevronDown className="w-4 h-4 text-white/90" />
          </button>

          {/* Right Actions: Export (⤓) & Search (🔍) */}
          <div className="flex items-center space-x-1">
            <button
              type="button"
              onClick={onOpenExport}
              className="w-10 h-10 rounded-full flex items-center justify-center text-white/90 hover:text-white hover:bg-white/10 transition-colors"
              title="Export Tasks"
            >
              <Download className="w-5 h-5" />
            </button>

            <button
              type="button"
              onClick={() => {
                setIsSearchOpen(!isSearchOpen);
                if (isSearchOpen) onSearchChange('');
              }}
              className={`w-10 h-10 rounded-full flex items-center justify-center transition-colors ${
                isSearchOpen ? 'bg-white/20 text-white' : 'text-white/90 hover:text-white hover:bg-white/10'
              }`}
              title="Search Tasks"
            >
              <Search className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>

      {/* Expandable Search Bar */}
      {isSearchOpen && (
        <div className="bg-white border-b border-gray-200 px-3 sm:px-4 py-2 animate-fadeIn">
          <div className="max-w-4xl mx-auto relative flex items-center">
            <Search className="w-4 h-4 text-gray-400 absolute left-3 pointer-events-none" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => onSearchChange(e.target.value)}
              placeholder="Search by description, unit, status, account, remarks..."
              autoFocus
              className="w-full pl-9 pr-8 py-2 text-xs sm:text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary focus:border-transparent"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => onSearchChange('')}
                className="absolute right-2.5 p-1 text-gray-400 hover:text-gray-600 rounded"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      )}

      {/* Subheader Count Bar matching Android: Total Items: 17 | Pending: 12 */}
      <div className="bg-[#F9FAFB] border-b border-[#E5E7EB]">
        <div className="max-w-4xl mx-auto px-4 py-2 flex items-center text-xs sm:text-[13.5px] text-[#374151]">
          <span>Total Items:&nbsp;</span>
          <strong className="text-gray-900 font-bold">{totalCount}</strong>
          <span className="text-gray-300 mx-2 sm:mx-3">|</span>
          <span>Pending:&nbsp;</span>
          <strong className="text-gray-900 font-bold">{pendingCount}</strong>
        </div>
      </div>
    </header>
  );
};
