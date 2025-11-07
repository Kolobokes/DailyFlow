package com.dailyflow.app.ui.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.Priority
import com.dailyflow.app.data.model.Task
import com.dailyflow.app.data.repository.CategoryRepository
import com.dailyflow.app.data.repository.TaskRepository
import com.dailyflow.app.receiver.ReminderReceiver
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
import java.time.ZoneId
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
    val defaultEndTime: LocalTime? = null
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
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

    init {
        viewModelScope.launch {
            val categories = categoryRepository.getTaskCategories().first()
            if (taskId != null) {
                val task = taskRepository.getTaskById(taskId)
                _uiState.value = TaskDetailUiState(task = task, categories = categories, isNewTask = false, isLoading = false)
            } else {
                val defaultDate = selectedDate?.let { LocalDate.parse(it) }
                val defaultStartTime = startTime?.let { LocalTime.parse(it) }
                val defaultEndTime = endTime?.let { LocalTime.parse(it) }
                _uiState.value = TaskDetailUiState(
                    categories = categories, 
                    isNewTask = true, 
                    isLoading = false, 
                    defaultDate = defaultDate,
                    defaultStartTime = defaultStartTime,
                    defaultEndTime = defaultEndTime
                )
            }
        }
    }

    fun saveTask(
        title: String, 
        description: String?, 
        categoryId: String, 
        startDateTime: LocalDateTime?, 
        endDateTime: LocalDateTime?, 
        reminderEnabled: Boolean, 
        reminderMinutes: Int?, 
        priority: Priority
    ) {
        viewModelScope.launch {
            val taskToSave = if (taskId == null) {
                Task(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = description,
                    categoryId = categoryId,
                    startDateTime = startDateTime,
                    endDateTime = endDateTime,
                    reminderEnabled = reminderEnabled,
                    reminderMinutes = reminderMinutes,
                    priority = priority
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
                    priority = priority
                )
            }
            if (taskId == null) {
                taskRepository.insertTask(taskToSave)
            } else {
                taskRepository.updateTask(taskToSave)
            }

            if (reminderEnabled && startDateTime != null && reminderMinutes != null) {
                if (scheduleNotification(taskToSave.id, title, description ?: "", startDateTime.minusMinutes(reminderMinutes.toLong()))) {
                    _navigateBack.emit(Unit)
                }
            } else {
                cancelNotification(taskToSave.id)
                _navigateBack.emit(Unit)
            }
        }
    }

    private fun scheduleNotification(taskId: String, title: String, message: String, reminderDateTime: LocalDateTime): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                _showExactAlarmPermissionDialog.value = true
                return false
            }
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, taskId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        val reminderTime = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
        return true
    }

    private fun cancelNotification(taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, taskId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    fun dismissExactAlarmPermissionDialog() {
        _showExactAlarmPermissionDialog.value = false
    }
}