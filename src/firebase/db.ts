import {
  collection,
  doc,
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
  DEFAULT_ACCOUNTS,
  DEFAULT_STATUSES,
  DEFAULT_CATEGORIES,
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

// Delete task
export async function deleteTask(taskId: string): Promise<void> {
  const docRef = doc(db, 'Tasks', taskId);
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

// Initialize workspace for users with default accounts, statuses & sample tasks
export async function initializeUserWorkspace(uid: string): Promise<void> {
  try {
    const qAcc = query(
      collection(db, 'Account_List'),
      where('ownerUid', '==', uid)
    );
    const accSnap = await getDocs(qAcc);
    const isNewUser = accSnap.empty;

    const existingNames = new Set(
      accSnap.docs.map((d) => (d.data().accountName || '').toLowerCase().trim())
    );

    // Seed missing predefined master accounts
    for (const acc of DEFAULT_ACCOUNTS) {
      if (!existingNames.has(acc.accountName.toLowerCase().trim())) {
        await addDoc(collection(db, 'Account_List'), {
          ownerUid: uid,
          accountName: acc.accountName,
          accountCode: acc.accountCode,
          createdAt: serverTimestamp(),
        });
      }
    }

    // Check and seed missing default statuses
    const qStatus = query(
      collection(db, 'Status_List'),
      where('ownerUid', '==', uid)
    );
    const statusSnap = await getDocs(qStatus);
    const existingStatuses = new Set(
      statusSnap.docs.map((d) => (d.data().statusName || '').toLowerCase().trim())
    );

    for (const st of DEFAULT_STATUSES) {
      if (!existingStatuses.has(st.toLowerCase().trim())) {
        await addDoc(collection(db, 'Status_List'), {
          ownerUid: uid,
          statusName: st,
          isDefault: true,
          createdAt: serverTimestamp(),
        });
      }
    }

    // Check and seed missing default categories
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
  } catch (err) {
    console.error('Error initializing user workspace:', err);
  }
}
