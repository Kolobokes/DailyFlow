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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private data class DateRange(val start: LocalDate, val end: LocalDate) {
    fun ensureOrder(): DateRange = if (start.isAfter(end)) DateRange(end, start) else this
}

enum class AnalyticsPeriodPreset {
    WEEK,
    MONTH,
    QUARTER,
    YEAR,
    CUSTOM
}

data class CategoryDistribution(
    val categoryId: String?,
    val name: String,
    val color: String?,
    val count: Int,
    val percent: Float
)

data class OverdueTaskItem(
    val id: String,
    val title: String,
    val dueDate: LocalDateTime?,
    val categoryName: String?,
    val categoryColor: String?
)

data class CompletionStats(
    val planned: Int,
    val completed: Int,
    val completionPercent: Float,
    val label: String
)

data class WorkloadHeatmapCell(
    val dayOfWeek: DayOfWeek,
    val hour: Int,
    val count: Int,
    val intensity: Float
)

data class AnalyticsUiState(
    val selectedPreset: AnalyticsPeriodPreset = AnalyticsPeriodPreset.MONTH,
    val startDate: LocalDate = LocalDate.now().minusDays(29),
    val endDate: LocalDate = LocalDate.now(),
    val categoryDistribution: List<CategoryDistribution> = emptyList(),
    val totalTasks: Int = 0,
    val overdueDistribution: List<CategoryDistribution> = emptyList(),
    val overdueTasks: List<OverdueTaskItem> = emptyList(),
    val completionStats: CompletionStats? = null,
    val workloadHeatmap: List<WorkloadHeatmapCell> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    taskRepository: TaskRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val _selectedPreset = MutableStateFlow(AnalyticsPeriodPreset.MONTH)
    private val _customRange = MutableStateFlow(
        DateRange(
            start = LocalDate.now().minusDays(29),
            end = LocalDate.now()
        )
    )
    private val _currentRange = MutableStateFlow(
        DateRange(
            start = LocalDate.now().minusDays(29),
            end = LocalDate.now()
        )
    )

    private val dateLabelFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())

    private val tasksFlow = taskRepository.getAllTasksSortedByDate()
    private val categoriesFlow = categoryRepository.getTaskCategories()

    private val combinedState = combine(
        _selectedPreset,
        _currentRange,
        tasksFlow,
        categoriesFlow
    ) { preset, range, tasks, categories ->
        val normalizedRange = range.ensureOrder()
        val startDateTime = normalizedRange.start.atStartOfDay()
        val endDateTime = normalizedRange.end.plusDays(1).atStartOfDay()
        val categoriesMap = categories.associateBy { it.id }

        val tasksInRange = tasks.filter { task ->
            val point = task.startDateTime
                ?: task.createdAt
            point != null && !point.isBefore(startDateTime) && point.isBefore(endDateTime)
        }
        val totalTasks = tasksInRange.size
        val distribution = buildDistribution(
            tasksInRange,
            categoriesMap,
            totalTasks
        )

        val now = LocalDateTime.now()
        val overdueTasks = tasks.filter { task ->
            task.status == TaskStatus.PENDING && isTaskOverdue(task, now)
        }
        val overdueDistribution = buildDistribution(
            overdueTasks,
            categoriesMap,
            overdueTasks.size
        )
        val overdueItems = overdueTasks
            .sortedBy { it.endDateTime ?: it.startDateTime ?: it.createdAt }
            .map { task ->
                val category = categoriesMap[task.categoryId]
                OverdueTaskItem(
                    id = task.id,
                    title = task.title,
                    dueDate = task.endDateTime ?: task.startDateTime ?: task.createdAt,
                    categoryName = category?.name,
                    categoryColor = category?.color
                )
            }

        val completionStats = buildCompletionStats(
            preset = preset,
            normalizedRange = normalizedRange,
            tasks = tasks
        )

        val workload = buildWorkloadHeatmap(tasksInRange)

        AnalyticsUiState(
            selectedPreset = preset,
            startDate = normalizedRange.start,
            endDate = normalizedRange.end,
            categoryDistribution = distribution,
            totalTasks = totalTasks,
            overdueDistribution = overdueDistribution,
            overdueTasks = overdueItems,
            completionStats = completionStats,
            workloadHeatmap = workload,
            isLoading = false
        )
    }

    val uiState: StateFlow<AnalyticsUiState> = combinedState.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun selectPreset(preset: AnalyticsPeriodPreset) {
        if (preset == AnalyticsPeriodPreset.CUSTOM) {
            _selectedPreset.value = preset
            _currentRange.value = _customRange.value.ensureOrder()
        } else {
            _selectedPreset.value = preset
            _currentRange.value = presetRange(preset)
        }
    }

    fun updateCustomStart(date: LocalDate) {
        _customRange.update { current ->
            DateRange(
                start = date,
                end = if (date.isAfter(current.end)) date else current.end
            )
        }
        applyCustomRange()
    }

    fun updateCustomEnd(date: LocalDate) {
        _customRange.update { current ->
            DateRange(
                start = if (date.isBefore(current.start)) date else current.start,
                end = if (date.isBefore(current.start)) current.start else date
            )
        }
        applyCustomRange()
    }

    private fun applyCustomRange() {
        _selectedPreset.value = AnalyticsPeriodPreset.CUSTOM
        _currentRange.value = _customRange.value.ensureOrder()
    }

    private fun presetRange(preset: AnalyticsPeriodPreset): DateRange {
        val end = LocalDate.now()
        return when (preset) {
            AnalyticsPeriodPreset.WEEK -> DateRange(end.minusDays(6), end)
            AnalyticsPeriodPreset.MONTH -> DateRange(end.minusDays(29), end)
            AnalyticsPeriodPreset.QUARTER -> DateRange(end.minusDays(89), end)
            AnalyticsPeriodPreset.YEAR -> DateRange(end.minusDays(364), end)
            AnalyticsPeriodPreset.CUSTOM -> _customRange.value.ensureOrder()
        }
    }

    private fun buildDistribution(
        tasks: List<Task>,
        categories: Map<String, Category>,
        total: Int
    ): List<CategoryDistribution> {
        if (tasks.isEmpty() || total == 0) return emptyList()
        val grouped = tasks.groupBy { it.categoryId }
        return grouped.map { (categoryId, items) ->
            val category = categoryId?.let { categories[it] }
            val name = category?.name ?: UNCATEGORIZED_NAME
            val color = category?.color
            val count = items.size
            val percent = (count.toFloat() / total.toFloat()) * 100f
            CategoryDistribution(
                categoryId = categoryId,
                name = name,
                color = color,
                count = count,
                percent = percent
            )
        }.sortedByDescending { it.count }
    }

    private fun isTaskOverdue(task: Task, now: LocalDateTime): Boolean {
        val due = task.endDateTime ?: task.startDateTime ?: task.createdAt
        return due != null && due.isBefore(now)
    }

    private fun buildCompletionStats(
        preset: AnalyticsPeriodPreset,
        normalizedRange: DateRange,
        tasks: List<Task>
    ): CompletionStats? {
        val statsRange = if (preset == AnalyticsPeriodPreset.CUSTOM) {
            normalizedRange.ensureOrder()
        } else {
            val today = LocalDate.now()
            DateRange(today, today)
        }
        val startDateTime = statsRange.start.atStartOfDay()
        val endDateTime = statsRange.end.plusDays(1).atStartOfDay()
        val tasksForStats = tasks.filter { task ->
            val point = task.startDateTime ?: task.createdAt
            point != null && !point.isBefore(startDateTime) && point.isBefore(endDateTime)
        }
        if (tasksForStats.isEmpty()) return null
        val planned = tasksForStats.size
        val completed = tasksForStats.count { it.status == TaskStatus.COMPLETED }
        val percent = if (planned == 0) 0f else (completed.toFloat() / planned.toFloat()) * 100f
        val label = if (statsRange.start == statsRange.end) {
            statsRange.start.format(dateLabelFormatter)
        } else {
            "${statsRange.start.format(dateLabelFormatter)} — ${statsRange.end.format(dateLabelFormatter)}"
        }
        return CompletionStats(
            planned = planned,
            completed = completed,
            completionPercent = percent,
            label = label
        )
    }

    private fun buildWorkloadHeatmap(tasks: List<Task>): List<WorkloadHeatmapCell> {
        if (tasks.isEmpty()) return emptyList()
        val counts = mutableMapOf<Pair<DayOfWeek, Int>, Int>()
        var maxCount = 0
        tasks.forEach { task ->
            val dateTime = task.startDateTime ?: task.createdAt ?: return@forEach
            val day = dateTime.dayOfWeek
            val hour = dateTime.hour
            val key = day to hour
            val value = (counts[key] ?: 0) + 1
            counts[key] = value
            if (value > maxCount) maxCount = value
        }
        if (maxCount == 0) return emptyList()
        val dayOrder = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
        return dayOrder.flatMap { day ->
            (0 until 24).map { hour ->
                val count = counts[day to hour] ?: 0
                val intensity = if (maxCount > 0) count.toFloat() / maxCount.toFloat() else 0f
                WorkloadHeatmapCell(
                    dayOfWeek = day,
                    hour = hour,
                    count = count,
                    intensity = intensity
                )
            }
        }
    }

    companion object {
        private const val UNCATEGORIZED_NAME = "Без категории"
    }
}

