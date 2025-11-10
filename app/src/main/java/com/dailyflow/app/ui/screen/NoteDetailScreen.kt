package com.dailyflow.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.ChecklistItem
import com.dailyflow.app.ui.viewmodel.NoteDetailViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

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
    var isChecklist by remember { mutableStateOf(false) }
    val checklistItems = remember { mutableStateListOf<ChecklistItem>() }

    LaunchedEffect(uiState) {
        val note = uiState.note
        if (!uiState.isNewNote && note != null) {
            title = note.title
            content = note.content
            selectedCategory = uiState.categories.find { it.id == note.categoryId }
            dateTime = note.dateTime
            isCompleted = note.isCompleted
        }
        isChecklist = uiState.isChecklist
        checklistItems.clear()
        val initialItems = when {
            uiState.checklistItems.isNotEmpty() -> uiState.checklistItems
            isChecklist && note != null -> parseLegacyChecklist(note.content)
            else -> emptyList()
        }
        checklistItems.addAll(initialItems)
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
                            content = buildContent(isChecklist, content, checklistItems),
                            categoryId = selectedCategory?.id,
                            dateTime = dateTime,
                            isCompleted = isCompleted,
                            isChecklist = isChecklist,
                            checklistItems = sanitizedChecklist(checklistItems)
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

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Чек-лист")
                Switch(checked = isChecklist, onCheckedChange = { checked ->
                    if (checked && !isChecklist) {
                        if (checklistItems.isEmpty() && content.isNotBlank()) {
                            checklistItems.clear()
                            checklistItems.addAll(content.lines().filter { it.isNotBlank() }.map { ChecklistItem(UUID.randomUUID().toString(), it, false) })
                        }
                    }
                    if (!checked && isChecklist) {
                        content = checklistItems.joinToString("\n") { it.text }
                    }
                    isChecklist = checked
                })
            }

            if (isChecklist) {
                ChecklistEditor(
                    items = checklistItems,
                    onUpdateItem = { index, item -> checklistItems[index] = item },
                    onRemoveItem = { index -> checklistItems.removeAt(index) },
                    onAddItem = {
                        checklistItems.add(ChecklistItem(UUID.randomUUID().toString(), "", false))
                    }
                )
            } else {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Содержание") },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
            
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
                        content = buildContent(isChecklist, content, checklistItems),
                        categoryId = selectedCategory?.id,
                        dateTime = dateTime,
                        isCompleted = isCompleted,
                        isChecklist = isChecklist,
                        checklistItems = sanitizedChecklist(checklistItems)
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

private fun buildContent(isChecklist: Boolean, content: String, items: List<ChecklistItem>): String {
    return if (isChecklist) {
        sanitizedChecklist(items).joinToString("\n") { "${it.isChecked},${it.text}" }
    } else {
        content
    }
}

private fun sanitizedChecklist(items: List<ChecklistItem>): List<ChecklistItem> =
    items.filter { it.text.isNotBlank() }.map { item ->
        if (item.id.isBlank()) item.copy(id = UUID.randomUUID().toString()) else item
    }

private fun parseLegacyChecklist(content: String): List<ChecklistItem> =
    content.lines()
        .mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val parts = line.split(",")
            val checked = parts.getOrNull(0)?.toBooleanStrictOrNull() ?: false
            val text = parts.getOrNull(1) ?: parts.lastOrNull() ?: ""
            ChecklistItem(UUID.randomUUID().toString(), text, checked)
        }

@Composable
private fun ChecklistEditor(
    items: List<ChecklistItem>,
    onUpdateItem: (Int, ChecklistItem) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onAddItem: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                ChecklistItemRow(
                    item = item,
                    onCheckedChange = { checked -> onUpdateItem(index, item.copy(isChecked = checked)) },
                    onTextChange = { text -> onUpdateItem(index, item.copy(text = text)) },
                    onRemove = { onRemoveItem(index) }
                )
            }
        }
        OutlinedButton(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить пункт")
        }
    }
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = item.isChecked, onCheckedChange = onCheckedChange)
        OutlinedTextField(
            value = item.text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Пункт") },
            textStyle = LocalTextStyle.current.copy(textDecoration = if (item.isChecked) TextDecoration.LineThrough else null)
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Удалить пункт")
        }
    }
}
