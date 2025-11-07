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
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _filterDate = MutableStateFlow<LocalDate?>(null)
    val filterDate: StateFlow<LocalDate?> = _filterDate.asStateFlow()

    private val _showCancelled = MutableStateFlow(false)
    val showCancelled: StateFlow<Boolean> = _showCancelled.asStateFlow()

    private val _showOverdue = MutableStateFlow(true)
    val showOverdue: StateFlow<Boolean> = _showOverdue.asStateFlow()

    private val _showCompleted = MutableStateFlow(true)
    val showCompleted: StateFlow<Boolean> = _showCompleted.asStateFlow()

    private val allTasks: StateFlow<List<Task>> = taskRepository.getAllTasksSortedByDate()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val groupedTasks: StateFlow<Map<LocalDate, List<Task>>> = combine(
        allTasks, filterDate, showCancelled, showOverdue, showCompleted
    ) { tasks, date, showC, showO, showComp ->
        val filteredTasks = tasks.filter { task ->
            val status = task.status
            val isOverdue = task.endDateTime?.isBefore(java.time.LocalDateTime.now()) == true && status == TaskStatus.PENDING

            (if (date != null) task.startDateTime?.toLocalDate() == date else true) &&
            (showC || status != TaskStatus.CANCELLED) &&
            (showO || !isOverdue) &&
            (showComp || status != TaskStatus.COMPLETED)
        }
        filteredTasks.filter { it.startDateTime != null }.groupBy { it.startDateTime!!.toLocalDate() }.toSortedMap(compareByDescending { it })
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

    fun setFilterDate(date: LocalDate?) {
        _filterDate.value = date
    }

    fun toggleShowCancelled(show: Boolean) {
        _showCancelled.value = show
    }

    fun toggleShowOverdue(show: Boolean) {
        _showOverdue.value = show
    }

    fun toggleShowCompleted(show: Boolean) {
        _showCompleted.value = show
    }

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val status = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.PENDING
            taskRepository.updateTaskStatus(taskId, status)
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