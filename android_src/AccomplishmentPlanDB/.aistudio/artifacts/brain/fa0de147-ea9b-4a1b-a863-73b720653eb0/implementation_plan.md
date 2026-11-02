# Multi-User SaaS Architecture & Security Specification

A comprehensive architectural blueprint to evolve the Accomplishment Plan DB application into a secure, multi-tenant SaaS platform. This specification guarantees complete data isolation across independent user accounts using Firebase Authentication, document-level `ownerUid` binding, zero-trust server-side Firestore Security Rules, and private Cloud Storage paths.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Data Isolation & Security Guarantee**: All data access is strictly gated behind Firebase Authentication. The application guarantees that no user—regardless of role, document ID manipulation, or API inspection—can query, mutate, or view another user's records. All queries must include an `ownerUid == auth.uid` filter, reinforced by server-side security rules.

- **Authentication Strategy**: Supports both **Google Sign-In** (via Jetpack Credential Manager) and **Email/Password** registration/login to accommodate new user sign-ups and institutional accounts.
- **Initial Workspace Provisioning**: When a user registers for the first time, a dedicated private workspace is initialized with standard master sheet defaults (default `Account_List` items such as Civil Maintenance, Factory Logistics, etc., and default `Status_List` items). Users can optionally click a "Load Sample Plan" option to populate the 17 template tasks or begin with a clean sheet.
- **Quotation Attachments**: Uploaded quotation files are routed directly to user-specific paths in Firebase Storage (`users/{uid}/quotations/{taskId}_{suppId}_{filename}`). Storage security rules verify that the uploading user matches the path `{uid}` parameter.
- **Zero Production Disruption**: Because no documents currently exist in production Firestore, no legacy data migration is required. Initial schema bootstrapping will deploy without risk of orphaned records.

---

## 1. Overview & Core Concept

- **What It Does**: Transforms the single-tenant accomplishment planning utility into a multi-user SaaS application. Every registered user receives a private, isolated workspace for tracking tasks, monthly completion metrics, accounts, statuses, and supplier quotation documents.
- **Target Audience / Persona**: Plant managers, project coordinators, and site engineers managing daily accomplishments, procurement workflows, and monthly status sheets across manufacturing units.
- **Key Value**: Provides enterprise-grade privacy and multi-tenancy without altering the reference UI layout. Cross-platform parity ensures identical security and data models across Android and future React/Vite web clients.

---

## 2. User Experience & Visual Design

The UI layout strictly adheres to the approved PDF specification. Navigation and screen designs are preserved:

### Key User Flows

1. **Authentication Gateway**:
   - When launching the app in an unauthenticated state, a clean, modal-style sign-in screen appears with the matching corporate blue header (`#1E88E5`).
   - Users can choose **"Sign in with Google"** or switch to **"Create Account / Email Login"**.
   - Upon successful authentication, the user is transitioned immediately into their personal Task List.
2. **Personal Workspace Navigation**:
   - **Task List Screen**: Displays only tasks where `ownerUid == currentUser.uid`. Total and Pending counters calculate dynamically from the user's real-time collection.
   - **User Profile / Logout**: Accessible via the gear icon or header action, displaying the current user's email/display name with a clean "Sign Out" option.
   - **Update Task Screen**: Displays and updates existing tasks owned by the user. Prevents unauthorized writes.
   - **Supplier Quotations**: Tapping `Supp 1`, `Supp 2`, or `Supp 3` launches the file picker to upload files directly to `users/{uid}/quotations/` with progress and Drive sync status.

### Visual Identity & Theme Tokens

- **Dominant Brand Blue**: `#1E88E5` (Header top bars and primary action buttons)
- **Background & Card Surfaces**: Pure White (`#FFFFFF`) with 1dp border (`#D0D7DE`)
- **Card Spacing & Layout**: Identical compact row structure:
  - Row 1: Task ID & Description (bold) + Priority Badge (🔴 High, 🟡 Med, 🟢 Low)
  - Row 2: Amount (TK) + Payment Method
  - Row 3: Factory Unit Chip (`[🏢 Unit-1]`) + Status Pill with dropdown arrow
- **Footer**: Preserved verbatim on all screens: `"Developer by Md Al Helal Sarkar"`

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Collection Architecture (Root Collections with `ownerUid` vs. Nested Subcollections)**
  - *Chosen Approach*: Root-level collections (`Tasks`, `Account_List`, `Status_List`) with mandatory `ownerUid` field on every document.
  - *Why*: Enables high-performance collection group queries, streamlined indexing, and full interoperability with React/Vite web clients. Matches requirement 4 & 5 verbatim.
  - *Alternatives Considered*: Nested subcollections (`/users/{uid}/tasks/...`) were evaluated; while secure, root-level collections with `ownerUid` are simpler to query across combined filter dimensions and provide easier batch reporting.
- **Decision 2: Server-Side Ownership Enforcement via Firestore Rules**
  - *Chosen Approach*: Zero-trust security rules checking `request.auth != null && request.auth.uid == resource.data.ownerUid` on `read`, `update`, `delete`, and `request.resource.data.ownerUid == request.auth.uid` on `create`.
  - *Why*: Eliminates any possibility of unauthorized horizontal privilege escalation, even if malicious actors tamper with network requests or document IDs.
- **Decision 3: Storage Isolation**
  - *Chosen Approach*: `users/{uid}/quotations/{filename}` with rules enforcing `request.auth.uid == uid`.
  - *Why*: Prevents cross-tenant file inspection and isolates storage bandwidth.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Client Applications (Cross-Platform)                  │
