package com.helal.accomplishmentplan.nativeapp.domain.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.helal.accomplishmentplan.nativeapp.domain.logic.AmountParser

enum class Priority(val label: String) {
    HIGH("High"),
    MED("Med"),
    LOW("Low");

    companion object {
        fun fromRaw(raw: String?): Priority {
            return when (raw?.trim()?.lowercase()) {
                "high", "h" -> HIGH
                "low", "l" -> LOW
                else -> MED
            }
        }
    }
}

@Keep
@IgnoreExtraProperties
data class TaskItem(
    val id: String = "",
    val ownerUid: String = "",
    val taskNumber: Int = 0,
    val description: String = "",
    val amount: String = "",
    val paymentMode: String = DefaultWorkspaceData.DEFAULT_PAYMENTS.first(),
    val unit: String = DefaultWorkspaceData.DEFAULT_UNITS.first(),
    val priority: Priority = Priority.MED,
    val status: String = DefaultWorkspaceData.DEFAULT_STATUSES.first(),
    val expectedDate: String = "",
    val accountName: String = "",
    val accountCode: String = "",
    val category: String = "",
    val remarks: String = "",
    val monthYear: String = "",
    val supp1Uploaded: Boolean = false,
    val supp1FileUrl: String = "",
    val supp1FileName: String = "",
    val supp2Uploaded: Boolean = false,
    val supp2FileUrl: String = "",
    val supp2FileName: String = "",
    val supp3Uploaded: Boolean = false,
    val supp3FileUrl: String = "",
    val supp3FileName: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    val numericAmount: Double
        get() = AmountParser.parseToDouble(amount) ?: 0.0

    val formattedAmount: String
        get() = AmountParser.formatForDisplay(amount)

    val uploadedQuotationCount: Int
        get() = listOf(supp1Uploaded, supp2Uploaded, supp3Uploaded).count { it }
}
