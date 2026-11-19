package com.helal.accomplishmentplan.nativeapp.domain.logic

import com.helal.accomplishmentplan.nativeapp.domain.model.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskNumberingEngineTest {

    @Test
    fun `sorts deterministically by valid taskNumber then createdAt then id`() {
        val tasks = listOf(
            TaskItem(id = "c", taskNumber = 0, createdAtMillis = 100L),
            TaskItem(id = "b", taskNumber = 2, createdAtMillis = 300L),
            TaskItem(id = "a", taskNumber = 1, createdAtMillis = 200L),
            TaskItem(id = "d", taskNumber = 2, createdAtMillis = 150L)
        )

        val sorted = TaskNumberingEngine.sortTasksDeterministically(tasks)
        assertEquals(listOf("a", "d", "b", "c"), sorted.map { it.id })
    }

    @Test
    fun `computes atomic next task number from max of counter and existing tasks`() {
        val existing = listOf(
            TaskItem(id = "t1", taskNumber = 1),
            TaskItem(id = "t2", taskNumber = 4)
        )
        assertEquals(5, TaskNumberingEngine.computeNextTaskNumber(existing, counterLastNumber = 2))
        assertEquals(8, TaskNumberingEngine.computeNextTaskNumber(existing, counterLastNumber = 7))
        assertEquals(1, TaskNumberingEngine.computeNextTaskNumber(emptyList(), counterLastNumber = null))
    }

    @Test
    fun `delete renumbering compacts remaining tasks sequentially 1 to N`() {
        val existing = listOf(
            TaskItem(id = "t1", taskNumber = 1, createdAtMillis = 1L),
            TaskItem(id = "t2", taskNumber = 2, createdAtMillis = 2L),
            TaskItem(id = "t3", taskNumber = 3, createdAtMillis = 3L),
            TaskItem(id = "t4", taskNumber = 4, createdAtMillis = 4L)
        )

        val plan = TaskNumberingEngine.buildRenumberPlanAfterDelete(existing, deletedTaskId = "t2")
        assertEquals(3, plan.newLastTaskNumber)
        assertEquals(
            listOf(
                TaskNumberAssignment("t3", 2),
                TaskNumberAssignment("t4", 3)
            ),
            plan.updates
        )
    }

    @Test
    fun `month reassignment compacts source month and increments target month cleanly`() {
        val sourceTasks = listOf(
            TaskItem(id = "s1", taskNumber = 1, monthYear = "Nov-2026"),
            TaskItem(id = "s2", taskNumber = 2, monthYear = "Nov-2026"),
            TaskItem(id = "s3", taskNumber = 3, monthYear = "Nov-2026")
        )
        val targetTasks = listOf(
            TaskItem(id = "d1", taskNumber = 1, monthYear = "Dec-2026"),
            TaskItem(id = "d2", taskNumber = 2, monthYear = "Dec-2026")
        )

        val plan = TaskNumberingEngine.buildMonthReassignmentPlan(
            sourceMonthTasks = sourceTasks,
            targetMonthTasks = targetTasks,
            targetCounterLastNumber = 3,
            movingTaskId = "s1"
        )

        assertEquals(2, plan.sourceMonthPlan.newLastTaskNumber)
        assertEquals(
            listOf(TaskNumberAssignment("s2", 1), TaskNumberAssignment("s3", 2)),
            plan.sourceMonthPlan.updates
        )
        assertEquals(4, plan.movedTaskNewNumber)
        assertEquals(4, plan.targetMonthNewLastTaskNumber)
    }
}
