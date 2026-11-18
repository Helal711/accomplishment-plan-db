# Accomplishment Plan DB (Android WebView App)

Native Kotlin Android WebView application wrapping the live Accomplishment Plan DB Web App (`https://helal711.github.io/accomplishment-plan-db/`).

- **Application ID:** `com.helal.accomplishmentplan.webview`
- **Min / Target / Compile SDK:** `24` / `36` / `36`

## Local Build & Tests

From the `android_webview/` directory:

```bash
./gradlew testDebugUnitTest assembleDebug
```

Debug APK output:
`android_webview/app/build/outputs/apk/debug/app-debug.apk`

For signed release builds:

```bash
KEYSTORE_PATH=/path/to/upload.jks \
STORE_PASSWORD=... \
KEY_ALIAS=upload \
KEY_PASSWORD=... \
./gradlew assembleRelease
```

## GitHub Actions APK Generation

Push changes under `android_webview/` to `main`, or trigger **Build Android WebView APK** manually from the **Actions** tab (`workflow_dispatch`) to download `Accomplishment-Plan-DB-WebView-Debug-APK`.
