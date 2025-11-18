package com.dailyflow.app.util

import com.dailyflow.app.data.model.RecurrenceFrequency
import com.dailyflow.app.data.model.RecurrenceRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.max

object RecurrenceUtils {

    private const val MAX_OCCURRENCES = 365

    fun generateOccurrences(
        start: LocalDateTime,
        rule: RecurrenceRule,
        limit: Int = MAX_OCCURRENCES
    ): List<LocalDateTime> {
        if (rule.endDate != null && start.toLocalDate().isAfter(rule.endDate)) return emptyList()
        val maxAllowed = rule.occurrenceCount ?: limit
        if (maxAllowed <= 0) return emptyList()

        val daysOfWeek = when (rule.frequency) {
            RecurrenceFrequency.WEEKLY_DAYS -> {
                // Для WEEKLY_DAYS должны быть выбраны дни недели
                if (rule.daysOfWeek.isEmpty()) {
                    // Если дни не выбраны, используем день стартовой даты
                    setOf(start.dayOfWeek)
                } else {
                    rule.daysOfWeek
                }
            }
            RecurrenceFrequency.WEEKLY -> {
                // Для обычного WEEKLY используем день стартовой даты или выбранные дни
                rule.daysOfWeek.ifEmpty { setOf(start.dayOfWeek) }
            }
            else -> emptySet()
        }.sorted()
        
        val dayOfMonth = rule.dayOfMonth ?: start.dayOfMonth
        
        // Для WEEKLY_DAYS нужно начать с первого подходящего дня недели, если стартовая дата не совпадает
        val initialStart = when (rule.frequency) {
            RecurrenceFrequency.WEEKLY_DAYS -> {
                if (daysOfWeek.isNotEmpty() && start.dayOfWeek !in daysOfWeek) {
                    // Находим следующий подходящий день недели
                    val nextDay = daysOfWeek.firstOrNull { it >= start.dayOfWeek }
                        ?: daysOfWeek.first()
                    start.with(TemporalAdjusters.nextOrSame(nextDay))
                } else {
                    start
                }
            }
            else -> start
        }
        
        if (rule.endDate != null && initialStart.toLocalDate().isAfter(rule.endDate)) return emptyList()
        
        val occurrences = mutableListOf(initialStart)
        var current = initialStart
        
        while (occurrences.size < maxAllowed && occurrences.size < limit) {
            val next = when (rule.frequency) {
                RecurrenceFrequency.DAILY -> current.plusDays(rule.interval.toLong())
                RecurrenceFrequency.WEEKLY, RecurrenceFrequency.WEEKLY_DAYS -> nextWeeklyOccurrence(current, rule.interval, daysOfWeek, initialStart)
                RecurrenceFrequency.MONTHLY -> nextMonthlyOccurrence(current, rule.interval, dayOfMonth)
            }
            if (rule.endDate != null) {
                val nextDate = next.toLocalDate()
                // Проверяем, что следующая дата не позже конечной даты (включая саму конечную дату)
                if (nextDate.isAfter(rule.endDate)) break
            }
            occurrences += next
            current = next
        }
        return occurrences
    }

    fun estimateOccurrences(
        start: LocalDateTime,
        rule: RecurrenceRule,
        limit: Int = MAX_OCCURRENCES
    ): Int = generateOccurrences(start, rule, limit).size

    private fun nextWeeklyOccurrence(
        current: LocalDateTime,
        interval: Int,
        days: List<DayOfWeek>,
        baseStart: LocalDateTime
    ): LocalDateTime {
        if (days.isEmpty()) {
            // Если дни не указаны, используем день базовой стартовой даты
            return current.plusWeeks(max(interval, 1).toLong())
        }
        
        val orderedDays = days
        val currentDay = current.dayOfWeek
        val currentIndex = orderedDays.indexOf(currentDay)

        if (currentIndex == -1) {
            // Текущий день не в списке выбранных дней - находим следующий подходящий день
            val nextDaySameWeek = orderedDays.firstOrNull { it > currentDay }
            return if (nextDaySameWeek != null) {
                // Есть подходящий день в текущей неделе
                current.with(TemporalAdjusters.nextOrSame(nextDaySameWeek))
            } else {
                // Переходим к следующей неделе (или через interval недель)
                val weeksToAdd = max(interval, 1) - 1
                current.with(TemporalAdjusters.next(orderedDays.first()))
                    .plusWeeks(weeksToAdd.toLong())
            }
        }

        // Текущий день в списке - переходим к следующему дню из списка
        val nextIndex = (currentIndex + 1) % orderedDays.size
        val nextDay = orderedDays[nextIndex]
        
        // Если следующий день идет после текущего в той же неделе
        if (nextDay > currentDay) {
            return current.with(TemporalAdjusters.next(nextDay))
        } else {
            // Следующий день в следующей неделе (или через interval недель)
            val weeksToAdd = max(interval, 1) - 1
            return current.with(TemporalAdjusters.next(nextDay))
                .plusWeeks(weeksToAdd.toLong())
        }
    }

    private fun nextMonthlyOccurrence(
        current: LocalDateTime,
        interval: Int,
        dayOfMonth: Int
    ): LocalDateTime {
        val nextMonthBase = current.plusMonths(max(interval, 1).toLong())
        val length = nextMonthBase.toLocalDate().lengthOfMonth()
        val targetDay = dayOfMonth.coerceAtMost(length)
        return nextMonthBase.withDayOfMonth(targetDay)
    }
}

