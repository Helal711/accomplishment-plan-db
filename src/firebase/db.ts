import {
  collection,
  doc,
  getDoc,
  setDoc,
  addDoc,
  updateDoc,
  deleteDoc,
  query,
  where,
  onSnapshot,
  getDocs,
  serverTimestamp,
  writeBatch,
  runTransaction,
} from 'firebase/firestore';
import { db, auth } from './config';
import {
  TaskItem,
  AccountItem,
  StatusItem,
  CategoryItem,
  DEFAULT_ACCOUNTS,
  DEFAULT_STATUSES,
  DEFAULT_CATEGORIES,
} from '../types';

/**
 * Diagnostic logger that safely logs collection, operation, auth UID, and payload ownerUid.
 * Never logs credentials, tokens, or passwords.
 */
function logFirestoreOp(collectionName: string, operation: string, payloadOwnerUid?: string, docId?: string) {
  const currentAuthUid = auth.currentUser?.uid || 'UNAUTHENTICATED';
  console.log(
    `[Firestore Diagnostics] Collection: ${collectionName} | Operation: ${operation} | Auth UID: ${currentAuthUid} | Payload ownerUid: ${payloadOwnerUid || 'N/A'}${docId ? ` | Doc ID: ${docId}` : ''}`
  );
}

/**
 * Validates and sanitizes a taskNumber value.
 * Accepts only finite, positive integers (>= 1).
 * Rejects or returns null for null, undefined, empty strings, NaN, Infinity, negative numbers, 0, or non-integer values.
 * Does NOT silently convert invalid numbers into 1.
 */
export function sanitizeTaskNumber(raw: unknown): number | null {
  if (raw === null || raw === undefined) return null;
  let num: number;
  if (typeof raw === 'number') {
    num = raw;
  } else if (typeof raw === 'string') {
    const trimmed = raw.trim();
    if (!trimmed) return null;
    num = Number(trimmed);
  } else {
    return null;
  }
  if (!Number.isFinite(num) || !Number.isInteger(num) || num <= 0) {
    return null;
  }
  return num;
}

/**
 * Deterministically sorts tasks by taskNumber ASC, tie-breaker createdAt, tie-breaker id
 */
export function sortTasksDeterministically(tasks: TaskItem[]): TaskItem[] {
  return [...tasks].sort((a, b) => {
    const aNum = sanitizeTaskNumber(a.taskNumber) ?? 0;
    const bNum = sanitizeTaskNumber(b.taskNumber) ?? 0;
    if (aNum !== bNum) {
      return aNum - bNum;
    }
    const aTime = a.createdAt?.seconds || (typeof a.createdAt === 'number' ? a.createdAt : 0);
    const bTime = b.createdAt?.seconds || (typeof b.createdAt === 'number' ? b.createdAt : 0);
    if (aTime !== bTime) {
      return aTime - bTime;
    }
    return (a.id || '').localeCompare(b.id || '');
  });
}

// Subscribe to tasks for user and specific month
export function subscribeToTasks(
  uid: string,
  monthYear: string,
  onUpdate: (tasks: TaskItem[]) => void,
  onError?: (error: Error) => void
): () => void {
  if (!uid) {
    onUpdate([]);
    return () => {};
  }

  logFirestoreOp('Tasks', 'subscribe', uid);

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
        const rawTaskNum = data.taskNumber;
        // Strictly validate taskNumber. If invalid, preserve raw value or default safely without silently converting
        const validatedNum = sanitizeTaskNumber(rawTaskNum);
        const parsedTaskNumber = validatedNum ?? (typeof rawTaskNum === 'number' && Number.isFinite(rawTaskNum) ? Math.floor(rawTaskNum) : 0);

        return {
          id: docSnap.id,
          ownerUid: data.ownerUid || uid,
          taskNumber: parsedTaskNumber,
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

      const sorted = sortTasksDeterministically(items);
      onUpdate(sorted);
    },
    (err) => {
      console.error('[Firestore Diagnostics] Error subscribing to Tasks:', err);
      if (onError) onError(err);
    }
  );
}

// Subscribe to all accounts for user
export function subscribeToAccounts(
  uid: string,
  onUpdate: (accounts: AccountItem[]) => void
): () => void {
  if (!uid) {
    onUpdate([]);
    return () => {};
  }
  logFirestoreOp('Account_List', 'subscribe', uid);

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
    (err) => console.error('[Firestore Diagnostics] Error subscribing to accounts:', err)
  );
}

