package com.example.data

import android.util.Log
import com.example.model.AccountItem
import com.example.model.AccountMaster
import com.example.model.CategoryItem
import com.example.model.Priority
import com.example.model.StatusItem
import com.example.model.TaskItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TaskRepository(
    private val db: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private val tag = "TaskRepository"

    fun currentUserId(): String? = auth.currentUser?.uid

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to perform this operation.")
    }

    // Observe tasks for current user and selected month
    fun observeTasks(month: String): Flow<List<TaskItem>> {
        val uid = currentUserId() ?: return flowOf(emptyList())
        return db.collection("Tasks")
            .whereEqualTo("ownerUid", uid)
            .whereEqualTo("monthYear", month)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(TaskItem::class.java)?.copy(id = doc.id)
                }.sortedBy { it.taskNumber }
            }
            .catch { e ->
                Log.e(tag, "Error observing tasks for month $month: ${e.message}", e)
                emit(emptyList())
            }
    }

    // Observe all tasks for current user
    fun observeAllTasks(): Flow<List<TaskItem>> {
        val uid = currentUserId() ?: return flowOf(emptyList())
        return db.collection("Tasks")
            .whereEqualTo("ownerUid", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(TaskItem::class.java)?.copy(id = doc.id)
                }.sortedBy { it.taskNumber }
            }
            .catch { e ->
                Log.e(tag, "Error observing all tasks: ${e.message}", e)
                emit(emptyList())
            }
    }

    // Observe user master accounts
    fun observeAccounts(): Flow<List<AccountItem>> {
        val uid = currentUserId() ?: return flowOf(emptyList())
        return db.collection("Account_List")
            .whereEqualTo("ownerUid", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(AccountItem::class.java)?.copy(id = doc.id)
                }
            }
            .catch { e ->
                Log.e(tag, "Error observing accounts: ${e.message}", e)
                emit(emptyList())
            }
    }

    // Observe user custom statuses
    fun observeStatuses(): Flow<List<StatusItem>> {
        val uid = currentUserId() ?: return flowOf(emptyList())
        return db.collection("Status_List")
            .whereEqualTo("ownerUid", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(StatusItem::class.java)?.copy(id = doc.id)
                }
            }
            .catch { e ->
                Log.e(tag, "Error observing statuses: ${e.message}", e)
                emit(emptyList())
            }
    }

    // Observe user categories
    fun observeCategories(): Flow<List<CategoryItem>> {
        val uid = currentUserId() ?: return flowOf(emptyList())
        return db.collection("Category_List")
            .whereEqualTo("ownerUid", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CategoryItem::class.java)?.copy(id = doc.id)
                }.sortedBy { it.categoryName.lowercase() }
            }
            .catch { e ->
                Log.e(tag, "Error observing categories: ${e.message}", e)
                emit(emptyList())
            }
    }

    suspend fun addTask(task: TaskItem): Result<String> {
        return try {
            val uid = requireUserId()
            val payload = task.toMap().toMutableMap().apply {
                put("ownerUid", uid)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            val docRef = db.collection("Tasks").add(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(tag, "Failed to add task: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: TaskItem): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(task.id.isNotBlank()) { "Task ID must not be blank" }
            val payload = task.toMap().toMutableMap().apply {
                put("ownerUid", uid)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("Tasks").document(task.id).update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update task ${task.id}: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun quickUpdateStatus(taskId: String, newStatus: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("Tasks").document(taskId).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to quick update status: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun addAccount(name: String, code: String): Result<String> {
        return try {
            val uid = requireUserId()
            val isOfficial = AccountMaster.accounts.any {
                it.first.trim().equals(name.trim(), ignoreCase = true) &&
                it.second.trim() == code.trim()
            }
            if (!isOfficial) {
                return Result.failure(IllegalArgumentException("Only accounts from the official master list can be added."))
            }
            val docRef = db.collection("Account_List").add(
                mapOf(
                    "ownerUid" to uid,
                    "accountName" to name.trim(),
                    "accountCode" to code.trim(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAccount(accountId: String, name: String, code: String): Result<Unit> {
        return try {
            val isOfficial = AccountMaster.accounts.any {
                it.first.trim().equals(name.trim(), ignoreCase = true) &&
                it.second.trim() == code.trim()
            }
            if (!isOfficial) {
                return Result.failure(IllegalArgumentException("Only accounts from the official master list can be saved."))
            }
            db.collection("Account_List").document(accountId).update(
                mapOf(
                    "accountName" to name.trim(),
                    "accountCode" to code.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addStatus(statusName: String): Result<String> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("Status_List").add(
                mapOf(
                    "ownerUid" to uid,
                    "statusName" to statusName,
                    "isDefault" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeStatus(statusId: String): Result<Unit> {
        return try {
            db.collection("Status_List").document(statusId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCategory(categoryName: String): Result<String> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("Category_List").add(
                mapOf(
                    "ownerUid" to uid,
                    "categoryName" to categoryName.trim(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCategory(categoryId: String, newName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("Category_List").document(categoryId).update(
                mapOf(
                    "categoryName" to newName.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            db.collection("Category_List").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class AccountSyncSummary(
        val totalOfficial: Int,
        val retainedCount: Int,
        val addedCount: Int,
        val removedCount: Int
    )

    suspend fun cleanAndSyncOfficialAccounts(uid: String): AccountSyncSummary {
        val existingAccountsSnap = db.collection("Account_List")
            .whereEqualTo("ownerUid", uid)
            .get()
            .await()

        // 1. Safe backup representation
        val backupDocs = existingAccountsSnap.documents.map {
            mapOf(
                "id" to it.id,
                "accountName" to (it.getString("accountName") ?: ""),
                "accountCode" to (it.getString("accountCode") ?: ""),
                "ownerUid" to (it.getString("ownerUid") ?: uid)
            )
        }
        Log.i(tag, "Account_List backup for $uid: total ${backupDocs.size} existing records backed up")

        // Exact match set: (accountName.trim(), accountCode.trim())
        val officialCombos = AccountMaster.accounts.map { (name, code) ->
            name.trim() to code.trim()
        }.toSet()

        val seenOfficial = mutableSetOf<Pair<String, String>>()
        var retainedCount = 0
        var removedCount = 0

        // 2. Remove all previous accounts not in the official list, and deduplicate
        for (doc in existingAccountsSnap.documents) {
            val name = doc.getString("accountName")?.trim() ?: ""
            val code = doc.getString("accountCode")?.trim() ?: ""
            val combo = name to code

            if (officialCombos.contains(combo)) {
                if (seenOfficial.contains(combo)) {
                    // Duplicate record of official account -> remove duplicate
                    db.collection("Account_List").document(doc.id).delete().await()
                    removedCount++
                } else {
                    seenOfficial.add(combo)
                    retainedCount++
                }
            } else {
                // Not in official list -> delete old / unlisted account
                db.collection("Account_List").document(doc.id).delete().await()
                removedCount++
            }
        }

        // 3. Add any missing official accounts
        var addedCount = 0
        for ((name, code) in AccountMaster.accounts) {
            val combo = name.trim() to code.trim()
            if (!seenOfficial.contains(combo)) {
                db.collection("Account_List").add(
                    mapOf(
                        "ownerUid" to uid,
                        "accountName" to name,
                        "accountCode" to code,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                addedCount++
            }
        }

        Log.i(tag, "Account cleanup & sync for $uid: Retained=$retainedCount, Added=$addedCount, Removed=$removedCount, Total=${AccountMaster.accounts.size}")
        return AccountSyncSummary(
            totalOfficial = AccountMaster.accounts.size,
            retainedCount = retainedCount,
            addedCount = addedCount,
            removedCount = removedCount
        )
    }

    // Initialize fresh workspace with default accounts & statuses, and seed template tasks if desired
    suspend fun initializeUserWorkspace(seedSampleTasks: Boolean = true) {
        val uid = requireUserId()
        try {
            // Clean, replace and synchronize official accounts
            val syncSummary = cleanAndSyncOfficialAccounts(uid)

            // Fetch existing statuses
            val existingStatusesSnap = db.collection("Status_List")
                .whereEqualTo("ownerUid", uid)
                .get()
                .await()

            val existingStatusNames = existingStatusesSnap.documents.mapNotNull {
                it.getString("statusName")?.lowercase()?.trim()
            }.toSet()

            // Seed missing default statuses
            AccountMaster.defaultStatuses.forEach { statusName ->
                if (!existingStatusNames.contains(statusName.lowercase().trim())) {
                    db.collection("Status_List").add(
                        mapOf(
                            "ownerUid" to uid,
                            "statusName" to statusName,
                            "isDefault" to true,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            // Fetch existing categories
            val existingCategoriesSnap = db.collection("Category_List")
                .whereEqualTo("ownerUid", uid)
                .get()
                .await()

            val existingCategoryNames = existingCategoriesSnap.documents.mapNotNull {
                it.getString("categoryName")?.lowercase()?.trim()
            }.toSet()

            // Seed missing default categories
            AccountMaster.defaultCategories.forEach { catName ->
                if (!existingCategoryNames.contains(catName.lowercase().trim())) {
                    db.collection("Category_List").add(
                        mapOf(
                            "ownerUid" to uid,
                            "categoryName" to catName,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            if (existingAccountsSnap.isEmpty && seedSampleTasks) {
                seedTemplateTasks(uid)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error initializing user workspace: ${e.message}", e)
        }
    }

    private suspend fun seedTemplateTasks(uid: String) {
        val sampleTasks = listOf(
            TaskItem(
                ownerUid = uid,
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
                remarks = "Purchase orders submitted to procurement desk for bricks, cement and joint filler.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 2,
                description = "Air Conditioner maintenance...",
                amount = "125,500",
                paymentMode = "Bank",
                unit = "Unit-1",
                priority = "Low",
                status = "Done",
                expectedDate = "10 Nov 2026",
                accountName = "Air Conditioning",
                accountCode = "ACC-AC205",
                remarks = "Annual HVAC gas charging, coil cleaning, and thermostat inspection completed.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 3,
                description = "Raw Material transport logistics...",
                amount = "45,000",
                paymentMode = "Bank",
                unit = "Unit-1",
                priority = "Med",
                status = "Advance Ready",
                expectedDate = "19 Nov 2026",
                accountName = "Factory Logistics",
                accountCode = "ACC-FL508",
                remarks = "Truck freight advance for 3 containers arriving at Chittagong port depot.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 4,
                description = "Fire Extinguisher refill & inspection...",
                amount = "18,500",
                paymentMode = "Cash",
                unit = "Unit-2",
                priority = "High",
                status = "Pending",
                expectedDate = "22 Nov 2026",
                accountName = "Safety & Security",
                accountCode = "ACC-SS602",
                remarks = "Mandatory safety compliance testing for CO2 and foam cylinders in Unit-2.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 5,
                description = "Generator fuel and filter replacement...",
                amount = "82,000",
                paymentMode = "Bank",
                unit = "Unit-1",
                priority = "High",
                status = "Draft Ready",
                expectedDate = "15 Nov 2026",
                accountName = "Machinery Supplies",
                accountCode = "ACC-MS714",
                remarks = "Diesel 500 liters stock refilling plus bypass lubricating oil filters.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 6,
                description = "Packaging cartons bundle delivery...",
                amount = "34,200",
                paymentMode = "Cash",
                unit = "Unit-2",
                priority = "Med",
                status = "Done",
                expectedDate = "08 Nov 2026",
                accountName = "Commercial Seal",
                accountCode = "ACC-CS309",
                remarks = "1,000 corrugated 5-ply cartons received and stacked in warehouse.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 7,
                description = "Boiler room steam valve inspection...",
                amount = "67,000",
                paymentMode = "Bank",
                unit = "Unit-1",
                priority = "High",
                status = "In Progress",
                expectedDate = "20 Nov 2026",
                accountName = "Machinery Supplies",
                accountCode = "ACC-MS714",
                remarks = "Pressure safety valves calibration and thermal insulation wrap replacement.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 8,
                description = "Canteen weekly grocery supply...",
                amount = "28,400",
                paymentMode = "Cash",
                unit = "Unit-2",
                priority = "Low",
                status = "Done",
                expectedDate = "05 Nov 2026",
                accountName = "Admin & Canteen",
                accountCode = "ACC-AC820",
                remarks = "Weekly provisions for worker midday meals and clean drinking water dispenser jars.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 9,
                description = "Security uniforms & safety gears...",
                amount = "19,000",
                paymentMode = "Cash",
                unit = "Unit-1",
                priority = "Low",
                status = "Draft Ready",
                expectedDate = "25 Nov 2026",
                accountName = "Safety & Security",
                accountCode = "ACC-SS602",
                remarks = "High-visibility jackets, boots and flashlight sets for night shift guards.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 10,
                description = "Hydraulic oil drums for press machines...",
                amount = "95,000",
                paymentMode = "Bank",
                unit = "Unit-2",
                priority = "High",
                status = "Advance Ready",
                expectedDate = "21 Nov 2026",
                accountName = "Machinery Supplies",
                accountCode = "ACC-MS714",
                remarks = "ISO VG 68 heavy duty hydraulic lubricant drums for hydraulic press unit 2.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 11,
                description = "IT server room UPS battery swap...",
                amount = "58,000",
                paymentMode = "Bank",
                unit = "Unit-1",
                priority = "Med",
                status = "Pending",
                expectedDate = "27 Nov 2026",
                accountName = "Electrical Spares",
                accountCode = "ACC-EL412",
                remarks = "Replacing 16 sealed lead-acid backup battery modules for main ERP server.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 12,
                description = "Waste water treatment chemicals...",
                amount = "76,500",
                paymentMode = "Bank",
                unit = "Unit-2",
                priority = "High",
                status = "In Progress",
                expectedDate = "18 Nov 2026",
                accountName = "Civil Maintenance",
                accountCode = "ACC-CM101",
                remarks = "ETP plant coagulant, polymer and pH neutralizer chemicals supply.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 13,
                description = "Bolt Seal for Commercial...",
                amount = "22,000",
                paymentMode = "Cash",
                unit = "Unit-2",
                priority = "Med",
                status = "Advance Ready",
                expectedDate = "17 Nov 2026",
                accountName = "Commercial Seal",
                accountCode = "ACC-CS309",
                remarks = "Container high security ISO 17712 certified bolt seals for export consignments.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 14,
                description = "Office stationery & printer cartridges...",
                amount = "8,900",
                paymentMode = "Cash",
                unit = "Unit-1",
                priority = "Low",
                status = "Done",
                expectedDate = "03 Nov 2026",
                accountName = "Admin & Canteen",
                accountCode = "ACC-AC820",
                remarks = "A4 paper reams, toner cartridges, and file folders for accounts department.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 15,
                description = "Forklift tire replacement & alignment...",
                amount = "42,000",
                paymentMode = "Bank",
                unit = "Unit-2",
                priority = "Med",
                status = "Draft Ready",
                expectedDate = "24 Nov 2026",
                accountName = "Factory Logistics",
                accountCode = "ACC-FL508",
                remarks = "Solid rubber front and rear tires for 3-ton warehouse forklift unit.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 16,
                description = "Water chiller cooling tower cleaning...",
                amount = "31,500",
                paymentMode = "Cash",
                unit = "Unit-1",
                priority = "Med",
                status = "Done",
                expectedDate = "12 Nov 2026",
                accountName = "Air Conditioning",
                accountCode = "ACC-AC205",
                remarks = "Descaling of cooling tower fins and algae treatment completed.",
                monthYear = "Nov-2026"
            ),
            TaskItem(
                ownerUid = uid,
                taskNumber = 17,
                description = "Laboratory chemical testing reagents...",
                amount = "53,000",
                paymentMode = "Bank",
                unit = "Unit-2",
                priority = "High",
                status = "Draft Ready",
                expectedDate = "29 Nov 2026",
                accountName = "Civil Maintenance",
                accountCode = "ACC-CM101",
                remarks = "Analytical testing kits and titration indicators for QC department.",
                monthYear = "Nov-2026"
            )
        )

        sampleTasks.forEach { task ->
            addTask(task)
        }
    }
}
