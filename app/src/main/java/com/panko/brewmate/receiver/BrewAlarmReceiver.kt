package com.panko.brewmate.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.panko.brewmate.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BrewAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val drinkName = intent.getStringExtra("DRINK_NAME") ?: "Coffee"

        showNotification(context, drinkName)

        val scheduleId = intent.getStringExtra("SCHEDULE_ID")
        val userId = intent.getStringExtra("USER_ID")
        val isRecurrent = intent.getBooleanExtra("IS_RECURRENT", false)

        // Deletes the alarm after it rings if it's not recurrent (one time)
        if (scheduleId != null && userId != null && !isRecurrent) {

            // goAsync so the process doesn't die before it finishes
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Firebase.firestore
                        .collection("users")
                        .document(userId)
                        .collection("schedules")
                        .document(scheduleId)
                        .delete()
                        .await()
                } catch (e: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }

    }

    private fun showNotification(context: Context, drinkName: String) {
        val channelId = "brew_alarm_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        val channel = NotificationChannel(
            channelId,
            "Scheduled Brews",
            NotificationManager.IMPORTANCE_HIGH // High Priority is necessary to ensure it rings
            ).apply {
                description = "Notifications for scheduled coffee"
            }
        notificationManager.createNotificationChannel(channel)

        // Intent to open App when clicked
        val contentIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Notification for the scheduled brew
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("BrewMate Ready!")
            .setContentText("Time to brew your $drinkName.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}