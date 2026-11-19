package com.helal.accomplishmentplan.nativeapp.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.helal.accomplishmentplan.nativeapp.core.firebase.FirebaseConfigStatus
import com.helal.accomplishmentplan.nativeapp.core.session.SessionCleanupCoordinator
import com.helal.accomplishmentplan.nativeapp.data.firestore.FirestoreMappers
import com.helal.accomplishmentplan.nativeapp.data.firestore.FirestoreSchema
import com.helal.accomplishmentplan.nativeapp.domain.logic.WorkspaceSeedPlanner
import com.helal.accomplishmentplan.nativeapp.domain.model.AccountItem
import com.helal.accomplishmentplan.nativeapp.domain.model.CategoryItem
import com.helal.accomplishmentplan.nativeapp.domain.model.StatusItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

interface MasterDataRepository {
    fun observeAccounts(ownerUid: String): Flow<List<AccountItem>>
    fun observeStatuses(ownerUid: String): Flow<List<StatusItem>>
    fun observeCategories(ownerUid: String): Flow<List<CategoryItem>>

    /**
     * Additive-only workspace seeding. Inserts missing default accounts, statuses, and categories
     * for ownerUid without EVER deleting or overwriting user-created records.
     */
    suspend fun initializeUserWorkspaceAdditive(ownerUid: String): Result<Unit>

    suspend fun addAccount(ownerUid: String, accountName: String, accountCode: String): Result<AccountItem>
    suspend fun updateAccount(ownerUid: String, accountId: String, accountName: String, accountCode: String): Result<Unit>
    suspend fun deleteAccount(ownerUid: String, accountId: String): Result<Unit>

    suspend fun addCustomStatus(ownerUid: String, statusName: String): Result<StatusItem>
    suspend fun deleteCustomStatus(ownerUid: String, statusId: String): Result<Unit>

    suspend fun addCategory(ownerUid: String, categoryName: String): Result<CategoryItem>
    suspend fun updateCategory(ownerUid: String, categoryId: String, categoryName: String): Result<Unit>
    suspend fun deleteCategory(ownerUid: String, categoryId: String): Result<Unit>
}

