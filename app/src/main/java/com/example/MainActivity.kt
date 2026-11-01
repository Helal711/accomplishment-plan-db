package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.TaskListScreen
import com.example.ui.screens.UpdateTaskScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ScreenState
import com.example.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    val currentUser by viewModel.currentUser.collectAsState()

                    if (currentUser == null) {
                        AuthScreen(
                            onAuthSuccess = {
                                // Handled automatically by FirebaseAuth listener in ViewModel
                            }
                        )
                    } else {
                        AccomplishmentPlanApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AccomplishmentPlanApp(viewModel: TaskViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedTask by viewModel.selectedTask.collectAsState()
    val availableStatuses by viewModel.availableStatusNames.collectAsState()

    when (currentScreen) {
        ScreenState.TASK_LIST -> {
            TaskListScreen(viewModel = viewModel)
        }
        ScreenState.UPDATE_TASK -> {
            selectedTask?.let { task ->
                UpdateTaskScreen(
                    task = task,
                    isNewTask = false,
                    availableStatuses = availableStatuses,
                    onSaveTask = { updated -> viewModel.updateTask(updated) },
                    onNavigateBack = { viewModel.navigateBackToTaskList() }
                )
            } ?: TaskListScreen(viewModel = viewModel)
        }
        ScreenState.NEW_TASK -> {
            selectedTask?.let { task ->
                UpdateTaskScreen(
                    task = task,
                    isNewTask = true,
                    availableStatuses = availableStatuses,
                    onSaveTask = { created -> viewModel.addTask(created) },
                    onNavigateBack = { viewModel.navigateBackToTaskList() }
                )
            } ?: TaskListScreen(viewModel = viewModel)
        }
    }
}
