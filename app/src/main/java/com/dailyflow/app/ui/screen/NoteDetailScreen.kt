package com.dailyflow.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.ui.viewmodel.NoteDetailViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    navController: NavController,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var dateTime by remember { mutableStateOf<java.time.LocalDateTime?>(null) }
    var isCompleted by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(uiState) {
        if (!uiState.isNewNote && uiState.note != null) {
            val note = uiState.note!!
            title = note.title
            content = note.content
            selectedCategory = uiState.categories.find { it.id == note.categoryId }
            dateTime = note.dateTime
            isCompleted = note.isCompleted
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dateTime = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Отмена") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewNote) "Новая заметка" else "Редактировать заметку") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (!uiState.isNewNote) {
                        IconButton(onClick = { 
                            viewModel.deleteNote()
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                    IconButton(onClick = { 
                        viewModel.saveNote(
                            title = title,
                            content = content,
                            categoryId = selectedCategory?.id,
                            dateTime = dateTime,
                            isCompleted = isCompleted
                        )
                        navController.popBackStack()
                     }) {
                        Icon(Icons.Default.Done, contentDescription = "Сохранить")
                    }
                }
            )
        }
    ) {
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
                label = { Text("Заголовок") },
                modifier = Modifier.fillMaxWidth()
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

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Содержание") },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isCompleted, onCheckedChange = { isCompleted = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Выполнено")
            }
            
            Button(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = dateTime?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "Выбрать дату")
            }

            Button(
                onClick = { 
                    viewModel.saveNote(
                        title = title,
                        content = content,
                        categoryId = selectedCategory?.id,
                        dateTime = dateTime,
                        isCompleted = isCompleted
                    )
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
        }
    }
}
