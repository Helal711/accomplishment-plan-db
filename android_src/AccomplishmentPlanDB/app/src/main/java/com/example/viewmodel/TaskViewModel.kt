package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TaskRepository
import com.example.model.AccountItem
import com.example.model.AccountMaster
import com.example.model.CategoryItem
import com.example.model.Priority
import com.example.model.StatusItem
import com.example.model.TaskItem
import com.example.ui.screens.signOutUser
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenState {
    TASK_LIST,
    UPDATE_TASK,
    NEW_TASK
}

class TaskViewModel(
    private val repository: TaskRepository = TaskRepository(),
    customAuth: FirebaseAuth? = null
) : ViewModel() {

    private val auth: FirebaseAuth by lazy {
        customAuth ?: Firebase.auth
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _currentScreen = MutableStateFlow(ScreenState.TASK_LIST)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    private val _selectedTask = MutableStateFlow<TaskItem?>(null)
    val selectedTask: StateFlow<TaskItem?> = _selectedTask.asStateFlow()

    private val _selectedMonth = MutableStateFlow("Nov-2026")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _availableMonths = MutableStateFlow(AccountMaster.defaultMonths)
    val availableMonths: StateFlow<List<String>> = _availableMonths.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        try {
            _currentUser.value = auth.currentUser
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                _currentUser.value = user
                if (user != null) {
                    viewModelScope.launch {
                        try {
                            repository.initializeUserWorkspace(seedSampleTasks = true)
                        } catch (e: Exception) {
                            android.util.Log.e("TaskViewModel", "Workspace init: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TaskViewModel", "Error attaching FirebaseAuth listener: ${e.message}")
        }
    }

    // Dynamic tasks for current user and month from Firestore
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<TaskItem>> = combine(_currentUser, _selectedMonth) { user, month ->
        user to month
    }.flatMapLatest { (user, month) ->
        if (user != null) {
            repository.observeTasks(month)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic accounts for current user
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val accounts: StateFlow<List<AccountItem>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.observeAccounts()
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic statuses for current user
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val customStatuses: StateFlow<List<StatusItem>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.observeStatuses()
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableStatusNames: StateFlow<List<String>> = customStatuses.combine(flowOf(AccountMaster.defaultStatuses)) { custom, defaults ->
        val names = custom.map { it.statusName }
        val list = if (names.isNotEmpty()) names else defaults
        list.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountMaster.defaultStatuses.sortedWith(String.CASE_INSENSITIVE_ORDER))

    // Dynamic categories for current user
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<CategoryItem>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.observeCategories()
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCategoryNames: StateFlow<List<String>> = categories.combine(flowOf(AccountMaster.defaultCategories)) { custom, defaults ->
        val names = custom.map { it.categoryName }
        val list = if (names.isNotEmpty()) names else defaults
        list.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountMaster.defaultCategories.sortedWith(String.CASE_INSENSITIVE_ORDER))

    // Filtered tasks by search query
    val filteredTasks = combine(tasks, _searchQuery) { taskList, query ->
        if (query.isBlank()) {
            taskList
        } else {
            taskList.filter { task ->
                task.description.contains(query, ignoreCase = true) ||
                task.unit.contains(query, ignoreCase = true) ||
                task.status.contains(query, ignoreCase = true) ||
                task.accountName.contains(query, ignoreCase = true) ||
                task.category.contains(query, ignoreCase = true) ||
                task.taskNumber.toString().contains(query)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalItemsCount = tasks.combine(flowOf(Unit)) { list, _ ->
        list.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingItemsCount = tasks.combine(flowOf(Unit)) { list, _ ->
        list.count { !it.isDone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun openUpdateTask(task: TaskItem) {
        _selectedTask.value = task
        _currentScreen.value = ScreenState.UPDATE_TASK
    }

    fun openNewTask() {
        val nextNumber = (tasks.value.maxOfOrNull { it.taskNumber } ?: 0) + 1
        val uid = auth.currentUser?.uid ?: ""
        _selectedTask.value = TaskItem(
            id = "",
            ownerUid = uid,
            taskNumber = nextNumber,
            description = "",
            amount = "",
            paymentMode = "Cash",
            unit = "Unit-1",
            priority = "High",
            status = "Draft Ready",
            expectedDate = "17 Nov 2026",
            accountName = "M_Supply expenses(Other)",
            accountCode = AccountMaster.getCodeForAccount("M_Supply expenses(Other)"),
            remarks = "",
            category = "",
            monthYear = _selectedMonth.value
        )
        _currentScreen.value = ScreenState.NEW_TASK
    }

    fun navigateBackToTaskList() {
        _currentScreen.value = ScreenState.TASK_LIST
        _selectedTask.value = null
    }

    fun updateTask(updatedTask: TaskItem) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.updateTask(updatedTask)
            _isSyncing.value = false
            navigateBackToTaskList()
        }
    }

    fun quickUpdateStatus(taskId: String, newStatus: String) {
        viewModelScope.launch {
            repository.quickUpdateStatus(taskId, newStatus)
        }
    }

    fun addTask(newTask: TaskItem) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.addTask(newTask)
            _isSyncing.value = false
            navigateBackToTaskList()
        }
    }

    fun deleteTask(taskId: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isSyncing.value = true
            val res = repository.deleteTask(taskId, _selectedMonth.value)
            _isSyncing.value = false
            if (res.isSuccess) {
                navigateBackToTaskList()
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Failed to delete task")
            }
        }
    }

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    fun addMonth(newMonth: String) {
        if (newMonth.isNotBlank() && !_availableMonths.value.contains(newMonth)) {
            _availableMonths.value = _availableMonths.value + newMonth
            _selectedMonth.value = newMonth
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        _isSearchOpen.value = !_isSearchOpen.value
        if (!_isSearchOpen.value) {
            _searchQuery.value = ""
        }
    }

    fun addCustomStatus(status: String) {
        viewModelScope.launch {
            repository.addStatus(status)
        }
    }

    fun removeCustomStatus(statusName: String) {
        viewModelScope.launch {
            val item = customStatuses.value.firstOrNull { it.statusName == statusName }
            if (item != null && item.id.isNotBlank()) {
                repository.removeStatus(item.id)
            }
        }
    }

    fun addCategory(categoryName: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val trimmed = categoryName.trim()
            if (trimmed.isBlank()) {
                onResult(false, "Category name cannot be empty")
                return@launch
            }
            val existing = categories.value.any { it.categoryName.equals(trimmed, ignoreCase = true) }
            if (existing) {
                onResult(false, "Category \"$trimmed\" already exists")
                return@launch
            }
            val res = repository.addCategory(trimmed)
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Failed to add category")
            }
        }
    }

    fun updateCategory(categoryId: String, newName: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val trimmed = newName.trim()
            if (trimmed.isBlank()) {
                onResult(false, "Category name cannot be empty")
                return@launch
            }
            val existing = categories.value.any { it.id != categoryId && it.categoryName.equals(trimmed, ignoreCase = true) }
            if (existing) {
                onResult(false, "Category \"$trimmed\" already exists")
                return@launch
            }
            val res = repository.updateCategory(categoryId, trimmed)
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Failed to update category")
            }
        }
    }

    fun deleteCategory(categoryId: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = repository.deleteCategory(categoryId)
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Failed to delete category")
            }
        }
    }

        fun addAccount(name: String, code: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val trimmedCode = code.trim()
            if (trimmedName.isBlank() || trimmedCode.isBlank()) {
                onResult(false, "Account name and code cannot be empty")
                return@launch
            }
            val res = repository.addAccount(trimmedName, trimmedCode)
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Failed to add account")
            }
        }
    }

    fun updateAccount(id: String, name: String, code: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val trimmedCode = code.trim()
            if (trimmedName.isBlank() || trimmedCode.isBlank()) {
                onResult(false, "Account name and code cannot be empty")
                return@launch
            }
            val res = repository.updateAccount(id, trimmedName, trimmedCode)
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Failed to update account")
            }
        }
    }

    fun deleteAccount(id: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = repository.deleteAccount(id)
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Failed to delete account")
            }
        }
    }

    fun signOut(context: Context) {
        signOutUser(context, onSignedOut = {
            _currentUser.value = null
            _currentScreen.value = ScreenState.TASK_LIST
        }, viewModelScope)
    }
}
