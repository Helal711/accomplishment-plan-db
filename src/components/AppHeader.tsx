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
import { getStatusColor } from '../utils/statusColors';
import {
  HIDDEN_DASHBOARD_SUMMARY_STATUSES,
  isHiddenDashboardSummaryStatus,
} from '../utils/statusOptions';
import { isQuickSearchShortcut } from '../utils/keyboardShortcuts';
import { AnimatedCount } from './AnimatedCount';

export { HIDDEN_DASHBOARD_SUMMARY_STATUSES, isHiddenDashboardSummaryStatus };

export function filterVisibleDashboardStatusCounts<T extends { name: string }>(
  statusCounts: T[]
): T[] {
  return statusCounts.filter((st) => !isHiddenDashboardSummaryStatus(st.name));
}

export interface StatusSummaryButtonAppearance {
  hasTasks: boolean;
  backgroundColor: string;
  textColor: string;
  borderColor: string;
  boxShadow?: string;
  dotColor: string;
  badgeBackgroundColor: string;
  badgeTextColor: string;
}

export function getStatusSummaryButtonAppearance(
  statusName: string,
  count: number,
  isActive: boolean
): StatusSummaryButtonAppearance {
  if (count <= 0) {
    return {
      hasTasks: false,
      backgroundColor: '#FFFFFF',
      textColor: '#111827',
      borderColor: isActive ? '#111827' : '#D1D5DB',
      boxShadow: isActive ? '0 0 0 1.5px #111827' : undefined,
      dotColor: '#9CA3AF',
      badgeBackgroundColor: isActive ? '#111827' : '#F3F4F6',
      badgeTextColor: isActive ? '#FFFFFF' : '#111827',
    };
  }

  const colors = getStatusColor(statusName);
  return {
    hasTasks: true,
    backgroundColor: colors.bg,
    textColor: colors.text,
    borderColor: isActive ? colors.dot : colors.border,
    boxShadow: isActive ? `0 0 0 1.5px ${colors.dot}` : undefined,
    dotColor: colors.dot,
    badgeBackgroundColor: isActive ? colors.dot : 'rgba(255, 255, 255, 0.85)',
    badgeTextColor: isActive ? '#FFFFFF' : colors.text,
  };
}

interface AppHeaderProps {
  selectedMonth: string;
  totalCount: number;
  pendingCount: number;
  statusCounts?: { name: string; count: number }[];
  selectedStatus?: string | null;
  onSelectStatus?: (status: string | null) => void;
  searchQuery: string;
  onSearchChange: (query: string) => void;
  resultCountLabel?: string;
  filteredCount?: number;
  loadingTasks?: boolean;
  hasActiveFilter?: boolean;
  hasClearableFilter?: boolean;
  onClearFilters?: () => void;
  onOpenMonthSelector: () => void;
  isMonthModalOpen?: boolean;
  onOpenExport: () => void;
  onOpenStatusManager: () => void;
  onOpenAccountManager: () => void;
  onOpenCategoryManager: () => void;
}

