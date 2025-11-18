package com.dailyflow.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String? = null,
    val categoryId: String? = null,
    val startDateTime: LocalDateTime? = null,
    val endDateTime: LocalDateTime? = null,
    val reminderEnabled: Boolean = false,
    val reminderMinutes: Int? = null,
    val priority: Priority = Priority.LOW,
    val status: TaskStatus = TaskStatus.PENDING,
    val seriesId: String? = null,
    val isException: Boolean = false,
    val originalStartDateTime: LocalDateTime? = null,
    val sequenceNumber: Int? = null,
    val attachedFileUri: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
