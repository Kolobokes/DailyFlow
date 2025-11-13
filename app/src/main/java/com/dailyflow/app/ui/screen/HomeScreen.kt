package com.dailyflow.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.data.model.RecurrenceScope
import com.dailyflow.app.data.model.TaskStatus
import com.dailyflow.app.ui.components.SwipeableTaskCard
import com.dailyflow.app.ui.navigation.Screen
import com.dailyflow.app.ui.viewmodel.HomeViewModel
import com.dailyflow.app.ui.viewmodel.RecurringActionDialogState
import com.dailyflow.app.ui.viewmodel.RecurringActionType
import com.himanshoe.kalendar.Kalendar
import com.himanshoe.kalendar.KalendarEvent
import com.himanshoe.kalendar.KalendarEvents
import com.himanshoe.kalendar.KalendarType
import com.himanshoe.kalendar.color.KalendarColors
import com.himanshoe.kalendar.ui.firey.DaySelectionMode
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val tasks by viewModel.tasksForSelectedDate.collectAsState()
    val notes by viewModel.notesForSelectedDate.collectAsState()
    val overdueTasks by viewModel.overdueTasks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val dailyProgress by viewModel.dailyProgress.collectAsState()

    var showCalendar by remember { mutableStateOf(false) }
    var showRecurringActionDialog by remember { mutableStateOf(false) }
    var recurringDialogState by remember { mutableStateOf<RecurringActionDialogState?>(null) }

    val selectedLocalDate = selectedDate.toLocalDate()
    val selectedKotlinDate = selectedLocalDate.toKotlinLocalDate()

    LaunchedEffect(Unit) {
        viewModel.recurringActionDialog.collectLatest { state ->
            recurringDialogState = state
            showRecurringActionDialog = true
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(
                    Screen.TaskDetail.createRoute(null, selectedLocalDate.toString())
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectPreviousDay() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Предыдущий день")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showCalendar = !showCalendar }
                ) {
                Text(
                        text = selectedDate.format(
                            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ru"))
                        ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                    Icon(
                        imageVector = if (showCalendar) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showCalendar) "Свернуть календарь" else "Развернуть календарь"
                    )
                }
                IconButton(onClick = { viewModel.selectNextDay() }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Следующий день")
                }
            }

            if (showCalendar) {
                Kalendar(
                    currentDay = selectedKotlinDate,
                    kalendarType = KalendarType.Firey,
                    showLabel = true,
                    events = KalendarEvents(
                        events = tasks.mapNotNull { task ->
                            task.startDateTime?.toLocalDate()?.let { startDate ->
                                KalendarEvent(
                                    date = startDate.toKotlinLocalDate(),
                                    eventName = task.title
                                )
                            }
                        }
                    ),
                    kalendarColors = KalendarColors.default(),
                    daySelectionMode = DaySelectionMode.Single,
                    onDayClick = { date, _ ->
                        viewModel.updateSelectedDate(date.toJavaLocalDate().atStartOfDay())
                        showCalendar = false
                    },
                    headerContent = { _, _ -> }
                )
            }
            
            LinearProgressIndicator(
                progress = dailyProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                 if (overdueTasks.isNotEmpty()) {
                    Text(
                        text = "Просроченные задачи: ${overdueTasks.size}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable { navController.navigate(Screen.OverdueTasks.route) }
                    )
                }
                if (notes.isNotEmpty()) {
                    Text(
                        text = "Заметки на сегодня: ${notes.size}",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.Notes.createRoute(selectedLocalDate.toString()))
                        }
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                VerticalTimeline(
                    tasks = tasks,
                    categories = categories,
                    selectedDate = selectedLocalDate,
                    onTaskClick = { task ->
                        navController.navigate(Screen.TaskDetail.createRoute(task.id, null))
                    },
                    onToggle = { task, isCompleted ->
                        viewModel.toggleTaskCompletion(task.id, isCompleted)
                    },
                    onDelete = { task -> viewModel.deleteTask(task.id) },
                    onCancel = { task -> viewModel.cancelTask(task.id) },
                    onAddTask = { hour ->
                        val startTime = LocalTime.of(hour, 0)
                        val endTime = if (hour == 23) null else startTime.plusHours(1)
                        navController.navigate(
                            Screen.TaskDetail.createRoute(
                                taskId = null,
                                selectedDate = selectedLocalDate.toString(),
                                startTime = startTime.toString(),
                                endTime = endTime?.toString()
                            )
                        )
                    }
                )
            }
        }
    }

    if (showRecurringActionDialog && recurringDialogState != null) {
        val state = recurringDialogState!!
        val isDelete = state.actionType == RecurringActionType.DELETE
        AlertDialog(
            onDismissRequest = {
                showRecurringActionDialog = false
                viewModel.dismissRecurringActionDialog()
            },
            title = {
                Text(if (isDelete) "Удаление повторяющейся задачи" else "Отмена повторяющейся задачи")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Выберите, к каким задачам применить действие")
                    TextButton(onClick = {
                        showRecurringActionDialog = false
                        viewModel.onRecurringActionScopeSelected(RecurrenceScope.THIS)
                    }) {
                        Text("Только к этой задаче")
                    }
                    TextButton(onClick = {
                        showRecurringActionDialog = false
                        viewModel.onRecurringActionScopeSelected(RecurrenceScope.THIS_AND_FUTURE)
                    }) {
                        Text("К этой и будущим задачам")
                    }
                    TextButton(onClick = {
                        showRecurringActionDialog = false
                        viewModel.onRecurringActionScopeSelected(RecurrenceScope.ENTIRE_SERIES)
                    }) {
                        Text("Ко всей серии")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showRecurringActionDialog = false
                    viewModel.dismissRecurringActionDialog()
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun VerticalTimeline(
    tasks: List<com.dailyflow.app.data.model.Task>,
    categories: List<com.dailyflow.app.data.model.Category>,
    selectedDate: LocalDate,
    onTaskClick: (com.dailyflow.app.data.model.Task) -> Unit,
    onToggle: (com.dailyflow.app.data.model.Task, Boolean) -> Unit,
    onDelete: (com.dailyflow.app.data.model.Task) -> Unit,
    onCancel: (com.dailyflow.app.data.model.Task) -> Unit,
    onAddTask: (Int) -> Unit,
) {
    val hourHeight = 64.dp
    val scrollState = rememberScrollState()
    val dayStart = remember(selectedDate) { selectedDate.atStartOfDay() }
    val dayEnd = dayStart.plusDays(1)

    val currentDateTime by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(60_000L)
        }
    }

    data class TimelineTask(
        val task: com.dailyflow.app.data.model.Task,
        val effectiveStart: LocalDateTime,
        val effectiveEnd: LocalDateTime,
        val color: Color
    )

    val rawTimelineTasks = tasks.mapNotNull { task ->
        val start = task.startDateTime ?: return@mapNotNull null
        val end = task.endDateTime ?: start.plusHours(1)
        if (end <= dayStart || start >= dayEnd) return@mapNotNull null
        val adjustedStart = if (start.isBefore(dayStart)) dayStart else start
        val adjustedEnd = if (end.isAfter(dayEnd)) dayEnd else end
        val categoryColor = categories.find { it.id == task.categoryId }?.color?.let { Color(android.graphics.Color.parseColor(it)) }
            ?: Color.White
        TimelineTask(task, adjustedStart, adjustedEnd, categoryColor)
    }.sortedBy { it.effectiveStart }

    data class TimelineTaskSlot(
        val task: com.dailyflow.app.data.model.Task,
        val effectiveStart: LocalDateTime,
        val effectiveEnd: LocalDateTime,
        val color: Color,
        val slotIndex: Int,
        var parallelCount: Int
    )

    val slottedTasks = mutableListOf<TimelineTaskSlot>()
    val active = mutableListOf<TimelineTaskSlot>()
    rawTimelineTasks.forEach { current ->
        active.removeAll { it.effectiveEnd <= current.effectiveStart }
        val usedSlots = active.map { it.slotIndex }.toMutableSet()
        val nextSlot = generateSequence(0) { it + 1 }.first { it !in usedSlots }
        val slotted = TimelineTaskSlot(current.task, current.effectiveStart, current.effectiveEnd, current.color, nextSlot, 1)
        active.add(slotted)
        active.forEach { it.parallelCount = max(it.parallelCount, active.size) }
        slottedTasks.add(slotted)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = hourHeight * 24)
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier
                .height(hourHeight * 24)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
            ) {
                for (hour in 0..23) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(hourHeight),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:00", hour),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, end = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val density = LocalDensity.current
                val slotHeight = maxHeight / 24f
                val minuteHeight = slotHeight / 60f
                val outlineColor = MaterialTheme.colorScheme.outlineVariant
                val minuteHeightPx = with(density) { minuteHeight.toPx() }
                val slotHeightPx = with(density) { slotHeight.toPx() }
                var autoScrolled by remember(selectedDate) { mutableStateOf(false) }

                LaunchedEffect(selectedDate, maxHeight, autoScrolled) {
                    if (!autoScrolled && maxHeight > 0.dp) {
                        if (selectedDate == currentDateTime.toLocalDate()) {
                            val minutes = currentDateTime.hour * 60 + currentDateTime.minute
                            val targetPx = (minuteHeightPx * minutes - slotHeightPx).toInt().coerceAtLeast(0)
                            scrollState.scrollTo(targetPx)
                        } else {
                            scrollState.scrollTo(0)
                        }
                        autoScrolled = true
                    }
                }

                Column(modifier = Modifier.matchParentSize()) {
                    for (hour in 0..23) {
                        val hourStart = dayStart.plusHours(hour.toLong())
                        val hourEnd = if (hour == 23) dayEnd else hourStart.plusHours(1)
                        val occupiedTasks = slottedTasks.filter { it.effectiveStart < hourEnd && it.effectiveEnd > hourStart }
                        val occupied = occupiedTasks.isNotEmpty()
                        val slotColor = occupiedTasks.firstOrNull()?.color
                        HourSlotBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(slotHeight),
                            showAdd = !occupied,
                            hour = hour,
                            outlineColor = outlineColor,
                            slotColor = slotColor,
                            isCurrent = selectedDate == currentDateTime.toLocalDate() && currentDateTime.hour == hour,
                            onAddTask = onAddTask
                        )
                    }
                }

                slottedTasks.forEach { item ->
                    val startMinutes = Duration.between(dayStart, item.effectiveStart).toMinutes().coerceAtLeast(0).toInt()
                    val durationMinutes = Duration.between(item.effectiveStart, item.effectiveEnd).toMinutes().coerceAtLeast(15).toInt()
                    val topOffset = minuteHeight * startMinutes
                    val height = (minuteHeight * durationMinutes).coerceAtLeast(slotHeight / 2f)
                    val category = categories.find { it.id == item.task.categoryId }
                    val width = maxWidth / item.parallelCount
                    val xOffset = width * item.slotIndex
                    val statusBackground = when (item.task.status) {
                        TaskStatus.COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                        TaskStatus.CANCELLED -> Color(0xFFE53935).copy(alpha = 0.15f)
                        TaskStatus.PENDING -> item.color.copy(alpha = 0.6f)
                    }
                    SwipeableTaskCard(
                        task = item.task,
                        category = category,
                        onClick = { onTaskClick(item.task) },
                        onToggle = { isCompleted -> onToggle(item.task, isCompleted) },
                        onDelete = { onDelete(item.task) },
                        onCancel = { onCancel(item.task) },
                        modifier = Modifier
                            .width(width)
                            .offset(x = xOffset, y = topOffset)
                            .height(height)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        containerColorOverride = statusBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun HourSlotBox(
    modifier: Modifier,
    showAdd: Boolean,
    hour: Int,
    outlineColor: Color,
    slotColor: Color?,
    isCurrent: Boolean,
    onAddTask: (Int) -> Unit,
) {
    val baseColor = slotColor?.copy(alpha = 0.25f) ?: Color.Transparent
    val highlightOverlay = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
    val baseModifier = modifier
        .padding(vertical = 3.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(baseColor)

    if (showAdd) {
        Box(
            modifier = baseModifier
                .border(1.dp, outlineColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable { onAddTask(hour) },
        contentAlignment = Alignment.Center
    ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Добавить задачу",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        Box(
            modifier = baseModifier
                .background(highlightOverlay)
                .border(1.dp, outlineColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        )
    }
}