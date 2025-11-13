package com.dailyflow.app.ui.screen

import android.graphics.Color.parseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.R
import com.dailyflow.app.ui.navigation.Screen
import com.dailyflow.app.ui.viewmodel.AnalyticsPeriodPreset
import com.dailyflow.app.ui.viewmodel.AnalyticsUiState
import com.dailyflow.app.ui.viewmodel.AnalyticsViewModel
import com.dailyflow.app.ui.viewmodel.CategoryDistribution
import com.dailyflow.app.ui.viewmodel.CompletionStats
import com.dailyflow.app.ui.viewmodel.OverdueTaskItem
import com.dailyflow.app.ui.viewmodel.WorkloadHeatmapCell
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = remember { Locale("ru") }

    val showStartPicker = remember { mutableStateOf(false) }
    val showEndPicker = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analytics_title)) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                        androidx.compose.material3.Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PeriodSelection(
                uiState = uiState,
                onPresetSelected = { preset -> viewModel.selectPreset(preset) },
                onStartClick = { showStartPicker.value = true },
                onEndClick = { showEndPicker.value = true }
            )

            CompletionReportCard(stats = uiState.completionStats)

            CategoryReportCard(uiState = uiState)

            WorkloadReportCard(uiState = uiState)

            OverdueReportCard(
                uiState = uiState,
                onViewAll = { navController.navigate(Screen.OverdueTasks.route) }
            )
        }
    }

    if (showStartPicker.value) {
        val startPickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = uiState.startDate.toEpochMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    startPickerState.selectedDateMillis?.let {
                        val date = Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.updateCustomStart(date)
                    }
                    showStartPicker.value = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            val russianContext = remember(context) {
                val config = android.content.res.Configuration(configuration)
                config.setLocale(locale)
                context.createConfigurationContext(config)
            }
            val russianConfiguration = remember(russianContext) { russianContext.resources.configuration }
            androidx.compose.runtime.CompositionLocalProvider(
                LocalContext provides russianContext,
                LocalConfiguration provides russianConfiguration
            ) {
                androidx.compose.material3.DatePicker(state = startPickerState)
            }
        }
    }

    if (showEndPicker.value) {
        val endPickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = uiState.endDate.toEpochMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    endPickerState.selectedDateMillis?.let {
                        val date = Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.updateCustomEnd(date)
                    }
                    showEndPicker.value = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            val russianContext = remember(context) {
                val config = android.content.res.Configuration(configuration)
                config.setLocale(locale)
                context.createConfigurationContext(config)
            }
            val russianConfiguration = remember(russianContext) { russianContext.resources.configuration }
            androidx.compose.runtime.CompositionLocalProvider(
                LocalContext provides russianContext,
                LocalConfiguration provides russianConfiguration
            ) {
                androidx.compose.material3.DatePicker(state = endPickerState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PeriodSelection(
    uiState: AnalyticsUiState,
    onPresetSelected: (AnalyticsPeriodPreset) -> Unit,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.analytics_period_label),
            style = MaterialTheme.typography.titleMedium
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsPeriodPreset.values().forEach { preset ->
                FilterChip(
                    selected = uiState.selectedPreset == preset,
                    onClick = { onPresetSelected(preset) },
                    label = {
                        Text(
                            text = stringResource(
                                when (preset) {
                                    AnalyticsPeriodPreset.WEEK -> R.string.analytics_period_week
                                    AnalyticsPeriodPreset.MONTH -> R.string.analytics_period_month
                                    AnalyticsPeriodPreset.QUARTER -> R.string.analytics_period_quarter
                                    AnalyticsPeriodPreset.YEAR -> R.string.analytics_period_year
                                    AnalyticsPeriodPreset.CUSTOM -> R.string.analytics_period_custom
                                }
                            )
                        )
                    }
                )
            }
        }

        if (uiState.selectedPreset == AnalyticsPeriodPreset.CUSTOM) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onStartClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.analytics_start_date) + ": " +
                                uiState.startDate.formatForDisplay()
                    )
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = onEndClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.analytics_end_date) + ": " +
                                uiState.endDate.formatForDisplay()
                    )
                }
            }
        } else {
            Text(
                text = stringResource(
                    R.string.analytics_selected_period,
                    uiState.startDate.formatForDisplay(),
                    uiState.endDate.formatForDisplay()
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompletionReportCard(stats: CompletionStats?) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.analytics_completion_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (stats == null) {
                Text(
                    text = stringResource(R.string.analytics_completion_no_tasks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(R.string.analytics_completion_for_period, stats.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val percent = stats.completionPercent.coerceIn(0f, 100f)
                val progress = percent / 100f

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.analytics_completion_planned, stats.planned),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(
                                R.string.analytics_completion_completed,
                                stats.completed
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Text(
                        text = stringResource(
                            R.string.analytics_completion_percentage,
                            percent.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryReportCard(uiState: AnalyticsUiState) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.analytics_category_report_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.analytics_total_tasks, uiState.totalTasks),
                style = MaterialTheme.typography.bodyMedium
            )

            if (uiState.categoryDistribution.isEmpty()) {
                Text(
                    text = stringResource(R.string.analytics_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CategoryDistributionChart(uiState.categoryDistribution)
            }
        }
    }
}

@Composable
private fun OverdueReportCard(
    uiState: AnalyticsUiState,
    onViewAll: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.analytics_overdue_report_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.analytics_overdue_total, uiState.overdueTasks.size),
                style = MaterialTheme.typography.bodyMedium
            )

            if (uiState.overdueTasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.analytics_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (uiState.overdueDistribution.isNotEmpty()) {
                    CategoryDistributionChart(uiState.overdueDistribution)
                }
                OverdueTaskList(
                    tasks = uiState.overdueTasks.take(5)
                )
                if (uiState.overdueTasks.size > 5) {
                    TextButton(onClick = onViewAll) {
                        Text(text = stringResource(R.string.analytics_view_all_overdue))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDistributionChart(items: List<CategoryDistribution>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            val progress = (item.percent / 100f).coerceIn(0f, 1f)
            val fallbackColor = MaterialTheme.colorScheme.primary
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(parseCategoryColor(item.color, fallbackColor))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (item.categoryId == null) stringResource(R.string.analytics_uncategorized) else item.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", item.percent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = parseCategoryColor(item.color, fallbackColor),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = stringResource(R.string.analytics_item_count, item.count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OverdueTaskList(tasks: List<OverdueTaskItem>) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tasks.forEach { task ->
            val fallbackColor = MaterialTheme.colorScheme.primary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        task.categoryName?.let { name ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(parseCategoryColor(task.categoryColor, fallbackColor))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        val due = task.dueDate
                        Text(
                            text = due?.format(formatter)
                                ?: stringResource(R.string.analytics_overdue_no_due),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkloadReportCard(uiState: AnalyticsUiState) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.analytics_workload_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.analytics_workload_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.workloadHeatmap.isEmpty()) {
                Text(
                    text = stringResource(R.string.analytics_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                WorkloadHeatmap(uiState.workloadHeatmap)
            }
        }
    }
}

private fun parseCategoryColor(colorString: String?, fallback: Color): Color =
    colorString?.let { runCatching { Color(parseColor(it)) }.getOrNull() } ?: fallback

private fun LocalDate.toEpochMillis(): Long =
    this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun LocalDate.formatForDisplay(): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())
    return this.format(formatter)
}

@Composable
private fun WorkloadHeatmap(cells: List<WorkloadHeatmapCell>) {
    val dayOrder = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )
    val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val hourMarks = listOf(0, 6, 12, 18, 23)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(modifier = Modifier.width(32.dp))
            hourMarks.forEach { hour ->
                Text(
                    text = String.format("%02d", hour),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(36.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val rowScroll = rememberScrollState()
        Column(
            modifier = Modifier.horizontalScroll(rowScroll),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            dayOrder.forEachIndexed { index, day ->
                val rowCells = cells.filter { it.dayOfWeek == day }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = dayLabels[index],
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(28.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        rowCells.forEach { cell ->
                            val intensity = cell.intensity
                            val color = if (intensity == 0f) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f + 0.75f * intensity)
                            }
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell.count > 0) {
                                    Text(
                                        text = cell.count.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.analytics_workload_legend_low),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(0.2f, 0.5f, 0.8f, 1f).forEach { intensity ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f + 0.75f * intensity)
                            )
                    )
                }
            }
            Text(
                text = stringResource(R.string.analytics_workload_legend_high),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}