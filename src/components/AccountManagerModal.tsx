import React, { useState, useMemo } from 'react';
import { BookOpen, Search, X, CheckCircle2, RefreshCw, ShieldCheck, Download } from 'lucide-react';
import { AccountItem, OFFICIAL_ACCOUNTS } from '../types';
import { AccountSyncReport } from '../firebase/db';

interface AccountManagerModalProps {
  isOpen: boolean;
  accounts: AccountItem[];
  onSyncOfficialList?: () => Promise<AccountSyncReport | undefined>;
  onClose: () => void;
}

export const AccountManagerModal: React.FC<AccountManagerModalProps> = ({
  isOpen,
  accounts,
  onSyncOfficialList,
  onClose,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [syncing, setSyncing] = useState(false);
  const [syncReport, setSyncReport] = useState<AccountSyncReport | null>(null);
  const [syncMessage, setSyncMessage] = useState<string | null>(null);

  const displayList = useMemo(() => {
    // If user has accounts synced from Firestore, use them; otherwise show official master accounts
    const sourceList = accounts.length > 0 ? accounts : OFFICIAL_ACCOUNTS.map((a, idx) => ({
      id: `off_${idx}`,
      ownerUid: '',
      accountName: a.accountName,
      accountCode: a.accountCode,
    }));

    if (!searchQuery.trim()) return sourceList;
    const q = searchQuery.toLowerCase().trim();
    return sourceList.filter(
      (a) =>
        a.accountName.toLowerCase().includes(q) ||
        a.accountCode.toLowerCase().includes(q)
    );
  }, [accounts, searchQuery]);

  if (!isOpen) return null;

  const handleRunSync = async () => {
    if (!onSyncOfficialList) return;
    try {
      setSyncing(true);
      setSyncMessage(null);
      const rep = await onSyncOfficialList();
      if (rep) {
        setSyncReport(rep);
        setSyncMessage('Official Master List Synchronized & Cleaned Successfully!');
      }
    } catch (err: any) {
      console.error('Manual account sync error:', err);
      setSyncMessage(`Sync notice: ${err.message || 'Offline mode active - Official list loaded'}`);
    } finally {
      setSyncing(false);
    }
  };

  const downloadBackup = () => {
    const backupData = localStorage.getItem('account_list_backup_' + (accounts[0]?.ownerUid || 'user'));
    if (!backupData) {
      alert('No previous backup record found in local storage.');
      return;
    }
    const blob = new Blob([backupData], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `account_list_backup_${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fadeIn">
      <div className="bg-white rounded-xl max-w-2xl w-full p-5 shadow-2xl border border-gray-100 flex flex-col max-h-[88vh]">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-gray-100 flex-shrink-0">
          <div className="flex items-center space-x-2 text-app-primary">
            <BookOpen className="w-5 h-5" />
            <div>
              <h3 className="font-semibold text-gray-900 text-base">
                Official Account Master List ({OFFICIAL_ACCOUNTS.length})
              </h3>
              <p className="text-[11px] text-gray-500">
                Phase 1 Chart of Accounts: 154 official entries strictly verified
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Sync Action Banner */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 my-3 flex-shrink-0">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
            <div className="flex items-start space-x-2">
              <ShieldCheck className="w-4 h-4 text-app-primary flex-shrink-0 mt-0.5" />
              <div className="text-xs text-gray-700">
                <span className="font-semibold text-gray-900">Official Master List Enforcement:</span>{' '}
                Only verified Account Name + Code combinations are permitted. Tasks remain safe and intact.
              </div>
            </div>
            {onSyncOfficialList && (
              <button
                type="button"
                onClick={handleRunSync}
                disabled={syncing}
                className="inline-flex items-center space-x-1.5 px-3 py-1.5 bg-app-primary hover:bg-app-dark-blue text-white rounded-md text-xs font-semibold shadow-sm transition-all disabled:opacity-50 flex-shrink-0"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${syncing ? 'animate-spin' : ''}`} />
                <span>{syncing ? 'Syncing...' : 'Sync & Clean List'}</span>
              </button>
            )}
          </div>

          {syncReport && (
            <div className="mt-2.5 pt-2 border-t border-blue-200/60 text-[11px] text-gray-700 grid grid-cols-2 sm:grid-cols-4 gap-2">
              <div className="bg-white/80 p-1.5 rounded">
                <span className="text-gray-500 block">Official Rows:</span>
                <span className="font-bold text-gray-900">{syncReport.totalCsvRows}</span>
              </div>
              <div className="bg-white/80 p-1.5 rounded">
                <span className="text-gray-500 block">Retained:</span>
                <span className="font-bold text-green-700">{syncReport.retainedAccountsCount}</span>
              </div>
              <div className="bg-white/80 p-1.5 rounded">
                <span className="text-gray-500 block">New Added:</span>
                <span className="font-bold text-blue-700">{syncReport.newAccountsAddedCount}</span>
              </div>
              <div className="bg-white/80 p-1.5 rounded">
                <span className="text-gray-500 block">Unlisted Removed:</span>
                <span className="font-bold text-red-700">{syncReport.unlistedAccountsRemovedCount}</span>
              </div>
            </div>
          )}

          {syncMessage && (
            <div className="mt-2 text-xs text-green-800 font-medium flex items-center space-x-1.5">
              <CheckCircle2 className="w-3.5 h-3.5 text-green-600" />
              <span>{syncMessage}</span>
            </div>
          )}
        </div>

        {/* Search input */}
        <div className="relative mb-2 flex-shrink-0">
          <Search className="w-4 h-4 text-gray-400 absolute left-3 top-3 pointer-events-none" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by account name or code (e.g. Salaries, 4521001, 4532404)..."
            className="w-full pl-9 pr-8 py-2 text-xs border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-app-primary"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-2.5 top-2.5 text-gray-400 hover:text-gray-600"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Accounts List Table */}
        <div className="flex-1 overflow-y-auto divide-y divide-gray-100 border border-gray-200 rounded-lg text-xs">
          {displayList.length === 0 ? (
            <div className="p-8 text-center text-gray-500">
              No official accounts found matching "{searchQuery}"
            </div>
          ) : (
            displayList.map((acc, index) => {
              const isSpecial4532404 = acc.accountCode === '4532404';
              return (
                <div
                  key={`${acc.accountName}__${acc.accountCode}__${index}`}
                  className="p-2.5 flex items-center justify-between hover:bg-gray-50 transition-colors"
                >
                  <div className="min-w-0 pr-3">
                    <div className="font-medium text-gray-900 truncate">
                      {acc.accountName}
                    </div>
                    {isSpecial4532404 && (
                      <span className="text-[10px] text-amber-700 bg-amber-50 border border-amber-200 rounded px-1.5 py-0.2 mt-0.5 inline-block">
                        Distinct 4532404 Entry
                      </span>
                    )}
                  </div>
                  <div className="flex items-center space-x-2 flex-shrink-0">
                    <span className="font-mono text-gray-700 bg-gray-100 px-2 py-0.5 rounded text-[11px] font-semibold border border-gray-200">
                      {acc.accountCode}
                    </span>
                    <span className="text-[10px] bg-green-50 text-green-700 border border-green-200 px-1.5 py-0.5 rounded font-medium">
                      Official
                    </span>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Footer info & actions */}
        <div className="pt-3 mt-3 border-t border-gray-100 flex items-center justify-between flex-shrink-0">
          <button
            type="button"
            onClick={downloadBackup}
            className="text-[11px] text-gray-500 hover:text-gray-800 flex items-center space-x-1"
            title="Download JSON backup taken prior to cleanup"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Download Local Backup</span>
          </button>
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-1.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-semibold rounded-lg transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
