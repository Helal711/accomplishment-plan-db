import {
  collection,
  doc,
  getDoc,
  addDoc,
  updateDoc,
  deleteDoc,
  query,
  where,
  onSnapshot,
  getDocs,
  serverTimestamp,
} from 'firebase/firestore';
import { db } from './config';
import {
  TaskItem,
  AccountItem,
  StatusItem,
  CategoryItem,
  OFFICIAL_ACCOUNTS,
  DEFAULT_ACCOUNTS,
  OFFICIAL_STATUSES,
  DEFAULT_STATUSES,
  DEFAULT_CATEGORIES,
  getAccountKey,
} from '../types';

// Subscribe to tasks for user and specific month
export function subscribeToTasks(
  uid: string,
  monthYear: string,
  onUpdate: (tasks: TaskItem[]) => void,
  onError?: (error: Error) => void
): () => void {
  const q = query(
    collection(db, 'Tasks'),
    where('ownerUid', '==', uid),
    where('monthYear', '==', monthYear)
  );

  return onSnapshot(
    q,
    (snapshot) => {
      const items: TaskItem[] = snapshot.docs.map((docSnap) => {
        const data = docSnap.data();
        return {
          id: docSnap.id,
          ownerUid: data.ownerUid || uid,
          taskNumber: typeof data.taskNumber === 'number' ? data.taskNumber : 1,
          description: data.description || '',
          amount: data.amount || '',
          paymentMode: data.paymentMode || 'Cash',
          unit: data.unit || 'Unit-1',
          priority: data.priority || 'High',
          status: data.status || 'Draft Ready',
          expectedDate: data.expectedDate || '',
          accountName: data.accountName || 'Civil Maintenance',
          accountCode: data.accountCode || 'ACC-CM101',
          category: data.category || '',
          remarks: data.remarks || '',
          monthYear: data.monthYear || monthYear,
          createdAt: data.createdAt,
          updatedAt: data.updatedAt,
        };
      });
      // Sort by taskNumber ascending
      items.sort((a, b) => a.taskNumber - b.taskNumber);
      onUpdate(items);
    },
    (err) => {
      console.error('Error subscribing to tasks:', err);
      if (onError) onError(err);
    }
  );
}

// Subscribe to all accounts for user
export function subscribeToAccounts(
  uid: string,
  onUpdate: (accounts: AccountItem[]) => void
): () => void {
  const q = query(
    collection(db, 'Account_List'),
    where('ownerUid', '==', uid)
  );

  return onSnapshot(
    q,
    (snapshot) => {
      const items: AccountItem[] = snapshot.docs.map((d) => ({
        id: d.id,
        ownerUid: d.data().ownerUid,
        accountName: d.data().accountName || '',
        accountCode: d.data().accountCode || '',
        createdAt: d.data().createdAt,
      }));
      items.sort((a, b) => a.accountName.localeCompare(b.accountName));
      onUpdate(items);
    },
    (err) => console.error('Error subscribing to accounts:', err)
  );
}

// Subscribe to all custom statuses for user
export function subscribeToStatuses(
  uid: string,
  onUpdate: (statuses: StatusItem[]) => void
): () => void {
  const q = query(
    collection(db, 'Status_List'),
    where('ownerUid', '==', uid)
  );

  return onSnapshot(
    q,
    (snapshot) => {
      const items: StatusItem[] = snapshot.docs.map((d) => ({
        id: d.id,
        ownerUid: d.data().ownerUid,
        statusName: d.data().statusName || '',
        isDefault: !!d.data().isDefault,
        createdAt: d.data().createdAt,
      }));
      onUpdate(items);
    },
    (err) => console.error('Error subscribing to statuses:', err)
  );
}

