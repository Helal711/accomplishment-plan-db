export type Priority = 'High' | 'Med' | 'Low';

export interface TaskItem {
  id: string;
  ownerUid: string;
  taskNumber: number;
  description: string;
  amount: string;
  paymentMode: string;
  unit: string;
  priority: Priority;
  status: string;
  expectedDate: string;
  accountName: string;
  accountCode: string;
  category?: string;
  remarks: string;
  monthYear: string;
  createdAt?: any;
  updatedAt?: any;
}

export interface AccountItem {
  id: string;
  ownerUid: string;
  accountName: string;
  accountCode: string;
  createdAt?: any;
}

export interface CategoryItem {
  id: string;
  ownerUid: string;
  categoryName: string;
  createdAt?: any;
  updatedAt?: any;
}

export interface StatusItem {
  id: string;
  ownerUid: string;
  statusName: string;
  isDefault?: boolean;
  createdAt?: any;
}

export interface UserProfile {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
}

export const OFFICIAL_ACCOUNTS = [
  { accountName: "M_Salaries wages(Basic salary)", accountCode: "4521001" },
  { accountName: "M_Salaries wages(Allowances)", accountCode: "4521002" },
  { accountName: "M_Salaries wages(Korean)", accountCode: "4521003" },
  { accountName: "Salaries wages(Overtime)", accountCode: "4521004" },
  { accountName: "M_Bonus(Bonus & incentives)", accountCode: "4521051" },
  { accountName: "M_Miscellaneous salaries(Other)", accountCode: "4521101" },
  { accountName: "M_Commissions(TEST fee)", accountCode: "4532201" },
  { accountName: "M_Commissions(General Maintenance fee)", accountCode: "4532202" },
  { accountName: "M_Commissions(IT maintenance fee)", accountCode: "4532203" },
  { accountName: "M_Commissions(Financial fee)", accountCode: "4532204" },
  { accountName: "M_Commissions(Document issuance fee)", accountCode: "4532205" },
  { accountName: "M_Commissions(Audit fee)", accountCode: "4532209" },
  { accountName: "M_Commissions(Consulting fee)", accountCode: "4532211" },
  { accountName: "M_Commissions(COMMISSION)", accountCode: "4532212" },
  { accountName: "M_Commissions(Other)", accountCode: "4532213" },
  { accountName: "M_Supply expenses(Factory equipment)", accountCode: "4532151" },
  { accountName: "M_Supply expenses(Factory equipment Rent)", accountCode: "4532152" },
  { accountName: "M_Supply expenses(Computer supplies)", accountCode: "4532153" },
  { accountName: "M_Supply expenses(Other)", accountCode: "4532154" },
  { accountName: "M_Supply expenses(Local materials)", accountCode: "4532155" },
  { accountName: "M_Supply expenses(Machine Spare part)", accountCode: "4532156" },
  { accountName: "M_Supply expenses(PVC)", accountCode: "4532158" },
  { accountName: "M_Outsourcing processing expenses (Other general)", accountCode: "4532404" },
  { accountName: "M_Publication expenses(Other)", accountCode: "4532001" },
  { accountName: "M_Mold expenses(Other)", accountCode: "4532101" },
  { accountName: "M_Insurance premiums(Fire-Fixed asset)", accountCode: "4531752" },
  { accountName: "M_Insurance premiums(Fire-Inventory asset)", accountCode: "4531753" },
  { accountName: "M_Insurance premiums(Accident insurance)", accountCode: "4531754" },
  { accountName: "M_Insurance premiums(Automobile)", accountCode: "4531755" },
  { accountName: "M_Insurance premiums(Other)", accountCode: "4531756" },
  { accountName: "M_Travel expenses(Overseas-Air fare)", accountCode: "4531151" },
  { accountName: "M_Travel expenses(Overseas-Daily expenses)", accountCode: "4531152" },
  { accountName: "M_Travel expenses(Overseas-Hotel)", accountCode: "4531153" },
  { accountName: "M_Travel expenses(Overseas-Business trip expenses)", accountCode: "4531154" },
  { accountName: "M_Travel expenses(Domestic-Business trip expenses)", accountCode: "4531155" },
  { accountName: "M_Travel expenses(Out-of-office expenses)", accountCode: "4531156" },
  { accountName: "M_Travel expenses(Other)", accountCode: "4531157" },
  { accountName: "M_Utility expenses(Water)", accountCode: "4531401" },
  { accountName: "M_Utility expenses(Light)", accountCode: "4531402" },
  { accountName: "M_Utility expenses(Heating)", accountCode: "4531403" },
  { accountName: "M_Utility expenses(Other)", accountCode: "4531404" },
  { accountName: "M_Repair expenses(Other)", accountCode: "4531701" },
  { accountName: "M_Employee benefits(Overtime meal)", accountCode: "4531006" },
  { accountName: "M_Employee benefits(Dining together)", accountCode: "4531007" },
  { accountName: "M_Employee benefits(Meal)", accountCode: "4531008" },
  { accountName: "M_Employee benefits(Congratulations and condolences)", accountCode: "4531009" },
  { accountName: "M_Employee benefits(Other)", accountCode: "4531014" },
  { accountName: "M_Convention expenses(Other)", accountCode: "4531051" },
  { accountName: "M_Taxes and Dues(Property tax)", accountCode: "4531453" },
  { accountName: "M_Taxes and Dues(Automobile tax)", accountCode: "4531454" },
  { accountName: "M_Taxes and Dues(Other)", accountCode: "4531459" },
  { accountName: "M_Rental expenses(Maintenance fee)", accountCode: "4531601" },
  { accountName: "M_Rental expenses(Rent)", accountCode: "4531602" },
  { accountName: "M_Rental expenses(Land Rent)", accountCode: "4531603" },
  { accountName: "M_Vehicle fuel expenses(Fuel)", accountCode: "4531202" },
  { accountName: "M_Vehicle repair expenses(Repair)", accountCode: "4531252" },
  { accountName: "M_Vehicle maintenance expenses(Other)", accountCode: "4531301" },
  { accountName: "M_Vehicle maintenance expenses(Rent)", accountCode: "4531302" },
  { accountName: "M_Entertainment expenses(Other)", accountCode: "4531101" },
  { accountName: "M_Communication expenses(Telephone)", accountCode: "4531351" },
  { accountName: "M_Communication expenses(Cell phone)", accountCode: "4531352" },
  { accountName: "M_Communication expenses(Other)", accountCode: "4531353" },
  { accountName: "M_Training Expenses(Other)", accountCode: "4531951" },
  { accountName: "S_Salaries wages(Basic salary)", accountCode: "6111001" },
  { accountName: "S_Salaries wages(Allowance)", accountCode: "6111002" },
  { accountName: "S_Salaries wages(Korean)", accountCode: "6111003" },
  { accountName: "S_Salaries wages(Overtime)", accountCode: "6111004" },
  { accountName: "S_Bonus(Bonus & incentives)", accountCode: "6111051" },
  { accountName: "S_Miscellaneous salaries(Other)", accountCode: "6111101" },
  { accountName: "S_Commissions(TEST fee)", accountCode: "6112501" },
  { accountName: "S_Commissions(General Maintenance fee)", accountCode: "6112502" },
  { accountName: "S_Commissions(IT maintenance fee)", accountCode: "6112503" },
  { accountName: "S_Commissions(Financial fee)", accountCode: "6112504" },
  { accountName: "S_Commissions(Document issuance fee)", accountCode: "6112505" },
  { accountName: "S_Commissions(Audit fee)", accountCode: "6112509" },
  { accountName: "S_Commissions(Consulting fee)", accountCode: "6112513" },
  { accountName: "S_Commissions(COMMISSION)", accountCode: "6112514" },
  { accountName: "S_Commissions(Other)", accountCode: "6112515" },
  { accountName: "S_Supply expenses(Office equipment)", accountCode: "6112451" },
  { accountName: "S_Supply expenses(Office equipment Rent)", accountCode: "6112452" },
  { accountName: "S_Supply expenses(Computer supplies)", accountCode: "6112453" },
  { accountName: "S_Supply expenses(Other)", accountCode: "6112454" },
  { accountName: "S_Publication expenses(Other)", accountCode: "6112251" },
  { accountName: "S_Mold expenses(Other)", accountCode: "6112401" },
  { accountName: "S_Insurance premiums(Inland transport)", accountCode: "6111802" },
  { accountName: "S_Insurance premiums(Fire-Fixed asset)", accountCode: "6111803" },
  { accountName: "S_Insurance premiums(Fire-Inventory asset)", accountCode: "6111804" },
  { accountName: "S_Insurance premiums(Accident insurance)", accountCode: "6111805" },
  { accountName: "S_Insurance premiums(Automobile)", accountCode: "6111806" },
  { accountName: "S_Insurance premiums(Bonds)", accountCode: "6111807" },
  { accountName: "S_Insurance premiums(Other)", accountCode: "6111809" },
  { accountName: "S_Travel expenses(Overseas-Air fare)", accountCode: "6111301" },
  { accountName: "S_Travel expenses(Overseas-Daily expenses)", accountCode: "6111302" },
  { accountName: "S_Travel expenses(Overseas-Hotel)", accountCode: "6111303" },
  { accountName: "S_Travel expenses(Overseas-Business trip expenses)", accountCode: "6111304" },
  { accountName: "S_Travel expenses(Domestic-Business trip expenses)", accountCode: "6111305" },
  { accountName: "S_Travel expenses(Out-of-office expenses)", accountCode: "6111306" },
  { accountName: "S_Travel expenses(Other)", accountCode: "6111307" },
  { accountName: "S_Utility expenses(Other)", accountCode: "6112051" },
  { accountName: "S_Repair expenses(Other)", accountCode: "6111751" },
  { accountName: "S_Employee benefits(Overtime meal)", accountCode: "6111206" },
  { accountName: "S_Employee benefits(Dining together)", accountCode: "6111207" },
  { accountName: "S_Employee benefits(Meal)", accountCode: "6111208" },
  { accountName: "S_Employee benefits(Congratulations and condolences)", accountCode: "6111209" },
  { accountName: "S_Employee benefits(Other)", accountCode: "6111214" },
  { accountName: "S_Convention expenses(Other)", accountCode: "6112301" },
  { accountName: "S_Taxes and Dues(Property tax)", accountCode: "6111503" },
  { accountName: "S_Taxes and Dues(Automobile tax)", accountCode: "6111504" },
  { accountName: "S_Taxes and Dues(Other)", accountCode: "6111509" },
  { accountName: "S_Rental expenses(Maintenance fee)", accountCode: "6111701" },
  { accountName: "S_Rental expenses(Rent)", accountCode: "6111702" },
  { accountName: "S_Rental expenses(House Rent)", accountCode: "6111703" },
  { accountName: "S_Vehicle fuel expenses(Fuel)", accountCode: "6111901" },
  { accountName: "S_Vehicle repair expenses(Repair)", accountCode: "6111951" },
  { accountName: "S_Vehicle maintenance expenses(Rent)", accountCode: "6112001" },
  { accountName: "S_Vehicle maintenance expenses(Other)", accountCode: "6112002" },
  { accountName: "S_Entertainment expenses(Other)", accountCode: "6111401" },
  { accountName: "S_Communication expenses(Telephone)", accountCode: "6111451" },
  { accountName: "S_Communication expenses(Cell phone)", accountCode: "6111452" },
  { accountName: "S_Communication expenses(Other)", accountCode: "6111453" },
  { accountName: "S_Training Expenses(Other)", accountCode: "6112201" },
  { accountName: "OE_Donation (Other)", accountCode: "7411551" },
  { accountName: "M_Outsourcing processing expenses(Finished-Affiliated company)", accountCode: "4532401" },
  { accountName: "M_Outsourcing processing expenses(Finished-General)", accountCode: "4532402" },
  { accountName: "M_Outsourcing processing expenses(Other-Affiliated company)", accountCode: "4532403" },
  { accountName: "M_Outsourcing processing expenses(Other-General)", accountCode: "4532404" },
  { accountName: "M_Freight expenses(Other)", accountCode: "4531858" },
  { accountName: "M_Freight expenses(Import-Transportation-Domestic)", accountCode: "4531861" },
  { accountName: "M_Freight expenses(Import-Transportation-Sea)", accountCode: "4531862" },
  { accountName: "M_Freight expenses(Import-Transportation-Air)", accountCode: "4531863" },
  { accountName: "M_Freight expenses(Import)", accountCode: "4531864" },
  { accountName: "S_Freight expenses(Other)", accountCode: "6112106" },
  { accountName: "S_Freight expenses(Export-Transportation-Domestic)", accountCode: "6112108" },
  { accountName: "S_Freight expenses(Export-Transportation-Sea)", accountCode: "6112109" },
  { accountName: "S_Freight expenses(Export-Transportation-Air)", accountCode: "6112110" },
  { accountName: "S_Freight expenses(Export)", accountCode: "6112111" },
  { accountName: "S_Freight expenses(Domestic-Air)", accountCode: "6112112" },
  { accountName: "FE_Interest expenses(Other)", accountCode: "7211106" },
  { accountName: "Land", accountCode: "1200911" },
  { accountName: "Buildings", accountCode: "1200921" },
  { accountName: "Auxiliary Facilities", accountCode: "1200931" },
  { accountName: "Structure", accountCode: "1200941" },
  { accountName: "Machinery", accountCode: "1200951" },
  { accountName: "Vehicles", accountCode: "1200961" },
  { accountName: "Tools and instruments", accountCode: "1200971" },
  { accountName: "Office equipment", accountCode: "1200981" },
  { accountName: "Factory equipment", accountCode: "1200986" },
  { accountName: "Furniture and Fixture", accountCode: "1200990" },
  { accountName: "Computer& Accessories", accountCode: "1200994" },
  { accountName: "Construction in progress", accountCode: "1200999" },
  { accountName: "Prepaid Income Tax", accountCode: "1111714" },
  { accountName: "Short-term advance payments in N-Foreign Curr.(Other)", accountCode: "1111311" },
  { accountName: "Current Other Deposit", accountCode: "1111375" },
  { accountName: "Software", accountCode: "1201521" },
];

