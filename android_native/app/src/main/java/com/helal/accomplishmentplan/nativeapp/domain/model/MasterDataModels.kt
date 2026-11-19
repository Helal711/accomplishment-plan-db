package com.helal.accomplishmentplan.nativeapp.domain.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class AccountItem(
    val id: String = "",
    val ownerUid: String = "",
    val accountName: String = "",
    val accountCode: String = "",
    val createdAtMillis: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class StatusItem(
    val id: String = "",
    val ownerUid: String = "",
    val statusName: String = "",
    val isDefault: Boolean = false,
    val createdAtMillis: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class CategoryItem(
    val id: String = "",
    val ownerUid: String = "",
    val categoryName: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class MonthCounter(
    val id: String = "",
    val ownerUid: String = "",
    val monthYear: String = "",
    val lastTaskNumber: Int = 0,
    val updatedAtMillis: Long = 0L
)
