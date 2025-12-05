package com.dailyflow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.Task
import com.dailyflow.app.data.model.TaskStatus
import com.dailyflow.app.data.model.RecurrenceScope
import com.dailyflow.app.data.repository.CategoryRepository
import com.dailyflow.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
            tasks.filter { 
                val taskDate = it.endDateTime?.toLocalDate() ?: it.startDateTime?.toLocalDate() ?: it.createdAt.toLocalDate()
                taskDate == date 
            }
        } else {
            tasks
        }
        // Группируем по дате окончания задачи (endDateTime) - приоритет endDateTime
        filteredTasks.groupBy { task ->
            // Используем endDateTime как основной ключ для группировки
            task.endDateTime?.toLocalDate() 
                ?: task.startDateTime?.toLocalDate() 
                ?: task.createdAt.toLocalDate()
        }.toSortedMap(compareByDescending { it })
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

    private val _recurringActionDialog = MutableSharedFlow<RecurringActionDialogState>()
    val recurringActionDialog: SharedFlow<RecurringActionDialogState> = _recurringActionDialog.asSharedFlow()

    private var pendingRecurringAction: PendingRecurringAction? = null

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.updateTaskStatus(taskId, TaskStatus.COMPLETED)
        }
    }

    fun cycleTaskStatus(taskId: String, currentStatus: TaskStatus) {
        viewModelScope.launch {
            val nextStatus = when (currentStatus) {
                TaskStatus.PENDING -> TaskStatus.COMPLETED
                TaskStatus.COMPLETED -> TaskStatus.CANCELLED
                TaskStatus.CANCELLED -> TaskStatus.PENDING
            }
            taskRepository.updateTaskStatus(taskId, nextStatus)
        }
    }

    fun cancelTask(taskId: String) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            if (task.seriesId.isNullOrBlank()) {
                taskRepository.updateTaskStatus(taskId, TaskStatus.CANCELLED)
            } else {
                pendingRecurringAction = PendingRecurringAction(task, RecurringActionType.CANCEL)
                _recurringActionDialog.emit(RecurringActionDialogState(task, RecurringActionType.CANCEL))
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            if (task.seriesId.isNullOrBlank()) {
                taskRepository.deleteTask(task)
            } else {
                pendingRecurringAction = PendingRecurringAction(task, RecurringActionType.DELETE)
                _recurringActionDialog.emit(RecurringActionDialogState(task, RecurringActionType.DELETE))
            }
        }
    }

    fun onRecurringActionScopeSelected(scope: RecurrenceScope) {
        val pending = pendingRecurringAction ?: return
        viewModelScope.launch {
            when (pending.actionType) {
                RecurringActionType.DELETE -> taskRepository.deleteRecurringTask(pending.task.id, scope)
                RecurringActionType.CANCEL -> taskRepository.cancelRecurringTask(pending.task.id, scope)
            }
            pendingRecurringAction = null
        }
    }

    fun dismissRecurringActionDialog() {
        pendingRecurringAction = null
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