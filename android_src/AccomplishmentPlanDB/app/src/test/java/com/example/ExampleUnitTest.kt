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
        // Required verification items:
        assertEquals("4521001", AccountMaster.getCodeForAccount("M_Salaries wages(Basic salary)"))
        assertEquals("6111001", AccountMaster.getCodeForAccount("S_Salaries wages(Basic salary)"))
        assertEquals("1200951", AccountMaster.getCodeForAccount("Machinery"))
        assertEquals("1201521", AccountMaster.getCodeForAccount("Software"))

        // Verify total predefined accounts count
        assertEquals(154, AccountMaster.accounts.size)

        // Verify dual account names sharing code 4532404
        assertEquals("4532404", AccountMaster.getCodeForAccount("M_Outsourcing processing expenses (Other general)"))
        assertEquals("4532404", AccountMaster.getCodeForAccount("M_Outsourcing processing expenses(Other-General)"))
    }

    @Test
    fun testAccountCodeAutomaticLookupAndTaskEdit() {
        // Immediate update on account selection change
        var selectedAccount = "M_Salaries wages(Basic salary)"
        var code = AccountMaster.getCodeForAccount(selectedAccount)
        assertEquals("4521001", code)

        // Change selection -> immediately updates code
        selectedAccount = "Software"
        code = AccountMaster.getCodeForAccount(selectedAccount)
        assertEquals("1201521", code)

        // Editing a task restores saved account name and code
        val existingTask = TaskItem(
            id = "t_101",
            ownerUid = "uid_1",
            description = "Custom development",
            accountName = "Machinery",
            accountCode = "1200951"
        )
        assertEquals("Machinery", existingTask.accountName)
        assertEquals("1200951", existingTask.accountCode)

        // Custom account remains available and takes precedence
        val customAccounts = listOf("Custom Safety Rig" to "ACC-SAFE999")
        val customLookup = AccountMaster.getCodeForAccount("Custom Safety Rig", customAccounts)
        assertEquals("ACC-SAFE999", customLookup)
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
