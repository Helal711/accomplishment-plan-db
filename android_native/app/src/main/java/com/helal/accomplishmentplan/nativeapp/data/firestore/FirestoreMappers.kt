package com.helal.accomplishmentplan.nativeapp.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.helal.accomplishmentplan.nativeapp.domain.logic.AmountParser
import com.helal.accomplishmentplan.nativeapp.domain.logic.MonthUtils
import com.helal.accomplishmentplan.nativeapp.domain.logic.TaskNumberingEngine
import com.helal.accomplishmentplan.nativeapp.domain.model.AccountItem
import com.helal.accomplishmentplan.nativeapp.domain.model.CategoryItem
import com.helal.accomplishmentplan.nativeapp.domain.model.DefaultWorkspaceData
import com.helal.accomplishmentplan.nativeapp.domain.model.MonthCounter
import com.helal.accomplishmentplan.nativeapp.domain.model.Priority
import com.helal.accomplishmentplan.nativeapp.domain.model.StatusItem
import com.helal.accomplishmentplan.nativeapp.domain.model.TaskItem
import java.util.Date

/**
 * Pure, null-safe Firestore document <-> domain model mappers.
 * - Enforces ownerUid ownership defensively on both reads and writes
 * - Safely deserializes String, Long, Int, and Double amounts
 * - Enforces firestore.rules string length bounds
 */
object FirestoreMappers {

    fun taskFromMap(
        docId: String,
        data: Map<String, Any?>,
        expectedOwnerUid: String
    ): TaskItem? {
        val docOwner = (data[FirestoreSchema.FIELD_OWNER_UID] as? String).orEmpty().trim()
        if (docOwner.isEmpty() || docOwner != expectedOwnerUid.trim()) {
            return null
        }

        val taskNumber = TaskNumberingEngine.sanitizeTaskNumber(data[FirestoreSchema.FIELD_TASK_NUMBER]) ?: 0
        val amountStr = AmountParser.rawToStorageString(data[FirestoreSchema.FIELD_AMOUNT])
        val rawMonth = (data[FirestoreSchema.FIELD_MONTH_YEAR] as? String).orEmpty()

        return TaskItem(
            id = docId,
            ownerUid = docOwner,
            taskNumber = taskNumber,
            description = (data[FirestoreSchema.FIELD_DESCRIPTION] as? String).orEmpty(),
            amount = amountStr,
            paymentMode = (data[FirestoreSchema.FIELD_PAYMENT_MODE] as? String)
                ?.takeIf { it.isNotBlank() } ?: DefaultWorkspaceData.DEFAULT_PAYMENTS.first(),
            unit = (data[FirestoreSchema.FIELD_UNIT] as? String)
                ?.takeIf { it.isNotBlank() } ?: DefaultWorkspaceData.DEFAULT_UNITS.first(),
            priority = Priority.fromRaw(data[FirestoreSchema.FIELD_PRIORITY] as? String),
            status = (data[FirestoreSchema.FIELD_STATUS] as? String)
                ?.takeIf { it.isNotBlank() } ?: DefaultWorkspaceData.DEFAULT_STATUSES.first(),
            expectedDate = (data[FirestoreSchema.FIELD_EXPECTED_DATE] as? String).orEmpty(),
            accountName = (data[FirestoreSchema.FIELD_ACCOUNT_NAME] as? String).orEmpty(),
            accountCode = (data[FirestoreSchema.FIELD_ACCOUNT_CODE] as? String).orEmpty(),
            category = (data[FirestoreSchema.FIELD_CATEGORY] as? String).orEmpty(),
            remarks = (data[FirestoreSchema.FIELD_REMARKS] as? String).orEmpty(),
            monthYear = if (rawMonth.isNotBlank()) MonthUtils.toDisplayLabelOrRaw(rawMonth) else "",
            supp1Uploaded = data[FirestoreSchema.FIELD_SUPP1_UPLOADED] as? Boolean ?: false,
            supp1FileUrl = (data[FirestoreSchema.FIELD_SUPP1_FILE_URL] as? String).orEmpty(),
            supp1FileName = (data[FirestoreSchema.FIELD_SUPP1_FILE_NAME] as? String).orEmpty(),
            supp2Uploaded = data[FirestoreSchema.FIELD_SUPP2_UPLOADED] as? Boolean ?: false,
            supp2FileUrl = (data[FirestoreSchema.FIELD_SUPP2_FILE_URL] as? String).orEmpty(),
            supp2FileName = (data[FirestoreSchema.FIELD_SUPP2_FILE_NAME] as? String).orEmpty(),
            supp3Uploaded = data[FirestoreSchema.FIELD_SUPP3_UPLOADED] as? Boolean ?: false,
            supp3FileUrl = (data[FirestoreSchema.FIELD_SUPP3_FILE_URL] as? String).orEmpty(),
            supp3FileName = (data[FirestoreSchema.FIELD_SUPP3_FILE_NAME] as? String).orEmpty(),
            createdAtMillis = extractMillis(data[FirestoreSchema.FIELD_CREATED_AT]),
            updatedAtMillis = extractMillis(data[FirestoreSchema.FIELD_UPDATED_AT])
        )
    }

