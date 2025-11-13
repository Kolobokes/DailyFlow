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
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    enum class StatusFilter { ALL, OVERDUE, COMPLETED, CANCELLED }

    private val _filterDate = MutableStateFlow<LocalDate?>(null)
    val filterDate: StateFlow<LocalDate?> = _filterDate.asStateFlow()

    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    val statusFilter: StateFlow<StatusFilter> = _statusFilter.asStateFlow()

    private val allTasks: StateFlow<List<Task>> = taskRepository.getAllTasksSortedByDate()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val groupedTasks: StateFlow<Map<LocalDate, List<Task>>> = combine(
        allTasks, filterDate, statusFilter
    ) { tasks, date, statusFilter ->
        val now = java.time.LocalDateTime.now()
        val filteredByStatus = when (statusFilter) {
            StatusFilter.ALL -> tasks
            StatusFilter.OVERDUE -> tasks.filter { it.endDateTime?.isBefore(now) == true && it.status == TaskStatus.PENDING }
            StatusFilter.COMPLETED -> tasks.filter { it.status == TaskStatus.COMPLETED }
            StatusFilter.CANCELLED -> tasks.filter { it.status == TaskStatus.CANCELLED }
        }
        val filteredTasks = filteredByStatus.filter { task ->
            val matchesDate = date?.let { task.startDateTime?.toLocalDate() == it } ?: true
            matchesDate
        }
        filteredTasks.filter { it.startDateTime != null }
            .groupBy { it.startDateTime!!.toLocalDate() }
            .toSortedMap(compareByDescending { it })
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

    fun setStatusFilter(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    private val _recurringActionDialog = MutableSharedFlow<RecurringActionDialogState>()
    val recurringActionDialog: SharedFlow<RecurringActionDialogState> = _recurringActionDialog.asSharedFlow()

    private var pendingRecurringAction: PendingRecurringAction? = null

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val status = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.PENDING
            taskRepository.updateTaskStatus(taskId, status)
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
}
