# Accomplishment Plan DB - Android Application

Accomplishment Plan DB is a native Android application built with Kotlin and Jetpack Compose. It allows users to track, plan, and manage daily accomplishment tasks, master accounts, categories, and workflow statuses with real-time Cloud Firestore synchronization and secure multi-user data isolation.

**Developer:** Md Al Helal Sarkar

---

## Features

- **Multi-User Authentication**: Firebase Email/Password authentication and Google Sign-In with Jetpack Credential Manager.
- **Strict Data Isolation**: Every task, account, and status record is scoped to the authenticated user's `ownerUid`. Users can only access and modify their own records.
- **Task Management**:
  - Full CRUD operations: Create, Read, Update, Delete tasks.
  - Automated account code lookup from Master Account List.
  - Priority management (High, Medium, Low) with visual indicators.
  - Workflow status tracking (Pending, In Progress, Completed, Approved, etc.).
- **Filtering & Search**:
  - Month/Year selector filter.
  - Status filter.
  - Real-time text search querying description, account name, account code, and remarks.
- **Responsive Layout**:
  - Designed with Material Design 3.
  - Full edge-to-edge system insets handling (`navigationBarsPadding`, `statusBarsPadding`).
  - Scrollable forms with accessible action buttons on all Android screen sizes and densities.
- **Spark Plan Compatible**: Operates completely within Firebase free tier limits (Firebase Auth & Cloud Firestore). No paid backend or storage buckets required.

---

## Tech Stack & Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines & StateFlow
- **Backend**: Firebase Cloud Firestore & Firebase Authentication
- **Local Unit Testing**: JUnit4 & Robolectric
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)

---

## Project Structure

```
.
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/           # Firestore TaskRepository
│   │   │   │   ├── model/          # TaskItem, AccountItem, StatusItem
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/ # TaskCard, Dialogs
│   │   │   │   │   ├── screens/    # AuthScreen, TaskListScreen, UpdateTaskScreen
│   │   │   │   │   └── theme/      # Color, Theme, Typography
│   │   │   │   ├── util/           # DateUtils, ExportUtils
│   │   │   │   ├── viewmodel/      # TaskViewModel
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/                # XML layouts, mipmaps, strings
│   │   │   └── AndroidManifest.xml
│   │   └── test/                   # Local JVM & Robolectric unit tests
│   ├── build.gradle.kts
│   ├── google-services.json
│   └── proguard-rules.pro
├── firestore.rules                 # Security rules for Firestore
├── build.gradle.kts                # Root build file
├── settings.gradle.kts             # Settings and repositories
└── README.md
```

---

## Getting Started

### Prerequisites

- Android Studio Jellyfish / Koala (or newer)
- Android SDK 36 (Minimum SDK 24)
- JDK 17 (or JDK 11+)
- A Firebase project with Cloud Firestore and Authentication enabled

### Building the Project

1. Clone or extract the repository.
2. Open the project in Android Studio, or build from terminal.
3. **Debug Keystore Setup** (since keystores are never committed to version control):
   Copy your local Android debug keystore into the project root:
   ```bash
   cp ~/.android/debug.keystore ./debug.keystore
   ```
   Or generate a fresh debug keystore:
   ```bash
   keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
   ```
4. Run the debug build:
   ```bash
   ./gradlew assembleDebug
   ```
   Or run the unit and Robolectric tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```

### Release Signing

For release builds (APK/AAB), set the following environment variables:
- `KEYSTORE_PATH`: Absolute path to your release `.jks` or `.keystore` file.
- `STORE_PASSWORD`: Keystore password.
- `KEY_PASSWORD`: Key password.

Then build the release bundle:
```bash
./gradlew bundleRelease
```

### Deploying Firestore Security Rules

Deploy the included `firestore.rules` using the Firebase CLI:
```bash
firebase deploy --only firestore:rules
```

---

## Security & Secrets

- **Keystore & Signing Credentials**: Excluded via `.gitignore`. For release builds, supply `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` via environment variables.
- **Firebase Client Configuration (`google-services.json`)**: Contains public project identifiers and API keys used by the Android client to identify the Firebase project. Access control and data isolation are strictly enforced server-side by Firebase Authentication and Firestore Security Rules.