    fun taskToCreateMap(
        ownerUid: String,
        task: TaskItem,
        assignedTaskNumber: Int,
        timestampValue: Any = FieldValue.serverTimestamp()
    ): Map<String, Any> {
        require(ownerUid.isNotBlank()) { "ownerUid is required for task creation." }
        require(assignedTaskNumber > 0) { "assignedTaskNumber must be positive." }

        val cleanDescription = task.description.trim().take(FirestoreSchema.MAX_DESCRIPTION_LEN)
        require(cleanDescription.isNotEmpty()) { "Task description must not be empty." }

        val normalizedMonth = MonthUtils.toDisplayLabelOrRaw(task.monthYear)
            .take(FirestoreSchema.MAX_MONTH_YEAR_LEN)
        require(normalizedMonth.isNotEmpty()) { "Task monthYear must not be empty." }

        return linkedMapOf(
            FirestoreSchema.FIELD_OWNER_UID to ownerUid.trim(),
            FirestoreSchema.FIELD_TASK_NUMBER to assignedTaskNumber,
            FirestoreSchema.FIELD_DESCRIPTION to cleanDescription,
            FirestoreSchema.FIELD_AMOUNT to AmountParser.rawToStorageString(task.amount)
                .take(FirestoreSchema.MAX_AMOUNT_LEN),
            FirestoreSchema.FIELD_PAYMENT_MODE to task.paymentMode.trim()
                .ifBlank { DefaultWorkspaceData.DEFAULT_PAYMENTS.first() },
            FirestoreSchema.FIELD_UNIT to task.unit.trim()
                .ifBlank { DefaultWorkspaceData.DEFAULT_UNITS.first() },
            FirestoreSchema.FIELD_PRIORITY to task.priority.label,
            FirestoreSchema.FIELD_STATUS to task.status.trim()
                .ifBlank { DefaultWorkspaceData.DEFAULT_STATUSES.first() }
                .take(FirestoreSchema.MAX_STATUS_NAME_LEN),
            FirestoreSchema.FIELD_EXPECTED_DATE to task.expectedDate.trim(),
            FirestoreSchema.FIELD_ACCOUNT_NAME to task.accountName.trim()
                .take(FirestoreSchema.MAX_ACCOUNT_NAME_LEN),
            FirestoreSchema.FIELD_ACCOUNT_CODE to task.accountCode.trim()
                .take(FirestoreSchema.MAX_ACCOUNT_CODE_LEN),
            FirestoreSchema.FIELD_CATEGORY to task.category.trim()
                .take(FirestoreSchema.MAX_CATEGORY_NAME_LEN),
            FirestoreSchema.FIELD_REMARKS to task.remarks.trim()
                .take(FirestoreSchema.MAX_REMARKS_LEN),
            FirestoreSchema.FIELD_MONTH_YEAR to normalizedMonth,
            FirestoreSchema.FIELD_SUPP1_UPLOADED to task.supp1Uploaded,
            FirestoreSchema.FIELD_SUPP1_FILE_URL to task.supp1FileUrl.trim(),
            FirestoreSchema.FIELD_SUPP1_FILE_NAME to task.supp1FileName.trim()
                .take(FirestoreSchema.MAX_SUPP_FILE_NAME_LEN),
            FirestoreSchema.FIELD_SUPP2_UPLOADED to task.supp2Uploaded,
            FirestoreSchema.FIELD_SUPP2_FILE_URL to task.supp2FileUrl.trim(),
            FirestoreSchema.FIELD_SUPP2_FILE_NAME to task.supp2FileName.trim()
                .take(FirestoreSchema.MAX_SUPP_FILE_NAME_LEN),
            FirestoreSchema.FIELD_SUPP3_UPLOADED to task.supp3Uploaded,
            FirestoreSchema.FIELD_SUPP3_FILE_URL to task.supp3FileUrl.trim(),
            FirestoreSchema.FIELD_SUPP3_FILE_NAME to task.supp3FileName.trim()
                .take(FirestoreSchema.MAX_SUPP_FILE_NAME_LEN),
            FirestoreSchema.FIELD_CREATED_AT to timestampValue,
            FirestoreSchema.FIELD_UPDATED_AT to timestampValue
        )
    }

