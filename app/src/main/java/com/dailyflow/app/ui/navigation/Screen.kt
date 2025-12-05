package com.dailyflow.app.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object Notes : Screen("notes") {
        fun createRoute(date: String? = null): String {
            return if (date != null) {
                "notes?date=$date"
            } else {
                "notes"
            }
        }
    }
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
    object NoteDetail : Screen("note_detail?noteId={noteId}&categoryId={categoryId}") {
        val routeWithArgs = "note_detail?noteId={noteId}&categoryId={categoryId}"
        fun createRoute(noteId: String?, categoryId: String? = null): String {
            val params = mutableListOf<String>()
            noteId?.let { params.add("noteId=$it") }
            categoryId?.let { params.add("categoryId=$it") }
            return if (params.isNotEmpty()) "note_detail?${params.joinToString("&")}" else "note_detail"
        }
    }
    object TaskDetail : Screen("task_detail?taskId={taskId}&selectedDate={selectedDate}&startTime={startTime}&endTime={endTime}") {
        val routeWithArgs = "task_detail?taskId={taskId}&selectedDate={selectedDate}&startTime={startTime}&endTime={endTime}"
        fun createRoute(taskId: String?, selectedDate: String? = null, startTime: String? = null, endTime: String? = null): String {
            var route = "task_detail"
            val params = mutableListOf<String>()
            taskId?.let { params.add("taskId=$it") }
            selectedDate?.let { params.add("selectedDate=$it") }
            startTime?.let { params.add("startTime=$it") }
            endTime?.let { params.add("endTime=$it") }
            if (params.isNotEmpty()) {
                route += "?" + params.joinToString("&")
            }
            return route
        }
    }
    object CategoryManagement : Screen("category_management")
    object OverdueTasks : Screen("overdue_tasks")
    object NotificationSettings : Screen("notification_settings")
    object LanguageSelection : Screen("language_selection")
}
