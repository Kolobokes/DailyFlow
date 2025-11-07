package com.dailyflow.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.ui.viewmodel.StatisticsViewModel
import com.dailyflow.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val statistics by viewModel.statistics.collectAsState()
    val categoryStats by viewModel.categoryStatistics.collectAsState()
    val priorityStats by viewModel.priorityStatistics.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Overall statistics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Общая статистика",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard(
                            title = "Выполнено",
                            value = statistics.completedTasks.toString(),
                            color = CompletedColor,
                            icon = Icons.Default.CheckCircle
                        )
                        
                        StatCard(
                            title = "В ожидании",
                            value = statistics.pendingTasks.toString(),
                            color = PendingColor,
                            icon = Icons.Default.Schedule
                        )
                        
                        StatCard(
                            title = "Отменено",
                            value = statistics.cancelledTasks.toString(),
                            color = CancelledColor,
                            icon = Icons.Default.Cancel
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Прогресс выполнения")
                            Text("${statistics.completionPercentage.toInt()}%" )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = statistics.completionPercentage / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        
        // Category statistics
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Статистика по категориям",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    categoryStats.forEach { (category, stats) ->
                        /*CategoryStatItem(
                            categoryName = category,
                            completed = stats.completed,
                            total = stats.total,
                            color = getCategoryColor(category)
                        )*/
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        
        // Priority statistics
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Статистика по приоритетам",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    priorityStats.forEach { (priority, count) ->
                        /*PriorityStatItem(
                            priority = priority,
                            count = count,
                            color = getPriorityColor(priority)
                        )*/
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        
        // Time-based statistics
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Статистика по времени",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard(
                            title = "Сегодня",
                            value = statistics.todayTasks.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            icon = Icons.Default.Today
                        )
                        
                        StatCard(
                            title = "На этой неделе",
                            value = statistics.weekTasks.toString(),
                            color = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Default.DateRange
                        )
                        
                        StatCard(
                            title = "В этом месяце",
                            value = statistics.monthTasks.toString(),
                            color = MaterialTheme.colorScheme.tertiary,
                            icon = Icons.Default.CalendarMonth
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/*@Composable
fun CategoryStatItem(
    categoryName: String,
    completed: Int,
    total: Int,
    color: Color
) {
    val percentage = if (total > 0) (completed.toFloat() / total * 100f) else 0f
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$completed/$total (${percentage.toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = percentage / 100f,
            modifier = Modifier.fillMaxWidth(),
            color = color
        )
    }
}*/

/*@Composable
fun PriorityStatItem(
    priority: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = priority,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}*/

/*private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "работа" -> WorkColor
        "личное" -> PersonalColor
        "здоровье" -> HealthColor
        "образование" -> EducationColor
        "финансы" -> FinanceColor
        else -> DefaultColor
    }
}*/

/*private fun getPriorityColor(priority: String): Color {
    return when (priority.lowercase()) {
        "высокий" -> HighPriorityColor
        "средний" -> MediumPriorityColor
        "низкий" -> LowPriorityColor
        else -> MaterialTheme.colorScheme.primary
    }
}*/
