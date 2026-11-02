package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class Priority(val label: String) {
    HIGH("High"),
    MED("Med"),
    LOW("Low");

    companion object {
        fun fromLabel(label: String): Priority {
            return entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: MED
        }
    }
}

@IgnoreExtraProperties
data class TaskItem(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val taskNumber: Int = 1,
    val description: String = "",
    val amount: String = "",
    val paymentMode: String = "Cash",
    val unit: String = "Unit-1",
    val priority: String = "High",
    val status: String = "Draft Ready",
    val expectedDate: String = "17 Nov 2026",
    val accountName: String = "Civil Maintenance",
    val accountCode: String = "ACC-CM101",
    val category: String = "",
    val remarks: String = "",
    val monthYear: String = "Nov-2026",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val priorityEnum: Priority
        get() = Priority.fromLabel(priority)

    val isDone: Boolean
        get() = status.equals("Done", ignoreCase = true)

    val formattedAmount: String
        get() = if (amount.endsWith("TK", ignoreCase = true)) amount else "$amount TK"

    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "ownerUid" to ownerUid,
            "taskNumber" to taskNumber,
            "description" to description,
            "amount" to amount,
            "paymentMode" to paymentMode,
            "unit" to unit,
            "priority" to priority,
            "status" to status,
            "expectedDate" to expectedDate,
            "accountName" to accountName,
            "accountCode" to accountCode,
            "category" to category,
            "remarks" to remarks,
            "monthYear" to monthYear
        )
        return map
    }
}

@IgnoreExtraProperties
data class AccountItem(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val accountName: String = "",
    val accountCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "ownerUid" to ownerUid,
        "accountName" to accountName,
        "accountCode" to accountCode
    )
}

@IgnoreExtraProperties
data class StatusItem(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val statusName: String = "",
    val isDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "ownerUid" to ownerUid,
        "statusName" to statusName,
        "isDefault" to isDefault
    )
}

@IgnoreExtraProperties
data class CategoryItem(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val categoryName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "ownerUid" to ownerUid,
        "categoryName" to categoryName
    )
}

