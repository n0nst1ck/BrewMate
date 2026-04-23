package com.panko.brewmate.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.panko.brewmate.BrewMateApplication
import com.panko.brewmate.MainActivity
import com.panko.brewmate.model.BrewSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InventoryCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val drinkName = intent.getStringExtra("DRINK_NAME") ?: "Scheduled Drink"

        // Unpack Settings
        val brewSettings = if (Build.VERSION.SDK_INT >= 33) {
            intent.getSerializableExtra("BREW_SETTINGS", BrewSettings::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("BREW_SETTINGS") as? BrewSettings
        } ?: return // If settings fail to load, just abort

        val app = context.applicationContext as BrewMateApplication
        val coffeeMakerRepo = app.coffeeMakerRepository

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val missingItems = coffeeMakerRepo.getMissingIngredients(brewSettings)

                if (missingItems.isNotEmpty()) {
                    // Turn list ["Water", "Coffee Beans"] into "Water, Coffee Beans"
                    val itemsString = missingItems.joinToString(", ")
                    showLowInventoryNotification(context, drinkName, itemsString)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showLowInventoryNotification(context: Context, drinkName: String, missingItems: String) {
        val channelId = "inventory_check_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Refill Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to refill your machine before a scheduled brew"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Warning icon
            .setContentTitle("Machine Needs Refill! ⚠️")
            .setContentText("Add $missingItems for your $drinkName.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Add $missingItems for your $drinkName.")) // Expands for long lists
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() / 1000).toInt(), notification)
    }
}