package com.helal.accomplishmentplan.nativeapp.domain.logic

import com.helal.accomplishmentplan.nativeapp.domain.model.AccountItem
import com.helal.accomplishmentplan.nativeapp.domain.model.CategoryItem
import com.helal.accomplishmentplan.nativeapp.domain.model.DefaultAccountSeed
import com.helal.accomplishmentplan.nativeapp.domain.model.DefaultWorkspaceData
import com.helal.accomplishmentplan.nativeapp.domain.model.StatusItem
import java.util.Locale

data class WorkspaceSeedPlan(
    val accountsToInsert: List<DefaultAccountSeed>,
    val statusesToInsert: List<String>,
    val categoriesToInsert: List<String>
) {
    val hasAnyWork: Boolean
        get() = accountsToInsert.isNotEmpty() ||
            statusesToInsert.isNotEmpty() ||
            categoriesToInsert.isNotEmpty()
}

/**
 * Pure additive-only workspace seeding planner.
 * Computes ONLY missing default records and NEVER marks any user-created account,
 * status, or category for deletion or modification.
 */
object WorkspaceSeedPlanner {

    fun computeAdditivePlan(
        existingAccounts: List<AccountItem>,
        existingStatuses: List<StatusItem>,
        existingCategories: List<CategoryItem>,
        defaultAccounts: List<DefaultAccountSeed> = DefaultWorkspaceData.DEFAULT_ACCOUNTS,
        defaultStatuses: List<String> = DefaultWorkspaceData.DEFAULT_STATUSES,
        defaultCategories: List<String> = DefaultWorkspaceData.DEFAULT_CATEGORIES
    ): WorkspaceSeedPlan {
        val existingAccountNames = existingAccounts
            .map { it.accountName.trim().lowercase(Locale.ROOT) }
            .toSet()

        val accountsToInsert = defaultAccounts.filter { seed ->
            seed.accountName.trim().lowercase(Locale.ROOT) !in existingAccountNames
        }

        val existingStatusNames = existingStatuses
            .map { it.statusName.trim().lowercase(Locale.ROOT) }
            .toSet()

        val statusesToInsert = defaultStatuses.filter { status ->
            status.trim().lowercase(Locale.ROOT) !in existingStatusNames
        }

        val existingCategoryNames = existingCategories
            .map { it.categoryName.trim().lowercase(Locale.ROOT) }
            .toSet()

        val categoriesToInsert = defaultCategories.filter { category ->
            category.trim().lowercase(Locale.ROOT) !in existingCategoryNames
        }

        return WorkspaceSeedPlan(
            accountsToInsert = accountsToInsert,
            statusesToInsert = statusesToInsert,
            categoriesToInsert = categoriesToInsert
        )
    }
}
