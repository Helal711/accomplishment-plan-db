package com.helal.accomplishmentplan.nativeapp.domain.logic

import com.helal.accomplishmentplan.nativeapp.domain.model.Priority
import com.helal.accomplishmentplan.nativeapp.domain.model.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskFilterEngineTest {

    private val sampleTasks = listOf(
        TaskItem(
            id = "1",
            ownerUid = "u1",
            taskNumber = 1,
            description = "ফ্যাক্টরি ইলেকট্রিক্যাল ক্যাবল ক্রয়",
            amount = "45000",
            paymentMode = "Bank",
            unit = "Unit-1",
            priority = Priority.HIGH,
            status = "Pending",
            expectedDate = "2026-11-20",
            accountName = "M_Supply expenses(Local materials)",
            accountCode = "4532155",
            category = "Electrical",
            remarks = "জরুরি ভিত্তিতে সম্পন্ন করতে হবে",
            monthYear = "Nov-2026"
        ),
        TaskItem(
            id = "2",
            ownerUid = "u1",
            taskNumber = 2,
            description = "Fire safety inspection renewal",
            amount = "125,000",
            paymentMode = "Cheque",
            unit = "Unit-2",
            priority = Priority.MED,
            status = "Done",
            expectedDate = "2026-11-25",
            accountName = "M_Commissions(TEST fee)",
            accountCode = "4532201",
            category = "Compliance & fire safety",
            remarks = "Approved by HOD",
            monthYear = "Nov-2026"
        ),
        TaskItem(
            id = "3",
            ownerUid = "u1",
            taskNumber = 3,
            description = "Sewing machine spare needles",
            amount = "8500",
            paymentMode = "Cash",
            unit = "Unit-3",
            priority = Priority.LOW,
            status = "Done",
            expectedDate = "2026-11-28",
            accountName = "M_Supply expenses(Machine Spare part)",
            accountCode = "4532156",
            category = "Needle",
            remarks = "Local supplier",
            monthYear = "Nov-2026"
        )
    )

    @Test
    fun `matches Bengali Unicode text across description and remarks`() {
        val byDesc = TaskFilterEngine.filterTasks(sampleTasks, "ইলেকট্রিক্যাল", null)
        assertEquals(listOf("1"), byDesc.map { it.id })

        val byRemarks = TaskFilterEngine.filterTasks(sampleTasks, "জরুরি", null)
        assertEquals(listOf("1"), byRemarks.map { it.id })
    }

    @Test
    fun `matches formatted amounts, unformatted amounts, Bengali digits, account codes, and task number`() {
        assertEquals(listOf("2"), TaskFilterEngine.filterTasks(sampleTasks, "125000", null).map { it.id })
        assertEquals(listOf("2"), TaskFilterEngine.filterTasks(sampleTasks, "125,000 tk", null).map { it.id })
        assertEquals(listOf("1"), TaskFilterEngine.filterTasks(sampleTasks, "৪৫০০০", null).map { it.id })
        assertEquals(listOf("3"), TaskFilterEngine.filterTasks(sampleTasks, "4532156", null).map { it.id })
        assertEquals(listOf("2"), TaskFilterEngine.filterTasks(sampleTasks, "#2", null).map { it.id })
    }

    @Test
    fun `combines multi-field search with active status filter simultaneously`() {
        val combined = TaskFilterEngine.filterTasks(sampleTasks, "Unit-2 cheque", "Done")
        assertEquals(listOf("2"), combined.map { it.id })

        val mismatchStatus = TaskFilterEngine.filterTasks(sampleTasks, "Unit-2", "Pending")
        assertEquals(emptyList<String>(), mismatchStatus.map { it.id })
    }

    @Test
    fun `computes status counts preserving zero-count defaults and custom task statuses`() {
        val summaries = TaskFilterEngine.computeStatusSummaries(
            tasks = sampleTasks,
            configuredStatuses = listOf("Draft Ready", "Done", "Pending", "Cancelled")
        )
        assertEquals(
            mapOf("Draft Ready" to 0, "Done" to 2, "Pending" to 1, "Cancelled" to 0),
            summaries.associate { it.statusName to it.count }
        )
    }
}
