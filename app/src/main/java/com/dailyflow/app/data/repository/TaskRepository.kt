package com.dailyflow.app.data.repository

import com.dailyflow.app.data.dao.TaskDao
import com.dailyflow.app.data.model.Task
import com.dailyflow.app.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    
    fun getAllActiveTasks(): Flow<List<Task>> = taskDao.getAllActiveTasks()

    fun getAllTasksSortedByDate(): Flow<List<Task>> = taskDao.getAllTasksSortedByDate()
    
    fun getTasksForDate(date: LocalDateTime): Flow<List<Task>> = taskDao.getTasksForDate(date)

    fun getTasksForDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Task>> = taskDao.getTasksForDateRange(start, end)
    
    fun getOverdueTasks(): Flow<List<Task>> = taskDao.getOverdueTasks(LocalDateTime.now())
    
    suspend fun getTaskById(id: String): Task? = taskDao.getTaskById(id)
    
    suspend fun insertTask(task: Task) = taskDao.insertTask(task)
    
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    
    suspend fun updateTaskStatus(id: String, status: TaskStatus) = taskDao.updateTaskStatus(id, status)
    
    suspend fun getCompletedTasksCountForDate(date: LocalDateTime): Int = 
        taskDao.getCompletedTasksCountForDate(date)
    
    suspend fun getTotalTasksCountForDate(date: LocalDateTime): Int = 
        taskDao.getTotalTasksCountForDate(date)
    
    suspend fun getDailyProgress(date: LocalDateTime): Float {
        val completed = getCompletedTasksCountForDate(date)
        val total = getTotalTasksCountForDate(date)
        return if (total > 0) completed.toFloat() / total else 0f
    }
}
