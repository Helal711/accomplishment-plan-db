package com.helal.accomplishmentplan.nativeapp.domain.logic

import com.helal.accomplishmentplan.nativeapp.domain.model.TaskItem
import java.text.Normalizer
import java.util.Locale

data class StatusCountSummary(
    val statusName: String,
    val count: Int
)

/**
 * Multi-field Unicode/Bengali-safe task search and combined status filter matching
 * the Accomplishment Plan DB Web App (src/utils/taskFilter.ts).
 */
object TaskFilterEngine {

    fun filterTasks(
        tasks: List<TaskItem>,
        searchQuery: String,
        selectedStatus: String?
    ): List<TaskItem> {
        val normalizedStatus = selectedStatus?.trim()?.takeIf {
            it.isNotEmpty() && !it.equals("ALL", ignoreCase = true)
        }
        val queryTokens = tokenizeQuery(searchQuery)

        return tasks.filter { task ->
            val matchesStatus = normalizedStatus == null ||
                normalizeText(task.status) == normalizeText(normalizedStatus)
            if (!matchesStatus) return@filter false
            if (queryTokens.isEmpty()) return@filter true

            val haystack = buildSearchHaystack(task)
            queryTokens.all { token -> haystack.contains(token) }
        }
    }

    /**
     * Computes status counts across all available statuses plus any status present on tasks
     * (preserving zero-count statuses so dynamic zero-count pill styling works cleanly).
     */
    fun computeStatusSummaries(
        tasks: List<TaskItem>,
        configuredStatuses: List<String>
    ): List<StatusCountSummary> {
        val countsByNormalized = mutableMapOf<String, Int>()
        val canonicalNames = linkedMapOf<String, String>()

        configuredStatuses.forEach { status ->
            val trimmed = status.trim()
            if (trimmed.isNotEmpty()) {
                val key = normalizeText(trimmed)
                canonicalNames.putIfAbsent(key, trimmed)
                countsByNormalized.putIfAbsent(key, 0)
            }
        }

        tasks.forEach { task ->
            val trimmed = task.status.trim()
            if (trimmed.isNotEmpty()) {
                val key = normalizeText(trimmed)
                canonicalNames.putIfAbsent(key, trimmed)
                countsByNormalized[key] = (countsByNormalized[key] ?: 0) + 1
            }
        }

        return canonicalNames.map { (key, displayName) ->
            StatusCountSummary(
                statusName = displayName,
                count = countsByNormalized[key] ?: 0
            )
        }
    }

    internal fun buildSearchHaystack(task: TaskItem): String {
        val fields = mutableListOf(
            task.description,
            task.paymentMode,
            task.accountName,
            task.accountCode,
            task.status,
            task.category,
            task.unit,
            task.priority.label,
            task.expectedDate,
            task.remarks,
            task.monthYear,
            task.taskNumber.toString(),
            "#${task.taskNumber}",
            task.supp1FileName,
            task.supp2FileName,
            task.supp3FileName
        )
        fields.addAll(AmountParser.searchTokens(task.amount))
        return normalizeText(fields.filter { it.isNotBlank() }.joinToString(" "))
    }

    internal fun tokenizeQuery(rawQuery: String): List<String> {
        val normalized = normalizeText(AmountParser.normalizeBengaliDigits(rawQuery))
        if (normalized.isEmpty()) return emptyList()
        return normalized.split(Regex("\\s+")).filter { it.isNotEmpty() }
    }

    internal fun normalizeText(input: String): String {
        val nfc = Normalizer.normalize(input, Normalizer.Form.NFC)
        return nfc.trim().lowercase(Locale.ROOT)
    }
}