    fun taskToUpdateMap(
        ownerUid: String,
        task: TaskItem,
        overrideTaskNumber: Int? = null,
        timestampValue: Any = FieldValue.serverTimestamp()
    ): Map<String, Any> {
        require(ownerUid.isNotBlank()) { "ownerUid is required for task update." }
        val cleanDescription = task.description.trim().take(FirestoreSchema.MAX_DESCRIPTION_LEN)
        require(cleanDescription.isNotEmpty()) { "Task description must not be empty." }

        val normalizedMonth = MonthUtils.toDisplayLabelOrRaw(task.monthYear)
            .take(FirestoreSchema.MAX_MONTH_YEAR_LEN)
        require(normalizedMonth.isNotEmpty()) { "Task monthYear must not be empty." }

        val map = linkedMapOf<String, Any>(
            FirestoreSchema.FIELD_OWNER_UID to ownerUid.trim(),
            FirestoreSchema.FIELD_DESCRIPTION to cleanDescription,
            FirestoreSchema.FIELD_AMOUNT to AmountParser.rawToStorageString(task.amount)
                .take(FirestoreSchema.MAX_AMOUNT_LEN),
            FirestoreSchema.FIELD_PAYMENT_MODE to task.paymentMode.trim()
                .ifBlank { DefaultWorkspaceData.DEFAULT_PAYMENTS.first() },
            FirestoreSchema.FIELD_UNIT to task.unit.trim()
                .ifBlank { DefaultWorkspaceData.DEFAULT_UNITS.first() },
            FirestoreSchema.FIELD_PRIORITY to task.priority.label,
            FirestoreSchema.FIELD_STATUS to task.status.trim()
                .ifBlank { DefaultWorkspaceData.DEFAULT_STATUSES.first() }
                .take(FirestoreSchema.MAX_STATUS_NAME_LEN),
            FirestoreSchema.FIELD_EXPECTED_DATE to task.expectedDate.trim(),
            FirestoreSchema.FIELD_ACCOUNT_NAME to task.accountName.trim()
                .take(FirestoreSchema.MAX_ACCOUNT_NAME_LEN),
            FirestoreSchema.FIELD_ACCOUNT_CODE to task.accountCode.trim()
                .take(FirestoreSchema.MAX_ACCOUNT_CODE_LEN),
            FirestoreSchema.FIELD_CATEGORY to task.category.trim()
                .take(FirestoreSchema.MAX_CATEGORY_NAME_LEN),
            FirestoreSchema.FIELD_REMARKS to task.remarks.trim()
                .take(FirestoreSchema.MAX_REMARKS_LEN),
            FirestoreSchema.FIELD_MONTH_YEAR to normalizedMonth,
            FirestoreSchema.FIELD_SUPP1_UPLOADED to task.supp1Uploaded,
            FirestoreSchema.FIELD_SUPP1_FILE_URL to task.supp1FileUrl.trim(),
            FirestoreSchema.FIELD_SUPP1_FILE_NAME to task.supp1FileName.trim()
                .take(FirestoreSchema.MAX_SUPP_FILE_NAME_LEN),
            FirestoreSchema.FIELD_SUPP2_UPLOADED to task.supp2Uploaded,
            FirestoreSchema.FIELD_SUPP2_FILE_URL to task.supp2FileUrl.trim(),
            FirestoreSchema.FIELD_SUPP2_FILE_NAME to task.supp2FileName.trim()
                .take(FirestoreSchema.MAX_SUPP_FILE_NAME_LEN),
            FirestoreSchema.FIELD_SUPP3_UPLOADED to task.supp3Uploaded,
            FirestoreSchema.FIELD_SUPP3_FILE_URL to task.supp3FileUrl.trim(),
            FirestoreSchema.FIELD_SUPP3_FILE_NAME to task.supp3FileName.trim()
                .take(FirestoreSchema.MAX_SUPP_FILE_NAME_LEN),
            FirestoreSchema.FIELD_UPDATED_AT to timestampValue
        )
        if (overrideTaskNumber != null && overrideTaskNumber > 0) {
            map[FirestoreSchema.FIELD_TASK_NUMBER] = overrideTaskNumber
        }
        return map
    }

