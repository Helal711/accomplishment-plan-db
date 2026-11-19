package com.helal.accomplishmentplan.nativeapp.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class UserMonthPreferencesStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `persists selected month and custom months isolated per ownerUid and defaults to current month`() =
        runTest {
            val fixedClock = Calendar.getInstance(TimeZone.getDefault()).apply {
                set(2026, Calendar.NOVEMBER, 18, 10, 0, 0)
            }.timeInMillis

            val dataStoreScope = TestScope(UnconfinedTestDispatcher(testScheduler))
            val dataStore = PreferenceDataStoreFactory.create(
                scope = dataStoreScope,
                produceFile = { tempFolder.newFile("uid_month_prefs.preferences_pb") }
            )
            val store = UserMonthPreferencesStore(
                dataStore = dataStore,
                clockMillis = { fixedClock }
            )

            // Unset users default to real current month (not hardcoded)
            assertEquals("Nov-2026", store.observeSelectedMonthDisplay("user_a").first())
            assertEquals("Nov-2026", store.observeSelectedMonthDisplay("user_b").first())

            // Save User A's month and custom months
            store.saveSelectedMonth("user_a", "Feb-2027")
            store.addCustomMonth("user_a", "Mar-2027")
            store.addCustomMonth("user_a", "Jan-2027")

            // User A reads persisted value; User B remains completely isolated
            assertEquals("Feb-2027", store.observeSelectedMonthDisplay("user_a").first())
            assertEquals("Nov-2026", store.observeSelectedMonthDisplay("user_b").first())
            assertEquals(listOf("Mar-2027", "Jan-2027"), store.observeCustomMonths("user_a").first())
            assertEquals(emptyList<String>(), store.observeCustomMonths("user_b").first())

            // Reset User A back to current month
            store.resetSelectedMonthToCurrent("user_a")
            assertEquals("Nov-2026", store.observeSelectedMonthDisplay("user_a").first())
        }
}
