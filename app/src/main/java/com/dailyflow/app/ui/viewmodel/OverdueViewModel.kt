package com.dailyflow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.Task
import com.dailyflow.app.data.model.TaskStatus
import com.dailyflow.app.data.repository.CategoryRepository
import com.dailyflow.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OverdueViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _filterDate = MutableStateFlow<LocalDate?>(null)
    val filterDate: StateFlow<LocalDate?> = _filterDate.asStateFlow()

    private val overdueTasks: StateFlow<List<Task>> = taskRepository.getOverdueTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val groupedOverdueTasks: StateFlow<Map<LocalDate, List<Task>>> = combine(
        overdueTasks,
        filterDate
    ) { tasks, date ->
        val filteredTasks = if (date != null) {
            tasks.filter { it.endDateTime?.toLocalDate() == date }
        } else {
            tasks
        }
        filteredTasks.groupBy { it.endDateTime!!.toLocalDate() }.toSortedMap(compareByDescending { it })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.updateTaskStatus(taskId, TaskStatus.COMPLETED)
        }
    }

    fun cancelTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.updateTaskStatus(taskId, TaskStatus.CANCELLED)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId)
            task?.let { taskRepository.deleteTask(it) }
        }
    }

    fun markAllAsCompleted() {
        viewModelScope.launch {
            overdueTasks.value.forEach { task ->
                taskRepository.updateTaskStatus(task.id, TaskStatus.COMPLETED)
            }
        }
    }

    fun setFilterDate(date: LocalDate?) {
        _filterDate.value = date
    }
}