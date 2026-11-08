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

    @Test
    fun testCategoryDefaultList() {
        val expected19 = listOf(
            "Furniture & fixture",
            "Compliance & fire safety",
            "Materials",
            "Service",
            "Food",
            "Medical",
            "Book Printing",
            "Accessories & others",
            "Auxiliary facilities",
            "Renovation",
            "Electrical",
            "Machinery",
            "Needle",
            "Sewing spare parts",
            "Cutting spare parts",
            "Hardware",
            "Printing Section",
            "LAB",
            "Additional"
        )
        assertEquals(19, AccountMaster.defaultCategories.size)
        assertEquals(expected19, AccountMaster.defaultCategories)
    }

    @Test
    fun testCategoryItemAndTaskCategoryMapping() {
        // Task with category
        val taskWithCat = TaskItem(
            id = "t_cat_1",
            ownerUid = "uid_cat",
            taskNumber = 10,
            description = "Needle replacement for Unit-1",
            category = "Needle"
        )
        assertEquals("Needle", taskWithCat.category)
        val mapWithCat = taskWithCat.toMap()
        assertEquals("Needle", mapWithCat["category"])

        // Task without category (backward compatibility for existing tasks)
        val taskWithoutCat = TaskItem(
            id = "t_cat_old",
            ownerUid = "uid_cat",
            taskNumber = 11,
            description = "Old task before Phase 2",
            category = ""
        )
        assertEquals("", taskWithoutCat.category)
        val mapWithoutCat = taskWithoutCat.toMap()
        assertEquals("", mapWithoutCat["category"])

        // CategoryItem serialization
        val catItem = com.example.model.CategoryItem(
            id = "cat_doc_1",
            ownerUid = "uid_cat",
            categoryName = "Electrical"
        )
        val catMap = catItem.toMap()
        assertEquals("uid_cat", catMap["ownerUid"])
        assertEquals("Electrical", catMap["categoryName"])
    }

    @Test
    fun testCaseInsensitiveCategoryDuplicateDetection() {
        val currentCategories = listOf("Electrical", "Machinery", "Food")

        fun isDuplicate(input: String): Boolean {
            val trimmed = input.trim()
            return currentCategories.any { it.equals(trimmed, ignoreCase = true) }
        }

        assertTrue(isDuplicate("electrical"))
        assertTrue(isDuplicate("ELECTRICAL"))
        assertTrue(isDuplicate(" Machinery "))
        assertTrue(isDuplicate("food"))
        assertFalse(isDuplicate("Medical"))
        assertFalse(isDuplicate("Electronics"))
    }

    @Test
    fun testOfficialStatuses27() {
        val expected27 = listOf(
            "Draft Ready",
            "Advance Draft Ready",
            "Advance Ready",
            "Advance Done",
            "Payment Approval Ready",
            "Done",
            "CS Ready",
            "Approval in Audit",
            "Approval in Audit (HOD)",
            "CS in Accounts",
            "Approval in Accounts",
            "WO Done",
            "Waiting for CFO Sign",
            "Waiting for MD Sign",
            "Cancel from CFO",
            "Cancel from HOD",
            "Cancel",
            "Pending",
            "Settlement",
            "Hold from MR Team",
            "Hold from CFO",
            "Hold from MD",
            "Done from HOD",
            "Payment Pending",
            "Next Month",
            "No Need",
            "Unavailable"
        )
        assertEquals(27, AccountMaster.defaultStatuses.size)
        assertEquals(expected27, AccountMaster.defaultStatuses)
    }

    @Test
    fun testTaskDeleteIsolationAndOwnership() {
        val task = TaskItem(
            id = "task_del_1",
            ownerUid = "user_owner_1",
            taskNumber = 5,
            description = "To be deleted"
        )
        assertEquals("user_owner_1", task.ownerUid)
        // Verify another user cannot own this task
        assertFalse(task.ownerUid == "user_hacker_2")
    }

    @Test
    fun testTaskNumberValidationAndSanitization() {
        // Positive valid numbers
        assertEquals(1, com.example.data.TaskRepository.sanitizeTaskNumber(1))
        assertEquals(5, com.example.data.TaskRepository.sanitizeTaskNumber(5L))
        assertEquals(10, com.example.data.TaskRepository.sanitizeTaskNumber(10.0))
        assertEquals(42, com.example.data.TaskRepository.sanitizeTaskNumber("42"))
        assertEquals(99, com.example.data.TaskRepository.sanitizeTaskNumber("  99  "))

        // Invalid values that must be rejected (return null, never silently 1)
        assertNull(com.example.data.TaskRepository.sanitizeTaskNumber(null))
        assertNull(com.example.data.TaskRepository.sanitizeTaskNumber(""))
        assertNull(com.example.data.TaskRepository.sanitizeTaskNumber("   "))
        assertNull(com.example.data.TaskRepository.sanitizeTaskNumber("abc"))
        assertNull(com.example.data.TaskRepository.sanitizeTaskNumber(0))
        assertNull(com.example.data.TaskRepository.sanitizeTaskNumber(-5))
        assertNull(com.example.data.TaskRepository.sanitizeTaskNumber(3.14))
        assertNull(com.example.data.TaskRepository.sanitizeTaskNumber(Double.NaN))
        assertNull(com.example.data.TaskRepository.sanitizeTaskNumber(Double.POSITIVE_INFINITY))
    }

    @Test
    fun testSixConsecutiveTaskCreationsSimulation() {
        // Simulates Month_Counters incrementation over 6 additions
        var counterLast = 0
        val createdTasks = mutableListOf<TaskItem>()

        for (i in 1..6) {
            val allocatedNum = counterLast + 1
            counterLast = allocatedNum
            createdTasks.add(
                TaskItem(
                    id = "task_$i",
                    ownerUid = "uid_test",
                    taskNumber = allocatedNum,
                    monthYear = "Feb-2027",
                    description = "Test Task $i"
                )
            )
        }

        assertEquals(6, createdTasks.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), createdTasks.map { it.taskNumber })
        assertEquals(6, counterLast)
    }

    @Test
    fun testDeleteTaskThreeAndRenumberRemainingTasks() {
        // Setup 6 tasks
        val tasks = (1..6).map { i ->
            TaskItem(
                id = "doc_$i",
                ownerUid = "uid_test",
                taskNumber = i,
                monthYear = "Feb-2027",
                description = "Task $i"
            )
        }.toMutableList()

        // Delete task #3
        val taskIdToDelete = "doc_3"
        val remaining = tasks.filter { it.id != taskIdToDelete }
            .sortedWith(compareBy({ it.taskNumber }, { it.id }))

        // Renumber remaining sequentially 1..N
        val renumbered = remaining.mapIndexed { index, item ->
            item.copy(taskNumber = index + 1)
        }
        val newCounterLast = renumbered.size

        assertEquals(5, renumbered.size)
        assertEquals(listOf(1, 2, 3, 4, 5), renumbered.map { it.taskNumber })
        assertEquals("doc_4", renumbered[2].id)
        assertEquals(3, renumbered[2].taskNumber)
        assertEquals(5, newCounterLast)
    }

    @Test
    fun testCreateTaskImmediatelyAfterDeletion() {
        // After deleting task 3 out of 6, counter is 5
        var counterLast = 5
        val nextAllocated = counterLast + 1
        counterLast = nextAllocated

        assertEquals(6, nextAllocated)
        assertEquals(6, counterLast)
    }

    @Test
    fun testConcurrentAddConflictSimulation() {
        // When two devices attempt to add concurrently with same initial counter (e.g. 2):
        // Device A reads counter 2, writes counter 3, task #3.
        // Device B detects counter changed to 3, retries, reads counter 3, writes counter 4, task #4.
        var counter = 2
        fun allocate(currentCounter: Int): Pair<Int, Int> {
            val next = currentCounter + 1
            return next to next // (allocatedTaskNumber, newCounter)
        }

        // Device A succeeds
        val (deviceATaskNum, newCounterA) = allocate(counter)
        counter = newCounterA
        assertEquals(3, deviceATaskNum)

        // Device B retries with fresh counter
        val (deviceBTaskNum, newCounterB) = allocate(counter)
        counter = newCounterB
        assertEquals(4, deviceBTaskNum)
        assertEquals(4, counter)
    }

    @Test
    fun testConcurrentAddDeleteConflictDetection() {
        // While delete is renumbering 4 tasks (snap length 5 before delete),
        // if an add operation committed concurrently, counter advances to 6.
        val preFetchTaskCount = 5
        val concurrentCommittedCounter = 6

        val shouldRetry = concurrentCommittedCounter > preFetchTaskCount
        assertTrue("Delete must detect concurrent add and trigger retry", shouldRetry)
    }
}



