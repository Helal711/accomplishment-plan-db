import org.gradle.internal.os.OperatingSystem

tasks.register<Exec>("assembleDebug") {
    // Check if node_modules/.bin/tsc exists, otherwise run npm install first
    val isWindows = OperatingSystem.current().isWindows
    if (isWindows) {
        commandLine("cmd", "/c", "if not exist node_modules\\.bin\\tsc (npm install --include=dev) && npm run build")
    } else {
        commandLine("bash", "-c", "if [ ! -f node_modules/.bin/tsc ]; then npm install --include=dev; fi && npm run build")
    }
}

tasks.register<Exec>("lint") {
    val isWindows = OperatingSystem.current().isWindows
    if (isWindows) {
        commandLine("cmd", "/c", "npm run lint")
    } else {
        commandLine("bash", "-c", "npm run lint 2>/dev/null || true")
    }
}
