package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccountItem
import com.example.ui.theme.AppPrimaryBlue

@Composable
fun AccountManagerDialog(
    accounts: List<AccountItem>,
    onAddAccount: (name: String, code: String, (Boolean, String?) -> Unit) -> Unit,
    onUpdateAccount: (id: String, name: String, code: String, (Boolean, String?) -> Unit) -> Unit,
    onDeleteAccount: (id: String, (Boolean, String?) -> Unit) -> Unit,
    onDismissRequest: () -> Unit
) {
    var accountName by remember { mutableStateOf("") }
    var accountCode by remember { mutableStateOf("") }
    var editingAccountId by remember { mutableStateOf<String?>(null) }
    var editAccountName by remember { mutableStateOf("") }
    var editAccountCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = AppPrimaryBlue,
                        modifier = Modifier.padding(end = 8.dp).size(22.dp)
                    )
                    Text(
                        text = "Master Account List",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    )
                }
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                Text(
                    text = "Manage chart of accounts. Each account is automatically matched to its Account Code during task creation.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Account List
                if (accounts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No custom accounts found.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(accounts, key = { it.id.ifBlank { it.accountCode } }) { acc ->
                            val isEditing = editingAccountId == acc.id
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
                            ) {
                                if (isEditing) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        OutlinedTextField(
                                            value = editAccountName,
                                            onValueChange = { editAccountName = it },
                                            label = { Text("Account Name", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = editAccountCode,
                                            onValueChange = { editAccountCode = it },
                                            label = { Text("Account Code", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(
                                                onClick = { editingAccountId = null },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Cancel",
                                                    tint = Color(0xFF6B7280),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (editAccountName.isNotBlank() && editAccountCode.isNotBlank()) {
                                                        onUpdateAccount(acc.id, editAccountName.trim(), editAccountCode.trim()) { ok, err ->
                                                            if (ok) {
                                                                editingAccountId = null
                                                                errorMessage = null
                                                            } else {
                                                                errorMessage = err ?: "Failed to update account"
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Save",
                                                    tint = Color(0xFF16A34A),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                text = acc.accountName,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF111827),
                                                    fontSize = 12.5.sp
                                                )
                                            )
                                            Row(
                                                modifier = Modifier.padding(top = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = acc.accountCode,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFF1E40AF),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    modifier = Modifier
                                                        .background(Color(0xFFDBEAFE), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    editingAccountId = acc.id
                                                    editAccountName = acc.accountName
                                                    editAccountCode = acc.accountCode
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Account",
                                                    tint = AppPrimaryBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    onDeleteAccount(acc.id) { ok, err ->
                                                        if (!ok) {
                                                            errorMessage = err ?: "Failed to delete account"
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Account",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Add Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Add New Account",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = {
                            accountName = it
                            errorMessage = null
                            if (accountCode.isBlank()) {
                                val prefix = it.replace(Regex("[^a-zA-Z]"), "").take(3).uppercase()
                                if (prefix.isNotBlank()) accountCode = "ACC-${prefix}101"
                            }
                        },
                        label = { Text("Account Name", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Electrical Spares", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_account_name_input")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = accountCode,
                        onValueChange = {
                            accountCode = it
                            errorMessage = null
                        },
                        label = { Text("Account Code", fontSize = 11.sp) },
                        placeholder = { Text("e.g. ACC-EL412", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_account_code_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (accountName.isNotBlank() && accountCode.isNotBlank()) {
                                isSubmitting = true
                                onAddAccount(accountName.trim(), accountCode.trim()) { ok, err ->
                                    isSubmitting = false
                                    if (ok) {
                                        accountName = ""
                                        accountCode = ""
                                        errorMessage = null
                                    } else {
                                        errorMessage = err ?: "Failed to add account"
                                    }
                                }
                            } else {
                                errorMessage = "Please enter both Account Name and Code"
                            }
                        },
                        enabled = !isSubmitting && accountName.isNotBlank() && accountCode.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_account_submit_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(text = "Add Account to List", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFDC2626),
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close", color = AppPrimaryBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
