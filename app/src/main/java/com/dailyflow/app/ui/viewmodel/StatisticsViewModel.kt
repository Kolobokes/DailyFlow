package com.dailyflow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyflow.app.data.model.Task
import com.dailyflow.app.data.model.TaskStatus
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.repository.TaskRepository
import com.dailyflow.app.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    val allTasks: StateFlow<List<Task>> = taskRepository.getAllActiveTasks()
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
    
    val statistics: StateFlow<TaskStatistics> = allTasks
        .map { tasks -> calculateStatistics(tasks) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TaskStatistics()
        )
    
    val categoryStatistics: StateFlow<Map<String, CategoryStats>> = combine(
        allTasks, categories
    ) { tasks, cats ->
        calculateCategoryStatistics(tasks, cats)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )
    
    val priorityStatistics: StateFlow<Map<String, Int>> = allTasks
        .map { tasks ->
            tasks.groupingBy { task ->
                when (task.priority) {
                    com.dailyflow.app.data.model.Priority.HIGH -> "Высокий"
                    com.dailyflow.app.data.model.Priority.MEDIUM -> "Средний"
                    com.dailyflow.app.data.model.Priority.LOW -> "Низкий"
                }
            }.eachCount()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    
    private fun calculateStatistics(tasks: List<Task>): TaskStatistics {
        val completedTasks = tasks.count { it.status == TaskStatus.COMPLETED }
        val pendingTasks = tasks.count { it.status == TaskStatus.PENDING }
        val cancelledTasks = tasks.count { it.status == TaskStatus.CANCELLED }
        
        val totalTasks = tasks.size
        val completionPercentage = if (totalTasks > 0) {
            (completedTasks.toFloat() / totalTasks * 100f)
        } else 0f
        
        val now = LocalDateTime.now()
        val todayTasks = tasks.count { task ->
            task.startDateTime?.toLocalDate() == now.toLocalDate()
        }
        
        val weekStart = now.minusDays(now.dayOfWeek.value - 1L)
        val weekTasks = tasks.count { task ->
            task.startDateTime?.toLocalDate()?.isAfter(weekStart.toLocalDate().minusDays(1)) == true
        }
        
        val monthStart = now.withDayOfMonth(1)
        val monthTasks = tasks.count { task ->
            task.startDateTime?.toLocalDate()?.isAfter(monthStart.toLocalDate().minusDays(1)) == true
        }
        
        return TaskStatistics(
            completedTasks = completedTasks,
            pendingTasks = pendingTasks,
            cancelledTasks = cancelledTasks,
            totalTasks = totalTasks,
            completionPercentage = completionPercentage,
            todayTasks = todayTasks,
            weekTasks = weekTasks,
            monthTasks = monthTasks
        )
    }
    
    private fun calculateCategoryStatistics(
        tasks: List<Task>,
        categories: List<Category>
    ): Map<String, CategoryStats> {
        val categoryMap = categories.associateBy { it.id }
        
        return tasks.groupBy { task ->
            categoryMap[task.categoryId]?.name ?: "Без категории"
        }.mapValues { (_, categoryTasks) ->
            val completed = categoryTasks.count { it.status == TaskStatus.COMPLETED }
            val total = categoryTasks.size
            
            CategoryStats(
                completed = completed,
                total = total
            )
        }
    }
}

data class TaskStatistics(
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val cancelledTasks: Int = 0,
    val totalTasks: Int = 0,
    val completionPercentage: Float = 0f,
    val todayTasks: Int = 0,
    val weekTasks: Int = 0,
    val monthTasks: Int = 0
)

data class CategoryStats(
    val completed: Int = 0,
    val total: Int = 0
)
