package com.dailyflow.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.R
import com.dailyflow.app.data.model.RecurrenceScope
import com.dailyflow.app.data.model.Task
import com.dailyflow.app.ui.navigation.Screen
import com.dailyflow.app.ui.viewmodel.OverdueViewModel
import com.dailyflow.app.ui.viewmodel.RecurringActionDialogState
import com.dailyflow.app.ui.viewmodel.RecurringActionType
import com.dailyflow.app.ui.theme.OverdueColor
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import android.content.res.Configuration
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OverdueScreen(
    navController: NavController,
    viewModel: OverdueViewModel = hiltViewModel()
) {
    val groupedTasks by viewModel.groupedOverdueTasks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filterDate by viewModel.filterDate.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showRecurringDialog by remember { mutableStateOf(false) }
    var recurringDialogState by remember { mutableStateOf<RecurringActionDialogState?>(null) }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(Unit) {
        viewModel.recurringActionDialog.collectLatest { state ->
            recurringDialogState = state
            showRecurringDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Просроченные дела") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Фильтр")
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
                val context = LocalContext.current
                val russianContext = remember(context) {
                    val config = android.content.res.Configuration(context.resources.configuration)
                    config.setLocale(java.util.Locale("ru"))
                    context.createConfigurationContext(config)
                }
                val russianConfiguration = remember(russianContext) { russianContext.resources.configuration }
                
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
                    CompositionLocalProvider(
                        LocalContext provides russianContext,
                        LocalConfiguration provides russianConfiguration
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
            }

            if (groupedTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нет просроченных задач")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groupedTasks.forEach { (date, tasks) ->
                        stickyHeader(key = date.toString()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ru"))),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(
                            count = tasks.size,
                            key = { index -> tasks[index].id }
                        ) { index ->
                            val task = tasks[index]
                            OverdueTaskItem(
                                task = task,
                                category = categories.find { cat -> cat.id == task.categoryId },
                                onTaskClick = { navController.navigate(Screen.TaskDetail.createRoute(task.id)) },
                                onComplete = { viewModel.completeTask(task.id) },
                                onCancel = { viewModel.cancelTask(task.id) },
                                onDelete = { viewModel.deleteTask(task.id) }
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

@Composable
fun OverdueTaskItem(
    task: Task,
    category: com.dailyflow.app.data.model.Category?,
    onTaskClick: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTaskClick),
        colors = CardDefaults.cardColors(
            containerColor = OverdueColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = OverdueColor
                    )
                    
                    task.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        category?.let { cat ->
                            Icon(
                                imageVector = Icons.Default.Circle,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(android.graphics.Color.parseColor(cat.color))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(android.graphics.Color.parseColor(cat.color))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        Text(
                            text = "Просрочено",
                            style = MaterialTheme.typography.labelSmall,
                            color = OverdueColor
                        )
                    }
                }
                
                IconButton(
                    onClick = { showActions = !showActions }
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Действия")
                }
            }
            
            if (showActions) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onComplete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Выполнить")
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Отменить")
                    }
                     OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Удалить")
                    }
                }
            }
        }
    }
}
