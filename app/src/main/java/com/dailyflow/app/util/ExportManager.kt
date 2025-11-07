package com.dailyflow.app.util

import com.dailyflow.app.data.model.Task
import com.dailyflow.app.data.model.Note
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.TaskStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ExportManager {
    
    fun exportToMarkdown(
        tasks: List<Task>,
        notes: List<Note>,
        categories: List<Category>,
        date: LocalDateTime
    ): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale("ru"))
        val dateString = date.format(formatter)
        
        val sb = StringBuilder()
        
        // Header
        sb.appendLine("# 📅 $dateString")
        sb.appendLine()
        
        // Completed tasks
        val completedTasks = tasks.filter { it.status == TaskStatus.COMPLETED }
        if (completedTasks.isNotEmpty()) {
            sb.appendLine("## ✅ Выполнено:")
            completedTasks.forEach { task ->
                val category = categories.find { it.id == task.categoryId }
                val categoryEmoji = getCategoryEmoji(category?.icon)
                sb.appendLine("- [x] $categoryEmoji ${task.title}")
                task.description?.let { description ->
                    sb.appendLine("  - $description")
                }
            }
            sb.appendLine()
        }
        
        // Pending tasks
        val pendingTasks = tasks.filter { it.status == TaskStatus.PENDING }
        if (pendingTasks.isNotEmpty()) {
            sb.appendLine("## ⏳ Активные:")
            pendingTasks.forEach { task ->
                val category = categories.find { it.id == task.categoryId }
                val categoryEmoji = getCategoryEmoji(category?.icon)
                sb.appendLine("- [ ] $categoryEmoji ${task.title}")
                task.description?.let { description ->
                    sb.appendLine("  - $description")
                }
                task.startDateTime?.let { startTime ->
                    sb.appendLine("  - 🕐 ${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                }
            }
            sb.appendLine()
        }
        
        // Notes
        if (notes.isNotEmpty()) {
            sb.appendLine("## 📝 Заметки:")
            notes.forEach { note ->
                val category = note.categoryId?.let { id -> categories.find { it.id == id } }
                val categoryEmoji = getCategoryEmoji(category?.icon)
                sb.appendLine("- $categoryEmoji **${note.title}**")
                sb.appendLine("  $note.content")
            }
            sb.appendLine()
        }
        
        // Footer
        sb.appendLine("---")
        sb.appendLine("*Экспортировано из DailyFlow*")
        
        return sb.toString()
    }
    
    fun exportToPlainText(
        tasks: List<Task>,
        notes: List<Note>,
        categories: List<Category>,
        date: LocalDateTime
    ): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale("ru"))
        val dateString = date.format(formatter)
        
        val sb = StringBuilder()
        
        // Header
        sb.appendLine("$dateString")
        sb.appendLine("=".repeat(dateString.length))
        sb.appendLine()
        
        // Completed tasks
        val completedTasks = tasks.filter { it.status == TaskStatus.COMPLETED }
        if (completedTasks.isNotEmpty()) {
            sb.appendLine("ВЫПОЛНЕНО:")
            completedTasks.forEach { task ->
                val category = categories.find { it.id == task.categoryId }
                sb.appendLine("✓ ${task.title}")
                task.description?.let { description ->
                    sb.appendLine("  $description")
                }
            }
            sb.appendLine()
        }
        
        // Pending tasks
        val pendingTasks = tasks.filter { it.status == TaskStatus.PENDING }
        if (pendingTasks.isNotEmpty()) {
            sb.appendLine("АКТИВНЫЕ:")
            pendingTasks.forEach { task ->
                val category = categories.find { it.id == task.categoryId }
                sb.appendLine("- ${task.title}")
                task.description?.let { description ->
                    sb.appendLine("  $description")
                }
                task.startDateTime?.let { startTime ->
                    sb.appendLine("  Время: ${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                }
            }
            sb.appendLine()
        }
        
        // Notes
        if (notes.isNotEmpty()) {
            sb.appendLine("ЗАМЕТКИ:")
            notes.forEach { note ->
                sb.appendLine("- ${note.title}")
                sb.appendLine("  $note.content")
            }
            sb.appendLine()
        }
        
        return sb.toString()
    }
    
    fun exportToHtml(
        tasks: List<Task>,
        notes: List<Note>,
        categories: List<Category>,
        date: LocalDateTime
    ): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale("ru"))
        val dateString = date.format(formatter)
        
        val sb = StringBuilder()
        
        sb.appendLine("<!DOCTYPE html>")
        sb.appendLine("<html lang=\"ru\">")
        sb.appendLine("<head>")
        sb.appendLine("    <meta charset=\"UTF-8\">")
        sb.appendLine("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
        sb.appendLine("    <title>DailyFlow - $dateString</title>")
        sb.appendLine("    <style>")
        sb.appendLine("        body { font-family: Arial, sans-serif; margin: 20px; }")
        sb.appendLine("        h1 { color: #333; }")
        sb.appendLine("        h2 { color: #666; }")
        sb.appendLine("        .completed { color: #4CAF50; }")
        sb.appendLine("        .pending { color: #2196F3; }")
        sb.appendLine("        .note { background-color: #f5f5f5; padding: 10px; margin: 5px 0; border-radius: 5px; }")
        sb.appendLine("    </style>")
        sb.appendLine("</head>")
        sb.appendLine("<body>")
        sb.appendLine("    <h1>📅 $dateString</h1>")
        
        // Completed tasks
        val completedTasks = tasks.filter { it.status == TaskStatus.COMPLETED }
        if (completedTasks.isNotEmpty()) {
            sb.appendLine("    <h2>✅ Выполнено:</h2>")
            sb.appendLine("    <ul>")
            completedTasks.forEach { task ->
                sb.appendLine("        <li class=\"completed\">${task.title}</li>")
                task.description?.let { description ->
                    sb.appendLine("        <li style=\"margin-left: 20px; color: #666;\">$description</li>")
                }
            }
            sb.appendLine("    </ul>")
        }
        
        // Pending tasks
        val pendingTasks = tasks.filter { it.status == TaskStatus.PENDING }
        if (pendingTasks.isNotEmpty()) {
            sb.appendLine("    <h2>⏳ Активные:</h2>")
            sb.appendLine("    <ul>")
            pendingTasks.forEach { task ->
                sb.appendLine("        <li class=\"pending\">${task.title}</li>")
                task.description?.let { description ->
                    sb.appendLine("        <li style=\"margin-left: 20px; color: #666;\">$description</li>")
                }
            }
            sb.appendLine("    </ul>")
        }
        
        // Notes
        if (notes.isNotEmpty()) {
            sb.appendLine("    <h2>📝 Заметки:</h2>")
            notes.forEach { note ->
                sb.appendLine("    <div class=\"note\">")
                sb.appendLine("        <strong>${note.title}</strong><br>")
                sb.appendLine("        ${note.content}")
                sb.appendLine("    </div>")
            }
        }
        
        sb.appendLine("    <hr>")
        sb.appendLine("    <p><em>Экспортировано из DailyFlow</em></p>")
        sb.appendLine("</body>")
        sb.appendLine("</html>")
        
        return sb.toString()
    }
    
    private fun getCategoryEmoji(iconName: String?): String {
        return when (iconName) {
            "work" -> "💼"
            "home" -> "🏠"
            "health" -> "🏥"
            "education" -> "📚"
            "finance" -> "💰"
            "shopping" -> "🛒"
            "sport" -> "⚽"
            else -> "📋"
        }
    }
}
