package com.helal.accomplishmentplan.nativeapp.domain.logic

import java.util.Calendar
import java.util.Locale

data class YearMonthValue(
    val year: Int,
    val month: Int // 1..12
) : Comparable<YearMonthValue> {

    init {
        require(month in 1..12) { "Month must be between 1 and 12, got $month" }
        require(year in 2000..2100) { "Year must be between 2000 and 2100, got $year" }
    }

    /** Canonical DataStore persistence format: YYYY-MM (e.g., 2026-11). */
    fun toStorageKey(): String = String.format(Locale.US, "%04d-%02d", year, month)

    /** Firestore & UI display format: Mmm-YYYY (e.g., Nov-2026). */
    fun toDisplayLabel(): String = "${MonthUtils.SHORT_MONTH_NAMES[month - 1]}-$year"

    override fun compareTo(other: YearMonthValue): Int {
        val yearCmp = year.compareTo(other.year)
        return if (yearCmp != 0) yearCmp else month.compareTo(other.month)
    }
}

object MonthUtils {

    val SHORT_MONTH_NAMES: List<String> = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    private val STORAGE_REGEX = Regex("^(\\d{4})-(\\d{2})$")
    private val DISPLAY_REGEX = Regex("^([A-Za-z]{3})-(\\d{4})$")

    fun currentYearMonth(nowMillis: Long = System.currentTimeMillis()): YearMonthValue {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return YearMonthValue(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1
        )
    }

    fun currentDisplayMonth(nowMillis: Long = System.currentTimeMillis()): String {
        return currentYearMonth(nowMillis).toDisplayLabel()
    }

    fun parseStorageKey(storageKey: String?): YearMonthValue? {
        if (storageKey.isNullOrBlank()) return null
        val match = STORAGE_REGEX.matchEntire(storageKey.trim()) ?: return null
        val year = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        if (month !in 1..12 || year !in 2000..2100) return null
        return YearMonthValue(year, month)
    }

    fun parseDisplayLabel(displayLabel: String?): YearMonthValue? {
        if (displayLabel.isNullOrBlank()) return null
        val match = DISPLAY_REGEX.matchEntire(displayLabel.trim()) ?: return null
        val monthIdx = SHORT_MONTH_NAMES.indexOfFirst {
            it.equals(match.groupValues[1], ignoreCase = true)
        }
        val year = match.groupValues[2].toIntOrNull() ?: return null
        if (monthIdx < 0 || year !in 2000..2100) return null
        return YearMonthValue(year, monthIdx + 1)
    }

    fun toStorageKeyOrNull(raw: String?): String? {
        return (parseStorageKey(raw) ?: parseDisplayLabel(raw))?.toStorageKey()
    }

    fun toDisplayLabelOrRaw(raw: String): String {
        val parsed = parseStorageKey(raw) ?: parseDisplayLabel(raw)
        return parsed?.toDisplayLabel() ?: raw.trim()
    }

    fun monthsForYear(year: Int): List<YearMonthValue> {
        return (1..12).map { month -> YearMonthValue(year, month) }
    }

    /** Deterministic Month_Counters document ID matching Web App & firestore.rules: {uid}_{monthYear}. */
    fun counterDocumentId(ownerUid: String, monthYear: String): String {
        require(ownerUid.isNotBlank()) { "ownerUid must not be blank" }
        val normalizedMonth = toDisplayLabelOrRaw(monthYear)
        require(normalizedMonth.isNotBlank()) { "monthYear must not be blank" }
        return "${ownerUid.trim()}_$normalizedMonth"
    }

    fun sortMonthLabelsDescending(months: Collection<String>): List<String> {
        return months
            .map { toDisplayLabelOrRaw(it) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedWith(
                compareByDescending<String> { parseDisplayLabel(it) != null }
                    .thenComparator { a, b ->
                        val pa = parseDisplayLabel(a)
                        val pb = parseDisplayLabel(b)
                        when {
                            pa != null && pb != null -> pb.compareTo(pa)
                            else -> a.compareTo(b, ignoreCase = true)
                        }
                    }
            )
    }
}
