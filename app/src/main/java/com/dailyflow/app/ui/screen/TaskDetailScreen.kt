package com.dailyflow.app.ui.screen

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.data.model.Priority
import com.dailyflow.app.ui.components.SpinnerTimePicker
import com.dailyflow.app.ui.viewmodel.TaskDetailViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    navController: NavController,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val showPermissionDialog by viewModel.showExactAlarmPermissionDialog.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf<String?>("") }
    var priority by remember { mutableStateOf(Priority.LOW) }
    var selectedCategory by remember { mutableStateOf<com.dailyflow.app.data.model.Category?>(null) }
    var startDateTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var endDateTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderMinutes by remember { mutableStateOf("60") }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val minMillis = startDateTime?.toLocalDate()?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                return minMillis?.let { utcTimeMillis >= it } ?: true
            }
        }
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> /* TODO: Handle file URI */ }
    )

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collectLatest {
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState.isNewTask) {
            val defaultDate = uiState.defaultDate ?: LocalDate.now()
            val defaultStartTime = uiState.defaultStartTime ?: LocalTime.now().plusHours(1).truncatedTo(ChronoUnit.HOURS)
            val defaultEndTime = uiState.defaultEndTime ?: defaultStartTime.plusHours(1)
            
            startDateTime = LocalDateTime.of(defaultDate, defaultStartTime)
            endDateTime = LocalDateTime.of(defaultDate, defaultEndTime)
            
        } else if (uiState.task != null) {
            val task = uiState.task!!
            title = task.title
            description = task.description
            priority = task.priority
            selectedCategory = uiState.categories.find { it.id == task.categoryId }
            startDateTime = task.startDateTime
            endDateTime = task.endDateTime
            reminderEnabled = task.reminderEnabled
            reminderMinutes = task.reminderMinutes?.toString() ?: "60"
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExactAlarmPermissionDialog() },
            title = { Text("Требуется разрешение") },
            text = { Text("Для надежной работы напоминаний приложению требуется специальное разрешение на установку точных будильников.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        context.startActivity(intent)
                        viewModel.dismissExactAlarmPermissionDialog()
                    }
                ) {
                    Text("Перейти в настройки")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExactAlarmPermissionDialog() }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewTask) "Новая задача" else "Редактировать задачу") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description ?: "",
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )

                ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                        uiState.categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { showStartDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = startDateTime?.format(DateTimeFormatter.ofPattern("dd.MM HH:mm")) ?: "Начало")
                    }
                    Button(onClick = {
                        showEndDatePicker = true
                    }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = endDateTime?.format(DateTimeFormatter.ofPattern("dd.MM HH:mm")) ?: "Конец")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Напомнить")
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                    if (reminderEnabled) {
                        OutlinedTextField(
                            value = reminderMinutes,
                            onValueChange = { reminderMinutes = it },
                            label = { Text("За (мин)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Прикрепить файл")
                    }

                    PriorityIcon(priority = priority, onClick = {
                        priority = when(priority) {
                            Priority.LOW -> Priority.MEDIUM
                            Priority.MEDIUM -> Priority.HIGH
                            Priority.HIGH -> Priority.LOW
                        }
                    })
                }
                
                Button(onClick = {
                    coroutineScope.launch {
                        var finalEndDateTime = endDateTime
                        if (startDateTime != null && endDateTime != null && !endDateTime!!.isAfter(startDateTime)) {
                            finalEndDateTime = endDateTime!!.plusDays(1)
                        }

                        viewModel.saveTask(
                            title = title,
                            description = description,
                            categoryId = selectedCategory?.id ?: "",
                            startDateTime = startDateTime,
                            endDateTime = finalEndDateTime,
                            reminderEnabled = reminderEnabled,
                            reminderMinutes = reminderMinutes.toIntOrNull(),
                            priority = priority
                        )
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Сохранить")
                }
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    showStartDatePicker = false
                    showStartTimePicker = true
                }) {
                    Text("OK")
                }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Отмена") } }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showStartTimePicker) {
        val initialTime = remember(showStartTimePicker) {
             startDateTime?.toLocalTime() ?: LocalTime.now().plusHours(1).truncatedTo(ChronoUnit.HOURS)
        }
        var tempTime by remember(initialTime) { mutableStateOf(initialTime) }

        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Выберите время") },
            text = { SpinnerTimePicker(initialTime = initialTime, onTimeSelected = { tempTime = it }) },
            confirmButton = {
                TextButton(onClick = { 
                    showStartTimePicker = false
                    val date = startDatePickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    } ?: LocalDate.now()
                    val newStartDateTime = LocalDateTime.of(date, tempTime)
                    startDateTime = newStartDateTime

                    if (endDateTime != null && endDateTime!!.isBefore(newStartDateTime)) {
                        endDateTime = newStartDateTime.plusHours(1)
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = { TextButton(onClick = { showStartTimePicker = false }) { Text("Отмена") } }
        )
    }


    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    showEndDatePicker = false
                    showEndTimePicker = true
                }) {
                    Text("OK")
                }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Отмена") } }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    if (showEndTimePicker) {
        val initialTime = remember(showEndTimePicker) {
            endDateTime?.toLocalTime() ?: (startDateTime ?: LocalDateTime.now()).plusHours(1).toLocalTime().truncatedTo(ChronoUnit.HOURS)
        }
        var tempTime by remember(initialTime) { mutableStateOf(initialTime) }

        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("Выберите время") },
            text = { SpinnerTimePicker(initialTime = initialTime, onTimeSelected = { tempTime = it }) },
            confirmButton = {
                TextButton(onClick = { 
                    showEndTimePicker = false
                    val date = endDatePickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    } ?: startDateTime?.toLocalDate() ?: LocalDate.now()
                    
                    var potentialEndDateTime = LocalDateTime.of(date, tempTime)

                    if (startDateTime != null && potentialEndDateTime.isBefore(startDateTime)) {
                        potentialEndDateTime = startDateTime!!.plusHours(1)
                    }
                    
                    endDateTime = potentialEndDateTime
                }) {
                    Text("OK")
                }
            },
            dismissButton = { TextButton(onClick = { showEndTimePicker = false }) { Text("Отмена") } }
        )
    }
}

@Composable
fun PriorityIcon(priority: Priority, onClick: () -> Unit) {
    val color = when (priority) {
        Priority.HIGH -> Color.Red
        Priority.MEDIUM -> Color.Yellow
        Priority.LOW -> Color.Gray
    }
    IconButton(onClick = onClick) {
        Icon(Icons.Default.LocalFireDepartment, contentDescription = "Приоритет", tint = color)
    }
}
