package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.model.TaskItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Folder
import com.example.ui.components.CategoryManagerDialog
import androidx.compose.material.icons.filled.Book
import com.example.ui.components.AccountManagerDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.MonthSelectorDialog
import com.example.ui.components.StatusManagerDialog
import com.example.ui.components.TaskCard
import com.example.ui.theme.AppPrimaryBlue
import com.example.ui.theme.FooterTextColor
import com.example.viewmodel.TaskViewModel

@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tasks by viewModel.filteredTasks.collectAsState(initial = emptyList())
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val availableStatuses by viewModel.availableStatusNames.collectAsState()
    val totalCount by viewModel.totalItemsCount.collectAsState(initial = 0)
    val pendingCount by viewModel.pendingItemsCount.collectAsState(initial = 0)
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchOpen by viewModel.isSearchOpen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val userAccounts by viewModel.accounts.collectAsState(initial = emptyList())
    val customStatuses by viewModel.customStatuses.collectAsState(initial = emptyList())

    val statusCounts = remember(tasks, availableStatuses) {
        availableStatuses.map { st ->
            st to tasks.count { it.status.equals(st, ignoreCase = true) }
        }
    }

    var showMonthDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
        var showAccountDialog by remember { mutableStateOf(false) }
var showStatusDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<TaskItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppPrimaryBlue)
                    .statusBarsPadding()
            ) {
                // Header Bar matching reference wireframe exactly
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Gear Icon with settings & user menu
                    Box {
                        IconButton(
                            onClick = { showSettingsMenu = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false }
                        ) {
                            currentUser?.let { user ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Signed in as:",
                                                fontSize = 11.sp,
                                                color = Color(0xFF6B7280)
                                            )
                                            Text(
                                                text = user.email ?: "Signed in user",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF111827)
                                            )
                                            Text(
                                                text = "UID: " + user.uid.take(16) + "...",
                                                fontSize = 10.5.sp,
                                                color = Color(0xFF9CA3AF)
                                            )
                                        }
                                    },
                                    onClick = {}
                                )
                                Divider()
                            }

                            DropdownMenuItem(
                                text = { Text("Master Account List", fontSize = 13.5.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Book, contentDescription = null, tint = AppPrimaryBlue, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    showAccountDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Category Management", fontSize = 13.5.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = AppPrimaryBlue, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    showCategoryDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Dynamic Status Settings", fontSize = 13.5.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    showStatusDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Sign Out", fontSize = 13.5.sp, color = Color(0xFFDC2626)) },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    viewModel.signOut(context)
                                }
                            )
                        }
                    }

                    // Center Month Selector: 📅 Nov-2026 ▾
                    Row(
                        modifier = Modifier
                            .clickable { showMonthDialog = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("month_selector_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Calendar",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedMonth,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Month",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Right Actions: Export (⤓) and Search (🔍)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showExportDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("export_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export Data",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleSearch() },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("search_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Optional Search Bar
                if (isSearchOpen) {
                    Surface(
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search by description, unit, status...", fontSize = 13.sp) },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppPrimaryBlue,
                                unfocusedBorderColor = Color(0xFFD1D5DB)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("search_input_field")
                        )
                    }
                }

                // Subheader Count Bar: Total Items: X | Status1: Y | Status2: Z ...
                Surface(
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Items: ",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF374151)
                                )
                            )
                            Text(
                                text = "$totalCount",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                            )

                            if (statusCounts.isNotEmpty()) {
                                statusCounts.forEach { (stName, count) ->
                                    Text(
                                        text = "  |  $stName: ",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.5.sp,
                                            color = Color(0xFF374151)
                                        )
                                    )
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF111827)
                                        )
                                    )
                                }
                            } else {
                                Text(
                                    text = "  |  Pending: ",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF374151)
                                    )
                                )
                                Text(
                                    text = "$pendingCount",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827)
                                    )
                                )
                            }
                        }
                        Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openNewTask() },
                containerColor = AppPrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .testTag("add_task_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        bottomBar = {
            // Footer matching reference wireframe
            Surface(
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Developer by Md Al Helal Sarkar",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = FooterTextColor
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("footer_label")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (tasks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No tasks found for $selectedMonth",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280))
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tasks, key = { index, it -> it.id.ifBlank { "${it.taskNumber}-$index" } }) { _, task ->
                        TaskCard(
                            task = task,
                            displayIndex = task.taskNumber,
                            availableStatuses = availableStatuses,
                            onCardClick = { viewModel.openUpdateTask(task) },
                            onStatusChange = { newStatus ->
                                if (task.id.isNotBlank()) {
                                    viewModel.quickUpdateStatus(task.id, newStatus)
                                }
                            },
                            onDeleteClick = { taskToDelete = task }
                        )
                    }
                }
            }
        }
    }

    if (showMonthDialog) {
        MonthSelectorDialog(
            selectedMonth = selectedMonth,
            availableMonths = availableMonths,
            onMonthSelected = { viewModel.setSelectedMonth(it) },
            onAddNewMonth = { viewModel.addMonth(it) },
            onDismissRequest = { showMonthDialog = false }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            currentMonth = selectedMonth,
            tasks = tasks,
            onDismissRequest = { showExportDialog = false }
        )
    }

    if (showAccountDialog) {
        AccountManagerDialog(
            accounts = userAccounts,
            onAddAccount = { name, code, cb -> viewModel.addAccount(name, code, cb) },
            onUpdateAccount = { id, name, code, cb -> viewModel.updateAccount(id, name, code, cb) },
            onDeleteAccount = { id, cb -> viewModel.deleteAccount(id, cb) },
            onDismissRequest = { showAccountDialog = false }
        )
    }
    if (showStatusDialog) {
        StatusManagerDialog(
            customStatuses = customStatuses,
            onAddStatus = { viewModel.addCustomStatus(it) },
            onRemoveStatus = { viewModel.removeCustomStatus(it) },
            onDismissRequest = { showStatusDialog = false }
        )
    }

    if (showCategoryDialog) {
        CategoryManagerDialog(
            categories = categories,
            onAddCategory = { name -> viewModel.addCategory(name) },
            onEditCategory = { id, name -> viewModel.updateCategory(id, name) },
            onDeleteCategory = { id -> viewModel.deleteCategory(id) },
            onDismissRequest = { showCategoryDialog = false }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = {
                Text(
                    text = "Delete Task",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this task? This action cannot be undone.",
                    fontSize = 14.sp,
                    color = Color(0xFF374151)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val taskId = task.id
                        taskToDelete = null
                        if (taskId.isNotBlank()) {
                            viewModel.deleteTask(taskId) { success, msg ->
                                if (success) {
                                    Toast.makeText(context, "Task deleted successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, msg ?: "Failed to delete task", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { taskToDelete = null }
                ) {
                    Text("Cancel", color = Color(0xFF4B5563))
                }
            }
        )
    }
}
