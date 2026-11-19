package com.helal.accomplishmentplan.nativeapp.core.session

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionCleanupCoordinatorTest {

    @Test
    fun `cleanupAll invokes every registered hook once and clears registry`() {
        val coordinator = SessionCleanupCoordinator()
        val invoked = mutableListOf<String>()

        coordinator.register("tasks") { invoked += "tasks" }
        coordinator.register("accounts") { invoked += "accounts" }
        coordinator.register("failing") { error("listener already detached") }
        assertEquals(3, coordinator.activeRegistrationCount())

        coordinator.cleanupAll()

        assertEquals(0, coordinator.activeRegistrationCount())
        assertEquals(setOf("tasks", "accounts"), invoked.toSet())
    }
}
