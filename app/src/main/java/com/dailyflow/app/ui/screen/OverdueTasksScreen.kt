package com.dailyflow.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.data.model.RecurrenceScope
import com.dailyflow.app.ui.components.TaskCard
import com.dailyflow.app.ui.navigation.Screen
import com.dailyflow.app.ui.viewmodel.HomeViewModel
import com.dailyflow.app.ui.viewmodel.RecurringActionDialogState
import com.dailyflow.app.ui.viewmodel.RecurringActionType
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverdueTasksScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val overdueTasks by viewModel.overdueTasks.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showRecurringDialog by remember { mutableStateOf(false) }
    var recurringDialogState by remember { mutableStateOf<RecurringActionDialogState?>(null) }

    LaunchedEffect(Unit) {
        viewModel.recurringActionDialog.collectLatest { state ->
            recurringDialogState = state
            showRecurringDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Просроченные задачи") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) {
        LazyColumn(modifier = Modifier.padding(it)) {
            items(overdueTasks, key = { it.id }) { task ->
                val category = categories.find { it.id == task.categoryId }
                TaskCard(
                    task = task,
                    category = category,
                    onClick = { navController.navigate(Screen.TaskDetail.createRoute(task.id, null)) },
                    onToggle = { isCompleted ->
                        viewModel.toggleTaskCompletion(task.id, isCompleted)
                    },
                    onDelete = { viewModel.deleteTask(task.id) },
                    onCancel = { viewModel.cancelTask(task.id) }
                )
            }
        }
    }

    if (showRecurringDialog && recurringDialogState != null) {
        val state = recurringDialogState!!
        val isDelete = state.actionType == RecurringActionType.DELETE
        AlertDialog(
            onDismissRequest = {
                showRecurringDialog = false
                viewModel.dismissRecurringActionDialog()
            },
            title = { Text(if (isDelete) "Удаление повторяющейся задачи" else "Отмена повторяющейся задачи") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Выберите, к каким задачам применить действие")
                    TextButton(onClick = {
                        showRecurringDialog = false
                        viewModel.onRecurringActionScopeSelected(RecurrenceScope.THIS)
                    }) {
                        Text("Только к этой задаче")
                    }
                    TextButton(onClick = {
                        showRecurringDialog = false
                        viewModel.onRecurringActionScopeSelected(RecurrenceScope.THIS_AND_FUTURE)
                    }) {
                        Text("К этой и будущим задачам")
                    }
                    TextButton(onClick = {
                        showRecurringDialog = false
                        viewModel.onRecurringActionScopeSelected(RecurrenceScope.ENTIRE_SERIES)
                    }) {
                        Text("Ко всей серии")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showRecurringDialog = false
                    viewModel.dismissRecurringActionDialog()
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}