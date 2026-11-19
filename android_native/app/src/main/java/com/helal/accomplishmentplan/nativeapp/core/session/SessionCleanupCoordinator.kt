package com.helal.accomplishmentplan.nativeapp.core.session

import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates cleanup of all active user-scoped Firestore snapshot listeners and
 * coroutine jobs before FirebaseAuth.signOut() executes, preventing permission-denied
 * listener errors and cross-user data leaks.
 */
class SessionCleanupCoordinator {

    private val cleanupHooks = ConcurrentHashMap<String, () -> Unit>()

    fun register(key: String, cleanup: () -> Unit) {
        cleanupHooks[key] = cleanup
    }

    fun unregister(key: String) {
        cleanupHooks.remove(key)
    }

    fun cleanupAll() {
        val snapshot = cleanupHooks.entries.toList()
        cleanupHooks.clear()
        snapshot.forEach { (_, cleanup) ->
            runCatching { cleanup() }
        }
    }

    fun activeRegistrationCount(): Int = cleanupHooks.size
}