export const OFFICIAL_STATUSES = [
  'Draft Ready',
  'Advance Draft Ready',
  'Advance Ready',
  'Advance Done',
  'Payment Approval Ready',
  'Done',
  'CS Ready',
  'Approval in Audit',
  'Approval in Audit (HOD)',
  'CS in Accounts',
  'Approval in Accounts',
  'WO Done',
  'Waiting for CFO Sign',
  'Waiting for MD Sign',
  'Cancel from CFO',
  'Cancel from HOD',
  'Cancel',
  'Pending',
  'Settlement',
  'Hold from MR Team',
  'Hold from CFO',
  'Hold from MD',
  'Done from HOD',
  'Payment Pending',
  'Next Month',
  'No Need',
  'Unavailable',
] as const;

export const DEFAULT_STATUSES = [...OFFICIAL_STATUSES];

export const DEFAULT_UNITS = ['Unit-1', 'Unit-2', 'Unit-3'];

export const DEFAULT_PAYMENTS = ['Cash', 'Bank', 'Cheque'];

export const DEFAULT_MONTHS = ['Nov-2026', 'Dec-2026', 'Jan-2027', 'Oct-2026'];

export const DEFAULT_CATEGORIES = [
  'Furniture & fixture',
  'Compliance & fire safety',
  'Materials',
  'Service',
  'Food',
  'Medical',
  'Book Printing',
  'Accessories & others',
  'Auxiliary facilities',
  'Renovation',
  'Electrical',
  'Machinery',
  'Needle',
  'Sewing spare parts',
  'Cutting spare parts',
  'Hardware',
  'Printing Section',
  'LAB',
  'Additional',
];

export const DEFAULT_ACCOUNTS = OFFICIAL_ACCOUNTS;

export function getAccountKey(name: string, code: string): string {
  return `${name.trim().toLowerCase()}:::${code.trim()}`;
}

export const OFFICIAL_ACCOUNT_KEY_SET = new Set(
  OFFICIAL_ACCOUNTS.map((a) => getAccountKey(a.accountName, a.accountCode))
);

export function isOfficialAccount(name: string, code: string): boolean {
  return OFFICIAL_ACCOUNT_KEY_SET.has(getAccountKey(name, code));
}

export function getCodeForAccount(name: string, customAccounts: AccountItem[] = []): string {
  const custom = customAccounts.find((a) => a.accountName.toLowerCase().trim() === name.toLowerCase().trim());
  if (custom) return custom.accountCode;
  const def = OFFICIAL_ACCOUNTS.find((a) => a.accountName.toLowerCase().trim() === name.toLowerCase().trim());
  if (def) return def.accountCode;
  const prefix = name.replace(/[^a-zA-Z]/g, '').slice(0, 3).toUpperCase() || 'ACC';
  return `ACC-${prefix}001`;
}

