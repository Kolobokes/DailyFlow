package com.dailyflow.app.data.database

import androidx.room.TypeConverter
import com.dailyflow.app.data.model.Priority
import com.dailyflow.app.data.model.RecurrenceFrequency
import com.dailyflow.app.data.model.RecurrenceRule
import com.dailyflow.app.data.model.TaskStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(formatter)
    }
    
    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let { LocalDateTime.parse(it, formatter) }
    }
    
    @TypeConverter
    fun fromPriority(priority: Priority): String {
        return priority.name
    }
    
    @TypeConverter
    fun toPriority(priorityString: String): Priority {
        return Priority.valueOf(priorityString)
    }
    
    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toTaskStatus(statusString: String): TaskStatus {
        return TaskStatus.valueOf(statusString)
    }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromRecurrenceRule(rule: RecurrenceRule?): String? {
        if (rule == null) return null
        val days = rule.daysOfWeek.joinToString(",") { it.name }
        val parts = listOf(
            rule.frequency.name,
            rule.interval.toString(),
            days,
            rule.dayOfMonth?.toString() ?: "",
            rule.endDate?.toString() ?: "",
            rule.occurrenceCount?.toString() ?: ""
        )
        return parts.joinToString(";")
    }

    @TypeConverter
    fun toRecurrenceRule(value: String?): RecurrenceRule? {
        if (value.isNullOrBlank()) return null
        val parts = value.split(";")
        val frequency = parts.getOrNull(0)?.let { RecurrenceFrequency.valueOf(it) } ?: RecurrenceFrequency.DAILY
        val interval = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val days = parts.getOrNull(2)
            ?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { DayOfWeek.valueOf(it) }
            ?.toSet()
            ?: emptySet()
        val dayOfMonth = parts.getOrNull(3)?.toIntOrNull()
        val endDate = parts.getOrNull(4)?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
        val occurrenceCount = parts.getOrNull(5)?.toIntOrNull()
        return RecurrenceRule(
            frequency = frequency,
            interval = interval,
            daysOfWeek = days,
            dayOfMonth = dayOfMonth,
            endDate = endDate,
            occurrenceCount = occurrenceCount
        )
    }
}
