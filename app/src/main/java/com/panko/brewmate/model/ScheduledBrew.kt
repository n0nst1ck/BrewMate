package com.panko.brewmate.model

import android.provider.CalendarContract
import com.google.type.DayOfWeek
import java.util.UUID

data class ScheduledBrew(
    val id: String = UUID.randomUUID().toString(),
    val userID: String,
    val brewSettings: BrewSettings,

    // Recurrence
    val isRecurrent: Boolean = false,

    // Used when isRecurrent false
    val targetDateTimeMillis: Long? = null,

    // Used when isRecurrent true
    val hour: Int,
    val minute: Int,
    val recurrenceDays: Set<DayOfWeek> = emptySet(),

    val isActive: Boolean = true
)