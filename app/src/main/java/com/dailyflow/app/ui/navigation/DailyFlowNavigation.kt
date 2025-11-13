package com.dailyflow.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dailyflow.app.R
import com.dailyflow.app.ui.screen.*

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyFlowNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem(stringResource(R.string.nav_home), Icons.Default.Home, Screen.Home.route),
        BottomNavItem(stringResource(R.string.nav_tasks), Icons.Default.Assignment, Screen.Tasks.route),
        BottomNavItem(stringResource(R.string.nav_notes), Icons.Default.Note, Screen.Notes.route),
        BottomNavItem(stringResource(R.string.nav_statistics), Icons.Default.BarChart, Screen.Analytics.route),
        BottomNavItem(stringResource(R.string.nav_settings), Icons.Default.Settings, Screen.Settings.route)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { destination ->
                            destination.route?.substringBefore("?") == item.route
                        } == true,
                        onClick = {
                            val targetRoute = when (item.route) {
                                Screen.Notes.route -> Screen.Notes.createRoute()
                                else -> item.route
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
            composable(
                route = Screen.Notes.route,
                arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = "" })
            ) {
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
