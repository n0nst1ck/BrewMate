package com.panko.brewmate.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.panko.brewmate.BrewMateApplication
import com.panko.brewmate.MainActivity
import com.panko.brewmate.model.BrewSettings
import com.panko.brewmate.model.DrinkType
import com.panko.brewmate.model.BrewHistoryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BrewAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val drinkName = intent.getStringExtra("DRINK_NAME") ?: "Coffee"
        val userId = intent.getStringExtra("USER_ID")
        val scheduleId = intent.getStringExtra("SCHEDULE_ID") // Needed for cleanup
        val isRecurrent = intent.getBooleanExtra("IS_RECURRENT", false) // Needed for cleanup

        // Unpack Settings
        val brewSettings = if (Build.VERSION.SDK_INT >= 33) {
            intent.getSerializableExtra("BREW_SETTINGS", BrewSettings::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("BREW_SETTINGS") as? BrewSettings
        } ?: BrewSettings.DEFAULT

        // 🔔 Tell the user the coffee is making!
        showNotification(context, drinkName)

        // 🌟 GET THE REPOSITORIES FROM THE APP 🌟
        val app = context.applicationContext as BrewMateApplication
        val coffeeMakerRepo = app.coffeeMakerRepository
        val historyRepo = app.historyRepository

        if (userId != null) {
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1. Turn on machine if off
                    val currentState = coffeeMakerRepo.coffeeMakerState.value
                    if (!currentState.isPoweredOn) {
                        coffeeMakerRepo.togglePower()
                        delay(1500) // Wait for boot up
                    }

                    // 2. START THE BREW! (Fixed parameter to specificName)
                    val isBrewing = coffeeMakerRepo.startBrew(
                        drinkType = DrinkType.CUSTOM,
                        customSettings = brewSettings,
                        drinkName = drinkName
                    )

                    // 3. Add to History!
                    if (isBrewing) {
                        val historyItem = BrewHistoryItem(
                            userId = userId,
                            drinkName = drinkName,
                            settings = brewSettings,
                            timestamp = System.currentTimeMillis()
                        )
                        historyRepo.addHistoryItem(historyItem)
                    }

                    // 4. CLEANUP: Delete one-time schedules from Firestore
                    if (scheduleId != null && !isRecurrent) {
                        Firebase.firestore.collection("users").document(userId)
                            .collection("schedules").document(scheduleId)
                            .delete()
                            .await()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    // --- The Notification Function ---
    private fun showNotification(context: Context, drinkName: String) {
        val channelId = "brew_alarm_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Auto-Brew Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when your coffee starts brewing"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Brewing Started! ☕")
            .setContentText("Your $drinkName is being prepared automatically.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}