package com.dailyflow.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val textMeasurer = rememberTextMeasurer()
    val density = LocalContext.current.resources.displayMetrics.density

    val bottomNavItems = listOf(
        BottomNavItem(stringResource(R.string.nav_home), Icons.Default.AccessTime, Screen.Home.route),
        BottomNavItem(stringResource(R.string.nav_tasks), Icons.AutoMirrored.Filled.Assignment, Screen.Tasks.route),
        BottomNavItem(stringResource(R.string.nav_notes), Icons.AutoMirrored.Filled.Note, Screen.Notes.route),
        BottomNavItem(stringResource(R.string.nav_statistics), Icons.Default.BarChart, Screen.Analytics.route),
        BottomNavItem(stringResource(R.string.nav_settings), Icons.Default.Settings, Screen.Settings.route)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val showLabels = bottomNavItems.all { item ->
                    val result = textMeasurer.measure(text = item.title, style = MaterialTheme.typography.labelMedium)
                    (result.size.width / density) <= 64f
                }
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { if (showLabels) Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
