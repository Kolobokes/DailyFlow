package com.dailyflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dailyflow.app.data.model.*
import com.dailyflow.app.ui.theme.OverdueColor
import java.time.LocalDateTime
import java.time.LocalTime
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
