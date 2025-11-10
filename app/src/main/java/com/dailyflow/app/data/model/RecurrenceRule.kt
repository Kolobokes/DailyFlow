package com.dailyflow.app.data.model

import java.time.DayOfWeek
import java.time.LocalDate

data class RecurrenceRule(
    val frequency: RecurrenceFrequency = RecurrenceFrequency.DAILY,
    val interval: Int = 1,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val dayOfMonth: Int? = null,
    val endDate: LocalDate? = null,
    val occurrenceCount: Int? = null
)

