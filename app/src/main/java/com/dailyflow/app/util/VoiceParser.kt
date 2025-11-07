package com.dailyflow.app.util

import com.dailyflow.app.data.model.Priority
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

object VoiceParser {
    
    fun parseTaskFromVoice(text: String): ParsedTask {
        val title = extractTitle(text)
        val description = extractDescription(text)
        val dateTime = extractDateTime(text)
        val priority = extractPriority(text)
        val category = extractCategory(text)
        
        return ParsedTask(
            title = title,
            description = description,
            dateTime = dateTime,
            priority = priority,
            category = category
        )
    }
    
    fun parseNoteFromVoice(text: String): ParsedNote {
        val title = extractTitle(text)
        val content = text // For notes, the entire text is content
        
        return ParsedNote(
            title = title,
            content = content
        )
    }
    
    private fun extractTitle(text: String): String {
        // Remove common voice command prefixes
        val cleanText = text
            .replaceFirst(Regex("(создай|добавь|поставь|запланируй|напомни)\\s*", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("(задачу|дело|заметку)\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
        
        // Take first sentence as title
        return cleanText.split(".")[0].trim()
    }
    
    private fun extractDescription(text: String): String? {
        val sentences = text.split(".")
        return if (sentences.size > 1) {
            sentences.drop(1).joinToString(". ").trim().takeIf { it.isNotEmpty() }
        } else null
    }
    
    private fun extractDateTime(text: String): LocalDateTime? {
        val now = LocalDateTime.now()
        
        // Today
        if (text.contains(Regex("(сегодня|сейчас|немедленно)", RegexOption.IGNORE_CASE))) {
            return now
        }
        
        // Tomorrow
        if (text.contains(Regex("(завтра)", RegexOption.IGNORE_CASE))) {
            return now.plusDays(1)
        }
        
        // Day of week
        val dayPattern = Pattern.compile("(понедельник|вторник|среду|четверг|пятницу|субботу|воскресенье)", Pattern.CASE_INSENSITIVE)
        val dayMatcher = dayPattern.matcher(text)
        if (dayMatcher.find()) {
            val dayName = dayMatcher.group(1).lowercase()
            val targetDay = when (dayName) {
                "понедельник" -> 1
                "вторник" -> 2
                "среду" -> 3
                "четверг" -> 4
                "пятницу" -> 5
                "субботу" -> 6
                "воскресенье" -> 7
                else -> null
            }
            
            if (targetDay != null) {
                val currentDay = now.dayOfWeek.value
                val daysToAdd = if (targetDay >= currentDay) targetDay - currentDay else 7 - currentDay + targetDay
                return now.plusDays(daysToAdd.toLong())
            }
        }
        
        // Time patterns
        val timePattern = Pattern.compile("(\\d{1,2}):(\\d{2})")
        val timeMatcher = timePattern.matcher(text)
        if (timeMatcher.find()) {
            val hour = timeMatcher.group(1).toInt()
            val minute = timeMatcher.group(2).toInt()
            
            var targetDate = now.toLocalDate()
            
            // If time is in the past, assume tomorrow
            val targetTime = LocalTime.of(hour, minute)
            if (now.toLocalTime().isAfter(targetTime)) {
                targetDate = targetDate.plusDays(1)
            }
            
            return LocalDateTime.of(targetDate, targetTime)
        }
        
        // Relative time
        if (text.contains(Regex("(через)\\s*(\\d+)\\s*(час|минут)", RegexOption.IGNORE_CASE))) {
            val numberPattern = Pattern.compile("(\\d+)")
            val numberMatcher = numberPattern.matcher(text)
            if (numberMatcher.find()) {
                val number = numberMatcher.group(1).toInt()
                
                return when {
                    text.contains(Regex("час", RegexOption.IGNORE_CASE)) -> now.plusHours(number.toLong())
                    text.contains(Regex("минут", RegexOption.IGNORE_CASE)) -> now.plusMinutes(number.toLong())
                    else -> now.plusMinutes(number.toLong())
                }
            }
        }
        
        return null
    }
    
    private fun extractPriority(text: String): Priority {
        return when {
            text.contains(Regex("(важно|срочно|высокий|критично)", RegexOption.IGNORE_CASE)) -> Priority.HIGH
            text.contains(Regex("(средне|обычно|нормально)", RegexOption.IGNORE_CASE)) -> Priority.MEDIUM
            text.contains(Regex("(неважно|низкий|позже)", RegexOption.IGNORE_CASE)) -> Priority.LOW
            else -> Priority.MEDIUM
        }
    }
    
    private fun extractCategory(text: String): String? {
        return when {
            text.contains(Regex("(работа|офис|проект|встреча)", RegexOption.IGNORE_CASE)) -> "work"
            text.contains(Regex("(дом|семья|личное)", RegexOption.IGNORE_CASE)) -> "personal"
            text.contains(Regex("(здоровье|спорт|врач|тренировка)", RegexOption.IGNORE_CASE)) -> "health"
            text.contains(Regex("(учёба|образование|курс)", RegexOption.IGNORE_CASE)) -> "education"
            text.contains(Regex("(покупки|магазин|деньги)", RegexOption.IGNORE_CASE)) -> "shopping"
            else -> null
        }
    }
}

data class ParsedTask(
    val title: String,
    val description: String? = null,
    val dateTime: LocalDateTime? = null,
    val priority: Priority = Priority.MEDIUM,
    val category: String? = null
)

data class ParsedNote(
    val title: String,
    val content: String
)
