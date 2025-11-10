package com.dailyflow.app.data.dao

import androidx.room.*
import com.dailyflow.app.data.model.Task
import com.dailyflow.app.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TaskDao {
    
    @Query("SELECT * FROM tasks WHERE status != 'COMPLETED' AND status != 'CANCELLED' ORDER BY startDateTime")
    fun getAllActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY startDateTime DESC")
    fun getAllTasksSortedByDate(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE DATE(startDateTime) = DATE(:date) ORDER BY startDateTime")
    fun getTasksForDate(date: LocalDateTime): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE startDateTime < :end AND endDateTime >= :start ORDER BY startDateTime")
    fun getTasksForDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE endDateTime < :now AND status = 'PENDING'")
    fun getOverdueTasks(now: LocalDateTime): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Query("SELECT * FROM tasks WHERE seriesId = :seriesId ORDER BY sequenceNumber")
    suspend fun getTasksBySeries(seriesId: String): List<Task>

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteTasksByIds(ids: List<String>)
    
    @Update
    suspend fun updateTask(task: Task)
    
    @Delete
    suspend fun deleteTask(task: Task)
    
    @Query("UPDATE tasks SET status = :status WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: TaskStatus)

    @Query("SELECT COUNT(*) FROM tasks WHERE DATE(startDateTime) = DATE(:date) AND status = 'COMPLETED'")
    suspend fun getCompletedTasksCountForDate(date: LocalDateTime): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE DATE(startDateTime) = DATE(:date)")
    suspend fun getTotalTasksCountForDate(date: LocalDateTime): Int
}
