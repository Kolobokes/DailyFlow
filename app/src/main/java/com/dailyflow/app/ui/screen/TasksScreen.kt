package com.dailyflow.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.R
import com.dailyflow.app.data.model.RecurrenceScope
import com.dailyflow.app.ui.components.SwipeableTaskCard
import com.dailyflow.app.ui.navigation.Screen
import com.dailyflow.app.ui.viewmodel.TasksViewModel
import com.dailyflow.app.ui.viewmodel.RecurringActionDialogState
import com.dailyflow.app.ui.viewmodel.RecurringActionType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    navController: NavController,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val groupedTasks by viewModel.groupedTasks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filterDate by viewModel.filterDate.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showRecurringDialog by remember { mutableStateOf(false) }
    var recurringDialogState by remember { mutableStateOf<RecurringActionDialogState?>(null) }
    val datePickerState = rememberDatePickerState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.recurringActionDialog.collectLatest { state ->
            recurringDialogState = state
            showRecurringDialog = true
        }
    }

    LaunchedEffect(groupedTasks) {
        val today = LocalDate.now()
        val todayIndex = groupedTasks.keys.indexOf(today)
        if (todayIndex != -1) {
            coroutineScope.launch {
                // Calculate the exact index to scroll to
                val scrollIndex = groupedTasks.keys.take(todayIndex).sumOf { groupedTasks[it]?.size ?: 0 } + todayIndex
                listState.animateScrollToItem(index = scrollIndex)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Все задачи") },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Фильтр по дате")
                    }
                    if (filterDate != null) {
                        IconButton(onClick = { viewModel.setFilterDate(null) }) {
                            Icon(Icons.Default.FilterListOff, contentDescription = "Сбросить фильтр")
                        }
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(horizontal = 16.dp)
        ) {
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                val selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                viewModel.setFilterDate(selectedDate)
                            }
                            showDatePicker = false
                        }) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Отмена") } }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = statusFilter == TasksViewModel.StatusFilter.ALL, onClick = { viewModel.setStatusFilter(TasksViewModel.StatusFilter.ALL) }, label = { Text(stringResource(R.string.filter_all)) })
                FilterChip(selected = statusFilter == TasksViewModel.StatusFilter.OVERDUE, onClick = { viewModel.setStatusFilter(TasksViewModel.StatusFilter.OVERDUE) }, label = { Text(stringResource(R.string.filter_overdue)) })
                FilterChip(selected = statusFilter == TasksViewModel.StatusFilter.COMPLETED, onClick = { viewModel.setStatusFilter(TasksViewModel.StatusFilter.COMPLETED) }, label = { Text(stringResource(R.string.filter_completed)) })
                FilterChip(selected = statusFilter == TasksViewModel.StatusFilter.CANCELLED, onClick = { viewModel.setStatusFilter(TasksViewModel.StatusFilter.CANCELLED) }, label = { Text(stringResource(R.string.filter_cancelled)) })
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (groupedTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет задач для отображения")
                }
            } else {
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groupedTasks.forEach { (date, tasks) ->
                        stickyHeader {
                            Row(
                                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(tasks) { task ->
                            SwipeableTaskCard(
                                task = task,
                                category = categories.find { cat -> cat.id == task.categoryId },
                                onClick = { navController.navigate(Screen.TaskDetail.createRoute(task.id)) },
                                onToggle = { isCompleted -> viewModel.toggleTaskCompletion(task.id, isCompleted) },
                                onDelete = { viewModel.deleteTask(task.id) },
                                onCancel = { viewModel.cancelTask(task.id) }
                            )
                        }
                    }
                }
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
            title = { Text(if (isDelete) "Действие с повторяющейся задачей" else "Отмена повторяющейся задачи") },
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
