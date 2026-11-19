package com.helal.accomplishmentplan.nativeapp.domain.logic

import com.helal.accomplishmentplan.nativeapp.domain.model.AccountItem
import com.helal.accomplishmentplan.nativeapp.domain.model.CategoryItem
import com.helal.accomplishmentplan.nativeapp.domain.model.DefaultAccountSeed
import com.helal.accomplishmentplan.nativeapp.domain.model.StatusItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceSeedPlannerTest {

    @Test
    fun `additive seeding inserts only missing defaults and never removes user custom items`() {
        val existingAccounts = listOf(
            AccountItem(id = "a1", ownerUid = "u1", accountName = "Land", accountCode = "1200911"),
            AccountItem(id = "a_custom", ownerUid = "u1", accountName = "My Custom Vendor Ledger", accountCode = "ACC-CUS99")
        )
        val existingStatuses = listOf(
            StatusItem(id = "s1", ownerUid = "u1", statusName = "Done", isDefault = true),
            StatusItem(id = "s_custom", ownerUid = "u1", statusName = "Waiting MD Approval", isDefault = false)
        )
        val existingCategories = listOf(
            CategoryItem(id = "c1", ownerUid = "u1", categoryName = "Materials"),
            CategoryItem(id = "c_custom", ownerUid = "u1", categoryName = "Solar Expansion")
        )

        val plan = WorkspaceSeedPlanner.computeAdditivePlan(
            existingAccounts = existingAccounts,
            existingStatuses = existingStatuses,
            existingCategories = existingCategories,
            defaultAccounts = listOf(
                DefaultAccountSeed("Land", "1200911"),
                DefaultAccountSeed("Buildings", "1200921")
            ),
            defaultStatuses = listOf("Done", "Pending", "In Progress"),
            defaultCategories = listOf("Materials", "Electrical")
        )

        assertTrue(plan.hasAnyWork)
        assertEquals(listOf("Buildings"), plan.accountsToInsert.map { it.accountName })
        assertEquals(listOf("Pending", "In Progress"), plan.statusesToInsert)
        assertEquals(listOf("Electrical"), plan.categoriesToInsert)
    }

    @Test
    fun `seeding plan is empty when all defaults already exist case-insensitively`() {
        val plan = WorkspaceSeedPlanner.computeAdditivePlan(
            existingAccounts = listOf(AccountItem(id = "a1", ownerUid = "u1", accountName = "land", accountCode = "1200911")),
            existingStatuses = listOf(StatusItem(id = "s1", ownerUid = "u1", statusName = "DONE", isDefault = true)),
            existingCategories = listOf(CategoryItem(id = "c1", ownerUid = "u1", categoryName = "materials")),
            defaultAccounts = listOf(DefaultAccountSeed("Land", "1200911")),
            defaultStatuses = listOf("Done"),
            defaultCategories = listOf("Materials")
        )

        assertFalse(plan.hasAnyWork)
    }
}
