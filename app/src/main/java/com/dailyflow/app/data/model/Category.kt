package com.dailyflow.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: String,
    val icon: String,
    val forTasks: Boolean = true,
    val forNotes: Boolean = true,
    val isArchived: Boolean = false
)
