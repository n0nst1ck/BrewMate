package com.panko.brewmate.data

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.panko.brewmate.model.ScheduledBrew
import com.panko.brewmate.receiver.BrewAlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId

class AndroidAlarmScheduler(
    private val context: Context
) : SystemSchedulerInterface {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    @SuppressLint("ScheduleExactAlarm") // specific permission check handled in UI usually
    override fun schedule(brew: ScheduledBrew): Boolean {
        // 1. Create the Intent to trigger our Receiver
        val intent = Intent(context, BrewAlarmReceiver::class.java).apply {
            putExtra("DRINK_NAME", brew.drinkName)
            putExtra("BREW_ID", brew.id)
            putExtra("SCHEDULE_ID", brew.id)
            putExtra("USER_ID", brew.userID)
            putExtra("IS_RECURRENT", brew.isRecurrent)
        }

        // 2. Create PendingIntent (Unique ID based on brew.id hash)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            brew.id.hashCode(), // Unique Request Code per brew
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Calculate Time (Next occurrence of HH:MM)
        val now = LocalDateTime.now()
        var alarmTime = now.withHour(brew.hour).withMinute(brew.minute).withSecond(0)

        // If time is in the past (e.g. it's 10:00 and we set for 07:00), schedule for tomorrow
        if (alarmTime.isBefore(now)) {
            alarmTime = alarmTime.plusDays(1)
        }

        val triggerAtMillis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // 4. Set the Alarm
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    true
                } else {
                    Log.e("AlarmScheduler", "Permission SCHEDULE_EXACT_ALARM missing")
                    false
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                true
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            false
        }
    }

    override fun cancel(brewId: String): Boolean {
        return try {
            val intent = Intent(context, BrewAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                brewId.hashCode(), // Must match the ID used in schedule()
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            true
        } catch (e: Exception) {
            false
        }
    }
}