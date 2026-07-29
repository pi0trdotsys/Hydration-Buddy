package com.kropi.hydration.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Periodic reminder tick. 15 minutes is WorkManager's floor for periodic work —
 * plenty for spacing reminders that are themselves at least 20 minutes apart,
 * and frequent enough to keep the widget's "target so far" fresh.
 */
object NotificationScheduler {
    private const val WORK_NAME = "hydration_reminder_tick"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Enqueued from the "test notification" button — bypasses timing/interval gating. */
    fun sendTestNotification(context: Context) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(Data.Builder().putBoolean(ReminderWorker.INPUT_FORCE, true).build())
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
