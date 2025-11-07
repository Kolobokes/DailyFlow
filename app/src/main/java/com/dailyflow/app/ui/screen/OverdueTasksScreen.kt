package com.dailyflow.app.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.ui.components.TaskCard
import com.dailyflow.app.ui.navigation.Screen
import com.dailyflow.app.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverdueTasksScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val overdueTasks by viewModel.overdueTasks.collectAsState()
    val categories by viewModel.categories.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Просроченные задачи") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) {
        LazyColumn(modifier = Modifier.padding(it)) {
            items(overdueTasks, key = { it.id }) { task ->
                val category = categories.find { it.id == task.categoryId }
                TaskCard(
                    task = task,
                    category = category,
                    onClick = { navController.navigate(Screen.TaskDetail.createRoute(task.id, null)) },
                    onToggle = { isCompleted ->
                        viewModel.toggleTaskCompletion(task.id, isCompleted)
                    },
                    onDelete = { viewModel.deleteTask(task.id) },
                    onCancel = { viewModel.cancelTask(task.id) }
                )
            }
        }
    }
}