// Add a new task
export async function addTask(task: Omit<TaskItem, 'id'>): Promise<string> {
  const docRef = await addDoc(collection(db, 'Tasks'), {
    ...task,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  return docRef.id;
}

// Update an existing task
export async function updateTask(
  taskId: string,
  updates: Partial<TaskItem>
): Promise<void> {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { id, ...dataToUpdate } = updates;
  const docRef = doc(db, 'Tasks', taskId);
  await updateDoc(docRef, {
    ...dataToUpdate,
    updatedAt: serverTimestamp(),
  });
}

// Delete task with optional ownerUid verification for strict data isolation
export async function deleteTask(taskId: string, expectedOwnerUid?: string): Promise<void> {
  const docRef = doc(db, 'Tasks', taskId);
  if (expectedOwnerUid) {
    const snap = await getDoc(docRef);
    if (!snap.exists()) {
      throw new Error('Task not found or already deleted.');
    }
    const data = snap.data();
    if (data.ownerUid !== expectedOwnerUid) {
      throw new Error('Permission denied: You do not own this task document.');
    }
  }
  await deleteDoc(docRef);
}

// Quick status change
export async function quickUpdateTaskStatus(
  taskId: string,
  newStatus: string
): Promise<void> {
  const docRef = doc(db, 'Tasks', taskId);
  await updateDoc(docRef, {
    status: newStatus,
    updatedAt: serverTimestamp(),
  });
}

// Account CRUD
export async function addAccount(
  uid: string,
  accountName: string,
  accountCode: string
): Promise<string> {
  const docRef = await addDoc(collection(db, 'Account_List'), {
    ownerUid: uid,
    accountName: accountName.trim(),
    accountCode: accountCode.trim(),
    createdAt: serverTimestamp(),
  });
  return docRef.id;
}

export async function updateAccount(
  accountId: string,
  updates: { accountName?: string; accountCode?: string }
): Promise<void> {
  const docRef = doc(db, 'Account_List', accountId);
  await updateDoc(docRef, updates);
}

export async function deleteAccount(accountId: string): Promise<void> {
  const docRef = doc(db, 'Account_List', accountId);
  await deleteDoc(docRef);
}

// Status CRUD
export async function addStatus(
  uid: string,
  statusName: string,
  isDefault: boolean = false
): Promise<string> {
  const docRef = await addDoc(collection(db, 'Status_List'), {
    ownerUid: uid,
    statusName: statusName.trim(),
    isDefault,
    createdAt: serverTimestamp(),
  });
  return docRef.id;
}

export async function deleteStatus(statusId: string): Promise<void> {
  const docRef = doc(db, 'Status_List', statusId);
  await deleteDoc(docRef);
}

// Category Subscription & CRUD
export function subscribeToCategories(
  uid: string,
  onUpdate: (categories: CategoryItem[]) => void
): () => void {
  const q = query(
    collection(db, 'Category_List'),
    where('ownerUid', '==', uid)
  );

  return onSnapshot(
    q,
    (snapshot) => {
      const items: CategoryItem[] = snapshot.docs.map((d) => ({
        id: d.id,
        ownerUid: d.data().ownerUid,
        categoryName: d.data().categoryName || '',
        createdAt: d.data().createdAt,
        updatedAt: d.data().updatedAt,
      }));
      items.sort((a, b) => a.categoryName.localeCompare(b.categoryName));
      onUpdate(items);
    },
    (err) => console.error('Error subscribing to categories:', err)
  );
}

export async function addCategory(
  uid: string,
  categoryName: string
): Promise<string> {
  const docRef = await addDoc(collection(db, 'Category_List'), {
    ownerUid: uid,
    categoryName: categoryName.trim(),
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  return docRef.id;
}

export async function updateCategory(
  categoryId: string,
  categoryName: string
): Promise<void> {
  const docRef = doc(db, 'Category_List', categoryId);
  await updateDoc(docRef, {
    categoryName: categoryName.trim(),
    updatedAt: serverTimestamp(),
  });
}

export async function deleteCategory(categoryId: string): Promise<void> {
  const docRef = doc(db, 'Category_List', categoryId);
  await deleteDoc(docRef);
}

export interface AccountSyncReport {
  totalCsvRows: number;
  uniqueCombinations: number;
  retainedAccountsCount: number;
  newAccountsAddedCount: number;
  unlistedAccountsRemovedCount: number;
  duplicateAccountsRemovedCount: number;
  preservedCode4532404: boolean;
  tasksPreserved: boolean;
  backupTimestamp: string;
}

// Synchronize and clean Account_List for authenticated user against the official 154-entry master list
export async function syncOfficialAccountList(uid: string): Promise<AccountSyncReport> {
  const qAcc = query(
    collection(db, 'Account_List'),
    where('ownerUid', '==', uid)
  );
  const accSnap = await getDocs(qAcc);

  // 1. Safe Backup of existing user Account_List before performing any changes
  const backupAccounts = accSnap.docs.map((d) => ({
    id: d.id,
    ownerUid: d.data().ownerUid,
    accountName: d.data().accountName || '',
    accountCode: d.data().accountCode || '',
  }));
  const backupTimestamp = new Date().toISOString();
  try {
    localStorage.setItem(
      `account_list_backup_${uid}`,
      JSON.stringify({
        timestamp: backupTimestamp,
        uid,
        count: backupAccounts.length,
        accounts: backupAccounts,
      })
    );
  } catch (e) {
    console.warn('[Account Sync] LocalStorage backup write error (storage quota):', e);
  }

  // 2. Build official accounts map with composite key: name:::code
  const officialKeyMap = new Map<string, { accountName: string; accountCode: string }>();
  for (const acc of OFFICIAL_ACCOUNTS) {
    officialKeyMap.set(getAccountKey(acc.accountName, acc.accountCode), acc);
  }

  // 3. Classify existing docs: retained, unlisted (to delete), duplicate (to delete)
  const seenExistingKeys = new Set<string>();
  const toDeleteDocIds: string[] = [];
  let retainedCount = 0;
  let unlistedRemovedCount = 0;
  let duplicateRemovedCount = 0;

  for (const docSnap of accSnap.docs) {
    const data = docSnap.data();
    const name = (data.accountName || '').trim();
    const code = (data.accountCode || '').trim();
    const key = getAccountKey(name, code);

    if (!officialKeyMap.has(key)) {
      // Unlisted / old account -> queue for deletion
      toDeleteDocIds.push(docSnap.id);
      unlistedRemovedCount++;
    } else if (seenExistingKeys.has(key)) {
      // Duplicate of an already retained official account -> delete duplicate
      toDeleteDocIds.push(docSnap.id);
      duplicateRemovedCount++;
    } else {
      // Valid official account -> retain
      seenExistingKeys.add(key);
      retainedCount++;
    }
  }

  // Delete unlisted and duplicate records
  for (const docId of toDeleteDocIds) {
    try {
      await deleteDoc(doc(db, 'Account_List', docId));
    } catch (err) {
      console.error(`[Account Sync] Failed to delete doc ${docId}:`, err);
    }
  }

  // 4. Add missing official accounts
  let newAddedCount = 0;
  for (const [key, acc] of officialKeyMap.entries()) {
    if (!seenExistingKeys.has(key)) {
      try {
        await addDoc(collection(db, 'Account_List'), {
          ownerUid: uid,
          accountName: acc.accountName,
          accountCode: String(acc.accountCode).trim(),
          createdAt: serverTimestamp(),
        });
        seenExistingKeys.add(key);
        newAddedCount++;
      } catch (err) {
        console.error(`[Account Sync] Failed to add official account ${acc.accountName}:`, err);
      }
    }
  }

  // Confirm both 4532404 entries
  const key1 = getAccountKey('M_Outsourcing processing expenses (Other general)', '4532404');
  const key2 = getAccountKey('M_Outsourcing processing expenses(Other-General)', '4532404');
  const preservedCode4532404 = officialKeyMap.has(key1) && officialKeyMap.has(key2);

  const report: AccountSyncReport = {
    totalCsvRows: OFFICIAL_ACCOUNTS.length,
    uniqueCombinations: officialKeyMap.size,
    retainedAccountsCount: retainedCount,
    newAccountsAddedCount: newAddedCount,
    unlistedAccountsRemovedCount: unlistedRemovedCount,
    duplicateAccountsRemovedCount: duplicateRemovedCount,
    preservedCode4532404,
    tasksPreserved: true,
    backupTimestamp,
  };

  try {
    localStorage.setItem(`account_sync_report_${uid}`, JSON.stringify(report));
  } catch (e) {
    // Ignore quota issues
  }

  console.info('[Account Sync Report]:', report);
  return report;
}

// Downloadable JSON helper for browser backups
export function triggerJsonDownload(data: any, filename: string): void {
  if (typeof window === 'undefined') return;
  try {
    const jsonStr = JSON.stringify(data, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  } catch (err) {
    console.error('Failed to trigger JSON download:', err);
  }
}

export interface StatusSyncReport {
  totalOfficial: number;
  retainedStatusesCount: number;
  newStatusesAddedCount: number;
  unlistedStatusesRemovedCount: number;
  duplicateStatusesRemovedCount: number;
  backupTimestamp: string;
  backupData?: any;
}

// Synchronize and clean Status_List for authenticated user against the 27 official statuses
export async function syncOfficialStatusList(
  uid: string,
  forceDownloadBackup: boolean = false
): Promise<StatusSyncReport> {
  const qStatus = query(
    collection(db, 'Status_List'),
    where('ownerUid', '==', uid)
  );
  const statusSnap = await getDocs(qStatus);

  const officialNormSet = new Set(
    OFFICIAL_STATUSES.map((s) => s.toLowerCase().trim())
  );

  // 1. Safe Backup of existing user Status_List before performing any deletions
  const backupStatuses = statusSnap.docs.map((d) => ({
    id: d.id,
    ownerUid: d.data().ownerUid,
    statusName: d.data().statusName || '',
    isDefault: !!d.data().isDefault,
    createdAt: d.data().createdAt ? d.data().createdAt.toString() : null,
  }));
  const backupTimestamp = new Date().toISOString();
  const backupObj = {
    backupType: 'Status_List_Backup',
    timestamp: backupTimestamp,
    ownerUid: uid,
    count: backupStatuses.length,
    statuses: backupStatuses,
  };

  try {
    if (typeof window !== 'undefined') {
      localStorage.setItem(`status_list_backup_${uid}`, JSON.stringify(backupObj));
    }
  } catch (e) {
    console.warn('[Status Backup] LocalStorage write error:', e);
  }

  // 2. Identify unlisted and duplicate records
  const seenExistingNames = new Set<string>();
  const toDeleteDocIds: string[] = [];
  let retainedCount = 0;
  let unlistedRemovedCount = 0;
  let duplicateRemovedCount = 0;

  for (const docSnap of statusSnap.docs) {
    const data = docSnap.data();
    const name = (data.statusName || '').trim();
    const norm = name.toLowerCase();

    if (!officialNormSet.has(norm)) {
      // Unlisted / old status -> queue for deletion
      toDeleteDocIds.push(docSnap.id);
      unlistedRemovedCount++;
    } else if (seenExistingNames.has(norm)) {
      // Duplicate of already seen official status -> queue for deletion
      toDeleteDocIds.push(docSnap.id);
      duplicateRemovedCount++;
    } else {
      seenExistingNames.add(norm);
      retainedCount++;
    }
  }

  // If unlisted statuses exist or force download requested, create downloadable JSON backup
  if ((unlistedRemovedCount > 0 || forceDownloadBackup) && typeof window !== 'undefined') {
    triggerJsonDownload(backupObj, `status_list_backup_${uid}_${Date.now()}.json`);
  }

  // 3. Delete unlisted and duplicate records
  for (const docId of toDeleteDocIds) {
    await deleteDoc(doc(db, 'Status_List', docId));
  }

  // 4. Add missing official statuses
  let newAddedCount = 0;
  for (const st of OFFICIAL_STATUSES) {
    const norm = st.toLowerCase().trim();
    if (!seenExistingNames.has(norm)) {
      await addDoc(collection(db, 'Status_List'), {
        ownerUid: uid,
        statusName: st,
        isDefault: true,
        createdAt: serverTimestamp(),
      });
      newAddedCount++;
    }
  }

  const report: StatusSyncReport = {
    totalOfficial: OFFICIAL_STATUSES.length,
    retainedStatusesCount: retainedCount,
    newStatusesAddedCount: newAddedCount,
    unlistedStatusesRemovedCount: unlistedRemovedCount,
    duplicateStatusesRemovedCount: duplicateRemovedCount,
    backupTimestamp,
    backupData: backupObj,
  };

  try {
    if (typeof window !== 'undefined') {
      localStorage.setItem(`status_sync_report_${uid}`, JSON.stringify(report));
    }
  } catch (e) {
    // Ignore quota issues
  }

  console.info('[Status Sync Report]:', report);
  return report;
}

// Initialize workspace for users with default accounts, statuses & sample tasks
export async function initializeUserWorkspace(uid: string): Promise<AccountSyncReport | undefined> {
  try {
    // 1. Perform Phase 1 Official Master List synchronization & cleanup
    const syncReport = await syncOfficialAccountList(uid);

    // 2. Perform Feature 1 Official Status List synchronization & cleanup (27 official statuses)
    await syncOfficialStatusList(uid);

    const qAcc = query(
      collection(db, 'Account_List'),
      where('ownerUid', '==', uid)
    );
    const accSnap = await getDocs(qAcc);
    const isNewUser = accSnap.empty;

    // Check and seed missing default categories (only on initial seed so intentional deletions are respected)
    const seedFlagKey = `category_defaults_seeded_${uid}`;
    const alreadySeeded = typeof window !== 'undefined' && localStorage.getItem(seedFlagKey) === 'true';

    if (!alreadySeeded) {
      const qCat = query(
        collection(db, 'Category_List'),
        where('ownerUid', '==', uid)
      );
      const catSnap = await getDocs(qCat);
      const existingCategories = new Set(
        catSnap.docs.map((d) => (d.data().categoryName || '').toLowerCase().trim())
      );

      for (const cat of DEFAULT_CATEGORIES) {
        if (!existingCategories.has(cat.toLowerCase().trim())) {
          await addDoc(collection(db, 'Category_List'), {
            ownerUid: uid,
            categoryName: cat,
            createdAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          });
        }
      }

      if (typeof window !== 'undefined') {
        try {
          localStorage.setItem(seedFlagKey, 'true');
        } catch (e) {
          console.warn('LocalStorage quota warning:', e);
        }
      }
    }

    if (isNewUser) {

      // Seed sample tasks for Nov-2026 matching Android template
      const sampleTasks = [
        {
          ownerUid: uid,
          taskNumber: 1,
          description: 'Civil Maintenance material...',
          amount: '13,000',
          paymentMode: 'Cash',
          unit: 'Unit-1',
          priority: 'High' as const,
          status: 'Draft Ready',
          expectedDate: '17 Nov 2026',
          accountName: 'Civil Maintenance',
          accountCode: 'ACC-CM101',
          remarks: 'Purchase orders submitted to procurement desk for bricks, cement and joint filler.',
          monthYear: 'Nov-2026',
        },
        {
          ownerUid: uid,
          taskNumber: 2,
          description: 'Air Conditioner maintenance...',
          amount: '125,500',
          paymentMode: 'Bank',
          unit: 'Unit-1',
          priority: 'Low' as const,
          status: 'Done',
          expectedDate: '10 Nov 2026',
          accountName: 'Air Conditioning',
          accountCode: 'ACC-AC205',
          remarks: 'Annual HVAC gas charging, coil cleaning, and thermostat inspection completed.',
          monthYear: 'Nov-2026',
        },
        {
          ownerUid: uid,
          taskNumber: 3,
          description: 'Raw Material transport logistics...',
          amount: '45,000',
          paymentMode: 'Bank',
          unit: 'Unit-1',
          priority: 'Med' as const,
          status: 'Advance Ready',
          expectedDate: '19 Nov 2026',
          accountName: 'Factory Logistics',
          accountCode: 'ACC-FL508',
          remarks: 'Truck freight advance for 3 containers arriving at Chittagong port depot.',
          monthYear: 'Nov-2026',
        },
        {
          ownerUid: uid,
          taskNumber: 4,
          description: 'Fire Extinguisher refill & inspection...',
          amount: '18,500',
          paymentMode: 'Cash',
          unit: 'Unit-2',
          priority: 'High' as const,
          status: 'Pending',
          expectedDate: '22 Nov 2026',
          accountName: 'Safety & Security',
          accountCode: 'ACC-SS602',
          remarks: 'Mandatory safety compliance testing for CO2 and foam cylinders in Unit-2.',
          monthYear: 'Nov-2026',
        },
        {
          ownerUid: uid,
          taskNumber: 5,
          description: 'Packaging cartons bundle delivery...',
          amount: '34,200',
          paymentMode: 'Cash',
          unit: 'Unit-2',
          priority: 'Med' as const,
          status: 'Done',
          expectedDate: '08 Nov 2026',
          accountName: 'Commercial Seal',
          accountCode: 'ACC-CS309',
          remarks: '1,000 corrugated 5-ply cartons received and stacked in warehouse.',
          monthYear: 'Nov-2026',
        },
      ];

      for (const t of sampleTasks) {
        await addTask(t);
      }
    }
    return syncReport;
  } catch (err) {
    console.error('Error initializing user workspace:', err);
    return undefined;
  }
}