├──────────────────────────────────────┬──────────────────────────────────────┤
│          Android Client (Compose)    │         React / Vite Web Client      │
│  - CredentialManager & FirebaseAuth  │  - Firebase Web SDK Auth & Hooks     │
│  - TaskViewModel (StateFlow)         │  - React Query / Firestore Listeners │
│  - TaskRepository (ownerUid queries) │  - Storage Upload Client             │
└───────────────────┬──────────────────┴───────────────────┬──────────────────┘
                    │                                      │
                    ▼                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Firebase Backend (Shared Project)                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ 1. Firebase Authentication                                                  │
│    - Google Sign-In Provider & Email/Password Identity                      │
│    - Issues JWT containing auth.uid and identity tokens                     │
│                                                                             │
│ 2. Cloud Firestore (Database)                                               │
│    - Rules: Enforce auth != null && request.auth.uid == ownerUid            │
│    - Collection `Tasks`: User task cards with amount, status, quotations    │
│    - Collection `Account_List`: Custom master accounts & auto-fill codes    │
│    - Collection `Status_List`: Custom user status workflow definitions      │
│                                                                             │
│ 3. Firebase Cloud Storage                                                   │
│    - Path: users/{uid}/quotations/*                                         │
│    - Rules: match /users/{uid}/quotations/{allPaths=**} {                   │
│               allow read, write: if request.auth.uid == uid;                │
│             }                                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Firestore Schema Specification

#### Collection: `Tasks`
```json
{
  "id": "String (Auto-generated document ID)",
  "ownerUid": "String (Firebase Auth UID of the document owner)",
  "taskNumber": "Number (User-facing sequential ID e.g. 1, 2, 13)",
  "description": "String",
  "amount": "String",
  "paymentMode": "String (Cash | Bank | Cheque)",
  "unit": "String (Unit-1 | Unit-2)",
  "priority": "String (High | Med | Low)",
  "status": "String (Draft Ready | Advance Ready | Done | etc.)",
  "expectedDate": "String",
  "accountName": "String",
  "accountCode": "String",
  "remarks": "String",
  "monthYear": "String (e.g. Nov-2026)",
  "supp1Uploaded": "Boolean",
  "supp2Uploaded": "Boolean",
  "supp3Uploaded": "Boolean",
  "supp1FileUrl": "String (Optional Storage download URL)",
  "supp2FileUrl": "String (Optional Storage download URL)",
  "supp3FileUrl": "String (Optional Storage download URL)",
  "createdAt": "Timestamp (ServerTimestamp)",
  "updatedAt": "Timestamp (ServerTimestamp)"
}
```

#### Collection: `Account_List`
```json
{
  "id": "String",
  "ownerUid": "String",
  "accountName": "String",
  "accountCode": "String",
  "createdAt": "Timestamp"
}
```

#### Collection: `Status_List`
```json
{
  "id": "String",
  "ownerUid": "String",
  "statusName": "String",
  "isDefault": "Boolean",
  "createdAt": "Timestamp"
}
```

---

## 5. Security Rules Implementation

### `firestore.rules`
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(ownerUid) {
      return isAuthenticated() && request.auth.uid == ownerUid;
    }

    function isDocOwner() {
      return isAuthenticated() && request.auth.uid == resource.data.ownerUid;
    }

    function isCreateOwner() {
      return isAuthenticated() && request.auth.uid == request.resource.data.ownerUid;
    }

    // Tasks Collection
    match /Tasks/{taskId} {
      allow read: if isDocOwner();
      allow create: if isCreateOwner()
        && request.resource.data.description is string
        && request.resource.data.ownerUid == request.auth.uid;
      allow update: if isDocOwner()
        && request.resource.data.ownerUid == resource.data.ownerUid;
      allow delete: if isDocOwner();
    }

    // Account List Collection
    match /Account_List/{accountId} {
      allow read: if isDocOwner();
      allow create: if isCreateOwner()
        && request.resource.data.accountName is string
        && request.resource.data.ownerUid == request.auth.uid;
      allow update: if isDocOwner()
        && request.resource.data.ownerUid == resource.data.ownerUid;
      allow delete: if isDocOwner();
    }

    // Status List Collection
    match /Status_List/{statusId} {
      allow read: if isDocOwner();
      allow create: if isCreateOwner()
        && request.resource.data.statusName is string
        && request.resource.data.ownerUid == request.auth.uid;
      allow update: if isDocOwner()
        && request.resource.data.ownerUid == resource.data.ownerUid;
      allow delete: if isDocOwner();
    }

    // Default deny all other collections
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

### `storage.rules`
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /users/{uid}/quotations/{allPaths=**} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

---

## 6. Migration & Verification Strategy

1. **Existing Data Audit**:
   - Production database currently contains no user records.
   - Initial run requires zero migration of legacy documents.
2. **First-Run Workspace Seeding**:
   - When a user signs in for the first time, a setup routine initializes default accounts (Civil Maintenance, Air Conditioning, Commercial Seal) and statuses in `Account_List` and `Status_List` tagged with their `ownerUid`.
   - A single-tap prompt allows loading the 17 demonstration tasks into their private workspace so testing matches the PDF reference immediately.
3. **Automated Verification**:
   - Local unit and security rules integration testing using `@firebase/rules-unit-testing` and Robolectric repository rule tests.
   - Verification that querying without `whereEqualTo("ownerUid", uid)` is rejected by security rules and prohibited in application code.