// Subscribe to all custom statuses for user
export function subscribeToStatuses(
  uid: string,
  onUpdate: (statuses: StatusItem[]) => void
): () => void {
  if (!uid) {
    onUpdate([]);
    return () => {};
  }
  logFirestoreOp('Status_List', 'subscribe', uid);

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
    (err) => console.error('[Firestore Diagnostics] Error subscribing to statuses:', err)
  );
}

// Subscribe to all categories for user
export function subscribeToCategories(
  uid: string,
  onUpdate: (categories: CategoryItem[]) => void
): () => void {
  if (!uid) {
    onUpdate([]);
    return () => {};
  }
  logFirestoreOp('Category_List', 'subscribe', uid);

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
    (err) => console.error('[Firestore Diagnostics] Error subscribing to categories:', err)
  );
}

/**
 * Get next sequential task number for a given user and month.
 * Reads Month_Counters first; falls back to querying Tasks if counter document is missing.
 * Propagates errors instead of silently returning 1.
 */
export async function getNextTaskNumber(uid: string, monthYear: string): Promise<number> {
  const counterId = `${uid}_${monthYear}`;
  const counterRef = doc(db, 'Month_Counters', counterId);
  const counterSnap = await getDoc(counterRef);

  if (counterSnap.exists()) {
    const lastNum = counterSnap.data().lastTaskNumber;
    if (typeof lastNum === 'number' && lastNum >= 0) {
      return lastNum + 1;
    }
  }

  // Fallback to checking existing tasks in database
  const q = query(
    collection(db, 'Tasks'),
    where('ownerUid', '==', uid),
    where('monthYear', '==', monthYear)
  );
  const snap = await getDocs(q);
  if (snap.empty) return 1;

  let maxNum = 0;
  snap.docs.forEach((d) => {
    const num = d.data().taskNumber;
    if (typeof num === 'number' && num > maxNum) {
      maxNum = num;
    }
  });

  return maxNum + 1;
}

/**
 * Add a new task with concurrency-safe sequential serial number using a Firestore transaction
 * coordinated with the shared Month_Counters counter document (Month_Counters/{uid_monthYear}).
 * Guarantees that concurrent deletions or additions allocate correct sequential numbers without stale gaps.
 */
export async function addTask(task: Omit<TaskItem, 'id'>): Promise<string> {
  const currentAuthUid = auth.currentUser?.uid;
  if (!currentAuthUid) {
    throw new Error('Authentication required: Firebase user is not signed in.');
  }

  logFirestoreOp('Tasks', 'create_transaction_start', currentAuthUid);

  const counterId = `${currentAuthUid}_${task.monthYear}`;
  const counterRef = doc(db, 'Month_Counters', counterId);

  // Step A: Inspect counter document state with diagnostic error capture
  let counterSnap;
  try {
    counterSnap = await getDoc(counterRef);
    console.log(`[Firestore Diagnostics] Month_Counters getDoc success. exists: ${counterSnap.exists()}`);
  } catch (err: any) {
    console.error(`[Firestore Diagnostics] Error reading Month_Counters/${counterId}:`, err);
    throw new Error(`[Month_Counters read permission error]: ${err.message || 'Permission denied'}`);
  }

  // If counter document does not exist yet (first task in month or uninitialized sheet),
  // bootstrap baseline from existing tasks once before entering the transaction.
  let bootstrapBaseline: number | null = null;
  if (!counterSnap.exists()) {
    try {
      const q = query(
        collection(db, 'Tasks'),
        where('ownerUid', '==', currentAuthUid),
        where('monthYear', '==', task.monthYear)
      );
      const existingSnap = await getDocs(q);
      let maxFound = 0;
      existingSnap.docs.forEach((d) => {
        const sanitized = sanitizeTaskNumber(d.data().taskNumber);
        if (sanitized !== null && sanitized > maxFound) maxFound = sanitized;
      });
      bootstrapBaseline = maxFound;
      console.log(`[Firestore Diagnostics] Tasks query bootstrap success. maxFound: ${maxFound}`);
    } catch (err: any) {
      console.error(`[Firestore Diagnostics] Error querying Tasks for ownerUid: ${currentAuthUid}:`, err);
      throw new Error(`[Tasks query permission error]: ${err.message || 'Permission denied'}`);
    }
  }

  const newTaskRef = doc(collection(db, 'Tasks'));
  let allocatedTaskNumber = 1;

  // Run atomic transaction. Month_Counters acts as the single source of truth and concurrency lock.
  try {
    await runTransaction(db, async (transaction) => {
      const freshCounterSnap = await transaction.get(counterRef);
      let counterLast = 0;

      if (freshCounterSnap.exists()) {
        const raw = freshCounterSnap.data().lastTaskNumber;
        const sanitized = sanitizeTaskNumber(raw);
        counterLast = sanitized ?? (typeof raw === 'number' && Number.isFinite(raw) && raw >= 0 ? Math.floor(raw) : 0);
      } else if (bootstrapBaseline !== null) {
        counterLast = bootstrapBaseline;
      }

      allocatedTaskNumber = counterLast + 1;

      // 1. Create task document inside transaction
      transaction.set(newTaskRef, {
        ...task,
        ownerUid: currentAuthUid,
        taskNumber: allocatedTaskNumber,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });

      // 2. Increment / initialize counter document inside transaction atomically
      transaction.set(counterRef, {
        ownerUid: currentAuthUid,
        monthYear: task.monthYear,
        lastTaskNumber: allocatedTaskNumber,
        updatedAt: serverTimestamp(),
      }, { merge: true });
    });
  } catch (err: any) {
    console.error(`[Firestore Diagnostics] Error executing addTask transaction:`, err);
    throw new Error(`[Transaction save error]: ${err.message || 'Permission denied'}`);
  }

  logFirestoreOp('Tasks', 'create_transaction_success', currentAuthUid, newTaskRef.id);
  return newTaskRef.id;
}

