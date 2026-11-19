package com.helal.accomplishmentplan.nativeapp.data.repository

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.helal.accomplishmentplan.nativeapp.core.firebase.FirebaseConfigStatus
import com.helal.accomplishmentplan.nativeapp.core.session.SessionCleanupCoordinator
import com.helal.accomplishmentplan.nativeapp.data.firestore.FirestoreMappers
import com.helal.accomplishmentplan.nativeapp.data.firestore.FirestoreSchema
import com.helal.accomplishmentplan.nativeapp.domain.logic.MonthUtils
import com.helal.accomplishmentplan.nativeapp.domain.logic.TaskNumberingEngine
import com.helal.accomplishmentplan.nativeapp.domain.model.TaskItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

interface TaskRepository {
    fun observeTasksForMonth(ownerUid: String, monthYear: String): Flow<List<TaskItem>>
    fun observeAllTasks(ownerUid: String): Flow<List<TaskItem>>
    suspend fun addTask(ownerUid: String, task: TaskItem): Result<TaskItem>
    suspend fun updateTask(
        ownerUid: String,
        updatedTask: TaskItem,
        previousMonthYear: String
    ): Result<TaskItem>
    suspend fun updateTaskStatus(ownerUid: String, taskId: String, newStatus: String): Result<Unit>
    suspend fun deleteTaskAndRenumber(
        ownerUid: String,
        taskId: String,
        monthYear: String
    ): Result<Unit>
}

/**
 * Production Firestore TaskRepository:
 * - Strictly scoped to authenticated user's ownerUid on every query and write
 * - Atomic task creation with Month_Counters/{uid}_{monthYear}
 * - Safe retry-protected sequential renumbering after deletion
 * - Safe atomic month reassignment updating both source and target Month_Counters
 * - Deterministic listener cleanup via both awaitClose and SessionCleanupCoordinator
 */
