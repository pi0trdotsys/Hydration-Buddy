package com.kropi.hydration.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kropi.hydration.R
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.ui.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "hydration_reminders"
    const val REMINDER_NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.water_notification}")
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Przypomnienia o nawodnieniu",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Kropi przypomina o piciu wody w ciągu dnia"
            enableLights(true)
            lightColor = 0xFF00DFE8.toInt()
            enableVibration(true)
            vibrationPattern = longLongArrayOf(0, 120, 80, 160)
            setSound(soundUri, audioAttributes)
        }
        manager.createNotificationChannel(channel)
    }

    private fun longLongArrayOf(vararg v: Long) = v

    fun buildReminderNotification(context: Context, state: HydrationState, title: String, body: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val glassMl = state.settings.reminderGlassMl
        val drinkIntent = Intent(context, DrinkActionReceiver::class.java).apply {
            action = DrinkActionReceiver.ACTION_MARK_DRUNK
            putExtra(DrinkActionReceiver.EXTRA_ML, glassMl)
        }
        val drinkPendingIntent = PendingIntent.getBroadcast(
            context, 1, drinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = Intent(context, DrinkActionReceiver::class.java).apply {
            action = DrinkActionReceiver.ACTION_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, 2, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF00DFE8.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .addAction(0, "💧 Wypiłem/-am $glassMl ml", drinkPendingIntent)
            .addAction(0, "Za 20 min", snoozePendingIntent)
            .build()
    }
}
