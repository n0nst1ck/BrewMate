package com.panko.brewmate.model

import android.provider.CalendarContract
import java.time.DayOfWeek
import java.util.UUID

data class ScheduledBrew(
    val id: String = UUID.randomUUID().toString(),
    val userID: String = "",
    val brewSettings: BrewSettings = BrewSettings(),
    val drinkName: String = "Coffee",

    // Recurrence
    val isRecurrent: Boolean = false,

    // Used when isRecurrent false
    val targetDateTimeMillis: Long? = null,

    // Used when isRecurrent true
    val hour: Int = 0,
    val minute: Int = 0,
    val recurrenceDays: List<DayOfWeek> = emptyList(),

    val isActive: Boolean = true
)