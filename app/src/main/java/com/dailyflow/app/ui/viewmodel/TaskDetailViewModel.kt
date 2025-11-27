package com.dailyflow.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.Priority
import com.dailyflow.app.data.model.Task
import com.dailyflow.app.data.model.RecurrenceRule
import com.dailyflow.app.data.model.RecurrenceFrequency
import com.dailyflow.app.data.model.RecurrenceScope
import com.dailyflow.app.data.repository.CategoryRepository
import com.dailyflow.app.data.repository.TaskRepository
import com.dailyflow.app.data.repository.RecurringUpdateRequest
import com.dailyflow.app.notifications.ReminderScheduler
import com.dailyflow.app.export.TextExportManager
import com.dailyflow.app.util.FileStorageManager
import com.dailyflow.app.security.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

// Состояние экрана
data class TaskDetailUiState(
    val task: Task? = null,
    val categories: List<Category> = emptyList(),
    val isNewTask: Boolean = true,
    val isLoading: Boolean = true,
    val defaultDate: LocalDate? = null,
    val defaultStartTime: LocalTime? = null,
    val defaultEndTime: LocalTime? = null,
    val defaultReminderMinutes: Int = 60,
    val isRecurring: Boolean = false,
    val recurrenceRule: RecurrenceRule? = null,
    val createdOccurrences: Int = 0
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderScheduler: ReminderScheduler,
    private val exportManager: TextExportManager,
    private val fileStorageManager: FileStorageManager,
    private val settingsManager: SettingsManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String? = savedStateHandle.get("taskId")
    private val selectedDate: String? = savedStateHandle.get("selectedDate")
    private val startTime: String? = savedStateHandle.get("startTime")
    private val endTime: String? = savedStateHandle.get("endTime")

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private val _showExactAlarmPermissionDialog = MutableStateFlow(false)
    val showExactAlarmPermissionDialog: StateFlow<Boolean> = _showExactAlarmPermissionDialog.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

    private val _recurrenceScopeDialog = MutableSharedFlow<RecurrenceScopeDialogState>()
    val recurrenceScopeDialog: SharedFlow<RecurrenceScopeDialogState> = _recurrenceScopeDialog.asSharedFlow()

    private var pendingRecurringUpdate: RecurringUpdateRequest? = null

    init {
        viewModelScope.launch {
            val categories = categoryRepository.getTaskCategories().first()
            if (taskId != null) {
                val task = taskRepository.getTaskById(taskId)
                val template = task?.seriesId?.let { taskRepository.getRecurringTemplate(it) }
                _uiState.value = TaskDetailUiState(
                    task = task,
                    categories = categories,
                    isNewTask = false,
                    isLoading = false,
                    isRecurring = task?.seriesId != null,
                    recurrenceRule = template?.recurrenceRule
                )
            } else {
                val defaultDate = selectedDate?.let { LocalDate.parse(it) }
                val defaultStartTime = startTime?.let { LocalTime.parse(it) }
                val defaultEndTime = endTime?.let { LocalTime.parse(it) }
                val defaultReminderMinutes = settingsManager.defaultReminderMinutes.first()
                _uiState.value = TaskDetailUiState(
                    categories = categories, 
                    isNewTask = true, 
                    isLoading = false, 
                    defaultDate = defaultDate,
                    defaultStartTime = defaultStartTime,
                    defaultEndTime = defaultEndTime,
                    defaultReminderMinutes = defaultReminderMinutes
                )
            }
        }
    }

    suspend fun copyFileToStorage(sourceUri: android.net.Uri, taskId: String): String? {
        return fileStorageManager.copyFileToStorage(sourceUri, taskId)
    }

    fun getFileUri(fileName: String): android.net.Uri? {
        return fileStorageManager.getFileUri(fileName)
    }

    fun getFile(fileName: String): java.io.File? {
        return fileStorageManager.getFile(fileName)
    }

    fun saveTask(
        title: String,
        description: String?,
        categoryId: String?,
        startDateTime: LocalDateTime?,
        endDateTime: LocalDateTime?,
        reminderEnabled: Boolean,
        reminderMinutes: Int?,
        priority: Priority,
        isRecurring: Boolean,
        recurrenceRule: RecurrenceRule?,
        scope: RecurrenceScope? = null,
        attachedFileName: String? = null
    ) {
        viewModelScope.launch {
            val needsExactAlarmPermission = reminderEnabled &&
                    startDateTime != null &&
                    reminderMinutes != null &&
                    !reminderScheduler.canScheduleExactAlarms()

            if (needsExactAlarmPermission) {
                _showExactAlarmPermissionDialog.value = true
                return@launch
            }

            if (taskId == null && isRecurring && recurrenceRule != null && startDateTime != null && endDateTime != null) {
                taskRepository.createRecurringSeries(
                    title = title,
                    description = description,
                    categoryId = categoryId,
                    startDateTime = startDateTime,
                    endDateTime = endDateTime,
                    reminderEnabled = reminderEnabled,
                    reminderMinutes = reminderMinutes,
                    priority = priority,
                    recurrenceRule = recurrenceRule
                )
                _navigateBack.emit(Unit)
                return@launch
            }

            val finalTaskId = taskId ?: UUID.randomUUID().toString()
            
            // Если есть старый файл и он отличается от нового, удаляем старый
            val oldTask = _uiState.value.task
            if (oldTask != null && oldTask.attachedFileUri != null && oldTask.attachedFileUri != attachedFileName) {
                fileStorageManager.deleteFile(oldTask.attachedFileUri!!)
            }
            
            val taskToSave = if (taskId == null) {
                Task(
                    id = finalTaskId,
                    title = title,
                    description = description,
                    categoryId = categoryId,
                    startDateTime = startDateTime,
                    endDateTime = endDateTime,
                    reminderEnabled = reminderEnabled,
                    reminderMinutes = reminderMinutes,
                    priority = priority,
                    attachedFileUri = attachedFileName
                )
            } else {
                _uiState.value.task!!.copy(
                    title = title,
                    description = description,
                    categoryId = categoryId,
                    startDateTime = startDateTime,
                    endDateTime = endDateTime,
                    reminderEnabled = reminderEnabled,
                    reminderMinutes = reminderMinutes,
                    priority = priority,
                    attachedFileUri = attachedFileName
                )
            }

            if (taskId == null) {
                taskRepository.insertTask(taskToSave)
                _navigateBack.emit(Unit)
            } else {
                val existingTask = _uiState.value.task
                if (existingTask?.seriesId != null) {
                    val request = RecurringUpdateRequest(
                        title = title,
                        description = description,
                        categoryId = categoryId,
                        startDateTime = startDateTime,
                        endDateTime = endDateTime,
                        reminderEnabled = reminderEnabled,
                        reminderMinutes = reminderMinutes,
                        priority = priority,
                        recurrenceRule = if (isRecurring) recurrenceRule else null
                    )
                    if (scope == null) {
                        pendingRecurringUpdate = request
                        val allowSeriesScope = existingTask.seriesId != null
                        _recurrenceScopeDialog.emit(
                            RecurrenceScopeDialogState(
                                allowSeriesScope = allowSeriesScope
                            )
                        )
                        return@launch
                    }
                    taskRepository.updateRecurringTask(taskId, request, scope)
                } else {
                    taskRepository.updateTask(taskToSave)
                }
                _navigateBack.emit(Unit)
            }
        }
    }

    fun onRecurrenceScopeSelected(scope: RecurrenceScope) {
        val request = pendingRecurringUpdate ?: return
        val currentTaskId = taskId ?: return
        pendingRecurringUpdate = null
        viewModelScope.launch {
            taskRepository.updateRecurringTask(currentTaskId, request, scope)
            _navigateBack.emit(Unit)
        }
    }

    fun dismissRecurrenceScopeDialog() {
        pendingRecurringUpdate = null
    }

    fun dismissExactAlarmPermissionDialog() {
        _showExactAlarmPermissionDialog.value = false
    }

    suspend fun exportCurrentTask(): String? {
        val id = taskId ?: return null
        return exportManager.exportTask(id)
    }
}

data class RecurrenceScopeDialogState(
    val allowSeriesScope: Boolean
)