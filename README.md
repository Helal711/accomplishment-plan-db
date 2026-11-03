# Accomplishment Plan DB (Web & Android)

A complete cross-platform enterprise accomplishment plan and task tracking application with real-time Firebase Cloud Firestore synchronization and secure multi-user data isolation.

- **Developer:** Md Al Helal Sarkar
- **Live Web App:** [https://helal711.github.io/accomplishment-plan-db/](https://helal711.github.io/accomplishment-plan-db/)
- **Repository:** [https://github.com/Helal711/accomplishment-plan-db](https://github.com/Helal711/accomplishment-plan-db)
- **Firebase Project:** `accomplishment-plan-db` (Spark Plan, free tier, zero billing required)

---

## Architecture Overview

1. **Web Application (React + Vite + TypeScript + Tailwind CSS)**:
   - Root workspace (`src/`, `public/`, `index.html`, `vite.config.ts`).
   - Base path configured for GitHub Pages: `base: '/accomplishment-plan-db/'`.
   - CI/CD workflow: `.github/workflows/deploy-web.yml` (and `.github/workflows/main.yml`).

2. **Native Android Application (Kotlin + Jetpack Compose + Material 3)**:
   - Located in `android_src/AccomplishmentPlanDB/`.
   - Built with Android SDK 36, Gradle 9.3 Kotlin DSL, and MVVM + StateFlow.
   - CI/CD workflow: `.github/workflows/android-apk.yml` builds debug APK on GitHub Actions and uploads it as an artifact.

3. **Cloud Database & Security (Firebase Spark Plan)**:
   - Firestore collections: `Tasks`, `Account_List`, `Category_List`, `Dynamic_Statuses`.
   - Scoped strictly to authenticated users via `ownerUid`.

---

## Seven Issues Resolved Across Web and Android

### Issue 1: Correct Task Serial Numbers
- **Problem**: Serial numbers were tied to document task numbers or restarted unpredictably upon task deletion/filtering.
- **Solution**: Recomputed serial numbers dynamically as presentation numbers (`1., 2., 3., 4., ...`) across all views.
  - **Web**: `DashboardPage.tsx` passes `index + 1` to `TaskCard` via the `serialNumber` prop.
  - **Android**: `TaskListScreen.kt` uses `itemsIndexed` in `LazyColumn` to pass `displayIndex = index + 1` to `TaskCard.kt`.

### Issue 2: Show All Status Counts in Summary Bar
- **Problem**: Only showed `Total Items` and `Pending`.
- **Solution**: Summary bar dynamically reads all available statuses (Pending, In Progress, Done, Approved, Hold, Cancelled, etc.) from `Dynamic_Statuses` / `statusList` and aggregates counts for the active month filter.
  - Horizontally scrollable and responsive on mobile screens.

### Issue 3: Fix Developer Footer Position
- **Problem**: The footer moved with scrolling content.
- **Solution**: Permanently pinned the footer (`Developer by Md Al Helal Sarkar`) to the bottom of the screen.
  - **Web**: Positioned with `fixed bottom-0 left-0 right-0 z-30` with `pb-28` safe padding on `<main>` and floating action button positioned safely above it.
  - **Android**: Positioned in fixed bottom surface below the scrollable `LazyColumn`.

### Issue 4: Change Amount Input Placeholder
- **Problem**: Displayed `13,000` placeholder.
- **Solution**: Updated placeholder to `00,000` in both Web (`TaskFormPage.tsx`) and Android (`UpdateTaskScreen.kt`). Real values and validation remain untouched.

### Issue 5: Change Default Account Name
- **Problem**: Default account was set to `M_Salaries wages(Basic salary)` / `Civil Maintenance`.
- **Solution**: Changed default to `M_Supply expenses(Other)` (Account Code: `4532154`).
  - Added safe existence validation before saving tasks; existing saved tasks keep their original account names.

### Issue 6: Fix PDF Export
- **Problem**: Web PDF export triggered `window.print()` which printed the modal dialog and buttons rather than a clean report.
- **Solution**: Implemented an isolated HTML print container (`handleExportPDF`) that generates a clean corporate tabular report with company header, month filter, columns (SL, Date, Account Name, Code, Description, Amount TK, Status, Remarks), and total summary without any UI dialog buttons or modals.
  - Formatted text and CSV exports also updated to use sequential numbering.

### Issue 7: Fix Firestore Permission Error
- **Problem**: Users encountered `Missing or insufficient permissions` when adding tasks or subscribing to categories.
- **Solution**: 
  - Added Firestore security rules matching `/Category_List/{categoryId}` with `ownerUid == request.auth.uid`.
  - Added `category` string validation to `isValidTask`.
  - Allowed numeric `taskNumber`.
  - Allowed owner-preserving partial updates `(!('ownerUid' in incoming()) || incoming().ownerUid == existing().ownerUid)`.

---

## Building and Deployment

### 1. Web App Deployment to GitHub Pages
1. Push changes to `main` branch.
2. The GitHub Actions workflow (`deploy-web.yml`) runs `npm run build` and deploys the `dist/` directory to GitHub Pages.
3. Accessible at `https://helal711.github.io/accomplishment-plan-db/`.

### 2. Android APK Build via GitHub Actions
1. Navigate to your repository's **Actions** tab on GitHub.
2. Select the **Build Android APK** workflow (`.github/workflows/android-apk.yml`).
3. Click **Run workflow** (or push to `main`).
4. Once completed, download the **`Accomplishment-Plan-DB-Debug-APK`** artifact zip containing the installable `.apk` file.

### 3. Deploying Firestore Security Rules
To deploy the updated `firestore.rules` to your Firebase project `accomplishment-plan-db`:
```bash
firebase deploy --only firestore:rules --project accomplishment-plan-db
```
*(Rules are also included in the repository root and `android_src/AccomplishmentPlanDB/firestore.rules`).*

---

## Pre-Packaged Downloads
- `AccomplishmentPlanDB-Web.zip`: Full source for Web application.
- `AccomplishmentPlanDB-Android.zip`: Full source for native Android application.
