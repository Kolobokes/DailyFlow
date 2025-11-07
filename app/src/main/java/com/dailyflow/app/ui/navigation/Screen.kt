package com.dailyflow.app.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object Notes : Screen("notes?date={date}") {
        val routeWithArgs = "notes?date={date}"
        fun createRoute(date: String? = null): String {
            return date?.let { "notes?date=$it" } ?: "notes?date="
        }
    }
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
    object NoteDetail : Screen("note_detail?noteId={noteId}") {
        val routeWithArgs = "note_detail?noteId={noteId}"
        fun createRoute(noteId: String?): String {
            return if (noteId != null) "note_detail?noteId=$noteId" else "note_detail"
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
}
