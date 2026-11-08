package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import com.example.model.AccountItem
import com.example.model.AccountMaster
import com.example.model.Priority
import com.example.model.TaskItem
import com.example.ui.theme.AppBorderGray
import com.example.ui.theme.AppPrimaryBlue
import com.example.ui.theme.FooterTextColor
import com.example.ui.theme.PriorityHighRed
import com.example.ui.theme.PriorityLowGreen
import com.example.ui.theme.PriorityMedYellow
import java.util.Calendar

@Composable
fun UpdateTaskScreen(
    task: TaskItem,
    isNewTask: Boolean = false,
    availableStatuses: List<String>,
    userAccounts: List<AccountItem> = emptyList(),
    availableCategories: List<String> = AccountMaster.defaultCategories,
    onSaveTask: (TaskItem) -> Unit,
    onNavigateBack: () -> Unit,
    onDeleteTask: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Handle system back navigation
    BackHandler {
        onNavigateBack()
    }

    // Sort categories A-Z case-insensitively, ensuring task.category is included if non-blank
    val displayCategories = remember(availableCategories, task.category) {
        val set = availableCategories.toMutableSet()
        if (task.category.isNotBlank()) {
            set.add(task.category)
        }
        set.filter { it.isNotBlank() }.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    // Sort statuses A-Z case-insensitively, ensuring task.status is included
    val displayStatuses = remember(availableStatuses, task.status) {
        val set = availableStatuses.toMutableSet()
        if (task.status.isNotBlank()) {
            set.add(task.status)
        }
        set.filter { it.isNotBlank() }.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    val combinedAccounts = remember(userAccounts) {
        val predefined = AccountMaster.accounts
        val userCustom = userAccounts.map { it.accountName to it.accountCode }
        val customNames = userCustom.map { it.first.lowercase().trim() }.toSet()
        val remainingPredefined = predefined.filter { !customNames.contains(it.first.lowercase().trim()) }
        userCustom + remainingPredefined
    }

    var description by remember { mutableStateOf(task.description) }
    var amount by remember { mutableStateOf(task.amount) }
    var selectedPriority by remember { mutableStateOf(Priority.fromLabel(task.priority)) }
    var selectedUnit by remember { mutableStateOf(task.unit) }
    var selectedPayment by remember { mutableStateOf(task.paymentMode) }
    var expectedDate by remember { mutableStateOf(task.expectedDate) }
    val defaultTargetAccount = "M_Supply expenses(Other)"
    val defaultTargetCode = "4532154"
    var selectedAccountName by remember {
        val initialName = if (isNewTask) {
            if (combinedAccounts.any { it.first.equals(defaultTargetAccount, ignoreCase = true) }) {
                defaultTargetAccount
            } else {
                combinedAccounts.firstOrNull()?.first ?: defaultTargetAccount
            }
        } else {
            task.accountName.ifBlank { defaultTargetAccount }
        }
        mutableStateOf(initialName)
    }
    var accountCode by remember {
        val initialCode = if (isNewTask) {
            val code = AccountMaster.getCodeForAccount(selectedAccountName, combinedAccounts)
            if (code.isNotBlank()) code else defaultTargetCode
        } else {
            task.accountCode.ifBlank {
                AccountMaster.getCodeForAccount(
                    selectedAccountName,
                    combinedAccounts
                )
            }
        }
        mutableStateOf(initialCode)
    }
    var selectedStatus by remember { mutableStateOf(task.status) }
    var selectedCategory by remember { mutableStateOf(task.category) }
    var remarks by remember { mutableStateOf(task.remarks) }

    // Dropdown open states
    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var paymentMenuExpanded by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            // Blue Header: ← Update Task #1 or ← Add New Task with Delete action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppPrimaryBlue)
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = if (isNewTask) "Add New Task" else "Update Task #${task.taskNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (!isNewTask) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("delete_task_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Task",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Action Button & Footer matching reference wireframe
            Surface(
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            if (description.isBlank()) {
                                Toast.makeText(context, "Please enter a description", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val accountExists = combinedAccounts.any { it.first.equals(selectedAccountName, ignoreCase = true) }
                            if (!accountExists && combinedAccounts.isNotEmpty()) {
                                Toast.makeText(context, "Selected account \"$selectedAccountName\" does not exist in Account List. Please choose a valid account.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            val validatedCode = AccountMaster.getCodeForAccount(selectedAccountName, combinedAccounts)
                            val finalAccountCode = if (accountCode.isNotBlank()) accountCode else validatedCode
                            val updated = task.copy(
                                description = description.trim(),
                                amount = amount.trim(),
                                priority = selectedPriority.label,
                                unit = selectedUnit,
                                paymentMode = selectedPayment,
                                expectedDate = expectedDate,
                                accountName = selectedAccountName,
                                accountCode = finalAccountCode,
                                category = selectedCategory.trim(),
                                status = selectedStatus,
                                remarks = remarks.trim()
                            )
                            onSaveTask(updated)
                            Toast.makeText(
                                context,
                                if (isNewTask) "Task Created Successfully" else "Task Updated Successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("button_update_data")
                    ) {
                        Text(
                            text = if (isNewTask) "Save Task" else "Update Data",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Developer by Md Al Helal Sarkar",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = FooterTextColor
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .testTag("footer_label")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Description
            FormFieldLabel(text = "Description")
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Civil Maintenance material...", fontSize = 13.5.sp, color = Color(0xFF9CA3AF)) },
                singleLine = true,
                colors = defaultTextFieldColors(),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_description")
            )

            // 2. Row: Amount & Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Amount
                Column(modifier = Modifier.weight(1f)) {
                    FormFieldLabel(text = "Amount")
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        placeholder = { Text("00,000", fontSize = 13.5.sp, color = Color(0xFF9CA3AF)) },
                        singleLine = true,
                        colors = defaultTextFieldColors(),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_amount")
                    )
                }

                // Priority Dropdown
                Column(modifier = Modifier.weight(1f)) {
                    FormFieldLabel(text = "Priority")
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedFieldBox(
                            onClick = { priorityMenuExpanded = true },
                            modifier = Modifier.testTag("dropdown_priority")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val dotColor = when (selectedPriority) {
                                        Priority.HIGH -> PriorityHighRed
                                        Priority.MED -> PriorityMedYellow
                                        Priority.LOW -> PriorityLowGreen
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .background(dotColor, CircleShape)
                                    )
                                    Text(
                                        text = selectedPriority.label,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1F2937)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = priorityMenuExpanded,
                            onDismissRequest = { priorityMenuExpanded = false }
                        ) {
                            Priority.entries.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val dotColor = when (p) {
                                                Priority.HIGH -> PriorityHighRed
                                                Priority.MED -> PriorityMedYellow
                                                Priority.LOW -> PriorityLowGreen
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .background(dotColor, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(p.label, fontSize = 13.5.sp)
                                        }
                                    },
                                    onClick = {
                                        selectedPriority = p
                                        priorityMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Row: Factory Unit & Payment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Factory Unit
                Column(modifier = Modifier.weight(1f)) {
                    FormFieldLabel(text = "Factory Unit")
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedFieldBox(
                            onClick = { unitMenuExpanded = true },
                            modifier = Modifier.testTag("dropdown_unit")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedUnit,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF1F2937)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest = { unitMenuExpanded = false }
                        ) {
                            AccountMaster.defaultUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit, fontSize = 13.5.sp) },
                                    onClick = {
                                        selectedUnit = unit
                                        unitMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Payment Mode
                Column(modifier = Modifier.weight(1f)) {
                    FormFieldLabel(text = "Payment")
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedFieldBox(
                            onClick = { paymentMenuExpanded = true },
                            modifier = Modifier.testTag("dropdown_payment")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedPayment,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF1F2937)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = paymentMenuExpanded,
                            onDismissRequest = { paymentMenuExpanded = false }
                        ) {
                            AccountMaster.defaultPayments.forEach { payment ->
                                DropdownMenuItem(
                                    text = { Text(payment, fontSize = 13.5.sp) },
                                    onClick = {
                                        selectedPayment = payment
                                        paymentMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Row: Expected Date & Account Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Expected Date
                Column(modifier = Modifier.weight(1f)) {
                    FormFieldLabel(text = "Expected Date")
                    OutlinedFieldBox(
                        onClick = {
                            val cal = Calendar.getInstance()
                            val datePicker = DatePickerDialog(
                                context,
                                { _, year, monthOfYear, dayOfMonth ->
                                    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                                    val formatted = "$dayOfMonth ${months[monthOfYear]} $year"
                                    expectedDate = formatted
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            )
                            datePicker.show()
                        },
                        modifier = Modifier.testTag("button_select_date")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = expectedDate.ifBlank { "Select Date" },
                                fontSize = 13.sp,
                                color = if (expectedDate.isNotBlank()) Color(0xFF1F2937) else Color(0xFF6B7280)
                            )
                        }
                    }
                }

                // Account Info
                Column(modifier = Modifier.weight(1f)) {
                    FormFieldLabel(text = "Account Info")
                    OutlinedFieldBox(
                        onClick = { showAccountDialog = true },
                        modifier = Modifier.testTag("dropdown_account")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedAccountName.ifBlank { "Account Name" },
                                fontSize = 13.sp,
                                maxLines = 1,
                                color = Color(0xFF1F2937),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }

            // Code: [Auto-filled from Master Sheet]
            Text(
                text = "Code: ${accountCode.ifBlank { "[Auto-filled from Master Sheet]" }}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .testTag("code_auto_filled_hint")
            )

            // 5. Category Selection
            FormFieldLabel(text = "Category")
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedFieldBox(
                    onClick = { categoryMenuExpanded = true },
                    modifier = Modifier.testTag("dropdown_category")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCategory.ifBlank { "Select Category (Optional)" },
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedCategory.isNotBlank()) Color(0xFF1F2937) else Color(0xFF9CA3AF)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFF6B7280)
                        )
                    }
                }

                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("-- None (Optional) --", fontSize = 13.5.sp) },
                        onClick = {
                            selectedCategory = ""
                            categoryMenuExpanded = false
                        }
                    )
                    displayCategories.forEach { catOption ->
                        DropdownMenuItem(
                            text = { Text(catOption, fontSize = 13.5.sp) },
                            onClick = {
                                selectedCategory = catOption
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 6. Status Update
            FormFieldLabel(text = "Status Update")
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedFieldBox(
                    onClick = { statusMenuExpanded = true },
                    modifier = Modifier.testTag("dropdown_status_update")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedStatus,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1F2937)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFF6B7280)
                        )
                    }
                }

                DropdownMenu(
                    expanded = statusMenuExpanded,
                    onDismissRequest = { statusMenuExpanded = false }
                ) {
                    displayStatuses.forEach { statusOption ->
                        DropdownMenuItem(
                            text = { Text(statusOption, fontSize = 13.5.sp) },
                            onClick = {
                                selectedStatus = statusOption
                                statusMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 6. Remarks / Notes
            FormFieldLabel(text = "Remarks / Notes")
            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                placeholder = { Text("Type any additional notes here...", fontSize = 13.sp, color = Color(0xFF9CA3AF)) },
                minLines = 3,
                maxLines = 5,
                colors = defaultTextFieldColors(),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_remarks")
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAccountDialog) {
        AccountSelectionDialog(
            selectedAccountName = selectedAccountName,
            selectedAccountCode = accountCode,
            accounts = combinedAccounts,
            onAccountSelected = { name, code ->
                selectedAccountName = name
                accountCode = code
            },
            onDismissRequest = { showAccountDialog = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
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
                        showDeleteDialog = false
                        onDeleteTask()
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
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel", color = Color(0xFF4B5563))
                }
            }
        )
    }
}

@Composable
fun AccountSelectionDialog(
    selectedAccountName: String,
    selectedAccountCode: String = "",
    accounts: List<Pair<String, String>>,
    onAccountSelected: (name: String, code: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredAccounts = remember(searchQuery, accounts) {
        if (searchQuery.isBlank()) {
            accounts
        } else {
            val q = searchQuery.trim().lowercase()
            accounts.filter { (name, code) ->
                name.lowercase().contains(q) || code.lowercase().contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(12.dp),
        containerColor = Color.White,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Account",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    )
                    Text(
                        text = "${filteredAccounts.size} accounts",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search account or code...", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppPrimaryBlue,
                        unfocusedBorderColor = Color(0xFFD1D5DB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_search_field")
                )
            }
        },
        text = {
            if (filteredAccounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No accounts matching \"$searchQuery\"",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    items(filteredAccounts.size) { index ->
                        val (name, code) = filteredAccounts[index]
                        val isSelected = name.equals(selectedAccountName, ignoreCase = true) &&
                            (selectedAccountCode.isBlank() || code.equals(selectedAccountCode, ignoreCase = true))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAccountSelected(name, code)
                                    onDismissRequest()
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) AppPrimaryBlue else Color(0xFF1F2937)
                                    )
                                )
                                Text(
                                    text = "Code: $code",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = AppPrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < filteredAccounts.size - 1) {
                            Divider(color = Color(0xFFF3F4F6))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close", color = AppPrimaryBlue)
            }
        }
    )
}

@Composable
private fun FormFieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = Color(0xFF1F2937)
        ),
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
private fun OutlinedFieldBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = AppBorderGray,
                shape = RoundedCornerShape(4.dp)
            )
            .background(Color.White, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) {
        content()
    }
}

@Composable
private fun defaultTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppPrimaryBlue,
    unfocusedBorderColor = AppBorderGray,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedTextColor = Color(0xFF111827),
    unfocusedTextColor = Color(0xFF111827)
)