    fun accountFromMap(
        docId: String,
        data: Map<String, Any?>,
        expectedOwnerUid: String
    ): AccountItem? {
        val docOwner = (data[FirestoreSchema.FIELD_OWNER_UID] as? String).orEmpty().trim()
        if (docOwner.isEmpty() || docOwner != expectedOwnerUid.trim()) return null

        val name = (data[FirestoreSchema.FIELD_ACCOUNT_NAME] as? String).orEmpty().trim()
        val code = (data[FirestoreSchema.FIELD_ACCOUNT_CODE] as? String).orEmpty().trim()
        if (name.isEmpty()) return null

        return AccountItem(
            id = docId,
            ownerUid = docOwner,
            accountName = name,
            accountCode = code,
            createdAtMillis = extractMillis(data[FirestoreSchema.FIELD_CREATED_AT])
        )
    }

    fun accountToMap(
        ownerUid: String,
        accountName: String,
        accountCode: String,
        timestampValue: Any = FieldValue.serverTimestamp()
    ): Map<String, Any> {
        require(ownerUid.isNotBlank()) { "ownerUid is required." }
        val cleanName = accountName.trim().take(FirestoreSchema.MAX_ACCOUNT_NAME_LEN)
        val cleanCode = accountCode.trim().take(FirestoreSchema.MAX_ACCOUNT_CODE_LEN)
        require(cleanName.isNotEmpty()) { "Account name is required." }
        require(cleanCode.isNotEmpty()) { "Account code is required." }

        return linkedMapOf(
            FirestoreSchema.FIELD_OWNER_UID to ownerUid.trim(),
            FirestoreSchema.FIELD_ACCOUNT_NAME to cleanName,
            FirestoreSchema.FIELD_ACCOUNT_CODE to cleanCode,
            FirestoreSchema.FIELD_CREATED_AT to timestampValue
        )
    }

    fun statusFromMap(
        docId: String,
        data: Map<String, Any?>,
        expectedOwnerUid: String
    ): StatusItem? {
        val docOwner = (data[FirestoreSchema.FIELD_OWNER_UID] as? String).orEmpty().trim()
        if (docOwner.isEmpty() || docOwner != expectedOwnerUid.trim()) return null

        val name = (data[FirestoreSchema.FIELD_STATUS_NAME] as? String).orEmpty().trim()
        if (name.isEmpty()) return null

        return StatusItem(
            id = docId,
            ownerUid = docOwner,
            statusName = name,
            isDefault = data[FirestoreSchema.FIELD_IS_DEFAULT] as? Boolean ?: false,
            createdAtMillis = extractMillis(data[FirestoreSchema.FIELD_CREATED_AT])
        )
    }

    fun statusToMap(
        ownerUid: String,
        statusName: String,
        isDefault: Boolean,
        timestampValue: Any = FieldValue.serverTimestamp()
    ): Map<String, Any> {
        require(ownerUid.isNotBlank()) { "ownerUid is required." }
        val cleanName = statusName.trim().take(FirestoreSchema.MAX_STATUS_NAME_LEN)
        require(cleanName.isNotEmpty()) { "Status name is required." }

        return linkedMapOf(
            FirestoreSchema.FIELD_OWNER_UID to ownerUid.trim(),
            FirestoreSchema.FIELD_STATUS_NAME to cleanName,
            FirestoreSchema.FIELD_IS_DEFAULT to isDefault,
            FirestoreSchema.FIELD_CREATED_AT to timestampValue
        )
    }

