package com.dailyflow.app.ui.screen

import androidx.compose.foundation.layout.*
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.InsertDriveFile
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.R
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.ChecklistItem
import com.dailyflow.app.ui.viewmodel.NoteDetailViewModel
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

import androidx.appcompat.app.AppCompatDelegate // Added import

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    navController: NavController,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    
    // Инициализируем категорию при загрузке или при изменении списка категорий
    LaunchedEffect(uiState.categories, viewModel.initialCategoryId) {
        if (selectedCategory == null && viewModel.initialCategoryId != null && uiState.categories.isNotEmpty()) {
            selectedCategory = uiState.categories.find { it.id == viewModel.initialCategoryId }
        }
    }
    var dateTime by remember { mutableStateOf<java.time.LocalDateTime?>(null) }
    var isCompleted by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val context = LocalContext.current
    val currentLocale = remember(context) { 
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) appLocales.get(0) ?: Locale.getDefault() else Locale.getDefault()
    }
    
    val localizedContext = remember(context, currentLocale) {
        val config = Configuration(context.resources.configuration)
        config.setLocale(currentLocale)
        context.createConfigurationContext(config)
    }
    val localizedConfiguration = remember(localizedContext) { localizedContext.resources.configuration }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isChecklist by remember { mutableStateOf(false) }
    val checklistItems = remember { mutableStateListOf<ChecklistItem>() }
    var attachedFileName by remember { mutableStateOf<String?>(null) }
    var attachedFileDisplayName by remember { mutableStateOf<String?>(null) }
    val currentNoteId = remember(uiState.note?.id) { 
        uiState.note?.id ?: UUID.randomUUID().toString() 
    }
    
    // Сохраняем начальные значения для отслеживания изменений
    var initialTitle by remember { mutableStateOf("") }
    var initialContent by remember { mutableStateOf("") }
    var initialCategory by remember { mutableStateOf<Category?>(null) }
    var initialDateTime by remember { mutableStateOf<java.time.LocalDateTime?>(null) }
    var initialIsCompleted by remember { mutableStateOf(false) }
    var initialIsChecklist by remember { mutableStateOf(false) }
    val initialChecklistItems = remember { mutableStateListOf<ChecklistItem>() }
    var initialAttachedFileUri by remember { mutableStateOf<String?>(null) }
    
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    
    // Launcher для выбора файла
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val fileName = viewModel.copyFileToStorage(it, currentNoteId)
                if (fileName != null) {
                    attachedFileName = fileName
                    // Получаем отображаемое имя файла
                    try {
                        context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0 && cursor.moveToFirst()) {
                                attachedFileDisplayName = cursor.getString(nameIndex) ?: context.getString(R.string.file_default_name)
                            } else {
                                attachedFileDisplayName = context.getString(R.string.file_default_name)
                            }
                        } ?: run {
                            attachedFileDisplayName = it.path?.substringAfterLast('/') ?: context.getString(R.string.file_default_name)
                        }
                    } catch (e: Exception) {
                        attachedFileDisplayName = context.getString(R.string.file_default_name)
                    }
                        } else {
                            Toast.makeText(context, context.getString(R.string.file_save_error), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            val exportNoteTimestampFormatter = remember { DateTimeFormatter.ofPattern("yyyyMMdd_HHmm") }
    var pendingNoteExport by remember { mutableStateOf<String?>(null) }
    val noteExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val text = pendingNoteExport
        if (uri != null && text != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(text.toByteArray(StandardCharsets.UTF_8))
                }
                            }.onSuccess {
                                Toast.makeText(context, context.getString(R.string.note_saved_to_file), Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, context.getString(R.string.file_save_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                        pendingNoteExport = null
                    }
                
                    LaunchedEffect(uiState) {
                        val note = uiState.note
                        if (!uiState.isNewNote && note != null) {
                            title = note.title
                            content = note.content
                            selectedCategory = uiState.categories.find { it.id == note.categoryId }
                            dateTime = note.dateTime
                            isCompleted = note.isCompleted
                            attachedFileName = note.attachedFileUri
                            // Получаем отображаемое имя файла из сохраненного файла
                            if (note.attachedFileUri != null) {
                                val file = viewModel.getFile(note.attachedFileUri!!)
                                attachedFileDisplayName = file?.name?.let { fileName ->
                                    // Извлекаем оригинальное имя из формата: noteId_originalFileName
                                    val noteIdPrefix = "${note.id}_"
                                    if (fileName.startsWith(noteIdPrefix)) {
                                        fileName.removePrefix(noteIdPrefix)
                                    } else {
                                        fileName
                                    }
                                } ?: context.getString(R.string.file_default_name)
                            } else {
                                attachedFileDisplayName = null
                            }
            
            // Сохраняем начальные значения
            initialTitle = note.title
            initialContent = note.content
            initialCategory = uiState.categories.find { it.id == note.categoryId }
            initialDateTime = note.dateTime
            initialIsCompleted = note.isCompleted
            initialAttachedFileUri = note.attachedFileUri
        } else {
            // Для новой заметки начальные значения пустые
            initialTitle = ""
            initialContent = ""
            initialCategory = null
            initialDateTime = null
            initialIsCompleted = false
            initialAttachedFileUri = null
        }
        isChecklist = uiState.isChecklist
        initialIsChecklist = uiState.isChecklist
        checklistItems.clear()
        initialChecklistItems.clear()
        val initialItems = when {
            uiState.checklistItems.isNotEmpty() -> uiState.checklistItems
            isChecklist && note != null -> parseLegacyChecklist(note.content)
            else -> emptyList()
        }
        checklistItems.addAll(initialItems)
        initialChecklistItems.addAll(initialItems)
    }
    
    // Функция для проверки наличия изменений
    fun hasUnsavedChanges(): Boolean {
        val currentContent = if (isChecklist) {
            sanitizedChecklist(checklistItems).joinToString("\n") { "${it.isChecked},${it.text}" }
        } else {
            content
        }
        val savedInitialContent = if (initialIsChecklist) {
            sanitizedChecklist(initialChecklistItems).joinToString("\n") { "${it.isChecked},${it.text}" }
        } else {
            initialContent
        }
        
        return title != initialTitle ||
                currentContent != savedInitialContent ||
                selectedCategory?.id != initialCategory?.id ||
                dateTime != initialDateTime ||
                isCompleted != initialIsCompleted ||
                isChecklist != initialIsChecklist ||
                attachedFileName != initialAttachedFileUri ||
                checklistItems.size != initialChecklistItems.size ||
                checklistItems.zip(initialChecklistItems).any { (current, initial) ->
                    current.text != initial.text || current.isChecked != initial.isChecked
                }
    }
    
    // Функция для закрытия с проверкой изменений
    fun handleBackNavigation() {
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog = true
        } else {
            navController.popBackStack()
        }
    }
    
    // Функция для сохранения и закрытия
    fun saveAndClose() {
        keyboardController?.hide()
        viewModel.saveNote(
            title = title,
            content = buildContent(isChecklist, content, checklistItems),
            categoryId = selectedCategory?.id,
            dateTime = dateTime,
            isCompleted = isCompleted,
            isChecklist = isChecklist,
            checklistItems = sanitizedChecklist(checklistItems),
            attachedFileName = attachedFileName,
            noteIdForNewNote = if (uiState.isNewNote) currentNoteId else null
        )
        navController.popBackStack()
    }
    
    // Функция для открытия прикрепленного файла
    fun openAttachedFile(fileName: String) {
        try {
            val file = viewModel.getFile(fileName)
            if (file != null && file.exists()) {
                val uri = viewModel.getFileUri(fileName)
                if (uri != null) {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.note_open_file)))
                } else {
                    Toast.makeText(context, context.getString(R.string.note_file_open_error, ""), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, context.getString(R.string.note_file_not_found), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.note_file_open_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }
    
    // Функция для удаления прикрепленного файла
    fun removeAttachedFile() {
        attachedFileName?.let { fileName ->
            // Удаляем файл из хранилища
            val file = viewModel.getFile(fileName)
            file?.delete()
        }
        attachedFileName = null
        attachedFileDisplayName = null
    }

    // Перехватываем системную кнопку "Назад"
    BackHandler(enabled = true) {
        handleBackNavigation()
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
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) {
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
    
    // Диалог подтверждения несохраненных изменений
    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text(stringResource(R.string.unsaved_changes_title)) },
            text = { Text(stringResource(R.string.unsaved_changes_message)) },
            confirmButton = {
                Button(onClick = {
                    showUnsavedChangesDialog = false
                    saveAndClose()
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showUnsavedChangesDialog = false
                        navController.popBackStack()
                    }) {
                        Text(stringResource(R.string.discard_changes))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { showUnsavedChangesDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewNote) stringResource(R.string.new_note) else stringResource(R.string.edit_note)) },
                navigationIcon = {
                    IconButton(onClick = { handleBackNavigation() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!uiState.isNewNote) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val exportText = viewModel.exportCurrentNote()
                                if (exportText == null) {
                                    Toast.makeText(context, context.getString(R.string.note_export_failed), Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val baseName = (uiState.note?.title ?: "note").ifBlank { "note" }
                                val sanitized = baseName.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(40)
                                val timestamp = LocalDateTime.now().format(exportNoteTimestampFormatter)
                                val fileName = "${sanitized.ifBlank { "note" }}_$timestamp.txt"
                                pendingNoteExport = exportText
                                noteExportLauncher.launch(fileName)
                            }
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.task_detail_export_task))
                        }
                        IconButton(onClick = { 
                            viewModel.deleteNote()
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                    IconButton(onClick = { saveAndClose() }) {
                        Icon(Icons.Default.Done, contentDescription = stringResource(R.string.save))
                    }
                }
            )
        }
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.note_title)) },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.task_category)) },
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.checklist))
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
                val listState = rememberLazyListState()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        itemsIndexed(checklistItems, key = { _, item -> item.id }) { index, item ->
                            ChecklistItemRow(
                                item = item,
                                onCheckedChange = { checked -> checklistItems[index] = item.copy(isChecked = checked) },
                                onTextChange = { text -> checklistItems[index] = item.copy(text = text) },
                                onRemove = { checklistItems.removeAt(index) }
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val newIndex = checklistItems.size
                            checklistItems.add(ChecklistItem(UUID.randomUUID().toString(), "", false))
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(150)
                                if (newIndex < listState.layoutInfo.totalItemsCount) {
                                    listState.animateScrollToItem(newIndex)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.add_item))
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = content,
                                onValueChange = { content = it },
                                label = { Text(stringResource(R.string.note_content)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    // Задержка для появления клавиатуры
                                    kotlinx.coroutines.delay(300)
                                    // Прокручиваем немного вниз от текущей позиции, чтобы поле было видно
                                    val currentScroll = scrollState.value
                                    val targetScroll = (currentScroll + 150).coerceAtMost(scrollState.maxValue)
                                    scrollState.animateScrollTo(targetScroll)
                                }
                            }
                        },
                    minLines = 5,
                    maxLines = Int.MAX_VALUE,
                    singleLine = false
                )
            }
            
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
                    Text(stringResource(R.string.note_attached_file), style = MaterialTheme.typography.titleSmall)
                    Row {
                        if (attachedFileName != null) {
                            IconButton(onClick = { removeAttachedFile() }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.note_delete_file))
                            }
                        }
                        IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                            Icon(Icons.Default.AttachFile, contentDescription = stringResource(R.string.note_attach_file))
                        }
                    }
                }
                
                if (attachedFileName != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { openAttachedFile(attachedFileName!!) }
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
                                    text = attachedFileDisplayName ?: stringResource(R.string.file_default_name),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = stringResource(R.string.note_click_to_open),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = isCompleted, onCheckedChange = { isCompleted = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.task_status_completed), modifier = Modifier.weight(1f))
            }
            
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = dateTime?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: stringResource(R.string.select_date))
            }

            Button(
                onClick = { saveAndClose() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
            if (!uiState.isNewNote && uiState.note != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Создано: ${uiState.note!!.createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Обновлено: ${uiState.note!!.updatedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                ChecklistItemRow(
                    item = item,
                    onCheckedChange = { checked -> onUpdateItem(index, item.copy(isChecked = checked)) },
                    onTextChange = { text -> onUpdateItem(index, item.copy(text = text)) },
                    onRemove = { onRemoveItem(index) }
                )
            }
        }
        OutlinedButton(
            onClick = onAddItem,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_item))
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = item.text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.checklist_item_placeholder)) },
            textStyle = LocalTextStyle.current.copy(textDecoration = if (item.isChecked) TextDecoration.LineThrough else null),
            minLines = 1,
            maxLines = 5,
            singleLine = false
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_item))
        }
    }
}
