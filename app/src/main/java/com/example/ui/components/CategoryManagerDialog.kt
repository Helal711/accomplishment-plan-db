package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccountMaster
import com.example.model.CategoryItem
import com.example.ui.theme.AppPrimaryBlue

@Composable
fun CategoryManagerDialog(
    categories: List<CategoryItem>,
    onAddCategory: (String, (Boolean, String?) -> Unit) -> Unit,
    onEditCategory: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onDeleteCategory: (String, (Boolean, String?) -> Unit) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var newCategoryInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // State for editing category
    var editingCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var editCategoryNameInput by remember { mutableStateOf("") }

    // State for deleting category
    var categoryToDelete by remember { mutableStateOf<CategoryItem?>(null) }

    // Display list: if categories is empty, fall back to default categories so user immediately sees them
    val displayCategories = remember(categories) {
        if (categories.isNotEmpty()) {
            categories.sortedBy { it.categoryName.lowercase() }
        } else {
            AccountMaster.defaultCategories.map { CategoryItem(id = "", categoryName = it) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(12.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = AppPrimaryBlue,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = "Category Management",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    )
                    Text(
                        text = "${displayCategories.size} categories available",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Add, edit, or remove categories for tasks:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Add new category row
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
                        isError = errorMessage != null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_category_field")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val trimmed = newCategoryInput.trim()
                            if (trimmed.isBlank()) {
                                errorMessage = "Category name cannot be blank"
                                return@IconButton
                            }
                            val isDuplicate = displayCategories.any {
                                it.categoryName.trim().equals(trimmed, ignoreCase = true)
                            }
                            if (isDuplicate) {
                                errorMessage = "Category already exists"
                                return@IconButton
                            }

                            onAddCategory(trimmed) { success, error ->
                                if (success) {
                                    newCategoryInput = ""
                                    errorMessage = null
                                    Toast.makeText(context, "Category added", Toast.LENGTH_SHORT).show()
                                } else {
                                    errorMessage = error ?: "Failed to add"
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("add_category_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Category",
                            tint = AppPrimaryBlue
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFDC2626),
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Category list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(displayCategories, key = { it.id.ifBlank { it.categoryName } }) { category ->
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
                                Text(
                                    text = category.categoryName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1F2937)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Edit button
                                    IconButton(
                                        onClick = {
                                            editingCategory = category
                                            editCategoryNameInput = category.categoryName
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Category",
                                            tint = Color(0xFF4B5563),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Delete button
                                    IconButton(
                                        onClick = {
                                            categoryToDelete = category
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Category",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimaryBlue),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Close", color = Color.White)
            }
        }
    )

    // Edit Category Dialog
    if (editingCategory != null) {
        val target = editingCategory!!
        var editError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingCategory = null },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Edit Category",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editCategoryNameInput,
                        onValueChange = {
                            editCategoryNameInput = it
                            editError = null
                        },
                        placeholder = { Text("Category Name") },
                        singleLine = true,
                        isError = editError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (editError != null) {
                        Text(
                            text = editError ?: "",
                            color = Color(0xFFDC2626),
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editCategoryNameInput.trim()
                        if (trimmed.isBlank()) {
                            editError = "Category name cannot be blank"
                            return@Button
                        }
                        val isDuplicate = displayCategories.any {
                            it.id != target.id && it.categoryName.trim().equals(trimmed, ignoreCase = true)
                        }
                        if (isDuplicate) {
                            editError = "Category \"$trimmed\" already exists"
                            return@Button
                        }

                        if (target.id.isNotBlank()) {
                            onEditCategory(target.id, trimmed) { success, error ->
                                if (success) {
                                    editingCategory = null
                                    Toast.makeText(context, "Category updated", Toast.LENGTH_SHORT).show()
                                } else {
                                    editError = error ?: "Failed to update"
                                }
                            }
                        } else {
                            // If it's a default category before sync, add it as a new one
                            onAddCategory(trimmed) { success, error ->
                                if (success) {
                                    editingCategory = null
                                    Toast.makeText(context, "Category updated", Toast.LENGTH_SHORT).show()
                                } else {
                                    editError = error ?: "Failed to update"
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppPrimaryBlue),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (categoryToDelete != null) {
        val target = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Delete Category",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete category \"${target.categoryName}\"? Existing tasks will keep their saved category.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF4B5563))
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (target.id.isNotBlank()) {
                            onDeleteCategory(target.id) { success, error ->
                                categoryToDelete = null
                                if (success) {
                                    Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, error ?: "Failed to delete", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            categoryToDelete = null
                            Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        )
    }
}