    fun categoryFromMap(
        docId: String,
        data: Map<String, Any?>,
        expectedOwnerUid: String
    ): CategoryItem? {
        val docOwner = (data[FirestoreSchema.FIELD_OWNER_UID] as? String).orEmpty().trim()
        if (docOwner.isEmpty() || docOwner != expectedOwnerUid.trim()) return null

        val name = (data[FirestoreSchema.FIELD_CATEGORY_NAME] as? String).orEmpty().trim()
        if (name.isEmpty()) return null

        return CategoryItem(
            id = docId,
            ownerUid = docOwner,
            categoryName = name,
            createdAtMillis = extractMillis(data[FirestoreSchema.FIELD_CREATED_AT]),
            updatedAtMillis = extractMillis(data[FirestoreSchema.FIELD_UPDATED_AT])
        )
    }

    fun categoryToCreateMap(
        ownerUid: String,
        categoryName: String,
        timestampValue: Any = FieldValue.serverTimestamp()
    ): Map<String, Any> {
        require(ownerUid.isNotBlank()) { "ownerUid is required." }
        val cleanName = categoryName.trim().take(FirestoreSchema.MAX_CATEGORY_NAME_LEN)
        require(cleanName.isNotEmpty()) { "Category name is required." }

        return linkedMapOf(
            FirestoreSchema.FIELD_OWNER_UID to ownerUid.trim(),
            FirestoreSchema.FIELD_CATEGORY_NAME to cleanName,
            FirestoreSchema.FIELD_CREATED_AT to timestampValue,
            FirestoreSchema.FIELD_UPDATED_AT to timestampValue
        )
    }

    fun monthCounterFromMap(
        docId: String,
        data: Map<String, Any?>,
        expectedOwnerUid: String
    ): MonthCounter? {
        val docOwner = (data[FirestoreSchema.FIELD_OWNER_UID] as? String).orEmpty().trim()
        if (docOwner.isEmpty() || docOwner != expectedOwnerUid.trim()) return null

        val lastNumber = when (val raw = data[FirestoreSchema.FIELD_LAST_TASK_NUMBER]) {
            is Number -> raw.toInt().coerceAtLeast(0)
            is String -> raw.toIntOrNull()?.coerceAtLeast(0) ?: 0
            else -> 0
        }
        return MonthCounter(
            id = docId,
            ownerUid = docOwner,
            monthYear = (data[FirestoreSchema.FIELD_MONTH_YEAR] as? String).orEmpty(),
            lastTaskNumber = lastNumber,
            updatedAtMillis = extractMillis(data[FirestoreSchema.FIELD_UPDATED_AT])
        )
    }

    fun monthCounterToMap(
        ownerUid: String,
        monthYear: String,
        lastTaskNumber: Int,
        timestampValue: Any = FieldValue.serverTimestamp()
    ): Map<String, Any> {
        require(ownerUid.isNotBlank()) { "ownerUid is required." }
        val cleanMonth = MonthUtils.toDisplayLabelOrRaw(monthYear)
            .take(FirestoreSchema.MAX_MONTH_YEAR_LEN)
        require(cleanMonth.isNotEmpty()) { "monthYear is required." }

        return linkedMapOf(
            FirestoreSchema.FIELD_OWNER_UID to ownerUid.trim(),
            FirestoreSchema.FIELD_MONTH_YEAR to cleanMonth,
            FirestoreSchema.FIELD_LAST_TASK_NUMBER to lastTaskNumber.coerceAtLeast(0),
            FirestoreSchema.FIELD_UPDATED_AT to timestampValue
        )
    }

    internal fun extractMillis(raw: Any?): Long {
        return when (raw) {
            is Timestamp -> raw.toDate().time
            is Date -> raw.time
            is Number -> raw.toLong()
            else -> 0L
        }
    }
}
