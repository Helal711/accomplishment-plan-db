package com.helal.accomplishmentplan.nativeapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.helal.accomplishmentplan.nativeapp.domain.logic.MonthUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userMonthDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "accomplishment_plan_user_month_prefs"
)

/**
 * UID-scoped selected month and custom month persistence backed by Jetpack DataStore.
 * Canonical selected month is stored as YYYY-MM and exposed as Mmm-YYYY (or real current month
 * when no preference has been saved for this UID).
 */
class UserMonthPreferencesStore(
    private val dataStore: DataStore<Preferences>,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {

    constructor(context: Context) : this(context.applicationContext.userMonthDataStore)

    fun observeSelectedMonthDisplay(ownerUid: String): Flow<String> {
        require(ownerUid.isNotBlank()) { "ownerUid must not be blank." }
        val key = selectedMonthKey(ownerUid)
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { prefs ->
                val stored = prefs[key]
                val parsed = MonthUtils.parseStorageKey(stored)
                    ?: MonthUtils.parseDisplayLabel(stored)
                parsed?.toDisplayLabel()
                    ?: stored?.takeIf { it.isNotBlank() }
                    ?: MonthUtils.currentDisplayMonth(clockMillis())
            }
    }

    suspend fun saveSelectedMonth(ownerUid: String, monthInput: String) {
        require(ownerUid.isNotBlank()) { "ownerUid must not be blank." }
        val trimmed = monthInput.trim()
        require(trimmed.isNotEmpty()) { "Selected month must not be blank." }
        val valueToStore = MonthUtils.toStorageKeyOrNull(trimmed) ?: trimmed
        dataStore.edit { prefs ->
            prefs[selectedMonthKey(ownerUid)] = valueToStore
        }
    }

    fun observeCustomMonths(ownerUid: String): Flow<List<String>> {
        require(ownerUid.isNotBlank()) { "ownerUid must not be blank." }
        val key = customMonthsKey(ownerUid)
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { prefs ->
                MonthUtils.sortMonthLabelsDescending(prefs[key].orEmpty())
            }
    }

    suspend fun addCustomMonth(ownerUid: String, monthLabel: String) {
        require(ownerUid.isNotBlank()) { "ownerUid must not be blank." }
        val normalized = MonthUtils.toDisplayLabelOrRaw(monthLabel)
        require(normalized.isNotEmpty()) { "Month label must not be blank." }
        dataStore.edit { prefs ->
            val key = customMonthsKey(ownerUid)
            val updated = prefs[key].orEmpty().toMutableSet().apply { add(normalized) }
            prefs[key] = updated
        }
    }

    suspend fun resetSelectedMonthToCurrent(ownerUid: String) {
        require(ownerUid.isNotBlank()) { "ownerUid must not be blank." }
        val currentKey = MonthUtils.currentYearMonth(clockMillis()).toStorageKey()
        dataStore.edit { prefs ->
            prefs[selectedMonthKey(ownerUid)] = currentKey
        }
    }

    private fun selectedMonthKey(ownerUid: String) =
        stringPreferencesKey("selected_month_${ownerUid.trim()}")

    private fun customMonthsKey(ownerUid: String) =
        stringSetPreferencesKey("custom_months_${ownerUid.trim()}")
}
