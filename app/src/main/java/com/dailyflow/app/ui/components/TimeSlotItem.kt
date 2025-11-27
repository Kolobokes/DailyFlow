package com.dailyflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dailyflow.app.data.model.*
import com.dailyflow.app.ui.theme.*
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun TimeSlotItem(
    timeSlot: LocalTime,
    tasks: List<Task>,
    notes: List<Note>,
    categories: List<Category>,
    onTaskClick: (String) -> Unit,
    onNoteClick: (String) -> Unit,
    onTaskToggle: (String, Boolean) -> Unit,
    onNoteToggle: (String, Boolean) -> Unit,
    onDeleteTask: (String) -> Unit,
    onCancelTask: (String) -> Unit,
    onEmptySlotClick: (LocalTime) -> Unit,
    selectedDate: LocalDateTime
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Time column
        Column(
            modifier = Modifier.width(60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeSlot.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tasks
            tasks.forEach { task ->
                val taskStartOnThisDay = task.startDateTime?.toLocalDate() == selectedDate.toLocalDate()
                val showFullCard = if (taskStartOnThisDay) {
                    task.startDateTime?.hour == timeSlot.hour
                } else {
                    timeSlot.hour == 0
                }

                if (showFullCard) {
                    SwipeableTaskCard(
                        task = task,
                        category = categories.find { it.id == task.categoryId },
                        onClick = { onTaskClick(task.id) },
                        onToggle = { isCompleted -> onTaskToggle(task.id, isCompleted) },
                        onDelete = { onDeleteTask(task.id) },
                        onCancel = { onCancelTask(task.id) }
                    )
                } else {
                    ContinuingTaskCard(task = task, category = categories.find { it.id == task.categoryId })
                }
            }

            // Notes
            notes.forEach { note ->
                 SwipeableNoteCard(
                    note = note,
                    category = note.categoryId?.let { id -> categories.find { it.id == id } },
                    onClick = { onNoteClick(note.id) },
                    onToggle = { isCompleted -> onNoteToggle(note.id, isCompleted) }
                )
            }

            // Empty slot indicator
            if (tasks.isEmpty() && notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onEmptySlotClick(timeSlot) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Добавить задачу",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ContinuingTaskCard(task: Task, category: Category?) {
    val isOverdue = task.startDateTime?.isBefore(LocalDateTime.now()) == true && task.status == TaskStatus.PENDING
    val background = when (task.status) {
        TaskStatus.COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.12f)
        TaskStatus.CANCELLED -> Color(0xFFE53935).copy(alpha = 0.12f)
        TaskStatus.PENDING -> category?.color?.let { Color(android.graphics.Color.parseColor(it)) }?.copy(alpha = 0.2f)
            ?: MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(
                color = if (isOverdue && task.status == TaskStatus.PENDING) OverdueColor.copy(alpha = 0.2f) else background,
                shape = RoundedCornerShape(4.dp)
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SwipeableTaskCard(
    task: Task,
    category: Category?,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    containerColorOverride: Color? = null
) {
    val dismissState = rememberDismissState(
        confirmStateChange = { dismissValue ->
            when (dismissValue) {
                DismissValue.DismissedToEnd -> {
                    // Свайп вправо - выполнить задачу (PENDING -> COMPLETED)
                    if (task.status == TaskStatus.PENDING) {
                        onToggle(true)
                    }
                    false // Не подтверждаем, чтобы состояние сбросилось
                }
                DismissValue.DismissedToStart -> {
                    // Свайп влево - вернуть в работу (COMPLETED -> PENDING)
                    if (task.status == TaskStatus.COMPLETED) {
                        onToggle(false) // Вернуть в статус PENDING (не выполнено)
                    }
                    false // Не подтверждаем, чтобы состояние сбросилось
                }
                else -> false
            }
        }
    )
    
    // Сбрасываем состояние после действия с задержкой для завершения анимации
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != DismissValue.Default) {
            kotlinx.coroutines.delay(350) // Задержка для завершения анимации
            dismissState.reset()
        }
    }

    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
        background = {
            val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
            val completing = direction == DismissDirection.StartToEnd && task.status == TaskStatus.PENDING
            val returning = direction == DismissDirection.EndToStart && task.status == TaskStatus.COMPLETED
            val bgColor = when {
                completing -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                returning -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val icon = when {
                completing -> Icons.Default.Check
                returning -> Icons.AutoMirrored.Filled.Undo
                else -> Icons.Default.Check
            }
            val label = when {
                completing -> "Выполнить"
                returning -> "Не выполнено"
                else -> "Действие"
            }
            val alignment = if (direction == DismissDirection.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        dismissContent = {
            TaskCard(
                task = task,
                category = category,
                onClick = onClick,
                onToggle = onToggle,
                onDelete = onDelete,
                onCancel = onCancel,
                modifier = modifier,
                containerColorOverride = containerColorOverride
            )
        }
    )
}

@Composable
fun TaskCard(
    task: Task,
    category: Category?,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    containerColorOverride: Color? = null
) {
    val isOverdue = task.startDateTime?.isBefore(LocalDateTime.now()) == true && task.status == TaskStatus.PENDING
    val priorityColor = when (task.priority) {
        Priority.HIGH -> HighPriorityColor
        Priority.MEDIUM -> MediumPriorityColor
        Priority.LOW -> LowPriorityColor
    }
    val isCancelled = task.status == TaskStatus.CANCELLED
    val (statusColor, statusLabel) = when (task.status) {
        TaskStatus.COMPLETED -> Color(0xFF4CAF50) to "Выполнена"
        TaskStatus.CANCELLED -> Color(0xFFE53935) to "Отменена"
        TaskStatus.PENDING -> MaterialTheme.colorScheme.secondary to "В работе"
    }

    val baseContainerColor = containerColorOverride ?: when (task.status) {
        TaskStatus.COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.12f)
        TaskStatus.CANCELLED -> Color(0xFFE53935).copy(alpha = 0.12f)
        TaskStatus.PENDING -> category?.color?.let { Color(android.graphics.Color.parseColor(it)) }?.copy(alpha = 0.2f)
            ?: MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .statusMarker(statusColor)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = baseContainerColor)
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Заголовок задачи с индикатором важности
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Отображаем звездочку для MEDIUM (желтый) и HIGH (красный) приоритетов
                        if (task.priority == Priority.HIGH || task.priority == Priority.MEDIUM) {
                            val starColor = when (task.priority) {
                                Priority.HIGH -> HighPriorityColor
                                Priority.MEDIUM -> Color(0xFFFFC107) // Желтый цвет для MEDIUM
                                else -> HighPriorityColor
                            }
                            Icon(
                                Icons.Default.Star,
                                "Важно",
                                tint = starColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (task.status == TaskStatus.COMPLETED || isCancelled)
                                TextDecoration.LineThrough else TextDecoration.None,
                            color = if (task.status == TaskStatus.COMPLETED || isCancelled)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    val timeText = if (task.startDateTime != null && task.endDateTime != null) {
                        "${task.startDateTime.format(timeFormatter)} - ${task.endDateTime.format(timeFormatter)}"
                    } else {
                        task.startDateTime?.format(timeFormatter) ?: ""
                    }
                    if (timeText.isNotEmpty()) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Убрали текстовый статус - оставляем только визуальные индикаторы
                        Spacer(modifier = Modifier.width(0.dp))
                        if (isOverdue) {
                            Icon(Icons.Default.Warning, "Просрочено", tint = OverdueColor, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        category?.let { cat ->
                            Icon(
                                getCategoryIcon(cat.icon),
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
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                    }
                }
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Действия")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (task.status == TaskStatus.COMPLETED) "Вернуть в работу" else "Отметить выполненной") },
                        onClick = {
                            onToggle(task.status != TaskStatus.COMPLETED)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                if (task.status == TaskStatus.COMPLETED) Icons.Default.Undo else Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Отменить") },
                        onClick = {
                            onCancel()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
                }
            }
        }
    }
}

private fun Modifier.statusMarker(color: Color): Modifier =
    this.drawBehind {
        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset.Zero,
            size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)
        )
    }

@Composable
fun NoteCard(
    note: Note,
    category: Category?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Заметка: ${note.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (note.isCompleted) 
                        TextDecoration.LineThrough else TextDecoration.None,
                    color = if (note.isCompleted) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2
                )
                
                category?.let { cat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            getCategoryIcon(cat.icon),
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
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "work" -> Icons.Default.Work
        "home" -> Icons.Default.Home
        "health" -> Icons.Default.LocalHospital
        "education" -> Icons.Default.School
        "finance" -> Icons.Default.AttachMoney
        "shopping" -> Icons.Default.ShoppingCart
        "sport" -> Icons.Default.Sports
        else -> Icons.Default.Category
    }
}
