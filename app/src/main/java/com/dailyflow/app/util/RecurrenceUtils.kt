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

        val occurrences = mutableListOf(start)
        val daysOfWeek = rule.daysOfWeek.ifEmpty { setOf(start.dayOfWeek) }.sorted()
        val dayOfMonth = rule.dayOfMonth ?: start.dayOfMonth
        var current = start
        while (occurrences.size < maxAllowed && occurrences.size < limit) {
            val next = when (rule.frequency) {
                RecurrenceFrequency.DAILY -> current.plusDays(rule.interval.toLong())
                RecurrenceFrequency.WEEKLY, RecurrenceFrequency.WEEKLY_DAYS -> nextWeeklyOccurrence(current, rule.interval, daysOfWeek, start)
                RecurrenceFrequency.MONTHLY -> nextMonthlyOccurrence(current, rule.interval, dayOfMonth)
            }
            if (rule.endDate != null && next.toLocalDate().isAfter(rule.endDate)) break
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
        val orderedDays = if (days.isEmpty()) listOf(baseStart.dayOfWeek) else days
        val currentDay = current.dayOfWeek
        val currentIndex = orderedDays.indexOf(currentDay)

        if (currentIndex == -1) {
            val nextDaySameWeek = orderedDays.firstOrNull { it >= currentDay }
            return if (nextDaySameWeek != null) {
                current.with(TemporalAdjusters.nextOrSame(nextDaySameWeek))
            } else {
                current.with(TemporalAdjusters.next(orderedDays.first()))
                    .plusWeeks((max(interval, 1) - 1).toLong())
            }
        }

        val nextIndex = (currentIndex + 1) % orderedDays.size
        var next = current.with(TemporalAdjusters.next(orderedDays[nextIndex]))
        if (nextIndex <= currentIndex) {
            next = next.plusWeeks((max(interval, 1) - 1).toLong())
        }
        return next
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