// Update an existing task preserving serial number and ownerUid
export async function updateTask(
  taskId: string,
  updates: Partial<TaskItem>
): Promise<void> {
  const currentAuthUid = auth.currentUser?.uid;
  if (!currentAuthUid) {
    throw new Error('Authentication required: Firebase user is not signed in.');
  }

  logFirestoreOp('Tasks', 'update', currentAuthUid, taskId);

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { id, ownerUid, taskNumber, ...dataToUpdate } = updates;
  const docRef = doc(db, 'Tasks', taskId);

  await updateDoc(docRef, {
    ...dataToUpdate,
    ownerUid: currentAuthUid,
    updatedAt: serverTimestamp(),
  });
}

/**
 * Delete task, sequentially renumber remaining tasks for that month without gaps,
 * and synchronize the Month_Counters counter document using a safe transaction.
 * Handles concurrency with simultaneous task creations using retry.
 */
export async function deleteTask(taskId: string, monthYear?: string, ownerUid?: string): Promise<void> {
  const currentAuthUid = auth.currentUser?.uid || ownerUid;
  if (!currentAuthUid) {
    throw new Error('Authentication required to delete task.');
  }

  logFirestoreOp('Tasks', 'delete', currentAuthUid, taskId);

  // Verify target task exists and belongs to the authenticated user before proceeding
  const targetDocRef = doc(db, 'Tasks', taskId);
  const targetSnap = await getDoc(targetDocRef);
  if (!targetSnap.exists()) {
    // Document already deleted or does not exist
    return;
  }

  const targetData = targetSnap.data();
  if (targetData.ownerUid && targetData.ownerUid !== currentAuthUid) {
    throw new Error('Permission denied: You do not own this task document.');
  }

  const targetMonth = monthYear || targetData.monthYear;
  if (!targetMonth) {
    throw new Error('Task monthYear is required for deletion and renumbering.');
  }

  const counterId = `${currentAuthUid}_${targetMonth}`;
  const counterRef = doc(db, 'Month_Counters', counterId);

  const maxAttempts = 3;
  let lastError: Error | null = null;

  // Retry loop in case concurrent addTask commits while fetching tasks
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const q = query(
      collection(db, 'Tasks'),
      where('ownerUid', '==', currentAuthUid),
      where('monthYear', '==', targetMonth)
    );
    const snap = await getDocs(q);

    const targetExists = snap.docs.some((d) => d.id === taskId);
    if (!targetExists) {
      // Document was already removed concurrently
      return;
    }

    const remainingDocs = snap.docs
      .filter((d) => d.id !== taskId)
      .map((d) => ({ id: d.id, ...d.data() } as TaskItem));

    const sortedRemaining = sortTasksDeterministically(remainingDocs);

    try {
      await runTransaction(db, async (transaction) => {
        // All reads must occur before any writes
        const counterSnap = await transaction.get(counterRef);
        if (counterSnap.exists()) {
          const raw = counterSnap.data().lastTaskNumber;
          const currentLast = sanitizeTaskNumber(raw) ?? (typeof raw === 'number' && Number.isFinite(raw) && raw >= 0 ? Math.floor(raw) : 0);
          // If counter has advanced past our query count, a concurrent addTask occurred
          if (currentLast > snap.docs.length) {
            throw new Error('CONCURRENCY_CONFLICT_RETRY');
          }
        }

        // 1. Delete target task inside transaction
        transaction.delete(targetDocRef);

        // 2. Renumber remaining tasks sequentially 1..N
        sortedRemaining.forEach((task, index) => {
          const correctNumber = index + 1;
          if (task.taskNumber !== correctNumber) {
            transaction.update(doc(db, 'Tasks', task.id), {
              taskNumber: correctNumber,
              updatedAt: serverTimestamp(),
            });
          }
        });

        // 3. Synchronize counter to exact remaining count
        const newLast = sortedRemaining.length;
        transaction.set(counterRef, {
          ownerUid: currentAuthUid,
          monthYear: targetMonth,
          lastTaskNumber: newLast,
          updatedAt: serverTimestamp(),
        }, { merge: true });
      });

      logFirestoreOp('Tasks', 'delete_and_renumber_success', currentAuthUid, taskId);
      return;
    } catch (err: any) {
      lastError = err instanceof Error ? err : new Error(String(err));
      if (err?.message === 'CONCURRENCY_CONFLICT_RETRY' && attempt < maxAttempts - 1) {
        console.warn(`[deleteTask] Concurrency conflict detected (attempt ${attempt + 1}/${maxAttempts}); re-fetching and retrying...`);
        continue;
      }
      throw err;
    }
  }

  // Exhaustion of retries must throw an error rather than silently succeeding
  throw new Error(`Failed to delete and renumber task after ${maxAttempts} attempts: ${lastError?.message || 'Transaction aborted'}`);
}

