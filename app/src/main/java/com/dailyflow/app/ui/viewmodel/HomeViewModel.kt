package com.dailyflow.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyflow.app.data.model.Task
import com.dailyflow.app.data.model.Note
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.Priority
import com.dailyflow.app.data.model.TaskStatus
import com.dailyflow.app.data.repository.TaskRepository
import com.dailyflow.app.data.repository.NoteRepository
import com.dailyflow.app.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _selectedDate = MutableStateFlow(LocalDateTime.now())
    val selectedDate: StateFlow<LocalDateTime> = _selectedDate.asStateFlow()
    
    val allActiveTasks: StateFlow<List<Task>> = taskRepository.getAllActiveTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tasksForSelectedDate: StateFlow<List<Task>> = selectedDate
        .flatMapLatest { date ->
            taskRepository.getTasksForDateRange(date.toLocalDate().atStartOfDay(), date.toLocalDate().plusDays(1).atStartOfDay())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val notesForSelectedDate: StateFlow<List<Note>> = selectedDate
        .flatMapLatest { date ->
            noteRepository.getNotesForDate(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val overdueTasks: StateFlow<List<Task>> = taskRepository.getOverdueTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val dailyProgress: StateFlow<Float> = selectedDate
        .flatMapLatest { date ->
            flow {
                val progress = taskRepository.getDailyProgress(date)
                emit(progress)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0f
        )
    
    fun addTask(title: String, description: String?, categoryId: String, startDateTime: LocalDateTime?, endDateTime: LocalDateTime?, reminderEnabled: Boolean, reminderMinutes: Int?, priority: Priority) {
        viewModelScope.launch {
            Log.d("HomeViewModel", "Adding task with title: $title, description: $description, categoryId: $categoryId, startDateTime: $startDateTime, endDateTime: $endDateTime, reminderEnabled: $reminderEnabled, reminderMinutes: $reminderMinutes, priority: $priority")
            val newTask = Task(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                startDateTime = startDateTime ?: _selectedDate.value,
                endDateTime = endDateTime,
                categoryId = categoryId,
                reminderEnabled = reminderEnabled,
                reminderMinutes = reminderMinutes,
                priority = priority
            )
            taskRepository.insertTask(newTask)
        }
    }

    fun updateSelectedDate(date: LocalDateTime) {
        _selectedDate.value = date
    }

    fun selectNextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun selectPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }
    
    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val status = if (isCompleted) 
                com.dailyflow.app.data.model.TaskStatus.COMPLETED 
            else 
                com.dailyflow.app.data.model.TaskStatus.PENDING
            taskRepository.updateTaskStatus(taskId, status)
        }
    }
    
    fun toggleNoteCompletion(noteId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            noteRepository.updateNoteCompletion(noteId, isCompleted)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId)
            task?.let { taskRepository.deleteTask(it) }
        }
    }

    fun cancelTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.updateTaskStatus(taskId, TaskStatus.CANCELLED)
        }
    }
}