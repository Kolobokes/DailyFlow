package com.dailyflow.app.export

import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.Note
import com.dailyflow.app.data.model.Task
import com.dailyflow.app.data.repository.CategoryRepository
import com.dailyflow.app.data.repository.NoteRepository
import com.dailyflow.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextExportManager @Inject constructor(
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    private val formatter: TextExportFormatter
) {

    suspend fun exportDailyPlan(date: LocalDate): String {
        val start = date.atStartOfDay()
        val end = date.plusDays(1).atStartOfDay()
        val tasks = taskRepository.getTasksForDateRange(start, end).first()
        val categories = categoryRepository.getTaskCategories().first().associateBy(Category::id)
        return formatter.formatDailyPlan(date, tasks, categories)
    }

    suspend fun exportTask(taskId: String): String? {
        val task = taskRepository.getTaskById(taskId) ?: return null
        val category = task.categoryId?.let { id ->
            categoryRepository.getTaskCategories().first().firstOrNull { it.id == id }
        }
        return formatter.formatTask(task, category)
    }

    suspend fun exportNote(noteId: String): String? {
        val note = noteRepository.getNoteById(noteId) ?: return null
        val category = note.categoryId?.let { id ->
            categoryRepository.getNoteCategories().first().firstOrNull { it.id == id }
        }
        return formatter.formatNote(note, category)
    }
}