// Quick status change
export async function quickUpdateTaskStatus(
  taskId: string,
  newStatus: string
): Promise<void> {
  const currentAuthUid = auth.currentUser?.uid;
  if (!currentAuthUid) throw new Error('Authentication required.');

  logFirestoreOp('Tasks', 'quickUpdateStatus', currentAuthUid, taskId);
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
  logFirestoreOp('Account_List', 'create', uid);
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
  const currentAuthUid = auth.currentUser?.uid;
  logFirestoreOp('Account_List', 'update', currentAuthUid, accountId);
  const docRef = doc(db, 'Account_List', accountId);
  await updateDoc(docRef, updates);
}

export async function deleteAccount(accountId: string): Promise<void> {
  const currentAuthUid = auth.currentUser?.uid;
  logFirestoreOp('Account_List', 'delete', currentAuthUid, accountId);
  const docRef = doc(db, 'Account_List', accountId);
  await deleteDoc(docRef);
}

// Status CRUD
export async function addStatus(
  uid: string,
  statusName: string,
  isDefault: boolean = false
): Promise<string> {
  logFirestoreOp('Status_List', 'create', uid);
  const docRef = await addDoc(collection(db, 'Status_List'), {
    ownerUid: uid,
    statusName: statusName.trim(),
    isDefault,
    createdAt: serverTimestamp(),
  });
  return docRef.id;
}

export async function deleteStatus(statusId: string): Promise<void> {
  const currentAuthUid = auth.currentUser?.uid;
  logFirestoreOp('Status_List', 'delete', currentAuthUid, statusId);
  const docRef = doc(db, 'Status_List', statusId);
  await deleteDoc(docRef);
}

export async function addCategory(
  uid: string,
  categoryName: string
): Promise<string> {
  logFirestoreOp('Category_List', 'create', uid);
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
  const currentAuthUid = auth.currentUser?.uid;
  logFirestoreOp('Category_List', 'update', currentAuthUid, categoryId);
  const docRef = doc(db, 'Category_List', categoryId);
  await updateDoc(docRef, {
    categoryName: categoryName.trim(),
    updatedAt: serverTimestamp(),
  });
}

