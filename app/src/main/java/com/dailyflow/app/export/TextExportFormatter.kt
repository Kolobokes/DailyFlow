package com.dailyflow.app.export

import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.ChecklistItem
import com.dailyflow.app.data.model.Note
import com.dailyflow.app.data.model.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextExportFormatter @Inject constructor() {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale("ru"))
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("ru"))

    fun formatDailyPlan(
        date: LocalDate,
        tasks: List<Task>,
        categories: Map<String, Category>
    ): String {
        val builder = StringBuilder()
        builder.appendLine("План на ${date.format(dateFormatter)}")
        builder.appendLine()

        if (tasks.isEmpty()) {
            builder.append("На этот день задач нет.")
            return builder.toString()
        }

        tasks.sortedWith(compareBy<Task> { it.startDateTime ?: it.createdAt }.thenBy { it.title })
            .forEachIndexed { index, task ->
                builder.append(formatTask(task, categories[task.categoryId]))
                if (index != tasks.lastIndex) {
                    builder.appendLine()
                    builder.appendLine("———")
                    builder.appendLine()
                }
            }
        return builder.toString()
    }

    fun formatTask(task: Task, category: Category?): String {
        val builder = StringBuilder()
        builder.appendLine("Задача: ${task.title}")

        val start = task.startDateTime
        val end = task.endDateTime
        if (start != null || end != null) {
            builder.append("Время: ")
            when {
                start != null && end != null -> builder.append(
                    "${start.format(timeFormatter)} — ${end.format(timeFormatter)}"
                )
                start != null -> builder.append("начало в ${start.format(timeFormatter)}")
                else -> builder.append("до ${end!!.format(timeFormatter)}")
            }
            builder.appendLine()
        }

        task.reminderMinutes?.takeIf { task.reminderEnabled }?.let { minutes ->
            builder.appendLine("Напоминание: за $minutes минут до начала")
        }

        category?.let {
            builder.appendLine("Категория: ${it.name}")
        }

        task.description?.takeIf { it.isNotBlank() }?.let {
            builder.appendLine()
            builder.appendLine("Описание:")
            builder.appendLine(it.trim())
        }

        return builder.toString().trimEnd()
    }

    fun formatNote(note: Note, category: Category?): String {
        val builder = StringBuilder()
        builder.appendLine("Заметка: ${note.title}")

        note.dateTime?.let {
            builder.appendLine("Дата: ${it.format(dateTimeFormatter)}")
        }

        category?.let {
            builder.appendLine("Категория: ${it.name}")
        }

        if (note.isChecklist) {
            builder.appendLine()
            builder.appendLine("Чек-лист:")
            val items = note.checklistItems.orEmpty()
            if (items.isEmpty()) {
                builder.appendLine("  (пусто)")
            } else {
                items.forEach { item ->
                    builder.appendLine("  ${checklistMark(item)} ${item.text}")
                }
            }
        } else {
            note.content.takeIf { it.isNotBlank() }?.let {
                builder.appendLine()
                builder.appendLine("Содержание:")
                builder.appendLine(it.trim())
            }
        }

        return builder.toString().trimEnd()
    }

    private fun checklistMark(item: ChecklistItem): String = if (item.isChecked) "[x]" else "[ ]"
}

