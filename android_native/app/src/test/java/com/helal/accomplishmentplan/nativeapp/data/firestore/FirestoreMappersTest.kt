package com.helal.accomplishmentplan.nativeapp.data.firestore

import com.helal.accomplishmentplan.nativeapp.domain.model.Priority
import com.helal.accomplishmentplan.nativeapp.domain.model.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreMappersTest {

    @Test
    fun `taskFromMap rejects documents belonging to another ownerUid`() {
        val data = mapOf<String, Any?>(
            FirestoreSchema.FIELD_OWNER_UID to "other_user",
            FirestoreSchema.FIELD_TASK_NUMBER to 1L,
            FirestoreSchema.FIELD_DESCRIPTION to "Confidential task"
        )
        assertNull(FirestoreMappers.taskFromMap("doc1", data, expectedOwnerUid = "current_user"))
    }

    @Test
    fun `taskFromMap safely handles Long, Double, and String amount values plus supplier quotations`() {
        val longAmountTask = FirestoreMappers.taskFromMap(
            docId = "doc1",
            data = mapOf(
                FirestoreSchema.FIELD_OWNER_UID to "user_1",
                FirestoreSchema.FIELD_TASK_NUMBER to 3L,
                FirestoreSchema.FIELD_DESCRIPTION to "Generator fuel",
                FirestoreSchema.FIELD_AMOUNT to 55000L,
                FirestoreSchema.FIELD_PRIORITY to "High",
                FirestoreSchema.FIELD_MONTH_YEAR to "2026-11",
                FirestoreSchema.FIELD_SUPP1_UPLOADED to true,
                FirestoreSchema.FIELD_SUPP1_FILE_NAME to "quote_abc.pdf",
                FirestoreSchema.FIELD_SUPP1_FILE_URL to "https://example.invalid/quote_abc.pdf"
            ),
            expectedOwnerUid = "user_1"
        )

        assertNotNull(longAmountTask)
        assertEquals("55000", longAmountTask!!.amount)
        assertEquals(Priority.HIGH, longAmountTask.priority)
        assertEquals("Nov-2026", longAmountTask.monthYear)
        assertTrue(longAmountTask.supp1Uploaded)
        assertEquals("quote_abc.pdf", longAmountTask.supp1FileName)

        val doubleAmountTask = FirestoreMappers.taskFromMap(
            docId = "doc2",
            data = mapOf(
                FirestoreSchema.FIELD_OWNER_UID to "user_1",
                FirestoreSchema.FIELD_TASK_NUMBER to 4,
                FirestoreSchema.FIELD_DESCRIPTION to "Spare valve",
                FirestoreSchema.FIELD_AMOUNT to 12450.75
            ),
            expectedOwnerUid = "user_1"
        )
        assertEquals("12450.75", doubleAmountTask!!.amount)
    }

    @Test
    fun `taskToCreateMap always injects ownerUid and enforces firestore rules length bounds`() {
        val longDesc = "A".repeat(1500)
        val payload = FirestoreMappers.taskToCreateMap(
            ownerUid = "user_42",
            task = TaskItem(
                description = longDesc,
                amount = "9500",
                monthYear = "2026-11",
                remarks = "R".repeat(6000)
            ),
            assignedTaskNumber = 5,
            timestampValue = 123456L
        )

        assertEquals("user_42", payload[FirestoreSchema.FIELD_OWNER_UID])
        assertEquals(5, payload[FirestoreSchema.FIELD_TASK_NUMBER])
        assertEquals("Nov-2026", payload[FirestoreSchema.FIELD_MONTH_YEAR])
        assertEquals(
            FirestoreSchema.MAX_DESCRIPTION_LEN,
            (payload[FirestoreSchema.FIELD_DESCRIPTION] as String).length
        )
        assertEquals(
            FirestoreSchema.MAX_REMARKS_LEN,
            (payload[FirestoreSchema.FIELD_REMARKS] as String).length
        )
    }
}
