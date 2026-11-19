package com.helal.accomplishmentplan.nativeapp.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.helal.accomplishmentplan.nativeapp.R
import com.helal.accomplishmentplan.nativeapp.ui.components.BlueGradientHeader
import com.helal.accomplishmentplan.nativeapp.ui.components.InfoBannerCard
import com.helal.accomplishmentplan.nativeapp.ui.components.SummaryGlassChip
import com.helal.accomplishmentplan.nativeapp.ui.theme.LocalExtendedColors
import com.helal.accomplishmentplan.nativeapp.ui.theme.PureWhite

/**
 * Signed-in navigation shell for Phases 0–2.
 * Phase 3 (Dashboard UI), Phase 4 (Task Editor), Phase 5 (Master Data Managers),
 * and Phase 6 (Export Suite) will replace these foundation placeholders cleanly.
 */
@Composable
fun FoundationWorkspaceShell(
    userDisplayName: String,
    userEmail: String,
    selectedMonthLabel: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry = navController.currentBackStackEntryAsState().value
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                WorkspaceTab.entries.forEach { tab ->
                    val selected = when (tab) {
                        WorkspaceTab.DASHBOARD -> currentDestination?.hasRoute<DashboardRoute>() == true
                        WorkspaceTab.MANAGE -> currentDestination?.hasRoute<ManageRoute>() == true
                        WorkspaceTab.SETTINGS -> currentDestination?.hasRoute<SettingsRoute>() == true
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(tab.labelRes)
                            )
                        },
                        label = { Text(text = stringResource(tab.labelRes)) },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable<DashboardRoute> {
                FoundationTabPage(
                    greeting = "Hello, $userDisplayName",
                    subtitle = userEmail,
                    selectedMonthLabel = selectedMonthLabel,
                    cardTitle = stringResource(R.string.foundation_dashboard_ready_title),
                    cardBody = stringResource(R.string.foundation_dashboard_ready_body),
                    icon = Icons.Filled.CloudDone
                )
            }
            composable<ManageRoute> {
                FoundationTabPage(
                    greeting = stringResource(R.string.nav_manage),
                    subtitle = userEmail,
                    selectedMonthLabel = selectedMonthLabel,
                    cardTitle = stringResource(R.string.foundation_manage_title),
                    cardBody = stringResource(R.string.foundation_manage_body),
                    icon = Icons.Filled.Storage
                )
            }
            composable<SettingsRoute> {
                FoundationSettingsPage(
                    userDisplayName = userDisplayName,
                    userEmail = userEmail,
                    selectedMonthLabel = selectedMonthLabel,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun FoundationTabPage(
    greeting: String,
    subtitle: String,
    selectedMonthLabel: String,
    cardTitle: String,
    cardBody: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val extended = LocalExtendedColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BlueGradientHeader {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = PureWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = PureWhite.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryGlassChip(
                    label = "Active Month",
                    value = selectedMonthLabel,
                    modifier = Modifier.weight(1f)
                )
                SummaryGlassChip(
                    label = "Milestone",
                    value = "Phases 0–2",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoBannerCard(
                title = cardTitle,
                message = cardBody,
                icon = icon,
                containerColor = extended.successContainer,
                contentColor = extended.success
            )
        }
    }
}

@Composable
private fun FoundationSettingsPage(
    userDisplayName: String,
    userEmail: String,
    selectedMonthLabel: String,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BlueGradientHeader {
            Text(
                text = stringResource(R.string.foundation_settings_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = PureWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.bodyMedium,
                color = PureWhite.copy(alpha = 0.85f)
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.foundation_signed_in_as),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = userDisplayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Selected Month: $selectedMonthLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("settings_sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = stringResource(R.string.cd_sign_out)
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(text = stringResource(R.string.auth_action_sign_out))
            }
        }
    }
}
