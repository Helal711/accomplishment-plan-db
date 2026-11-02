# Accomplishment Plan DB - React Web Application

Accomplishment Plan DB is a responsive web application built with React, TypeScript, Vite, and Tailwind CSS. Rewritten from the original Android application, it allows users to track, plan, and manage daily accomplishment tasks, master accounts, categories, and workflow statuses with real-time Cloud Firestore synchronization and secure multi-user data isolation.

**Developer:** Md Al Helal Sarkar

---

## Features

- **Multi-User Authentication**: Firebase Email/Password authentication and Google Sign-In with real-time authentication state listeners.
- **Strict Data Isolation**: Every task, master account, and custom status record is scoped to the authenticated user's `ownerUid`.
- **Task Management**:
  - Full CRUD operations: Create, Read, Update, Delete tasks with task numbers, descriptions, amounts (TK), payment modes, factory units, priorities, statuses, expected dates, and remarks.
  - Automated account code lookup and search modal from the Master Account List.
  - Priority management (High, Med, Low) with visual indicator chips.
  - Dynamic workflow status tracking with inline status change dropdowns.
- **Filtering & Search**:
  - Month/Year selector filter (Nov-2026, Dec-2026, Jan-2027, etc.) with custom month creation.
  - Real-time text search querying description, unit, status, account name, account code, remarks, and task number.
  - Task item counts (Total Items, Pending Tasks).
- **Master Data Management**:
  - Account Master Dialog: Search, add, edit, and delete corporate accounts with codes.
  - Category Manager: Add, edit, and manage category taxonomies.
  - Dynamic Status Manager: Add and remove custom workflow statuses.
- **Export & Reporting**:
  - CSV export for spreadsheet analysis.
  - JSON raw data export.
  - Print-friendly table preview for monthly reporting.
- **Responsive Layout**: Designed to mirror the corporate blue Android wireframe and layout seamlessly on mobile devices, tablets, and desktop browsers.

---

## Tech Stack & Architecture

- **UI Framework**: React 18, TypeScript, Tailwind CSS
- **Icons**: Lucide React
- **Routing**: React Router DOM (HashRouter)
- **Backend & Persistence**: Firebase Cloud Firestore & Firebase Authentication
- **Build Tool**: Vite & TypeScript Compiler (`tsc`)
- **Port**: 3000 (0.0.0.0)

---

## Getting Started

### Development
```bash
npm install
npm run dev
```
The development server will start on `http://0.0.0.0:3000`.

### Production Build & Linting
```bash
npm run build
npm run lint
```
The optimized web assets are output to `dist/`.

---

## License & Author

Developed by Md Al Helal Sarkar.
