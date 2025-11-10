package com.dailyflow.app.data.repository

import com.dailyflow.app.data.dao.RecurringTemplateDao
import com.dailyflow.app.data.dao.TaskDao
import com.dailyflow.app.data.model.*
import com.dailyflow.app.notifications.ReminderScheduler
import com.dailyflow.app.util.RecurrenceUtils
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val recurringTemplateDao: RecurringTemplateDao,
    private val reminderScheduler: ReminderScheduler
) {
    
    fun getAllActiveTasks(): Flow<List<Task>> = taskDao.getAllActiveTasks()

    fun getAllTasksSortedByDate(): Flow<List<Task>> = taskDao.getAllTasksSortedByDate()
    
    fun getTasksForDate(date: LocalDateTime): Flow<List<Task>> = taskDao.getTasksForDate(date)

    fun getTasksForDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Task>> = taskDao.getTasksForDateRange(start, end)
    
    fun getOverdueTasks(): Flow<List<Task>> = taskDao.getOverdueTasks(LocalDateTime.now())
    
    suspend fun getTaskById(id: String): Task? = taskDao.getTaskById(id)
    
    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
        applyReminder(task)
    }
    
    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
        applyReminder(task)
    }
    
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
        reminderScheduler.cancel(task.id)
    }
    
    suspend fun updateTaskStatus(id: String, status: TaskStatus) {
        taskDao.updateTaskStatus(id, status)
        taskDao.getTaskById(id)?.let { applyReminder(it) }
    }
    
    suspend fun getCompletedTasksCountForDate(date: LocalDateTime): Int = 
        taskDao.getCompletedTasksCountForDate(date)
    
    suspend fun getTotalTasksCountForDate(date: LocalDateTime): Int = 
        taskDao.getTotalTasksCountForDate(date)
    
    suspend fun getDailyProgress(date: LocalDateTime): Float {
        val completed = getCompletedTasksCountForDate(date)
        val total = getTotalTasksCountForDate(date)
        return if (total > 0) completed.toFloat() / total else 0f
    }

    suspend fun getRecurringTemplate(seriesId: String): RecurringTemplate? =
        recurringTemplateDao.getTemplateById(seriesId)

    suspend fun createRecurringSeries(
        title: String,
        description: String?,
        categoryId: String?,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        reminderEnabled: Boolean,
        reminderMinutes: Int?,
        priority: Priority,
        recurrenceRule: RecurrenceRule
    ): Int {
        val durationMinutes = ChronoUnit.MINUTES.between(startDateTime, endDateTime).toInt().coerceAtLeast(1)
        val seriesId = UUID.randomUUID().toString()
        val template = RecurringTemplate(
            id = seriesId,
            title = title,
            description = description,
            categoryId = categoryId,
            priority = priority,
            startDateTime = startDateTime,
            durationMinutes = durationMinutes,
            recurrenceRule = recurrenceRule,
            reminderEnabled = reminderEnabled,
            reminderMinutes = reminderMinutes
        )
        recurringTemplateDao.insertTemplate(template)

        val occurrences = RecurrenceUtils.generateOccurrences(startDateTime, recurrenceRule)
        val tasks = occurrences.mapIndexed { index, occurrenceStart ->
            Task(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                categoryId = categoryId,
                startDateTime = occurrenceStart,
                endDateTime = occurrenceStart.plusMinutes(durationMinutes.toLong()),
                reminderEnabled = reminderEnabled,
                reminderMinutes = reminderMinutes,
                priority = priority,
                seriesId = seriesId,
                sequenceNumber = index + 1,
                originalStartDateTime = startDateTime
            )
        }
        taskDao.insertTasks(tasks)
        scheduleReminders(tasks)
        return tasks.size
    }

    suspend fun updateRecurringTask(
        taskId: String,
        request: RecurringUpdateRequest,
        scope: RecurrenceScope
    ) {
        val existing = taskDao.getTaskById(taskId) ?: return
        if (existing.seriesId.isNullOrBlank()) {
            val updated = existing.copy(
                title = request.title,
                description = request.description,
                categoryId = request.categoryId,
                startDateTime = request.startDateTime ?: existing.startDateTime,
                endDateTime = request.endDateTime ?: existing.endDateTime,
                reminderEnabled = request.reminderEnabled,
                reminderMinutes = request.reminderMinutes,
                priority = request.priority,
                updatedAt = LocalDateTime.now()
            )
            taskDao.updateTask(updated)
            applyReminder(updated)
            return
        }

        when (scope) {
            RecurrenceScope.THIS -> updateSingleOccurrence(existing, request)
            RecurrenceScope.THIS_AND_FUTURE -> updateThisAndFuture(existing, request)
            RecurrenceScope.ENTIRE_SERIES -> updateEntireSeries(existing, request)
        }
    }

    suspend fun deleteRecurringTask(taskId: String, scope: RecurrenceScope) {
        val existing = taskDao.getTaskById(taskId) ?: return
        val seriesId = existing.seriesId
        if (seriesId.isNullOrBlank() || scope == RecurrenceScope.THIS) {
            reminderScheduler.cancel(existing.id)
            taskDao.deleteTask(existing)
            return
        }

        val seriesTasks = taskDao.getTasksBySeries(seriesId)
        val now = LocalDateTime.now()
        when (scope) {
            RecurrenceScope.THIS_AND_FUTURE -> {
                val cutoffSequence = existing.sequenceNumber ?: Int.MAX_VALUE
                val idsToDelete = seriesTasks.filter { task ->
                    val seq = task.sequenceNumber ?: Int.MAX_VALUE
                    val isFuture = seq >= cutoffSequence ||
                            (task.startDateTime != null && existing.startDateTime != null && !task.startDateTime.isBefore(existing.startDateTime))
                    val isMutableStatus = task.status != TaskStatus.COMPLETED && task.status != TaskStatus.CANCELLED
                    task.id == existing.id || (isFuture && isMutableStatus)
                }.map { it.id }
                if (idsToDelete.isNotEmpty()) {
                    taskDao.deleteTasksByIds(idsToDelete)
                    cancelReminders(idsToDelete)
                }
            }
            RecurrenceScope.ENTIRE_SERIES -> {
                val deletable = seriesTasks.filter { it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED }
                if (deletable.isNotEmpty()) {
                    val ids = deletable.map { it.id }
                    taskDao.deleteTasksByIds(ids)
                    cancelReminders(ids)
                }
                val toDetach = seriesTasks.filter { it.status == TaskStatus.COMPLETED || it.status == TaskStatus.CANCELLED }
                toDetach.forEach { task ->
                    reminderScheduler.cancel(task.id)
                    taskDao.updateTask(task.copy(seriesId = null, updatedAt = now))
                }
                recurringTemplateDao.getTemplateById(seriesId)?.let { recurringTemplateDao.deleteTemplate(it) }
            }
            else -> Unit
        }
    }

    suspend fun cancelRecurringTask(taskId: String, scope: RecurrenceScope) {
        val existing = taskDao.getTaskById(taskId) ?: return
        val seriesId = existing.seriesId
        val now = LocalDateTime.now()
        if (seriesId.isNullOrBlank() || scope == RecurrenceScope.THIS) {
            val cancelled = existing.copy(status = TaskStatus.CANCELLED, updatedAt = now)
            taskDao.updateTask(cancelled)
            reminderScheduler.cancel(cancelled.id)
            return
        }

        val seriesTasks = taskDao.getTasksBySeries(seriesId)
        val cutoffSequence = existing.sequenceNumber ?: Int.MAX_VALUE
        val targetTasks = when (scope) {
            RecurrenceScope.THIS_AND_FUTURE -> seriesTasks.filter { task ->
                val seq = task.sequenceNumber ?: Int.MAX_VALUE
                val isFuture = seq >= cutoffSequence ||
                        (task.startDateTime != null && existing.startDateTime != null && !task.startDateTime.isBefore(existing.startDateTime))
                isFuture && task.status != TaskStatus.COMPLETED && task.status != TaskStatus.CANCELLED
            } + existing
            RecurrenceScope.ENTIRE_SERIES -> seriesTasks.filter { task ->
                task.status != TaskStatus.COMPLETED && task.status != TaskStatus.CANCELLED
            }
            else -> emptyList()
        }
        targetTasks.distinctBy { it.id }.forEach { task ->
            val updated = task.copy(status = TaskStatus.CANCELLED, updatedAt = now)
            taskDao.updateTask(updated)
            reminderScheduler.cancel(updated.id)
        }
    }

    private suspend fun updateSingleOccurrence(existing: Task, request: RecurringUpdateRequest) {
        val now = LocalDateTime.now()
        val updated = existing.copy(
            title = request.title,
            description = request.description,
            categoryId = request.categoryId,
            startDateTime = request.startDateTime ?: existing.startDateTime,
            endDateTime = request.endDateTime ?: existing.endDateTime,
            reminderEnabled = request.reminderEnabled,
            reminderMinutes = request.reminderMinutes,
            priority = request.priority,
            isException = true,
            originalStartDateTime = existing.originalStartDateTime ?: existing.startDateTime,
            updatedAt = now
        )
        taskDao.updateTask(updated)
        applyReminder(updated)
    }

    private suspend fun updateThisAndFuture(existing: Task, request: RecurringUpdateRequest) {
        val rule = request.recurrenceRule ?: return updateSingleOccurrence(existing, request)
        val start = request.startDateTime ?: existing.startDateTime ?: return
        val end = request.endDateTime ?: existing.endDateTime ?: return
        val durationMinutes = ChronoUnit.MINUTES.between(start, end).toInt().coerceAtLeast(1)
        val oldSeriesId = existing.seriesId ?: return updateSingleOccurrence(existing, request)

        val newSeriesId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        val template = RecurringTemplate(
            id = newSeriesId,
            title = request.title,
            description = request.description,
            categoryId = request.categoryId,
            priority = request.priority,
            startDateTime = start,
            durationMinutes = durationMinutes,
            recurrenceRule = rule,
            reminderEnabled = request.reminderEnabled,
            reminderMinutes = request.reminderMinutes,
            createdAt = now,
            updatedAt = now
        )
        recurringTemplateDao.insertTemplate(template)

        val seriesTasks = taskDao.getTasksBySeries(oldSeriesId)
        val cutoffSequence = existing.sequenceNumber ?: Int.MAX_VALUE
        val toDelete = seriesTasks.filter { task ->
            val seq = task.sequenceNumber ?: Int.MAX_VALUE
            task.id != existing.id &&
                    seq >= cutoffSequence &&
                    task.status != TaskStatus.COMPLETED &&
                    task.status != TaskStatus.CANCELLED
        }
        if (toDelete.isNotEmpty()) {
            val ids = toDelete.map { it.id }
            taskDao.deleteTasksByIds(ids)
            cancelReminders(ids)
        }

        val occurrences = RecurrenceUtils.generateOccurrences(start, rule)
        if (occurrences.isEmpty()) {
            updateSingleOccurrence(existing, request)
            return
        }

        val updatedCurrent = existing.copy(
            title = request.title,
            description = request.description,
            categoryId = request.categoryId,
            startDateTime = occurrences.first(),
            endDateTime = occurrences.first().plusMinutes(durationMinutes.toLong()),
            reminderEnabled = request.reminderEnabled,
            reminderMinutes = request.reminderMinutes,
            priority = request.priority,
            seriesId = newSeriesId,
            isException = false,
            originalStartDateTime = start,
            sequenceNumber = 1,
            status = if (existing.status == TaskStatus.CANCELLED) TaskStatus.PENDING else existing.status,
            updatedAt = now
        )
        taskDao.updateTask(updatedCurrent)
        applyReminder(updatedCurrent)

        val futureTasks = occurrences.drop(1).mapIndexed { index, occurrenceStart ->
            Task(
                id = UUID.randomUUID().toString(),
                title = request.title,
                description = request.description,
                categoryId = request.categoryId,
                startDateTime = occurrenceStart,
                endDateTime = occurrenceStart.plusMinutes(durationMinutes.toLong()),
                reminderEnabled = request.reminderEnabled,
                reminderMinutes = request.reminderMinutes,
                priority = request.priority,
                seriesId = newSeriesId,
                isException = false,
                originalStartDateTime = start,
                sequenceNumber = index + 2,
                status = TaskStatus.PENDING,
                createdAt = now,
                updatedAt = now
            )
        }
        if (futureTasks.isNotEmpty()) {
            taskDao.insertTasks(futureTasks)
            scheduleReminders(futureTasks)
        }
    }

    private suspend fun updateEntireSeries(existing: Task, request: RecurringUpdateRequest) {
        val rule = request.recurrenceRule ?: return updateSingleOccurrence(existing, request)
        val start = request.startDateTime ?: existing.startDateTime ?: return
        val end = request.endDateTime ?: existing.endDateTime ?: return
        val durationMinutes = ChronoUnit.MINUTES.between(start, end).toInt().coerceAtLeast(1)
        val seriesId = existing.seriesId ?: return updateSingleOccurrence(existing, request)
        val currentTemplate = recurringTemplateDao.getTemplateById(seriesId)
        val now = LocalDateTime.now()
        val updatedTemplate = (currentTemplate ?: RecurringTemplate(
            id = seriesId,
            title = request.title,
            description = request.description,
            categoryId = request.categoryId,
            priority = request.priority,
            startDateTime = start,
            durationMinutes = durationMinutes,
            recurrenceRule = rule,
            reminderEnabled = request.reminderEnabled,
            reminderMinutes = request.reminderMinutes,
            createdAt = now,
            updatedAt = now
        )).copy(
            title = request.title,
            description = request.description,
            categoryId = request.categoryId,
            priority = request.priority,
            startDateTime = start,
            durationMinutes = durationMinutes,
            recurrenceRule = rule,
            reminderEnabled = request.reminderEnabled,
            reminderMinutes = request.reminderMinutes,
            updatedAt = now
        )
        if (currentTemplate == null) {
            recurringTemplateDao.insertTemplate(updatedTemplate)
        } else {
            recurringTemplateDao.updateTemplate(updatedTemplate)
        }

        val seriesTasks = taskDao.getTasksBySeries(seriesId)
        val toReplace = seriesTasks.filter { task ->
            when {
                task.id == existing.id ->
                    task.status != TaskStatus.COMPLETED && task.status != TaskStatus.CANCELLED
                task.status != TaskStatus.COMPLETED && task.status != TaskStatus.CANCELLED -> {
                    val startDate = task.startDateTime
                    startDate == null || !startDate.isBefore(now)
                }
                else -> false
            }
        }
        val idsToDelete = toReplace.filter { it.id != existing.id }.map { it.id }
        if (idsToDelete.isNotEmpty()) {
            taskDao.deleteTasksByIds(idsToDelete)
            cancelReminders(idsToDelete)
        }
        val remaining = seriesTasks.filterNot { task -> toReplace.any { it.id == task.id } }
        val offset = remaining.mapNotNull { it.sequenceNumber }.maxOrNull() ?: 0

        val occurrences = RecurrenceUtils.generateOccurrences(start, rule)
        if (occurrences.isEmpty()) {
            updateSingleOccurrence(existing, request)
            return
        }

        val updatedCurrent = existing.copy(
            title = request.title,
            description = request.description,
            categoryId = request.categoryId,
            startDateTime = occurrences.first(),
            endDateTime = occurrences.first().plusMinutes(durationMinutes.toLong()),
            reminderEnabled = request.reminderEnabled,
            reminderMinutes = request.reminderMinutes,
            priority = request.priority,
            seriesId = seriesId,
            isException = false,
            originalStartDateTime = start,
            sequenceNumber = offset + 1,
            status = if (existing.status == TaskStatus.CANCELLED) TaskStatus.PENDING else existing.status,
            updatedAt = now
        )
        taskDao.updateTask(updatedCurrent)
        applyReminder(updatedCurrent)

        val futureTasks = occurrences.drop(1).mapIndexed { index, occurrenceStart ->
            Task(
                id = UUID.randomUUID().toString(),
                title = request.title,
                description = request.description,
                categoryId = request.categoryId,
                startDateTime = occurrenceStart,
                endDateTime = occurrenceStart.plusMinutes(durationMinutes.toLong()),
                reminderEnabled = request.reminderEnabled,
                reminderMinutes = request.reminderMinutes,
                priority = request.priority,
                seriesId = seriesId,
                isException = false,
                originalStartDateTime = start,
                sequenceNumber = offset + index + 2,
                status = TaskStatus.PENDING,
                createdAt = now,
                updatedAt = now
            )
        }
        if (futureTasks.isNotEmpty()) {
            taskDao.insertTasks(futureTasks)
            scheduleReminders(futureTasks)
        }
    }

    private fun applyReminder(task: Task) {
        if (task.reminderEnabled &&
            task.startDateTime != null &&
            task.reminderMinutes != null &&
            task.status == TaskStatus.PENDING
        ) {
            reminderScheduler.schedule(task)
        } else {
            reminderScheduler.cancel(task.id)
        }
    }

    private fun scheduleReminders(tasks: List<Task>) {
        tasks.forEach { applyReminder(it) }
    }

    private fun cancelReminders(taskIds: List<String>) {
        taskIds.forEach { reminderScheduler.cancel(it) }
    }
}

data class RecurringUpdateRequest(
    val title: String,
    val description: String?,
    val categoryId: String?,
    val startDateTime: LocalDateTime?,
    val endDateTime: LocalDateTime?,
    val reminderEnabled: Boolean,
    val reminderMinutes: Int?,
    val priority: Priority,
    val recurrenceRule: RecurrenceRule?
)
