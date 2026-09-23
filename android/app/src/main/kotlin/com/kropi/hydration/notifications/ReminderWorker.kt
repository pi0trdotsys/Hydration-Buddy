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
import com.kropi.hydration.data.HydrationPlan
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.Level
import com.kropi.hydration.data.NotificationTone
import com.kropi.hydration.data.SNARK_CLOSERS
import com.kropi.hydration.data.SNARK_JABS
import com.kropi.hydration.data.SNARK_TITLES
import com.kropi.hydration.data.SNARK_TITLES_AFTER_HOURS
import com.kropi.hydration.data.SNARK_TITLES_BEHIND
import com.kropi.hydration.data.pick
import com.kropi.hydration.data.snarkGapJab
import com.kropi.hydration.data.PlanStatus
import com.kropi.hydration.data.daypartFor
import com.kropi.hydration.data.formatMl
import com.kropi.hydration.data.hhmm
import com.kropi.hydration.data.plan
import com.kropi.hydration.data.timesText
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
        val plan = state.plan(now)
        val message = buildMessage(state, plan, sinceLast)
        val notification = NotificationHelper.buildReminderNotification(
            context = applicationContext,
            state = state,
            plan = plan,
            title = message.title,
            shortText = message.short,
            fullText = message.full,
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationHelper.REMINDER_NOTIFICATION_ID, notification)
    }

    private data class ReminderMessage(val title: String, val short: String, val full: String)

    /**
     * Treść liczona pod bieżący stan dnia: ile brakuje, o której wypaść kolejne
     * porcje i o której godzinie cel się domknie — zamiast ogólnego "napij się wody".
     */
    private fun buildMessage(state: HydrationState, plan: HydrationPlan, sinceLastMinutes: Long): ReminderMessage {
        val daypart = daypartFor(LocalTime.now().hour)
        val gap = (state.targetSoFar - state.total).coerceAtLeast(0)
        val pct = (state.progress * 100).toInt()
        val snarky = state.settings.notificationTone == NotificationTone.SNARKY
        val seed = state.intakes.size * 7 + LocalTime.now().hour + state.total / 100

        val title = if (snarky) {
            when {
                plan.status == PlanStatus.DONE -> pick(SNARK_TITLES.getValue(Level.DONE), seed)
                plan.status == PlanStatus.AFTER_HOURS -> pick(SNARK_TITLES_AFTER_HOURS, seed)
                gap >= state.settings.reminderGlassMl * 2 -> pick(SNARK_TITLES_BEHIND, seed)
                else -> pick(SNARK_TITLES.getValue(state.level), seed)
            }
        } else {
            when {
                plan.status == PlanStatus.DONE -> "💧 Cel na dziś zrobiony"
                plan.status == PlanStatus.AFTER_HOURS -> "💧 Dzień się kończy"
                state.progress >= 0.99 -> "💧 Ostatni akord"
                gap >= state.settings.reminderGlassMl * 2 -> "💧 Kropi się martwi"
                daypart == Daypart.MORNING -> "💧 Dzień dobry, czas na wodę"
                daypart == Daypart.EVENING -> "💧 Wieczorne przypomnienie"
                else -> "💧 Czas na łyk wody"
            }
        }

        val next = plan.next
        val short = when {
            plan.status == PlanStatus.DONE ->
                if (snarky) {
                    "Wypite ${formatMl(state.total)} z ${formatMl(state.goal)}. Nie mdlej z dumy."
                } else {
                    "Wypite ${formatMl(state.total)} z ${formatMl(state.goal)}. Piękna robota."
                }
            next == null ->
                "Okno picia zamknięte, a brakuje ${formatMl(plan.remainingMl)}. Dopij, ile dasz radę."
            plan.status == PlanStatus.AFTER_HOURS ->
                "Okno picia zamknięte, a brakuje ${formatMl(plan.remainingMl)}. " +
                    "Jeśli dasz radę, wypij ${formatMl(next.ml)} ok. ${next.time.hhmm()}."
            plan.sips.size == 1 ->
                "Ostatnie ${formatMl(plan.portionMl)} o ${next.time.hhmm()} i cel ${formatMl(state.goal)} zaliczony."
            else ->
                "Wypij ${formatMl(plan.portionMl)} o ${next.time.hhmm()} — i jeszcze ${plan.sips.size - 1}× " +
                    "do ${plan.windowEnd.hhmm()}, a cel ${formatMl(state.goal)} będzie zrobiony."
        }

        val hoursSince = sinceLastMinutes / 60
        val minsSince = sinceLastMinutes % 60
        val sinceText = if (hoursSince > 0) "${hoursSince}h ${minsSince}min" else "$minsSince min"

        val full = buildString {
            appendLine("Wypite: ${formatMl(state.total)} z ${formatMl(state.goal)} ($pct%).")
            if (gap > 0) {
                appendLine("O tej porze plan zakłada ${formatMl(state.targetSoFar)} — jesteś ${formatMl(gap)} pod kreską.")
            } else {
                appendLine("Plan na tę porę to ${formatMl(state.targetSoFar)} — jesteś na bieżąco.")
            }
            appendLine("Ostatni łyk: $sinceText temu.")
            appendLine()

            when (plan.status) {
                PlanStatus.DONE -> appendLine("Cel dnia domknięty — dalsze picie to już bonus.")
                PlanStatus.AFTER_HOURS -> {
                    appendLine(
                        "Okno picia (${state.settings.activeStartHour}:00–${plan.windowEnd.hhmm()}) już się zamknęło, " +
                            "a brakuje ${formatMl(plan.remainingMl)}.",
                    )
                    next?.let {
                        appendLine("Jeśli dasz radę, wypij jeszcze ${formatMl(it.ml)} ok. ${it.time.hhmm()} — reszta jutro.")
                    }
                }
                PlanStatus.TIGHT, PlanStatus.ON_TRACK -> {
                    appendLine("Plan do ${plan.windowEnd.hhmm()} — ${plan.sips.size} × ${formatMl(plan.portionMl)}:")
                    appendLine(plan.timesText())
                    plan.finishAt?.let {
                        appendLine("Ostatnia porcja o ${it.hhmm()} domyka cel ${formatMl(state.goal)}.")
                    }
                    if (plan.status == PlanStatus.TIGHT) {
                        appendLine(
                            "Zostało mało czasu, więc porcje są większe niż Twoja szklanka " +
                                "(${formatMl(state.settings.reminderGlassMl)}). Pij spokojnie, nie na raz.",
                        )
                    }
                }
            }

            appendLine()
            if (snarky) {
                snarkGapJab(sinceLastMinutes, seed)?.let { appendLine(it) }
                append(pick(SNARK_JABS.getValue(state.level), seed))
                append(" ")
                append(pick(SNARK_CLOSERS, seed * 3))
            } else {
                append(state.selfCare)
            }
        }.trim()

        return ReminderMessage(title, short, full)
    }

    private fun hasNotificationPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