export async function deleteCategory(categoryId: string): Promise<void> {
  const currentAuthUid = auth.currentUser?.uid;
  logFirestoreOp('Category_List', 'delete', currentAuthUid, categoryId);
  const docRef = doc(db, 'Category_List', categoryId);
  await deleteDoc(docRef);
}

/**
 * Initialize workspace for newly registered user.
 * Uses atomic chunked writeBatch operations to eliminate slow sequential requests.
 */
export async function initializeUserWorkspace(uid: string): Promise<void> {
  if (!uid) return;
  logFirestoreOp('Workspace', 'initialize', uid);

  try {
    const qAcc = query(collection(db, 'Account_List'), where('ownerUid', '==', uid));
    const accSnap = await getDocs(qAcc);
    const isNewUser = accSnap.empty;

    const existingAccountNames = new Set(
      accSnap.docs.map((d) => (d.data().accountName || '').toLowerCase().trim())
    );

    // 1. Seed accounts in batches of 100
    const accountsToAdd = DEFAULT_ACCOUNTS.filter(
      (a) => !existingAccountNames.has(a.accountName.toLowerCase().trim())
    );

    if (accountsToAdd.length > 0) {
      const batchSize = 100;
      for (let i = 0; i < accountsToAdd.length; i += batchSize) {
        const chunk = accountsToAdd.slice(i, i + batchSize);
        const batch = writeBatch(db);
        chunk.forEach((acc) => {
          const newDocRef = doc(collection(db, 'Account_List'));
          batch.set(newDocRef, {
            ownerUid: uid,
            accountName: acc.accountName,
            accountCode: acc.accountCode,
            createdAt: serverTimestamp(),
          });
        });
        await batch.commit();
      }
    }

    // 2. Check and seed missing default statuses
    const qStatus = query(collection(db, 'Status_List'), where('ownerUid', '==', uid));
    const statusSnap = await getDocs(qStatus);
    const existingStatuses = new Set(
      statusSnap.docs.map((d) => (d.data().statusName || '').toLowerCase().trim())
    );

    const statusesToAdd = DEFAULT_STATUSES.filter(
      (st) => !existingStatuses.has(st.toLowerCase().trim())
    );

    if (statusesToAdd.length > 0) {
      const batch = writeBatch(db);
      statusesToAdd.forEach((st) => {
        const newDocRef = doc(collection(db, 'Status_List'));
        batch.set(newDocRef, {
          ownerUid: uid,
          statusName: st,
          isDefault: true,
          createdAt: serverTimestamp(),
        });
      });
      await batch.commit();
    }

    // 3. Check and seed missing default categories
    const qCat = query(collection(db, 'Category_List'), where('ownerUid', '==', uid));
    const catSnap = await getDocs(qCat);
    const existingCategories = new Set(
      catSnap.docs.map((d) => (d.data().categoryName || '').toLowerCase().trim())
    );

    const categoriesToAdd = DEFAULT_CATEGORIES.filter(
      (cat) => !existingCategories.has(cat.toLowerCase().trim())
    );

    if (categoriesToAdd.length > 0) {
      const batch = writeBatch(db);
      categoriesToAdd.forEach((cat) => {
        const newDocRef = doc(collection(db, 'Category_List'));
        batch.set(newDocRef, {
          ownerUid: uid,
          categoryName: cat,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
      });
      await batch.commit();
    }

    // 4. Seed initial sample tasks for Nov-2026 if this is a brand new user
    if (isNewUser) {
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

      const taskBatch = writeBatch(db);
      sampleTasks.forEach((t) => {
        const newDocRef = doc(collection(db, 'Tasks'));
        taskBatch.set(newDocRef, {
          ...t,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
      });

      // Initialize Month_Counters document for Nov-2026
      const counterId = `${uid}_Nov-2026`;
      const counterRef = doc(db, 'Month_Counters', counterId);
      taskBatch.set(counterRef, {
        ownerUid: uid,
        monthYear: 'Nov-2026',
        lastTaskNumber: 5,
        updatedAt: serverTimestamp(),
      });

      await taskBatch.commit();
    }

    logFirestoreOp('Workspace', 'initialize_success', uid);
  } catch (err) {
    console.error('[Firestore Diagnostics] Error initializing user workspace:', err);
  }
}
