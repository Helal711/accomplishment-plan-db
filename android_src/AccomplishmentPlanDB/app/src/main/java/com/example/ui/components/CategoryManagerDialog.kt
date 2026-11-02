package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CategoryItem
import com.example.ui.theme.AppPrimaryBlue

@Composable
fun CategoryManagerDialog(
    categories: List<CategoryItem>,
    onAddCategory: (String) -> Unit,
    onEditCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var newCategoryInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var editingCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var editCategoryInput by remember { mutableStateOf("") }
    var deletingCategory by remember { mutableStateOf<CategoryItem?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(12.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = AppPrimaryBlue,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Category Management",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Manage task categories (${categories.size} available):",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories) { cat ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (editingCategory?.id == cat.id) {
                                    OutlinedTextField(
                                        value = editCategoryInput,
                                        onValueChange = { editCategoryInput = it },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            val trimmed = editCategoryInput.trim()
                                            if (trimmed.isNotBlank()) {
                                                val isDuplicate = categories.any {
                                                    it.id != cat.id && it.categoryName.equals(trimmed, ignoreCase = true)
                                                }
                                                if (isDuplicate) {
                                                    errorMessage = "Category \"$trimmed\" already exists"
                                                } else {
                                                    onEditCategory(cat.id, trimmed)
                                                    editingCategory = null
                                                    errorMessage = null
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { editingCategory = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFF9E9E9E), modifier = Modifier.size(16.dp))
                                    }
                                } else {
                                    Text(
                                        text = cat.categoryName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                editingCategory = cat
                                                editCategoryInput = cat.categoryName
                                                errorMessage = null
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = Color(0xFF6B7280),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { deletingCategory = cat },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626)),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryInput,
                        onValueChange = {
                            newCategoryInput = it
                            errorMessage = null
                        },
                        placeholder = { Text("New Category Name", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_category_field")
                    )
                    IconButton(
                        onClick = {
                            val trimmed = newCategoryInput.trim()
                            if (trimmed.isBlank()) {
                                errorMessage = "Category name cannot be blank"
                            } else if (categories.any { it.categoryName.equals(trimmed, ignoreCase = true) }) {
                                errorMessage = "Category \"$trimmed\" already exists"
                            } else {
                                onAddCategory(trimmed)
                                newCategoryInput = ""
                                errorMessage = null
                            }
                        },
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .testTag("add_category_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = AppPrimaryBlue)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimaryBlue)
            ) {
                Text("Close")
            }
        }
    )

    // Delete confirmation dialog
    if (deletingCategory != null) {
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Delete Category", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete category \"${deletingCategory?.categoryName}\"? Existing tasks will keep their saved category.") },
            confirmButton = {
                Button(
                    onClick = {
                        deletingCategory?.let { onDeleteCategory(it.id) }
                        deletingCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
