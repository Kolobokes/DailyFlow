package com.dailyflow.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dailyflow.app.ui.screen.*

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val screen: Screen
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyFlowNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem("Главная", Icons.Default.Home, Screen.Home),
        BottomNavItem("Задачи", Icons.Default.Assignment, Screen.Tasks),
        BottomNavItem("Заметки", Icons.Default.Note, Screen.Notes),
        BottomNavItem("Аналитика", Icons.Default.BarChart, Screen.Analytics),
        BottomNavItem("Настройки", Icons.Default.Settings, Screen.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            val targetRoute = when (item.screen) {
                                Screen.Notes -> Screen.Notes.createRoute(null)
                                else -> item.screen.route
                            }
                            navController.navigate(targetRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(Screen.Tasks.route) {
                TasksScreen(navController = navController)
            }
            composable(Screen.Notes.route) {
                NotesScreen(navController = navController)
            }
            composable(Screen.CategoryManagement.route) {
                CategoryManagementScreen(navController = navController)
            }
            composable(Screen.OverdueTasks.route) {
                OverdueTasksScreen(navController = navController)
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen(navController = navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
            composable(
                route = Screen.TaskDetail.route,
                arguments = listOf(
                    navArgument("taskId") { nullable = true },
                    navArgument("selectedDate") { nullable = true; type = NavType.StringType },
                    navArgument("startTime") { nullable = true; type = NavType.StringType },
                    navArgument("endTime") { nullable = true; type = NavType.StringType }
                )
            ) { 
                TaskDetailScreen(navController = navController)
            }
            composable(
                route = Screen.NoteDetail.route,
                arguments = listOf(navArgument("noteId") { nullable = true })
            ) { 
                NoteDetailScreen(navController = navController)
            }
        }
    }
}
