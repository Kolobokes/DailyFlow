package com.dailyflow.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "recurring_templates")
data class RecurringTemplate(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String? = null,
    val categoryId: String? = null,
    val priority: Priority = Priority.LOW,
    val startDateTime: LocalDateTime,
    val durationMinutes: Int,
    val recurrenceRule: RecurrenceRule,
    val reminderEnabled: Boolean = false,
    val reminderMinutes: Int? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

