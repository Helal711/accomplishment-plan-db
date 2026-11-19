package com.helal.accomplishmentplan.nativeapp.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.helal.accomplishmentplan.nativeapp.R
import kotlinx.serialization.Serializable

/**
 * Type-safe Compose Navigation destinations for Accomplishment Plan DB Native.
 */
@Serializable
data object AuthRoute

@Serializable
data object DashboardRoute

@Serializable
data object ManageRoute

@Serializable
data object SettingsRoute

enum class WorkspaceTab(
    val route: Any,
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD(
        route = DashboardRoute,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_tab_dashboard"
    ),
    MANAGE(
        route = ManageRoute,
        labelRes = R.string.nav_manage,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_tab_manage"
    ),
    SETTINGS(
        route = SettingsRoute,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
}