class FirestoreMasterDataRepository(
    private val configStatus: FirebaseConfigStatus,
    private val sessionCleanupCoordinator: SessionCleanupCoordinator,
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) : MasterDataRepository {

    companion object {
        private const val FIRESTORE_SAFE_BATCH_LIMIT = 400
    }

    override fun observeAccounts(ownerUid: String): Flow<List<AccountItem>> = callbackFlow {
        require(ownerUid.isNotBlank()) { "ownerUid is required." }
        if (!configStatus.isFirebaseInitialized) {
            close(notConfiguredError())
            return@callbackFlow
        }
        val listenerKey = "accounts_${ownerUid}_${UUID.randomUUID()}"

        val registration = firestoreProvider()
            .collection(FirestoreSchema.COLLECTION_ACCOUNTS)
            .whereEqualTo(FirestoreSchema.FIELD_OWNER_UID, ownerUid.trim())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty()
                    .mapNotNull { doc ->
                        doc.data?.let { FirestoreMappers.accountFromMap(doc.id, it, ownerUid) }
                    }
                    .sortedBy { it.accountName.lowercase() }
                trySend(items)
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

    override fun observeStatuses(ownerUid: String): Flow<List<StatusItem>> = callbackFlow {
        require(ownerUid.isNotBlank()) { "ownerUid is required." }
        if (!configStatus.isFirebaseInitialized) {
            close(notConfiguredError())
            return@callbackFlow
        }
        val listenerKey = "statuses_${ownerUid}_${UUID.randomUUID()}"

        val registration = firestoreProvider()
            .collection(FirestoreSchema.COLLECTION_STATUSES)
            .whereEqualTo(FirestoreSchema.FIELD_OWNER_UID, ownerUid.trim())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty()
                    .mapNotNull { doc ->
                        doc.data?.let { FirestoreMappers.statusFromMap(doc.id, it, ownerUid) }
                    }
                    .sortedWith(
                        compareByDescending<StatusItem> { it.isDefault }
                            .thenBy { it.createdAtMillis }
                            .thenBy { it.statusName.lowercase() }
                    )
                trySend(items)
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

    override fun observeCategories(ownerUid: String): Flow<List<CategoryItem>> = callbackFlow {
        require(ownerUid.isNotBlank()) { "ownerUid is required." }
        if (!configStatus.isFirebaseInitialized) {
            close(notConfiguredError())
            return@callbackFlow
        }
        val listenerKey = "categories_${ownerUid}_${UUID.randomUUID()}"

        val registration = firestoreProvider()
            .collection(FirestoreSchema.COLLECTION_CATEGORIES)
            .whereEqualTo(FirestoreSchema.FIELD_OWNER_UID, ownerUid.trim())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty()
                    .mapNotNull { doc ->
                        doc.data?.let { FirestoreMappers.categoryFromMap(doc.id, it, ownerUid) }
                    }
                    .sortedBy { it.categoryName.lowercase() }
                trySend(items)
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

    override suspend fun initializeUserWorkspaceAdditive(ownerUid: String): Result<Unit> =
        runFirestoreCall {
            require(ownerUid.isNotBlank()) { "ownerUid is required." }
            val db = requireFirestore()
            val uid = ownerUid.trim()

            val existingAccounts = db.collection(FirestoreSchema.COLLECTION_ACCOUNTS)
                .whereEqualTo(FirestoreSchema.FIELD_OWNER_UID, uid)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.data?.let { FirestoreMappers.accountFromMap(doc.id, it, uid) }
                }

            val existingStatuses = db.collection(FirestoreSchema.COLLECTION_STATUSES)
                .whereEqualTo(FirestoreSchema.FIELD_OWNER_UID, uid)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.data?.let { FirestoreMappers.statusFromMap(doc.id, it, uid) }
                }

            val existingCategories = db.collection(FirestoreSchema.COLLECTION_CATEGORIES)
                .whereEqualTo(FirestoreSchema.FIELD_OWNER_UID, uid)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.data?.let { FirestoreMappers.categoryFromMap(doc.id, it, uid) }
                }

            val plan = WorkspaceSeedPlanner.computeAdditivePlan(
                existingAccounts = existingAccounts,
                existingStatuses = existingStatuses,
                existingCategories = existingCategories
            )
            if (!plan.hasAnyWork) return@runFirestoreCall

            val pendingWrites = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()

            plan.accountsToInsert.forEach { seed ->
                val ref = db.collection(FirestoreSchema.COLLECTION_ACCOUNTS).document()
                pendingWrites += ref to FirestoreMappers.accountToMap(uid, seed.accountName, seed.accountCode)
            }
            plan.statusesToInsert.forEach { statusName ->
                val ref = db.collection(FirestoreSchema.COLLECTION_STATUSES).document()
                pendingWrites += ref to FirestoreMappers.statusToMap(uid, statusName, isDefault = true)
            }
            plan.categoriesToInsert.forEach { categoryName ->
                val ref = db.collection(FirestoreSchema.COLLECTION_CATEGORIES).document()
                pendingWrites += ref to FirestoreMappers.categoryToCreateMap(uid, categoryName)
            }

            pendingWrites.chunked(FIRESTORE_SAFE_BATCH_LIMIT).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (ref, payload) -> batch.set(ref, payload) }
                batch.commit().await()
            }
        }

    override suspend fun addAccount(
        ownerUid: String,
        accountName: String,
        accountCode: String
    ): Result<AccountItem> = runFirestoreCall {
        val db = requireFirestore()
        val payload = FirestoreMappers.accountToMap(ownerUid, accountName, accountCode)
        val ref = db.collection(FirestoreSchema.COLLECTION_ACCOUNTS).document()
        ref.set(payload).await()
        AccountItem(
            id = ref.id,
            ownerUid = ownerUid.trim(),
            accountName = accountName.trim(),
            accountCode = accountCode.trim()
        )
    }

    override suspend fun updateAccount(
        ownerUid: String,
        accountId: String,
        accountName: String,
        accountCode: String
    ): Result<Unit> = runFirestoreCall {
        require(accountId.isNotBlank()) { "accountId is required." }
        val db = requireFirestore()
        val cleanName = accountName.trim().take(FirestoreSchema.MAX_ACCOUNT_NAME_LEN)
        val cleanCode = accountCode.trim().take(FirestoreSchema.MAX_ACCOUNT_CODE_LEN)
        require(cleanName.isNotEmpty() && cleanCode.isNotEmpty()) {
            "Account name and code are required."
        }
        val ref = db.collection(FirestoreSchema.COLLECTION_ACCOUNTS).document(accountId)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            require(snap.exists() && snap.getString(FirestoreSchema.FIELD_OWNER_UID) == ownerUid.trim()) {
                "Account does not belong to current user."
            }
            tx.update(
                ref,
                mapOf(
                    FirestoreSchema.FIELD_ACCOUNT_NAME to cleanName,
                    FirestoreSchema.FIELD_ACCOUNT_CODE to cleanCode
                )
            )
        }.await()
    }

    override suspend fun deleteAccount(ownerUid: String, accountId: String): Result<Unit> =
        runFirestoreCall {
            require(accountId.isNotBlank()) { "accountId is required." }
            val db = requireFirestore()
            val ref = db.collection(FirestoreSchema.COLLECTION_ACCOUNTS).document(accountId)
            db.runTransaction { tx ->
                val snap = tx.get(ref)
                require(snap.exists() && snap.getString(FirestoreSchema.FIELD_OWNER_UID) == ownerUid.trim()) {
                    "Account does not belong to current user."
                }
                tx.delete(ref)
            }.await()
        }

    override suspend fun addCustomStatus(ownerUid: String, statusName: String): Result<StatusItem> =
        runFirestoreCall {
            val db = requireFirestore()
            val payload = FirestoreMappers.statusToMap(ownerUid, statusName, isDefault = false)
            val ref = db.collection(FirestoreSchema.COLLECTION_STATUSES).document()
            ref.set(payload).await()
            StatusItem(
                id = ref.id,
                ownerUid = ownerUid.trim(),
                statusName = statusName.trim(),
                isDefault = false
            )
        }

    override suspend fun deleteCustomStatus(ownerUid: String, statusId: String): Result<Unit> =
        runFirestoreCall {
            require(statusId.isNotBlank()) { "statusId is required." }
            val db = requireFirestore()
            val ref = db.collection(FirestoreSchema.COLLECTION_STATUSES).document(statusId)
            db.runTransaction { tx ->
                val snap = tx.get(ref)
                require(snap.exists() && snap.getString(FirestoreSchema.FIELD_OWNER_UID) == ownerUid.trim()) {
                    "Status does not belong to current user."
                }
                val isDefault = snap.getBoolean(FirestoreSchema.FIELD_IS_DEFAULT) ?: false
                require(!isDefault) { "Default statuses cannot be deleted." }
                tx.delete(ref)
            }.await()
        }

    override suspend fun addCategory(ownerUid: String, categoryName: String): Result<CategoryItem> =
        runFirestoreCall {
            val db = requireFirestore()
            val payload = FirestoreMappers.categoryToCreateMap(ownerUid, categoryName)
            val ref = db.collection(FirestoreSchema.COLLECTION_CATEGORIES).document()
            ref.set(payload).await()
            CategoryItem(
                id = ref.id,
                ownerUid = ownerUid.trim(),
                categoryName = categoryName.trim()
            )
        }

    override suspend fun updateCategory(
        ownerUid: String,
        categoryId: String,
        categoryName: String
    ): Result<Unit> = runFirestoreCall {
        require(categoryId.isNotBlank()) { "categoryId is required." }
        val cleanName = categoryName.trim().take(FirestoreSchema.MAX_CATEGORY_NAME_LEN)
        require(cleanName.isNotEmpty()) { "Category name is required." }
        val db = requireFirestore()
        val ref = db.collection(FirestoreSchema.COLLECTION_CATEGORIES).document(categoryId)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            require(snap.exists() && snap.getString(FirestoreSchema.FIELD_OWNER_UID) == ownerUid.trim()) {
                "Category does not belong to current user."
            }
            tx.update(
                ref,
                mapOf(
                    FirestoreSchema.FIELD_CATEGORY_NAME to cleanName,
                    FirestoreSchema.FIELD_UPDATED_AT to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    override suspend fun deleteCategory(ownerUid: String, categoryId: String): Result<Unit> =
        runFirestoreCall {
            require(categoryId.isNotBlank()) { "categoryId is required." }
            val db = requireFirestore()
            val ref = db.collection(FirestoreSchema.COLLECTION_CATEGORIES).document(categoryId)
            db.runTransaction { tx ->
                val snap = tx.get(ref)
                require(snap.exists() && snap.getString(FirestoreSchema.FIELD_OWNER_UID) == ownerUid.trim()) {
                    "Category does not belong to current user."
                }
                tx.delete(ref)
            }.await()
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
