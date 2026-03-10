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
import com.panko.brewmate.receiver.InventoryCheckReceiver // Make sure to import this!
import java.time.LocalDateTime
import java.time.ZoneId

class AndroidAlarmScheduler(
    private val context: Context
) : SystemSchedulerInterface {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    @SuppressLint("ScheduleExactAlarm")
    override fun schedule(brew: ScheduledBrew): Boolean {
        // 1. Create the Intent to trigger our Receiver
        val intent = Intent(context, BrewAlarmReceiver::class.java).apply {
            putExtra("DRINK_NAME", brew.drinkName)
            putExtra("BREW_ID", brew.id)
            putExtra("SCHEDULE_ID", brew.id)
            putExtra("USER_ID", brew.userID)
            putExtra("IS_RECURRENT", brew.isRecurrent)
            putExtra("BREW_SETTINGS", brew.brewSettings)
        }

        // 2. Create PendingIntent
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            brew.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Calculate Time
        val now = LocalDateTime.now()
        var alarmTime = now.withHour(brew.hour).withMinute(brew.minute).withSecond(0)

        if (alarmTime.isBefore(now)) {
            alarmTime = alarmTime.plusDays(1)
        }

        val triggerAtMillis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // 4. Set the Main Alarm
        val success = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    true
                } else {
                    Log.e("AlarmScheduler", "Permission SCHEDULE_EXACT_ALARM missing")
                    false
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                true
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            false
        }

        // 👇 NEW: 5. Set the 12-Hour Inventory Check Alarm
        if (success) {
            var checkTime = alarmTime.minusHours(12)

            // If the brew is LESS than 12 hours away, check almost immediately (5 secs)
            if (checkTime.isBefore(now)) {
                checkTime = now.plusSeconds(5)
            }

            val checkIntent = Intent(context, InventoryCheckReceiver::class.java).apply {
                putExtra("DRINK_NAME", brew.drinkName)
                putExtra("BREW_SETTINGS", brew.brewSettings)
            }

            // Use "_check" to make this a separate, unique alarm from the brew alarm
            val checkPendingIntent = PendingIntent.getBroadcast(
                context,
                (brew.id + "_check").hashCode(),
                checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    checkTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    checkPendingIntent
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        return success
    }

    override fun cancel(brewId: String): Boolean {
        return try {
            // Cancel main brew alarm
            val intent = Intent(context, BrewAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                brewId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)

            // 👇 NEW: Cancel the check alarm too!
            val checkIntent = Intent(context, InventoryCheckReceiver::class.java)
            val checkPendingIntent = PendingIntent.getBroadcast(
                context,
                (brewId + "_check").hashCode(), // Must match the ID used to schedule it
                checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(checkPendingIntent)

            true
        } catch (e: Exception) {
            false
        }
    }
}