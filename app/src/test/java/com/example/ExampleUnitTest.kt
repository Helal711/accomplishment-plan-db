package com.example

import com.example.model.AccountItem
import com.example.model.AccountMaster
import com.example.model.Priority
import com.example.model.StatusItem
import com.example.model.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTaskItemSerializationAndOwnerUid() {
        val task = TaskItem(
            id = "task_123",
            ownerUid = "user_abc",
            taskNumber = 1,
            description = "Civil Maintenance material...",
            amount = "13,000",
            paymentMode = "Cash",
            unit = "Unit-1",
            priority = "High",
            status = "Draft Ready",
            expectedDate = "17 Nov 2026",
            accountName = "Civil Maintenance",
            accountCode = "ACC-CM101",
            remarks = "Procurement notes",
            monthYear = "Nov-2026"
        )

        assertEquals("user_abc", task.ownerUid)
        assertEquals(Priority.HIGH, task.priorityEnum)
        assertFalse(task.isDone)
        assertEquals("13,000 TK", task.formattedAmount)

        val map = task.toMap()
        assertEquals("user_abc", map["ownerUid"])
        assertEquals("Civil Maintenance material...", map["description"])
        assertEquals("13,000", map["amount"])
        assertEquals("Cash", map["paymentMode"])
        assertEquals("Unit-1", map["unit"])
        assertEquals("High", map["priority"])
        assertEquals("Draft Ready", map["status"])
        assertEquals("17 Nov 2026", map["expectedDate"])
        assertEquals("Civil Maintenance", map["accountName"])
        assertEquals("ACC-CM101", map["accountCode"])
        assertEquals("Procurement notes", map["remarks"])
        assertEquals("Nov-2026", map["monthYear"])
        assertEquals(1, map["taskNumber"])

        // Ensure no quotation fields exist in map
        assertNull(map["supp1Uploaded"])
        assertNull(map["supp1FileName"])
        assertNull(map["supp1FileUrl"])
    }

    @Test
    fun testTaskDoneStateAndAmountFormatting() {
        val doneTask = TaskItem(
            status = "Done",
            amount = "50,000 TK"
        )
        assertTrue(doneTask.isDone)
        assertEquals("50,000 TK", doneTask.formattedAmount)

        val inProgressTask = TaskItem(
            status = "In Progress",
            amount = "25,000"
        )
        assertFalse(inProgressTask.isDone)
        assertEquals("25,000 TK", inProgressTask.formattedAmount)
    }

    @Test
    fun testAccountItemAndStatusItemOwnerUid() {
        val account = AccountItem(
            id = "acc_1",
            ownerUid = "user_abc",
            accountName = "Civil Maintenance",
            accountCode = "ACC-CM101"
        )
        assertEquals("user_abc", account.ownerUid)
        assertEquals("user_abc", account.toMap()["ownerUid"])
        assertEquals("Civil Maintenance", account.toMap()["accountName"])
        assertEquals("ACC-CM101", account.toMap()["accountCode"])

        val status = StatusItem(
            id = "stat_1",
            ownerUid = "user_abc",
            statusName = "Draft Ready",
            isDefault = true
        )
        assertEquals("user_abc", status.ownerUid)
        assertEquals("user_abc", status.toMap()["ownerUid"])
        assertEquals("Draft Ready", status.toMap()["statusName"])
        assertEquals(true, status.toMap()["isDefault"])
    }

    @Test
    fun testAccountMasterCodeLookup() {
        assertEquals("ACC-CM101", AccountMaster.getCodeForAccount("Civil Maintenance"))
        assertEquals("ACC-AC205", AccountMaster.getCodeForAccount("Air Conditioning"))
        assertEquals("ACC-CS309", AccountMaster.getCodeForAccount("Commercial Seal"))
        assertEquals("ACC-EL412", AccountMaster.getCodeForAccount("Electrical Spares"))
        assertEquals("ACC-FL508", AccountMaster.getCodeForAccount("Factory Logistics"))
        assertEquals("ACC-SS602", AccountMaster.getCodeForAccount("Safety & Security"))
        assertEquals("ACC-MS714", AccountMaster.getCodeForAccount("Machinery Supplies"))
        assertEquals("ACC-AC820", AccountMaster.getCodeForAccount("Admin & Canteen"))
    }

    @Test
    fun testPriorityEnumMapping() {
        assertEquals(Priority.HIGH, Priority.fromLabel("High"))
        assertEquals(Priority.HIGH, Priority.fromLabel("high"))
        assertEquals(Priority.MED, Priority.fromLabel("Med"))
        assertEquals(Priority.MED, Priority.fromLabel("med"))
        assertEquals(Priority.LOW, Priority.fromLabel("Low"))
        assertEquals(Priority.LOW, Priority.fromLabel("low"))
        assertEquals(Priority.MED, Priority.fromLabel("Unknown"))
    }

    @Test
    fun testTaskFilteringLogic() {
        val tasks = listOf(
            TaskItem(id = "1", taskNumber = 1, description = "Air Conditioner filter", unit = "Unit-1", status = "Done"),
            TaskItem(id = "2", taskNumber = 2, description = "Forklift tire", unit = "Unit-2", status = "Draft Ready"),
            TaskItem(id = "3", taskNumber = 3, description = "Water pump maintenance", unit = "Unit-1", status = "Pending")
        )

        // Filter by description
        val filterByDesc = tasks.filter { it.description.contains("Air", ignoreCase = true) }
        assertEquals(1, filterByDesc.size)
        assertEquals("1", filterByDesc[0].id)

        // Filter by Unit
        val filterByUnit = tasks.filter { it.unit.contains("Unit-1", ignoreCase = true) }
        assertEquals(2, filterByUnit.size)

        // Filter by Status
        val filterByStatus = tasks.filter { it.status.equals("Draft Ready", ignoreCase = true) }
        assertEquals(1, filterByStatus.size)
        assertEquals("2", filterByStatus[0].id)

        // Calculate pending count
        val pendingCount = tasks.count { !it.isDone }
        assertEquals(2, pendingCount)
    }
}
