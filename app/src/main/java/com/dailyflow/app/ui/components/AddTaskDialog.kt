package com.dailyflow.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.Priority
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAddTask: (String, String?, String, LocalDateTime?, LocalDateTime?, Boolean, Int?, Priority) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf<String?>("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var startDateTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var endDateTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var reminder by remember { mutableStateOf(false) }
    var reminderMinutes by remember { mutableStateOf<Int?>(null) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var priorityMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Добавить задачу")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description ?: "",
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                        categories.forEach { category ->
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
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = priorityMenuExpanded, onExpandedChange = { priorityMenuExpanded = !priorityMenuExpanded }) {
                    OutlinedTextField(
                        value = priority.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Приоритет") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = priorityMenuExpanded, onDismissRequest = { priorityMenuExpanded = false }) {
                        Priority.values().forEach { priorityValue ->
                            DropdownMenuItem(
                                text = { Text(priorityValue.name) },
                                onClick = {
                                    priority = priorityValue
                                    priorityMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAddTask(title, description, selectedCategory?.id ?: "", startDateTime, endDateTime, reminder, reminderMinutes, priority)
                    onDismiss()
                }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Отмена")
            }
        }
    )
}
