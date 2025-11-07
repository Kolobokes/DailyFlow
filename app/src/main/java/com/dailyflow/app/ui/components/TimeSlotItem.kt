package com.dailyflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Undo

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
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(
                color = when {
                    task.status == TaskStatus.COMPLETED -> Color.LightGray.copy(alpha = 0.4f)
                    isOverdue -> OverdueColor.copy(alpha = 0.2f)
                    else -> category?.color?.let { Color(android.graphics.Color.parseColor(it)) }?.copy(alpha = 0.2f)
                        ?: MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(4.dp)
            )
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
    var showMenu by remember { mutableStateOf(false) }

    val priorityColor = when (task.priority) {
        Priority.HIGH -> HighPriorityColor
        Priority.MEDIUM -> MediumPriorityColor
        Priority.LOW -> LowPriorityColor
    }

    val isCancelled = task.status == TaskStatus.CANCELLED
    val categoryColor = category?.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.White
    val baseColor = containerColorOverride ?: categoryColor
    val containerColor = when {
        task.status == TaskStatus.COMPLETED -> baseColor.copy(alpha = 0.45f)
        isCancelled -> baseColor.copy(alpha = 0.35f)
        isOverdue -> baseColor.copy(alpha = 0.6f)
        else -> baseColor
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.status == TaskStatus.COMPLETED || isCancelled) 
                        TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.status == TaskStatus.COMPLETED || isCancelled) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface
                )
                
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
