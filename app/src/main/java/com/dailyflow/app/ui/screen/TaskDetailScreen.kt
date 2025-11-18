package com.dailyflow.app.ui.screen

import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.data.model.Priority
import com.dailyflow.app.data.model.RecurrenceFrequency
import com.dailyflow.app.data.model.RecurrenceRule
import com.dailyflow.app.data.model.RecurrenceScope
import com.dailyflow.app.ui.components.SpinnerTimePicker
import com.dailyflow.app.ui.viewmodel.RecurrenceScopeDialogState
import com.dailyflow.app.ui.viewmodel.TaskDetailViewModel
import com.dailyflow.app.util.RecurrenceUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import androidx.compose.ui.res.stringResource
import com.dailyflow.app.R

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
    val russianContext = remember(context) {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale("ru"))
        context.createConfigurationContext(config)
    }
    val russianConfiguration = remember(russianContext) { russianContext.resources.configuration }
    val contentScrollState = rememberScrollState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf<String?>("") }
    var priority by remember { mutableStateOf(Priority.LOW) }
    var selectedCategory by remember { mutableStateOf<com.dailyflow.app.data.model.Category?>(null) }
    var startDateTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var endDateTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderMinutes by remember { mutableStateOf("60") }
    var isRecurring by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf(RecurrenceFrequency.DAILY) }
    var interval by remember { mutableStateOf("1") }
    var repeatEndType by remember { mutableStateOf(RepeatEndType.NEVER) }
    var repeatEndDate by remember { mutableStateOf<LocalDate?>(null) }
    var repeatOccurrenceCount by remember { mutableStateOf("") }
    var weeklyDays by remember { mutableStateOf(setOf<java.time.DayOfWeek>()) }
    var monthlyDayOfMonth by remember { mutableStateOf<Int?>(null) }
    var attachedFileName by remember { mutableStateOf<String?>(null) }
    var attachedFileDisplayName by remember { mutableStateOf<String?>(null) }
    val currentTaskId = remember(uiState.task?.id) { 
        uiState.task?.id ?: UUID.randomUUID().toString() 
    }

    var previewOccurrences by remember { mutableStateOf(0) }
    val exportTimestampFormatter = remember { DateTimeFormatter.ofPattern("yyyyMMdd_HHmm") }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showRepeatEndDatePicker by remember { mutableStateOf(false) }
    var showScopeDialog by remember { mutableStateOf(false) }
    var scopeDialogState by remember { mutableStateOf<RecurrenceScopeDialogState?>(null) }

    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val minMillis = startDateTime?.toLocalDate()?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                return minMillis?.let { utcTimeMillis >= it } ?: true
            }
        }
    )
    val repeatEndDatePickerState = rememberDatePickerState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val fileName = viewModel.copyFileToStorage(it, currentTaskId)
                if (fileName != null) {
                    attachedFileName = fileName
                    // Получаем отображаемое имя файла
                    try {
                        context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0 && cursor.moveToFirst()) {
                                attachedFileDisplayName = cursor.getString(nameIndex) ?: "Файл"
                            } else {
                                attachedFileDisplayName = "Файл"
                            }
                        } ?: run {
                            attachedFileDisplayName = it.path?.substringAfterLast('/') ?: "Файл"
                        }
                    } catch (e: Exception) {
                        attachedFileDisplayName = "Файл"
                    }
                } else {
                    Toast.makeText(context, "Не удалось сохранить файл", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    var pendingTaskExport by remember { mutableStateOf<String?>(null) }
    val taskExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val text = pendingTaskExport
        if (uri != null && text != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(text.toByteArray(StandardCharsets.UTF_8))
                }
            }.onSuccess {
                Toast.makeText(context, "Задача сохранена в файл", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Не удалось сохранить файл", Toast.LENGTH_SHORT).show()
            }
        }
        pendingTaskExport = null
    }

    LaunchedEffect(Unit) {
        launch {
            viewModel.navigateBack.collectLatest {
                navController.popBackStack()
            }
        }
        launch {
            viewModel.recurrenceScopeDialog.collectLatest { state ->
                scopeDialogState = state
                showScopeDialog = true
            }
        }
    }

    LaunchedEffect(uiState) {
        isRecurring = uiState.isRecurring
        uiState.recurrenceRule?.let { rule ->
            frequency = rule.frequency
            interval = rule.interval.toString()
            repeatEndDate = rule.endDate
            repeatOccurrenceCount = rule.occurrenceCount?.toString() ?: ""
            weeklyDays = rule.daysOfWeek
            monthlyDayOfMonth = rule.dayOfMonth
            repeatEndType = when {
                rule.endDate != null -> RepeatEndType.END_DATE
                rule.occurrenceCount != null -> RepeatEndType.COUNT
                else -> RepeatEndType.NEVER
            }
        }
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
            attachedFileName = task.attachedFileUri
            // Получаем отображаемое имя файла из сохраненного файла
            if (task.attachedFileUri != null) {
                val file = viewModel.getFile(task.attachedFileUri!!)
                attachedFileDisplayName = file?.name?.let { fileName ->
                    // Извлекаем оригинальное имя из формата: taskId_originalFileName
                    val taskIdPrefix = "${task.id}_"
                    if (fileName.startsWith(taskIdPrefix)) {
                        fileName.removePrefix(taskIdPrefix)
                    } else {
                        fileName
                    }
                } ?: "Файл"
            } else {
                attachedFileDisplayName = null
            }
        }
        if (frequency in listOf(RecurrenceFrequency.WEEKLY, RecurrenceFrequency.WEEKLY_DAYS) && weeklyDays.isEmpty()) {
            weeklyDays = setOf((startDateTime ?: LocalDateTime.now()).dayOfWeek)
        }
        if (frequency == RecurrenceFrequency.MONTHLY && monthlyDayOfMonth == null) {
            monthlyDayOfMonth = (startDateTime ?: LocalDateTime.now()).dayOfMonth
        }
    }

    LaunchedEffect(startDateTime, frequency, interval, weeklyDays, monthlyDayOfMonth, repeatEndType, repeatEndDate, repeatOccurrenceCount, isRecurring) {
        val rule = buildRecurrenceRule(
            isRecurring = isRecurring,
            frequency = frequency,
            interval = interval.toIntOrNull() ?: 1,
            weeklyDays = weeklyDays,
            monthlyDayOfMonth = monthlyDayOfMonth,
            repeatEndType = repeatEndType,
            repeatEndDate = repeatEndDate,
            repeatOccurrenceCount = repeatOccurrenceCount.toIntOrNull()
        )
        previewOccurrences = if (isRecurring && rule != null) {
            startDateTime?.let { RecurrenceUtils.estimateOccurrences(it, rule) } ?: 0
        } else 0
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
                },
                actions = {
                    if (!uiState.isNewTask) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val exportText = viewModel.exportCurrentTask()
                                if (exportText == null) {
                                    Toast.makeText(context, "Не удалось подготовить файл", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val baseName = (uiState.task?.title ?: "task").ifBlank { "task" }
                                val sanitized = baseName.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(40)
                                val timestamp = LocalDateTime.now().format(exportTimestampFormatter)
                                val fileName = "${sanitized.ifBlank { "task" }}_$timestamp.txt"
                                pendingTaskExport = exportText
                                taskExportLauncher.launch(fileName)
                            }
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Экспортировать задачу")
                        }
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
                    .verticalScroll(contentScrollState)
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Статус задачи - в самом верху
                if (!uiState.isNewTask && uiState.task != null) {
                    val task = uiState.task!!
                    val (statusColor, statusLabel) = when (task.status) {
                        com.dailyflow.app.data.model.TaskStatus.COMPLETED -> Color(0xFF4CAF50) to "Выполнена"
                        com.dailyflow.app.data.model.TaskStatus.CANCELLED -> Color(0xFFE53935) to "Отменена"
                        com.dailyflow.app.data.model.TaskStatus.PENDING -> MaterialTheme.colorScheme.secondary to "Не выполнена"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.15f), shape = CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = statusColor
                            )
                        }
                    }
                    Divider()
                }
                
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

                Divider()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Повторять")
                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { checked ->
                                isRecurring = checked
                            }
                        )
                    }

                    if (isRecurring) {
                        RecurrenceConfig(
                            frequency = frequency,
                            onFrequencyChange = { newFrequency ->
                                frequency = newFrequency
                                if (newFrequency in listOf(RecurrenceFrequency.WEEKLY, RecurrenceFrequency.WEEKLY_DAYS) && weeklyDays.isEmpty()) {
                                    weeklyDays = setOf((startDateTime ?: LocalDateTime.now()).dayOfWeek)
                                }
                                if (newFrequency == RecurrenceFrequency.MONTHLY && monthlyDayOfMonth == null) {
                                    monthlyDayOfMonth = (startDateTime ?: LocalDateTime.now()).dayOfMonth
                                }
                                if (newFrequency !in listOf(RecurrenceFrequency.WEEKLY, RecurrenceFrequency.WEEKLY_DAYS)) {
                                    weeklyDays = emptySet()
                                }
                            },
                            interval = interval,
                            onIntervalChange = { interval = it.filter { ch -> ch.isDigit() }.ifBlank { "1" } },
                            weeklyDays = weeklyDays,
                            onWeeklyDayToggle = { day ->
                                weeklyDays = if (weeklyDays.contains(day)) weeklyDays - day else weeklyDays + day
                            },
                            monthlyDayOfMonth = monthlyDayOfMonth,
                            onMonthlyDayChange = { monthlyDayOfMonth = it },
                            repeatEndType = repeatEndType,
                            onRepeatEndTypeChange = { repeatEndType = it },
                            repeatEndDate = repeatEndDate,
                            onRepeatEndDateClick = {
                                repeatEndType = RepeatEndType.END_DATE
                                showRepeatEndDatePicker = true
                            },
                            repeatOccurrenceCount = repeatOccurrenceCount,
                            onRepeatOccurrenceCountChange = { repeatOccurrenceCount = it.filter { ch -> ch.isDigit() } },
                            previewOccurrences = previewOccurrences
                        )
                    }
                }

                Divider()

                // Секция прикрепленного файла
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Прикрепленный файл", style = MaterialTheme.typography.titleSmall)
                        Row {
                            if (attachedFileName != null) {
                                IconButton(onClick = { 
                                    attachedFileName?.let { fileName ->
                                        viewModel.getFile(fileName)?.delete()
                                    }
                                    attachedFileName = null
                                    attachedFileDisplayName = null
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить файл")
                                }
                            }
                            IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Прикрепить файл")
                            }
                        }
                    }
                    
                    if (attachedFileName != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                try {
                                    val file = viewModel.getFile(attachedFileName!!)
                                    if (file != null && file.exists()) {
                                        val uri = viewModel.getFileUri(attachedFileName!!)
                                        if (uri != null) {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Открыть файл"))
                                        } else {
                                            Toast.makeText(context, "Не удалось открыть файл", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Файл не найден", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Не удалось открыть файл: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = attachedFileDisplayName ?: "Файл",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Нажмите для открытия",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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
                            categoryId = selectedCategory?.id,
                            startDateTime = startDateTime,
                            endDateTime = finalEndDateTime,
                            reminderEnabled = reminderEnabled,
                            reminderMinutes = reminderMinutes.toIntOrNull(),
                            priority = priority,
                            isRecurring = isRecurring,
                            recurrenceRule = buildRecurrenceRule(
                                isRecurring = isRecurring,
                                frequency = frequency,
                                interval = interval.toIntOrNull() ?: 1,
                                weeklyDays = weeklyDays,
                                monthlyDayOfMonth = monthlyDayOfMonth,
                                repeatEndType = repeatEndType,
                                repeatEndDate = repeatEndDate,
                                repeatOccurrenceCount = repeatOccurrenceCount.toIntOrNull()
                            ),
                            attachedFileName = attachedFileName
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
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) {
            CompositionLocalProvider(
                LocalContext provides russianContext,
                LocalConfiguration provides russianConfiguration
            ) {
                DatePicker(state = startDatePickerState)
            }
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
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = { TextButton(onClick = { showStartTimePicker = false }) { Text("Отмена") } }
        )
    }


    if (showRepeatEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showRepeatEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showRepeatEndDatePicker = false
                    repeatEndDatePickerState.selectedDateMillis?.let {
                        repeatEndDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = { TextButton(onClick = { showRepeatEndDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) {
            CompositionLocalProvider(
                LocalContext provides russianContext,
                LocalConfiguration provides russianConfiguration
            ) {
                DatePicker(state = repeatEndDatePickerState)
            }
        }
    }

    if (showScopeDialog) {
        val allowSeriesScope = scopeDialogState?.allowSeriesScope == true
        AlertDialog(
            onDismissRequest = {
                showScopeDialog = false
                viewModel.dismissRecurrenceScopeDialog()
            },
            title = { Text("Как применить изменения?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        showScopeDialog = false
                        viewModel.onRecurrenceScopeSelected(RecurrenceScope.THIS)
                    }) {
                        Text("Только эту задачу")
                    }
                    TextButton(
                        onClick = {
                            showScopeDialog = false
                            viewModel.onRecurrenceScopeSelected(RecurrenceScope.THIS_AND_FUTURE)
                        },
                        enabled = allowSeriesScope
                    ) {
                        Text("Эту и будущие задачи")
                    }
                    TextButton(
                        onClick = {
                            showScopeDialog = false
                            viewModel.onRecurrenceScopeSelected(RecurrenceScope.ENTIRE_SERIES)
                        },
                        enabled = allowSeriesScope
                    ) {
                        Text("Всю серию")
                    }
                    if (!allowSeriesScope) {
                        Text(
                            text = "Для применения к будущим задачам включите повторение и настройте правило.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showScopeDialog = false
                    viewModel.dismissRecurrenceScopeDialog()
                }) {
                    Text("Отмена")
                }
            }
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
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) {
            CompositionLocalProvider(
                LocalContext provides russianContext,
                LocalConfiguration provides russianConfiguration
            ) {
                DatePicker(state = endDatePickerState)
            }
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
                    Text(stringResource(R.string.ok))
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

private enum class RepeatEndType {
    NEVER, END_DATE, COUNT
}

private fun buildRecurrenceRule(
    isRecurring: Boolean,
    frequency: RecurrenceFrequency,
    interval: Int,
    weeklyDays: Set<DayOfWeek>,
    monthlyDayOfMonth: Int?,
    repeatEndType: RepeatEndType,
    repeatEndDate: LocalDate?,
    repeatOccurrenceCount: Int?
): RecurrenceRule? {
    if (!isRecurring) return null
    val endDate = when (repeatEndType) {
        RepeatEndType.END_DATE -> repeatEndDate
        else -> null
    }
    val occurrences = when (repeatEndType) {
        RepeatEndType.COUNT -> repeatOccurrenceCount?.takeIf { it > 0 }
        else -> null
    }
    return RecurrenceRule(
        frequency = frequency,
        interval = interval.coerceAtLeast(1),
        daysOfWeek = weeklyDays,
        dayOfMonth = monthlyDayOfMonth,
        endDate = endDate,
        occurrenceCount = occurrences
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurrenceConfig(
    frequency: RecurrenceFrequency,
    onFrequencyChange: (RecurrenceFrequency) -> Unit,
    interval: String,
    onIntervalChange: (String) -> Unit,
    weeklyDays: Set<DayOfWeek>,
    onWeeklyDayToggle: (DayOfWeek) -> Unit,
    monthlyDayOfMonth: Int?,
    onMonthlyDayChange: (Int?) -> Unit,
    repeatEndType: RepeatEndType,
    onRepeatEndTypeChange: (RepeatEndType) -> Unit,
    repeatEndDate: LocalDate?,
    onRepeatEndDateClick: () -> Unit,
    repeatOccurrenceCount: String,
    onRepeatOccurrenceCountChange: (String) -> Unit,
    previewOccurrences: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Параметры повторения", style = MaterialTheme.typography.titleMedium)
        FrequencySelector(frequency = frequency, onFrequencyChange = onFrequencyChange)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = interval,
                onValueChange = onIntervalChange,
                label = { Text("Интервал") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(120.dp)
            )
            Text("раз(а)")
        }

        when (frequency) {
            RecurrenceFrequency.WEEKLY, RecurrenceFrequency.WEEKLY_DAYS -> WeeklyDaySelector(
                selectedDays = weeklyDays,
                onToggle = onWeeklyDayToggle
            )
            RecurrenceFrequency.MONTHLY -> MonthlyDaySelector(
                dayOfMonth = monthlyDayOfMonth,
                onDayChange = onMonthlyDayChange
            )
            else -> Unit
        }

        RepeatEndSection(
            repeatEndType = repeatEndType,
            onRepeatEndTypeChange = onRepeatEndTypeChange,
            repeatEndDate = repeatEndDate,
            onRepeatEndDateClick = onRepeatEndDateClick,
            repeatOccurrenceCount = repeatOccurrenceCount,
            onRepeatOccurrenceCountChange = onRepeatOccurrenceCountChange
        )

        Text(
            text = "Будет создано $previewOccurrences задач",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencySelector(
    frequency: RecurrenceFrequency,
    onFrequencyChange: (RecurrenceFrequency) -> Unit
) {
    val options = listOf(
        RecurrenceFrequency.DAILY to "Ежедневно",
        RecurrenceFrequency.WEEKLY to "Еженедельно",
        RecurrenceFrequency.WEEKLY_DAYS to "По дням недели",
        RecurrenceFrequency.MONTHLY to "Ежемесячно"
    )
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = options.first { it.first == frequency }.second,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(),
            label = { Text("Частота") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onFrequencyChange(value)
                        expanded = false
                    }
                )
            }
        }
    }

}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun WeeklyDaySelector(
    selectedDays: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Дни недели")
        val ordered = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ordered.forEach { day ->
                val label = day.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                FilterChip(
                    selected = selectedDays.contains(day),
                    onClick = { onToggle(day) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun MonthlyDaySelector(
    dayOfMonth: Int?,
    onDayChange: (Int?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("День месяца")
        OutlinedTextField(
            value = dayOfMonth?.toString() ?: "",
            onValueChange = { value ->
                val filtered = value.filter { it.isDigit() }
                val number = filtered.takeIf { it.isNotBlank() }?.toInt()
                onDayChange(number?.coerceIn(1, 31))
            },
            label = { Text("Число") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(120.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatEndSection(
    repeatEndType: RepeatEndType,
    onRepeatEndTypeChange: (RepeatEndType) -> Unit,
    repeatEndDate: LocalDate?,
    onRepeatEndDateClick: () -> Unit,
    repeatOccurrenceCount: String,
    onRepeatOccurrenceCountChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Повторение заканчивается", style = MaterialTheme.typography.bodyMedium)
        RepeatEndRadio(
            label = "Никогда",
            selected = repeatEndType == RepeatEndType.NEVER,
            onClick = { onRepeatEndTypeChange(RepeatEndType.NEVER) }
        )
        RepeatEndRadio(
            label = "После даты",
            selected = repeatEndType == RepeatEndType.END_DATE,
            onClick = { onRepeatEndTypeChange(RepeatEndType.END_DATE) }
        )
        if (repeatEndType == RepeatEndType.END_DATE) {
            TextButton(onClick = onRepeatEndDateClick) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(repeatEndDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "Выбрать дату")
            }
        }
        RepeatEndRadio(
            label = "После количества повторов",
            selected = repeatEndType == RepeatEndType.COUNT,
            onClick = { onRepeatEndTypeChange(RepeatEndType.COUNT) }
        )
        if (repeatEndType == RepeatEndType.COUNT) {
            OutlinedTextField(
                value = repeatOccurrenceCount,
                onValueChange = onRepeatOccurrenceCountChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("Количество повторов") },
                modifier = Modifier.width(180.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatEndRadio(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

