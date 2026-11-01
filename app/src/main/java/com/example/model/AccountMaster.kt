package com.example.model

object AccountMaster {
    val accounts = listOf(
        "Civil Maintenance" to "ACC-CM101",
        "Air Conditioning" to "ACC-AC205",
        "Commercial Seal" to "ACC-CS309",
        "Electrical Spares" to "ACC-EL412",
        "Factory Logistics" to "ACC-FL508",
        "Safety & Security" to "ACC-SS602",
        "Machinery Supplies" to "ACC-MS714",
        "Admin & Canteen" to "ACC-AC820"
    )

    fun getCodeForAccount(name: String): String {
        return accounts.firstOrNull { it.first.equals(name, ignoreCase = true) }?.second
            ?: "ACC-${name.take(3).uppercase()}001"
    }

    val defaultUnits = listOf("Unit-1", "Unit-2", "Unit-3")
    val defaultPayments = listOf("Cash", "Bank", "Cheque")
    val defaultStatuses = listOf("Draft Ready", "Done", "Advance Ready", "Pending", "In Progress", "Cancelled")
    val defaultMonths = listOf("Nov-2026", "Dec-2026", "Jan-2027", "Oct-2026")
}