class FirestoreTaskRepository(
    private val configStatus: FirebaseConfigStatus,
    private val sessionCleanupCoordinator: SessionCleanupCoordinator,
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) : TaskRepository {

    companion object {
        private const val MAX_DELETE_RETRY_ATTEMPTS = 3
        private const val RETRY_BACKOFF_MS = 180L
    }

    override fun observeTasksForMonth(ownerUid: String, monthYear: String): Flow<List<TaskItem>> =
        callbackFlow {
            require(ownerUid.isNotBlank()) { "ownerUid is required." }
            if (!configStatus.isFirebaseInitialized) {
                close(notConfiguredError())
                return@callbackFlow
            }
            val normalizedMonth = MonthUtils.toDisplayLabelOrRaw(monthYear)
            val listenerKey = "tasks_month_${ownerUid}_${normalizedMonth}_${UUID.randomUUID()}"

            val registration = firestoreProvider()
                .collection(FirestoreSchema.COLLECTION_TASKS)
                .whereEqualTo(FirestoreSchema.FIELD_OWNER_UID, ownerUid.trim())
                .whereEqualTo(FirestoreSchema.FIELD_MONTH_YEAR, normalizedMonth)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val mapped = snapshot?.documents.orEmpty().mapNotNull { doc ->
                        doc.data?.let { data ->
                            FirestoreMappers.taskFromMap(doc.id, data, ownerUid)
                        }
                    }
                    trySend(TaskNumberingEngine.sortTasksDeterministically(mapped))
                }

            sessionCleanupCoordinator.register(listenerKey) {
                registration.remove()
                channel.close()
            }

            awaitClose {
                sessionCleanupCoordinator.unregister(listenerKey)
                registration.remove()
            }
        }

    override fun observeAllTasks(ownerUid: String): Flow<List<TaskItem>> = callbackFlow {
        require(ownerUid.isNotBlank()) { "ownerUid is required." }
        if (!configStatus.isFirebaseInitialized) {
            close(notConfiguredError())
            return@callbackFlow
        }
        val listenerKey = "tasks_all_${ownerUid}_${UUID.randomUUID()}"

        val registration = firestoreProvider()
            .collection(FirestoreSchema.COLLECTION_TASKS)
            .whereEqualTo(FirestoreSchema.FIELD_OWNER_UID, ownerUid.trim())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val mapped = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.data?.let { data ->
                        FirestoreMappers.taskFromMap(doc.id, data, ownerUid)
                    }
                }
                trySend(TaskNumberingEngine.sortTasksDeterministically(mapped))
            }

        sessionCleanupCoordinator.register(listenerKey) {
            registration.remove()
            channel.close()
        }

        awaitClose {
            sessionCleanupCoordinator.unregister(listenerKey)
            registration.remove()
        }
    }

    override suspend fun addTask(ownerUid: String, task: TaskItem): Result<TaskItem> =
        runFirestoreCall {
            require(ownerUid.isNotBlank()) { "ownerUid is required." }
            val db = requireFirestore()
            val normalizedMonth = MonthUtils.toDisplayLabelOrRaw(task.monthYear)
            val existingTasks = fetchTasksForMonthOnce(db, ownerUid, normalizedMonth)

            val counterRef = monthCounterRef(db, ownerUid, normalizedMonth)
            val newTaskRef = db.collection(FirestoreSchema.COLLECTION_TASKS).document()

            val assignedNumber = db.runTransaction { tx ->
                val counterSnap = tx.get(counterRef)
                val counterLast = counterSnap.data?.let { data ->
                    FirestoreMappers.monthCounterFromMap(counterSnap.id, data, ownerUid)
                        ?.lastTaskNumber
                }
                val nextNumber = TaskNumberingEngine.computeNextTaskNumber(
                    existingMonthTasks = existingTasks,
                    counterLastNumber = counterLast
                )

                val taskPayload = FirestoreMappers.taskToCreateMap(
                    ownerUid = ownerUid,
                    task = task.copy(monthYear = normalizedMonth),
                    assignedTaskNumber = nextNumber
                )
                tx.set(newTaskRef, taskPayload)
                tx.set(
                    counterRef,
                    FirestoreMappers.monthCounterToMap(
                        ownerUid = ownerUid,
                        monthYear = normalizedMonth,
                        lastTaskNumber = nextNumber
                    ),
                    SetOptions.merge()
                )
                nextNumber
            }.await()

            task.copy(
                id = newTaskRef.id,
                ownerUid = ownerUid.trim(),
                taskNumber = assignedNumber,
                monthYear = normalizedMonth
            )
        }

    override suspend fun updateTask(
        ownerUid: String,
        updatedTask: TaskItem,
        previousMonthYear: String
    ): Result<TaskItem> = runFirestoreCall {
        require(ownerUid.isNotBlank()) { "ownerUid is required." }
        require(updatedTask.id.isNotBlank()) { "Task id is required for update." }
        val db = requireFirestore()

        val oldMonth = MonthUtils.toDisplayLabelOrRaw(previousMonthYear)
        val newMonth = MonthUtils.toDisplayLabelOrRaw(updatedTask.monthYear)

        if (oldMonth.equals(newMonth, ignoreCase = true) || oldMonth.isBlank()) {
            val taskRef = db.collection(FirestoreSchema.COLLECTION_TASKS).document(updatedTask.id)
            db.runTransaction { tx ->
                val existingSnap = tx.get(taskRef)
                val existingOwner = existingSnap.getString(FirestoreSchema.FIELD_OWNER_UID)
                require(existingSnap.exists() && existingOwner == ownerUid.trim()) {
                    "Task does not exist or does not belong to current user."
                }
                val payload = FirestoreMappers.taskToUpdateMap(
                    ownerUid = ownerUid,
                    task = updatedTask.copy(monthYear = newMonth)
                )
                tx.update(taskRef, payload)
            }.await()
            updatedTask.copy(ownerUid = ownerUid.trim(), monthYear = newMonth)
        } else {
            reassignTaskMonthAtomically(
                db = db,
                ownerUid = ownerUid,
                updatedTask = updatedTask.copy(monthYear = newMonth),
                sourceMonth = oldMonth,
                targetMonth = newMonth
            )
        }
    }

    override suspend fun updateTaskStatus(
        ownerUid: String,
        taskId: String,
        newStatus: String
    ): Result<Unit> = runFirestoreCall {
        require(ownerUid.isNotBlank()) { "ownerUid is required." }
        require(taskId.isNotBlank()) { "taskId is required." }
        val cleanStatus = newStatus.trim().take(FirestoreSchema.MAX_STATUS_NAME_LEN)
        require(cleanStatus.isNotEmpty()) { "Status must not be blank." }

        val db = requireFirestore()
        val taskRef = db.collection(FirestoreSchema.COLLECTION_TASKS).document(taskId)
        db.runTransaction { tx ->
            val snap = tx.get(taskRef)
            val existingOwner = snap.getString(FirestoreSchema.FIELD_OWNER_UID)
            require(snap.exists() && existingOwner == ownerUid.trim()) {
                "Task does not exist or does not belong to current user."
            }
            tx.update(
                taskRef,
                mapOf(
                    FirestoreSchema.FIELD_STATUS to cleanStatus,
                    FirestoreSchema.FIELD_UPDATED_AT to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    override suspend fun deleteTaskAndRenumber(
        ownerUid: String,
        taskId: String,
        monthYear: String
    ): Result<Unit> = runFirestoreCall {
        require(ownerUid.isNotBlank()) { "ownerUid is required." }
        require(taskId.isNotBlank()) { "taskId is required." }
        val db = requireFirestore()
        val normalizedMonth = MonthUtils.toDisplayLabelOrRaw(monthYear)
        val counterRef = monthCounterRef(db, ownerUid, normalizedMonth)

        var lastError: Throwable? = null
        for (attempt in 1..MAX_DELETE_RETRY_ATTEMPTS) {
            try {
                val monthTasks = fetchTasksForMonthOnce(db, ownerUid, normalizedMonth)
                val targetExists = monthTasks.any { it.id == taskId }
                if (!targetExists) {
                    // Already deleted; ensure sequential numbering is consistent
                    val repairPlan = TaskNumberingEngine.buildRenumberPlanAfterDelete(monthTasks, taskId)
                    applyRenumberTransaction(db, ownerUid, normalizedMonth, counterRef, null, repairPlan)
                    return@runFirestoreCall
                }

                val plan = TaskNumberingEngine.buildRenumberPlanAfterDelete(monthTasks, taskId)
                val targetRef = db.collection(FirestoreSchema.COLLECTION_TASKS).document(taskId)
                applyRenumberTransaction(db, ownerUid, normalizedMonth, counterRef, targetRef, plan)

                // Verify post-delete task count stability for concurrent write safety
                val postTasks = fetchTasksForMonthOnce(db, ownerUid, normalizedMonth)
                val expectedCount = (monthTasks.size - 1).coerceAtLeast(0)
                if (postTasks.size == expectedCount &&
                    postTasks.none { it.id == taskId }
                ) {
                    return@runFirestoreCall
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                lastError = t
            }
            if (attempt < MAX_DELETE_RETRY_ATTEMPTS) {
                delay(RETRY_BACKOFF_MS * attempt)
            }
        }
        throw lastError ?: IllegalStateException("Failed to delete and renumber task after $MAX_DELETE_RETRY_ATTEMPTS attempts.")
    }

    private suspend fun reassignTaskMonthAtomically(
        db: FirebaseFirestore,
        ownerUid: String,
        updatedTask: TaskItem,
        sourceMonth: String,
        targetMonth: String
    ): TaskItem {
        val sourceTasks = fetchTasksForMonthOnce(db, ownerUid, sourceMonth)
        val targetTasks = fetchTasksForMonthOnce(db, ownerUid, targetMonth)
        val sourceCounterRef = monthCounterRef(db, ownerUid, sourceMonth)
        val targetCounterRef = monthCounterRef(db, ownerUid, targetMonth)
        val movingRef = db.collection(FirestoreSchema.COLLECTION_TASKS).document(updatedTask.id)

        val assignedNumber = db.runTransaction { tx ->
            val movingSnap = tx.get(movingRef)
            val existingOwner = movingSnap.getString(FirestoreSchema.FIELD_OWNER_UID)
            require(movingSnap.exists() && existingOwner == ownerUid.trim()) {
                "Task does not exist or does not belong to current user."
            }
            val targetCounterSnap = tx.get(targetCounterRef)
            val targetCounterLast = targetCounterSnap.data?.let { data ->
                FirestoreMappers.monthCounterFromMap(targetCounterSnap.id, data, ownerUid)
                    ?.lastTaskNumber
            }

            val plan = TaskNumberingEngine.buildMonthReassignmentPlan(
                sourceMonthTasks = sourceTasks,
                targetMonthTasks = targetTasks,
                targetCounterLastNumber = targetCounterLast,
                movingTaskId = updatedTask.id
            )

            // 1. Update moved task with new month and target task number
            val updatePayload = FirestoreMappers.taskToUpdateMap(
                ownerUid = ownerUid,
                task = updatedTask.copy(monthYear = targetMonth),
                overrideTaskNumber = plan.movedTaskNewNumber
            )
            tx.update(movingRef, updatePayload)

            // 2. Compact remaining tasks in source month
            plan.sourceMonthPlan.updates.forEach { assignment ->
                val ref = db.collection(FirestoreSchema.COLLECTION_TASKS).document(assignment.taskId)
                tx.update(
                    ref,
                    mapOf(
                        FirestoreSchema.FIELD_TASK_NUMBER to assignment.newTaskNumber,
                        FirestoreSchema.FIELD_UPDATED_AT to FieldValue.serverTimestamp()
                    )
                )
            }

            // 3. Update both source and target Month_Counters atomically
            tx.set(
                sourceCounterRef,
                FirestoreMappers.monthCounterToMap(
                    ownerUid = ownerUid,
                    monthYear = sourceMonth,
                    lastTaskNumber = plan.sourceMonthPlan.newLastTaskNumber
                ),
                SetOptions.merge()
            )
            tx.set(
                targetCounterRef,
                FirestoreMappers.monthCounterToMap(
                    ownerUid = ownerUid,
                    monthYear = targetMonth,
                    lastTaskNumber = plan.targetMonthNewLastTaskNumber
                ),
                SetOptions.merge()
            )

            plan.movedTaskNewNumber
        }.await()

        return updatedTask.copy(
            ownerUid = ownerUid.trim(),
            monthYear = targetMonth,
            taskNumber = assignedNumber
        )
    }

    private suspend fun applyRenumberTransaction(
        db: FirebaseFirestore,
        ownerUid: String,
        monthYear: String,
        counterRef: DocumentReference,
        deleteTargetRef: DocumentReference?,
        plan: com.helal.accomplishmentplan.nativeapp.domain.logic.RenumberPlan
    ) {
        db.runTransaction { tx ->
            if (deleteTargetRef != null) {
                val snap = tx.get(deleteTargetRef)
                if (snap.exists()) {
                    require(snap.getString(FirestoreSchema.FIELD_OWNER_UID) == ownerUid.trim()) {
                        "Cannot delete task belonging to another user."
                    }
                    tx.delete(deleteTargetRef)
                }
            }
            plan.updates.forEach { assignment ->
                val ref = db.collection(FirestoreSchema.COLLECTION_TASKS).document(assignment.taskId)
                tx.update(
                    ref,
                    mapOf(
                        FirestoreSchema.FIELD_TASK_NUMBER to assignment.newTaskNumber,
                        FirestoreSchema.FIELD_UPDATED_AT to FieldValue.serverTimestamp()
                    )
                )
            }
            tx.set(
                counterRef,
                FirestoreMappers.monthCounterToMap(
                    ownerUid = ownerUid,
                    monthYear = monthYear,
                    lastTaskNumber = plan.newLastTaskNumber
                ),
                SetOptions.merge()
            )
        }.await()
    }

    private suspend fun fetchTasksForMonthOnce(
        db: FirebaseFirestore,
        ownerUid: String,
        monthYear: String
    ): List<TaskItem> {
        val snapshot = db.collection(FirestoreSchema.COLLECTION_TASKS)
            .whereEqualTo(FirestoreSchema.FIELD_OWNER_UID, ownerUid.trim())
            .whereEqualTo(FirestoreSchema.FIELD_MONTH_YEAR, monthYear)
            .get()
            .await()

        val mapped = snapshot.documents.mapNotNull { doc ->
            doc.data?.let { data -> FirestoreMappers.taskFromMap(doc.id, data, ownerUid) }
        }
        return TaskNumberingEngine.sortTasksDeterministically(mapped)
    }

    private fun monthCounterRef(
        db: FirebaseFirestore,
        ownerUid: String,
        monthYear: String
    ): DocumentReference {
        val docId = MonthUtils.counterDocumentId(ownerUid, monthYear)
        return db.collection(FirestoreSchema.COLLECTION_MONTH_COUNTERS).document(docId)
    }

    private fun requireFirestore(): FirebaseFirestore {
        if (!configStatus.isFirebaseInitialized) throw notConfiguredError()
        return firestoreProvider()
    }

    private fun notConfiguredError(): IllegalStateException = IllegalStateException(
        "Firestore is not configured yet. Add google-services.json for com.helal.accomplishmentplan.nativeapp."
    )

    private suspend inline fun <T> runFirestoreCall(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
