package com.dailyflow.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dailyflow.app.R
import com.dailyflow.app.data.model.*
import com.dailyflow.app.ui.theme.HighPriorityColor
import com.dailyflow.app.ui.theme.LowPriorityColor
import com.dailyflow.app.ui.theme.MediumPriorityColor
import com.dailyflow.app.ui.theme.OverdueColor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableTaskCard(
    task: Task,
    category: Category?,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Используем key для пересоздания dismissState при изменении статуса задачи
    key("${task.id}_${task.status}") {
        val dismissState = rememberDismissState(
            confirmStateChange = { dismissValue ->
                val isCompleted = task.status == TaskStatus.COMPLETED
                when (dismissValue) {
                    DismissValue.DismissedToEnd -> {
                        // Свайп вправо - выполнить задачу (PENDING -> COMPLETED)
                        if (!isCompleted) {
                            onToggle(true)
                        }
                        false // Не подтверждаем dismiss, чтобы карточка вернулась в исходное положение
                    }
                    DismissValue.DismissedToStart -> {
                        // Свайп влево - вернуть в работу (COMPLETED -> PENDING)
                        if (isCompleted) {
                            onToggle(false)
                        }
                        false // Не подтверждаем dismiss, чтобы карточка вернулась в исходное положение
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

        SwipeableTaskCardContent(
            task = task,
            category = category,
            onClick = onClick,
            onToggle = onToggle,
            onDelete = onDelete,
            onCancel = onCancel,
            dismissState = dismissState,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwipeableTaskCardContent(
    task: Task,
    category: Category?,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    dismissState: androidx.compose.material.DismissState,
    modifier: Modifier = Modifier
) {
    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
        background = {
            val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
            val isCompleted = task.status == TaskStatus.COMPLETED

            val (backgroundColor, icon, text) = when (direction) {
                DismissDirection.StartToEnd -> Triple(
                    if (!isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                    Icons.Default.Check,
                    stringResource(R.string.task_status_completed)
                )
                DismissDirection.EndToStart -> Triple(
                    if (isCompleted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else Color.Transparent,
                    Icons.AutoMirrored.Filled.Undo,
                    stringResource(R.string.task_status_pending)
                )
            }

            val alignment = when (direction) {
                DismissDirection.StartToEnd -> Alignment.CenterStart
                DismissDirection.EndToStart -> Alignment.CenterEnd
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(animateColorAsState(targetValue = backgroundColor, label = "").value)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, contentDescription = text, tint = MaterialTheme.colorScheme.onPrimary)
                    Text(text, color = MaterialTheme.colorScheme.onPrimary)
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
                modifier = modifier
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    category: Category?,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOverdue = task.startDateTime?.isBefore(LocalDateTime.now()) == true && task.status == TaskStatus.PENDING
    val priorityColor = when (task.priority) {
        Priority.HIGH -> HighPriorityColor
        Priority.MEDIUM -> MediumPriorityColor
        Priority.LOW -> LowPriorityColor
    }
    val isCancelled = task.status == TaskStatus.CANCELLED
    val statusColor = when (task.status) {
        TaskStatus.COMPLETED -> Color(0xFF4CAF50)
        TaskStatus.CANCELLED -> Color(0xFFE53935)
        TaskStatus.PENDING -> MaterialTheme.colorScheme.secondary
    }

    val baseContainerColor = when (task.status) {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (task.priority == Priority.HIGH || task.priority == Priority.MEDIUM) {
                            val starColor = when (task.priority) {
                                Priority.HIGH -> HighPriorityColor
                                Priority.MEDIUM -> Color(0xFFFFC107)
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
                                .background(priorityColor, CircleShape)
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
                                    if (task.status == TaskStatus.COMPLETED) Icons.AutoMirrored.Filled.Undo else Icons.Default.Check,
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
