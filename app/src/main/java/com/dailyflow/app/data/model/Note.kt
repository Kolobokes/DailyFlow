package com.dailyflow.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val categoryId: String? = null,
    val dateTime: LocalDateTime? = null,
    val priority: Priority = Priority.MEDIUM,
    val isCompleted: Boolean = false,
    val isChecklist: Boolean = false,
    val checklistItems: List<ChecklistItem>? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
