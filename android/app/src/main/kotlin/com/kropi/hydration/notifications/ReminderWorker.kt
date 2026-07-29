package com.kropi.hydration.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kropi.hydration.data.Daypart
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.daypartFor
import com.kropi.hydration.widget.HydrationWidget
import java.time.LocalTime
import kotlin.math.ceil

/**
 * Runs every ~15 minutes (the WorkManager periodic floor). Each tick:
 *  1. Refreshes the home-screen widget so "target so far" stays current.
 *  2. Decides whether it's been too long since the last sip — if so, and we're
 *     inside the user's active drinking window, fires a reminder notification.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val INPUT_FORCE = "force"
    }

    override suspend fun doWork(): Result {
        val repo = HydrationRepository(applicationContext)
        val state = repo.current()

        HydrationWidget().updateAll(applicationContext)

        val force = inputData.getBoolean(INPUT_FORCE, false)
        maybeNotify(repo, state, force)
        return Result.success()
    }

    private suspend fun maybeNotify(repo: HydrationRepository, state: HydrationState, force: Boolean) {
        val settings = state.settings
        if (!force) {
            if (!settings.remindersEnabled) return
            if (state.remaining <= 0) return
            if (repo.isSnoozed()) return
        }

        val now = LocalTime.now()
        val nowMinutes = now.hour * 60 + now.minute
        val startMinutes = settings.activeStartHour * 60
        val endMinutes = settings.activeEndHour * 60
        if (!force && (nowMinutes < startMinutes || nowMinutes >= endMinutes)) return

        val numGlasses = ceil(state.goal.toDouble() / settings.reminderGlassMl).toInt().coerceAtLeast(1)
        val activeWindowMinutes = (endMinutes - startMinutes).coerceAtLeast(60)
        val intervalMinutes = (activeWindowMinutes / numGlasses).coerceIn(20, 180)

        val sinceLast = repo.minutesSinceLastIntake() ?: (nowMinutes - startMinutes).toLong().coerceAtLeast(0)
        if (!force && sinceLast < intervalMinutes) return

        if (!hasNotificationPermission()) return

        NotificationHelper.ensureChannel(applicationContext)
        val (title, body) = buildMessage(state, sinceLast)
        val notification = NotificationHelper.buildReminderNotification(applicationContext, state, title, body)
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationHelper.REMINDER_NOTIFICATION_ID, notification)
    }

    private fun buildMessage(state: HydrationState, sinceLastMinutes: Long): Pair<String, String> {
        val daypart = daypartFor(LocalTime.now().hour)
        val gap = (state.targetSoFar - state.total).coerceAtLeast(0)

        val title = when {
            state.progress >= 0.99 -> "💧 Ostatni akord"
            gap >= state.settings.reminderGlassMl * 2 -> "💧 Kropi się martwi"
            daypart == Daypart.MORNING -> "💧 Dzień dobry, czas na wodę"
            daypart == Daypart.EVENING -> "💧 Wieczorne przypomnienie"
            else -> "💧 Czas na łyk wody"
        }

        val hoursSince = sinceLastMinutes / 60
        val minsSince = sinceLastMinutes % 60
        val sinceText = if (hoursSince > 0) "${hoursSince}h ${minsSince}min" else "${minsSince} min"

        val body = buildString {
            append("Nie piłeś/aś wody od $sinceText. ")
            append("Powinieneś/aś mieć już ok. ${state.targetSoFar} ml, masz ${state.total} ml. ")
            append(state.selfCare)
        }

        return title to body
    }

    private fun hasNotificationPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