export const AppHeader: React.FC<AppHeaderProps> = ({
  selectedMonth,
  totalCount,
  pendingCount,
  statusCounts = [],
  selectedStatus = null,
  onSelectStatus,
  searchQuery,
  onSearchChange,
  resultCountLabel = '',
  filteredCount = totalCount,
  loadingTasks = false,
  hasActiveFilter = false,
  hasClearableFilter = false,
  onClearFilters,
  onOpenMonthSelector,
  isMonthModalOpen = false,
  onOpenExport,
  onOpenStatusManager,
  onOpenAccountManager,
  onOpenCategoryManager,
}) => {
  const { currentUser, logout } = useAuth();
  const [showSettingsMenu, setShowSettingsMenu] = useState(false);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const settingsMenuRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (settingsMenuRef.current && !settingsMenuRef.current.contains(event.target as Node)) {
        setShowSettingsMenu(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    const handleQuickSearchKeyDown = (event: KeyboardEvent) => {
      if (!isQuickSearchShortcut(event)) return;
      event.preventDefault();
      if (!isSearchOpen) {
        setIsSearchOpen(true);
        requestAnimationFrame(() => {
          searchInputRef.current?.focus();
          searchInputRef.current?.select();
        });
      } else {
        searchInputRef.current?.focus();
        searchInputRef.current?.select();
      }
    };
    window.addEventListener('keydown', handleQuickSearchKeyDown);
    return () => window.removeEventListener('keydown', handleQuickSearchKeyDown);
  }, [isSearchOpen]);

  const visibleStatusCounts = filterVisibleDashboardStatusCounts(statusCounts);

  return (
    <header className="sticky top-0 z-40 w-full bg-white shadow-sm">
      {/* Primary Corporate Blue Header */}
      <div className="bg-app-primary text-white">
        <div className="max-w-4xl mx-auto px-2 sm:px-4 h-12 sm:h-14 flex items-center justify-between">
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
            aria-expanded={isMonthModalOpen}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg hover:bg-white/10 transition-colors duration-200 font-semibold text-sm sm:text-base tracking-wide"
          >
            <Calendar className="w-4 h-4 text-white/90" />
            <span>{selectedMonth}</span>
            <ChevronDown
              className={`w-4 h-4 text-white/90 transition-transform duration-200 ${
                isMonthModalOpen ? 'rotate-180' : ''
              }`}
            />
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
                if (isSearchOpen) {
                  setIsSearchOpen(false);
                  onSearchChange('');
                } else {
                  setIsSearchOpen(true);
                  requestAnimationFrame(() => {
                    searchInputRef.current?.focus();
                  });
                }
              }}
              aria-label={isSearchOpen ? 'Close search' : 'Search tasks'}
              aria-expanded={isSearchOpen}
              aria-controls="dashboard-search-box"
              data-testid="header-search-toggle-btn"
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

      {/* Unified Result Count + Compact Search Row directly below the blue header */}
      <div
        data-testid="task-result-count-bar"
        className="bg-white border-b border-gray-200"
      >
        <div className="max-w-4xl mx-auto px-2 sm:px-4 py-1.5 sm:min-h-[42px] flex flex-col sm:flex-row sm:items-center sm:justify-between sm:gap-4">
          {/* Left: Dynamic Result Count + subtle Month/Year + Clear Filter */}
          <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5 min-w-0">
            {loadingTasks ? (
              <span className="text-[11px] sm:text-xs font-medium text-gray-400">
                Loading tasks...
              </span>
            ) : (
              <span
                data-testid="task-result-count"
                className={`text-[11px] sm:text-xs font-semibold leading-tight transition-colors ${
                  filteredCount === 0
                    ? 'text-amber-700'
                    : hasActiveFilter
                      ? 'text-app-primary'
                      : 'text-gray-700'
                }`}
              >
                {resultCountLabel}
              </span>
            )}
            <span aria-hidden="true" className="text-gray-300 text-[10px] select-none">
              •
            </span>
            <span
              data-testid="active-month-label"
              className="text-[10.5px] sm:text-[11px] font-medium text-gray-400 leading-tight flex-shrink-0"
            >
              {selectedMonth}
            </span>
            {!loadingTasks && hasClearableFilter && onClearFilters && (
              <button
                type="button"
                onClick={onClearFilters}
                data-testid="clear-active-filters-btn"
                className="text-[11px] sm:text-xs font-semibold text-app-primary hover:underline flex-shrink-0 py-0.5 px-1 rounded transition-colors"
              >
                Clear Filter
              </button>
            )}
          </div>

          {/* Right (Desktop) / Second Compact Row (Mobile): Expandable Compact Search Box */}
          <div
            id="dashboard-search-box"
            data-testid="header-search-container"
            data-open={isSearchOpen ? 'true' : 'false'}
            aria-hidden={!isSearchOpen}
            className="search-panel-collapse"
          >
            <div className="search-panel-inner">
              <div className="p-0.5">
                <div className="relative flex items-center w-full sm:w-[248px]">
                  <Search className="w-3.5 h-3.5 text-app-primary/70 absolute left-2.5 pointer-events-none" />
                  <input
                    ref={searchInputRef}
                    type="text"
                    value={searchQuery}
                    tabIndex={isSearchOpen ? 0 : -1}
                    onChange={(e) => onSearchChange(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Escape') {
                        e.preventDefault();
                        if (searchQuery) {
                          onSearchChange('');
                        } else {
                          setIsSearchOpen(false);
                        }
                      }
                    }}
                    aria-label="Search tasks"
                    data-testid="header-search-input"
                    placeholder="Search tasks..."
                    className="w-full pl-8 pr-7 py-1.5 text-xs bg-gray-50/70 focus:bg-white text-gray-800 placeholder:text-gray-400 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-app-primary/30 focus:border-app-primary transition-all duration-200"
                  />
                  {searchQuery && (
                    <button
                      type="button"
                      tabIndex={isSearchOpen ? 0 : -1}
                      onClick={() => {
                        onSearchChange('');
                        searchInputRef.current?.focus();
                      }}
                      aria-label="Clear search"
                      title="Clear search"
                      data-testid="header-search-clear-btn"
                      className="absolute right-1.5 p-1 text-gray-400 hover:text-gray-600 rounded transition-colors"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Subheader Count Bar: Ultra-compact on mobile (<640px), compact pills on desktop (>=640px) */}
      <div className="bg-[#F8FAFC] border-b border-[#E5E7EB]">
        <div
          data-testid="status-summary-bar"
          className="max-w-4xl mx-auto px-1 sm:px-4 py-[2px] sm:py-2 flex flex-wrap items-center gap-x-[3px] gap-y-[2px] sm:gap-2"
        >
          {/* Total Items Button (Neutral Blue Theme) */}
          <button
            type="button"
            title={`Total Items: ${totalCount}`}
            onClick={() => onSelectStatus?.(null)}
            aria-pressed={selectedStatus === null}
            className={`status-filter-pill relative inline-flex items-center gap-[2px] sm:gap-1 px-1 sm:px-2.5 py-[1px] sm:py-[5px] min-h-[19px] sm:min-h-[30px] max-w-full rounded-full text-[9px] sm:text-xs leading-none sm:leading-tight font-semibold border cursor-pointer ${
              selectedStatus === null
                ? 'bg-app-primary text-white border-app-primary shadow-sm ring-1 sm:ring-2 ring-blue-200'
                : 'bg-blue-50 text-blue-800 border-blue-200 hover:bg-blue-100/80'
            }`}
          >
            <span aria-hidden="true" className="absolute -inset-y-[3px] -inset-x-[2px] sm:hidden" />
            <span>Total Items:</span>
            <span
              className={`px-1 sm:px-1.5 py-0 sm:py-0.5 rounded-full text-[8px] sm:text-[11px] font-bold leading-none transition-colors duration-200 ${
                selectedStatus === null
                  ? 'bg-white/20 text-white'
                  : 'bg-white text-blue-900 border border-blue-200'
              }`}
            >
              <AnimatedCount value={totalCount} />
            </span>
          </button>

          {visibleStatusCounts.length > 0 ? (
            visibleStatusCounts.map((st, idx) => {
              const isActive =
                !!selectedStatus &&
                selectedStatus.trim().toLowerCase() === st.name.trim().toLowerCase();
              const appearance = getStatusSummaryButtonAppearance(st.name, st.count, isActive);

              return (
                <button
                  key={st.name || idx}
                  type="button"
                  title={`${st.name}: ${st.count}`}
                  onClick={() => onSelectStatus?.(isActive ? null : st.name)}
                  aria-pressed={isActive}
                  style={{
                    backgroundColor: appearance.backgroundColor,
                    color: appearance.textColor,
                    borderColor: appearance.borderColor,
                    boxShadow: appearance.boxShadow,
                  }}
                  className={`status-filter-pill relative inline-flex items-center gap-[2px] sm:gap-1 px-1 sm:px-2.5 py-[1px] sm:py-[5px] min-h-[19px] sm:min-h-[30px] max-w-full rounded-full text-[9px] sm:text-xs leading-none sm:leading-tight font-semibold border cursor-pointer hover:brightness-[0.97] ${
                    isActive ? 'shadow-sm' : ''
                  }`}
                >
                  <span aria-hidden="true" className="absolute -inset-y-[3px] -inset-x-[2px] sm:hidden" />
                  <span
                    className="w-[4px] h-[4px] sm:w-1.5 sm:h-1.5 rounded-full flex-shrink-0 transition-colors duration-200"
                    style={{ backgroundColor: appearance.dotColor }}
                    aria-hidden="true"
                  />
                  <span className="truncate max-w-[100px] sm:max-w-[210px]">{st.name}:</span>
                  <span
                    style={{
                      backgroundColor: appearance.badgeBackgroundColor,
                      color: appearance.badgeTextColor,
                    }}
                    className="px-1 sm:px-1.5 py-0 sm:py-0.5 rounded-full text-[8px] sm:text-[11px] font-bold leading-none flex-shrink-0 transition-colors duration-200"
                  >
                    <AnimatedCount value={st.count} />
                  </span>
                </button>
              );
            })
          ) : (
            (() => {
              const isActive = selectedStatus?.trim().toLowerCase() === 'pending';
              const appearance = getStatusSummaryButtonAppearance('Pending', pendingCount, isActive);
              return (
                <button
                  type="button"
                  title={`Pending: ${pendingCount}`}
                  onClick={() => onSelectStatus?.(isActive ? null : 'Pending')}
                  aria-pressed={isActive}
                  style={{
                    backgroundColor: appearance.backgroundColor,
                    color: appearance.textColor,
                    borderColor: appearance.borderColor,
                    boxShadow: appearance.boxShadow,
                  }}
                  className="status-filter-pill relative inline-flex items-center gap-[2px] sm:gap-1 px-1 sm:px-2.5 py-[1px] sm:py-[5px] min-h-[19px] sm:min-h-[30px] max-w-full rounded-full text-[9px] sm:text-xs leading-none sm:leading-tight font-semibold border cursor-pointer hover:brightness-[0.97]"
                >
                  <span aria-hidden="true" className="absolute -inset-y-[3px] -inset-x-[2px] sm:hidden" />
                  <span
                    className="w-[4px] h-[4px] sm:w-1.5 sm:h-1.5 rounded-full flex-shrink-0 transition-colors duration-200"
                    style={{ backgroundColor: appearance.dotColor }}
                    aria-hidden="true"
                  />
                  <span>Pending:</span>
                  <span
                    style={{
                      backgroundColor: appearance.badgeBackgroundColor,
                      color: appearance.badgeTextColor,
                    }}
                    className="px-1 sm:px-1.5 py-0 sm:py-0.5 rounded-full text-[8px] sm:text-[11px] font-bold leading-none flex-shrink-0 transition-colors duration-200"
                  >
                    <AnimatedCount value={pendingCount} />
                  </span>
                </button>
              );
            })()
          )}
        </div>
      </div>
    </header>
  );
};
