package com.helal.accomplishmentplan.nativeapp.domain.logic

import com.helal.accomplishmentplan.nativeapp.domain.model.TaskItem

data class TaskNumberAssignment(
    val taskId: String,
    val newTaskNumber: Int
)

data class RenumberPlan(
    val updates: List<TaskNumberAssignment>,
    val newLastTaskNumber: Int
)

data class MonthReassignmentPlan(
    val sourceMonthPlan: RenumberPlan,
    val movedTaskNewNumber: Int,
    val targetMonthNewLastTaskNumber: Int
)

/**
 * Pure deterministic task ordering, numbering, post-delete sequential renumbering,
 * and cross-month reassignment planning matching the Web App (src/firebase/db.ts).
 */
object TaskNumberingEngine {

    fun sanitizeTaskNumber(raw: Any?): Int? {
        val number = when (raw) {
            is Int -> raw
            is Long -> raw.toInt()
            is Double -> if (raw.isFinite()) raw.toInt() else return null
            is Number -> raw.toInt()
            is String -> raw.trim().toIntOrNull()
            else -> null
        }
        return number?.takeIf { it > 0 }
    }

    /**
     * Sorts tasks deterministically:
     * 1. Valid positive taskNumber ascending (invalid/<=0 placed at the end)
     * 2. createdAtMillis ascending
     * 3. document id ascending
     */
    fun sortTasksDeterministically(tasks: List<TaskItem>): List<TaskItem> {
        return tasks.sortedWith(
            compareBy<TaskItem>(
                { sanitizeTaskNumber(it.taskNumber) ?: Int.MAX_VALUE },
                { it.createdAtMillis },
                { it.id }
            )
        )
    }

    /**
     * Computes the next atomic taskNumber for a month:
     * max(counterLastNumber, maxExistingTaskNumber) + 1
     */
    fun computeNextTaskNumber(
        existingMonthTasks: List<TaskItem>,
        counterLastNumber: Int?
    ): Int {
        val maxFromTasks = existingMonthTasks
            .mapNotNull { sanitizeTaskNumber(it.taskNumber) }
            .maxOrNull() ?: 0
        val safeCounter = (counterLastNumber ?: 0).coerceAtLeast(0)
        return maxOf(maxFromTasks, safeCounter) + 1
    }

    /**
     * Builds the minimal sequential 1..N renumbering plan after removing deletedTaskId.
     */
    fun buildRenumberPlanAfterDelete(
        existingMonthTasks: List<TaskItem>,
        deletedTaskId: String
    ): RenumberPlan {
        val remaining = sortTasksDeterministically(
            existingMonthTasks.filter { it.id != deletedTaskId }
        )
        val updates = remaining.mapIndexedNotNull { index, task ->
            val expectedNumber = index + 1
            if (task.taskNumber != expectedNumber) {
                TaskNumberAssignment(taskId = task.id, newTaskNumber = expectedNumber)
            } else {
                null
            }
        }
        return RenumberPlan(
            updates = updates,
            newLastTaskNumber = remaining.size
        )
    }

    /**
     * Plans atomic month reassignment when an existing task moves from sourceMonth to targetMonth:
     * - Compacts remaining tasks in sourceMonth sequentially (1..N)
     * - Assigns the moved task the next monotonic number in targetMonth
     */
    fun buildMonthReassignmentPlan(
        sourceMonthTasks: List<TaskItem>,
        targetMonthTasks: List<TaskItem>,
        targetCounterLastNumber: Int?,
        movingTaskId: String
    ): MonthReassignmentPlan {
        val sourcePlan = buildRenumberPlanAfterDelete(sourceMonthTasks, movingTaskId)
        val targetExistingWithoutMoved = targetMonthTasks.filter { it.id != movingTaskId }
        val movedNewNumber = computeNextTaskNumber(targetExistingWithoutMoved, targetCounterLastNumber)
        return MonthReassignmentPlan(
            sourceMonthPlan = sourcePlan,
            movedTaskNewNumber = movedNewNumber,
            targetMonthNewLastTaskNumber = movedNewNumber
        )
    }
}
