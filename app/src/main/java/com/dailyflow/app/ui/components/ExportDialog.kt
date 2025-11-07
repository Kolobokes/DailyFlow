package com.dailyflow.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyflow.app.data.model.Task
import com.dailyflow.app.data.model.Note
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.util.ExportManager
import java.time.LocalDateTime

@Composable
fun ExportDialog(
    tasks: List<Task>,
    notes: List<Note>,
    categories: List<Category>,
    date: LocalDateTime,
    onDismiss: () -> Unit,
    onShare: (String, String) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.MARKDOWN) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Экспорт данных")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Выберите формат экспорта:")
                
                ExportFormat.values().forEach { format ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = format.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = format.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val content = when (selectedFormat) {
                        ExportFormat.MARKDOWN -> ExportManager.exportToMarkdown(tasks, notes, categories, date)
                        ExportFormat.PLAIN_TEXT -> ExportManager.exportToPlainText(tasks, notes, categories, date)
                        ExportFormat.HTML -> ExportManager.exportToHtml(tasks, notes, categories, date)
                    }
                    
                    val mimeType = when (selectedFormat) {
                        ExportFormat.MARKDOWN -> "text/markdown"
                        ExportFormat.PLAIN_TEXT -> "text/plain"
                        ExportFormat.HTML -> "text/html"
                    }
                    
                    onShare(content, mimeType)
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Поделиться")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

enum class ExportFormat(
    val displayName: String,
    val description: String
) {
    MARKDOWN("Markdown", "Форматированный текст с разметкой"),
    PLAIN_TEXT("Обычный текст", "Простой текстовый формат"),
    HTML("HTML", "Веб-страница для просмотра в браузере")
}
