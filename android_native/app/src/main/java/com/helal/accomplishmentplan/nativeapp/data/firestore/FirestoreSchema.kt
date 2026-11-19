package com.helal.accomplishmentplan.nativeapp.data.firestore

/**
 * Exact Firestore collection names, field keys, and security-rule length limits
 * matching firestore.rules and the React Web App (src/firebase/db.ts).
 */
object FirestoreSchema {

    const val COLLECTION_TASKS = "Tasks"
    const val COLLECTION_ACCOUNTS = "Account_List"
    const val COLLECTION_STATUSES = "Status_List"
    const val COLLECTION_CATEGORIES = "Category_List"
    const val COLLECTION_MONTH_COUNTERS = "Month_Counters"

    // Common fields
    const val FIELD_OWNER_UID = "ownerUid"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_UPDATED_AT = "updatedAt"

    // Tasks fields
    const val FIELD_TASK_NUMBER = "taskNumber"
    const val FIELD_DESCRIPTION = "description"
    const val FIELD_AMOUNT = "amount"
    const val FIELD_PAYMENT_MODE = "paymentMode"
    const val FIELD_UNIT = "unit"
    const val FIELD_PRIORITY = "priority"
    const val FIELD_STATUS = "status"
    const val FIELD_EXPECTED_DATE = "expectedDate"
    const val FIELD_ACCOUNT_NAME = "accountName"
    const val FIELD_ACCOUNT_CODE = "accountCode"
    const val FIELD_CATEGORY = "category"
    const val FIELD_REMARKS = "remarks"
    const val FIELD_MONTH_YEAR = "monthYear"
    const val FIELD_SUPP1_UPLOADED = "supp1Uploaded"
    const val FIELD_SUPP1_FILE_URL = "supp1FileUrl"
    const val FIELD_SUPP1_FILE_NAME = "supp1FileName"
    const val FIELD_SUPP2_UPLOADED = "supp2Uploaded"
    const val FIELD_SUPP2_FILE_URL = "supp2FileUrl"
    const val FIELD_SUPP2_FILE_NAME = "supp2FileName"
    const val FIELD_SUPP3_UPLOADED = "supp3Uploaded"
    const val FIELD_SUPP3_FILE_URL = "supp3FileUrl"
    const val FIELD_SUPP3_FILE_NAME = "supp3FileName"

    // Status_List fields
    const val FIELD_STATUS_NAME = "statusName"
    const val FIELD_IS_DEFAULT = "isDefault"

    // Category_List fields
    const val FIELD_CATEGORY_NAME = "categoryName"

    // Month_Counters fields
    const val FIELD_LAST_TASK_NUMBER = "lastTaskNumber"

    // Limits enforced by firestore.rules
    const val MAX_DESCRIPTION_LEN = 1000
    const val MAX_AMOUNT_LEN = 100
    const val MAX_REMARKS_LEN = 5000
    const val MAX_MONTH_YEAR_LEN = 50
    const val MAX_ACCOUNT_NAME_LEN = 200
    const val MAX_ACCOUNT_CODE_LEN = 50
    const val MAX_STATUS_NAME_LEN = 100
    const val MAX_CATEGORY_NAME_LEN = 100
    const val MAX_SUPP_FILE_NAME_LEN = 255
}
