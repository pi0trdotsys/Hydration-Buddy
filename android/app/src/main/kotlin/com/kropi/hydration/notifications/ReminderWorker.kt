package com.kropi.hydration.notifications

import com.kropi.hydration.R
import com.kropi.hydration.ui.daysText
import android.Manifest
import com.kropi.hydration.widget.updateAllWidgets
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kropi.hydration.backup.BackupStore
import com.kropi.hydration.data.Daypart
import com.kropi.hydration.data.Habits
import com.kropi.hydration.data.Workout
import com.kropi.hydration.data.WorkoutSession
import com.kropi.hydration.data.HydrationPlan
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.Level
import com.kropi.hydration.data.NotificationTone
import com.kropi.hydration.data.SNARK_CLOSERS
import com.kropi.hydration.data.SNARK_TITLES_SAVAGE
import com.kropi.hydration.data.SnarkIntensity
import com.kropi.hydration.data.snarkJab
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
import com.kropi.hydration.update.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        private const val MAX_NUDGE_ML = 500
    }

    override suspend fun doWork(): Result {
        val repo = HydrationRepository(applicationContext)
        val state = repo.current()
        // Health Connect: dopisz zaległe wpisy i odśwież wagę/aktywność (każde z nich ma własne wyłączniki).
        repo.syncHealth()
        repo.refreshProfileFromHealth()
        // Temperatura z Homebrew Weather i dzisiejsze treningi zmieniają cel dnia; potem czytamy stan jeszcze raz.
        repo.refreshOutdoor()
        val workouts = repo.syncWorkouts()
        WifiAnchor.sync(applicationContext, state.settings.homeWifiSsid)
        val fresh = repo.current()

        updateAllWidgets(applicationContext)

        val force = inputData.getBoolean(INPUT_FORCE, false)
        val sent = maybeNotify(repo, fresh, force)
        if (!sent && !force) maybeNudges(repo, fresh, workouts)
        maybeDailySummary(repo, fresh)
        // Niezależne od przypomnień: dzienna kopia zapasowa i wieść o nowej wersji.
        BackupStore.autoBackupIfDue(applicationContext, repo)
        maybeUpdateNotice()
        return Result.success()
    }

    /** Sprawdza GitHuba (najwyżej co 6 h) i raz na wersję powiadamia, że czeka aktualizacja. */
    @SuppressLint("MissingPermission")
    private suspend fun maybeUpdateNotice() {
        val version = withContext(Dispatchers.IO) { UpdateManager.backgroundCheck(applicationContext) } ?: return
        if (!hasNotificationPermission()) return
        NotificationHelper.ensureUpdateChannel(applicationContext)
        NotificationManagerCompat.from(applicationContext).notify(
            NotificationHelper.UPDATE_NOTIFICATION_ID,
            NotificationHelper.buildUpdateNotification(applicationContext, version),
        )
    }

    /**
     * Po zamknięciu okna picia Kropi podsumowuje dzień — raz na dobę, bo
     * przypomnienia już wtedy nie mają sensu, a bilans jeszcze tak.
     */
    // Uprawnienie sprawdza hasNotificationPermission() przed notify() — lint nie widzi tego przez helper.
    @SuppressLint("MissingPermission")
    private suspend fun maybeDailySummary(repo: HydrationRepository, state: HydrationState) {
        if (!state.settings.remindersEnabled) return
        val now = LocalTime.now()
        if (now.hour * 60 + now.minute < state.settings.activeEndHour * 60) return

        val today = java.time.LocalDate.now().toEpochDay()
        if (repo.summarySentDay() == today) return
        if (!hasNotificationPermission()) return

        val snarky = state.settings.notificationTone == NotificationTone.SNARKY
        val seed = state.intakes.size * 3 + state.total / 100
        val pct = (state.progress * 100).toInt()
        val reached = state.remaining <= 0

        val title = when {
            reached && snarky -> str(R.string.n_sum_title_reached_snarky)
            reached -> str(R.string.n_sum_title_reached)
            snarky -> str(R.string.n_sum_title_snarky)
            else -> str(R.string.n_sum_title)
        }
        val short = str(R.string.n_sum_short, formatMl(state.total), formatMl(state.goal), pct, applicationContext.resources.getQuantityString(R.plurals.n_sips, state.intakes.size, state.intakes.size))
        val full = buildString {
            appendLine(short)
            if (reached) {
                appendLine(str(R.string.n_sum_streak, applicationContext.daysText(state.streak)))
            } else {
                appendLine(str(R.string.n_sum_missing_streak, formatMl(state.remaining), applicationContext.daysText(state.streak)))
                if (state.streak > 0) {
                    appendLine(if (state.streakInfo.shields > 0) str(R.string.n_sum_shield_ok) else str(R.string.n_sum_shield_none))
                }
            }
            appendLine()
            append(
                if (snarky) {
                    snarkJab(state.level, state.settings.snarkIntensity, seed)
                } else {
                    state.selfCare
                },
            )
        }.trim()

        NotificationHelper.ensureChannel(applicationContext)
        NotificationManagerCompat.from(applicationContext).notify(
            NotificationHelper.SUMMARY_NOTIFICATION_ID,
            NotificationHelper.buildSummaryNotification(applicationContext, title, short, full),
        )
        repo.markSummarySent(today)
    }

    /**
     * Przypomnienia „poza planem": po treningu, gdy zagrożona jest seria i tuż przed godzinami, w których
     * zwykle nie pijesz. Wysyłamy najwyżej jedno na tik, w tej kolejności ważności, i żadne nie powtarza się
     * w tym samym dniu. Zwykłe przypomnienie godzinowe ma pierwszeństwo.
     */
    private suspend fun maybeNudges(repo: HydrationRepository, state: HydrationState, workouts: List<WorkoutSession>) {
        if (!Nudges.mayNudge(applicationContext, repo, state)) return
        val settings = state.settings
        val nowMs = System.currentTimeMillis()
        val today = java.time.LocalDate.now().toEpochDay()
        val nowMin = Nudges.nowMinute()

        if (settings.healthWorkouts) {
            Workout.nudgeCandidate(workouts, nowMs, repo.lastNotifiedWorkout())?.let { session ->
                repo.markWorkoutNotified(session.id)
                val ml = Workout.afterWorkoutMl(session)
                Nudges.send(
                    applicationContext, state,
                    str(R.string.n_workout_title),
                    str(R.string.n_workout_text, session.minutes, formatMl(ml), formatMl(state.breakdown?.workoutMl ?: 0)),
                    ml,
                )
                return
            }
        }

        val streak = state.streakInfo
        if (streak.atRisk && streak.days >= 3 && nowMin >= settings.activeEndHour * 60 - 120 && repo.lastNudge("streak") != today.toString()) {
            repo.markNudge("streak", today.toString())
            val ml = state.remaining.coerceAtMost(MAX_NUDGE_ML).coerceAtLeast(settings.reminderGlassMl)
            val shield = if (streak.shields > 0) str(R.string.n_streak_shield) else str(R.string.n_streak_noshield)
            Nudges.send(
                applicationContext, state,
                str(R.string.n_streak_title, applicationContext.daysText(streak.days)),
                str(R.string.n_streak_text, formatMl(state.remaining), shield),
                ml,
            )
            return
        }

        if (settings.learnedReminders) {
            val gap = Habits.gapAhead(state.habits.gaps, nowMin, repo.minutesSinceLastIntake()) ?: return
            val key = "$today:${gap.startHour}"
            if (repo.lastNudge("gap") == key) return
            repo.markNudge("gap", key)
            val ml = settings.reminderGlassMl
            Nudges.send(
                applicationContext, state,
                str(R.string.n_gap_title),
                str(R.string.n_gap_text, "${gap.startHour}:00", "${gap.endHour}:00", formatMl(ml)),
                ml,
            )
        }
    }

    // Uprawnienie sprawdza hasNotificationPermission() przed notify() — lint nie widzi tego przez helper.
    @SuppressLint("MissingPermission")
    private suspend fun maybeNotify(repo: HydrationRepository, state: HydrationState, force: Boolean): Boolean {
        val settings = state.settings
        if (!force) {
            if (!settings.remindersEnabled) return false
            if (state.remaining <= 0) return false
            if (repo.isSnoozed()) return false
            // Spotkanie albo „Nie przeszkadzać": przypomnienie czeka do następnego tiku, nie przepada.
            if (ReminderGate.currentHold(applicationContext, settings.respectDnd, settings.quietDuringMeetings) != null) return false
        }

        val now = LocalTime.now()
        val nowMinutes = now.hour * 60 + now.minute
        val startMinutes = settings.activeStartHour * 60
        val endMinutes = settings.activeEndHour * 60
        if (!force && (nowMinutes < startMinutes || nowMinutes >= endMinutes)) return false

        val numGlasses = ceil(state.goal.toDouble() / settings.reminderGlassMl).toInt().coerceAtLeast(1)
        val activeWindowMinutes = (endMinutes - startMinutes).coerceAtLeast(60)
        val intervalMinutes = (activeWindowMinutes / numGlasses).coerceIn(20, 180)

        val sinceLast = repo.minutesSinceLastIntake() ?: (nowMinutes - startMinutes).toLong().coerceAtLeast(0)
        if (!force && sinceLast < intervalMinutes) return false

        if (!hasNotificationPermission()) return false

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
        return true
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
        val intensity = state.settings.snarkIntensity
        val seed = state.intakes.size * 7 + LocalTime.now().hour + state.total / 100

        val title = if (snarky) {
            when {
                plan.status == PlanStatus.DONE -> pick(SNARK_TITLES.getValue(Level.DONE), seed)
                plan.status == PlanStatus.AFTER_HOURS -> pick(SNARK_TITLES_AFTER_HOURS, seed)
                intensity == SnarkIntensity.SAVAGE && gap > 0 -> pick(SNARK_TITLES_SAVAGE, seed)
                gap >= state.settings.reminderGlassMl * 2 -> pick(SNARK_TITLES_BEHIND, seed)
                else -> pick(SNARK_TITLES.getValue(state.level), seed)
            }
        } else {
            when {
                plan.status == PlanStatus.DONE -> str(R.string.n_rem_title_done)
                plan.status == PlanStatus.AFTER_HOURS -> str(R.string.n_rem_title_after)
                state.progress >= 0.99 -> str(R.string.n_rem_title_last)
                gap >= state.settings.reminderGlassMl * 2 -> str(R.string.n_rem_title_worried)
                daypart == Daypart.MORNING -> str(R.string.n_rem_title_morning)
                daypart == Daypart.EVENING -> str(R.string.n_rem_title_evening)
                else -> str(R.string.n_rem_title_default)
            }
        }

        val next = plan.next
        val short = when {
            plan.status == PlanStatus.DONE ->
                if (snarky) {
                    str(R.string.n_short_done_snarky, formatMl(state.total), formatMl(state.goal))
                } else {
                    str(R.string.n_short_done, formatMl(state.total), formatMl(state.goal))
                }
            next == null ->
                str(R.string.n_short_closed_nonext, formatMl(plan.remainingMl))
            plan.status == PlanStatus.AFTER_HOURS ->
                str(R.string.n_short_after, formatMl(plan.remainingMl), formatMl(next.ml), next.time.hhmm())
            plan.sips.size == 1 ->
                str(R.string.n_short_last, formatMl(plan.portionMl), next.time.hhmm(), formatMl(state.goal))
            plan.shortfallMl > 0 ->
                str(R.string.n_short_capped, formatMl(plan.portionMl), next.time.hhmm(), plan.sips.size - 1, plan.windowEnd.hhmm(), formatMl(plan.shortfallMl))
            else ->
                str(R.string.n_short_more, formatMl(plan.portionMl), next.time.hhmm(), plan.sips.size - 1, plan.windowEnd.hhmm(), formatMl(state.goal))
        }

        val hoursSince = sinceLastMinutes / 60
        val minsSince = sinceLastMinutes % 60
        val sinceText = if (hoursSince > 0) str(R.string.n_since_hm, hoursSince, minsSince) else str(R.string.n_since_m, minsSince)

        val full = buildString {
            appendLine(str(R.string.n_full_drunk, formatMl(state.total), formatMl(state.goal), pct))
            if (gap > 0) {
                appendLine(str(R.string.n_full_behind, formatMl(state.targetSoFar), formatMl(gap)))
            } else {
                appendLine(str(R.string.n_full_ontrack, formatMl(state.targetSoFar)))
            }
            appendLine(str(R.string.n_full_last_sip, sinceText))
            appendLine()

            when (plan.status) {
                PlanStatus.DONE -> appendLine(str(R.string.n_full_done))
                PlanStatus.AFTER_HOURS -> {
                    appendLine(str(R.string.n_full_after_closed, state.settings.activeStartHour, plan.windowEnd.hhmm(), formatMl(plan.remainingMl)))
                    next?.let {
                        appendLine(str(R.string.n_full_after_try, formatMl(it.ml), it.time.hhmm()))
                    }
                }
                PlanStatus.TIGHT, PlanStatus.ON_TRACK -> {
                    appendLine(str(R.string.n_full_plan, plan.windowEnd.hhmm(), plan.sips.size, formatMl(plan.portionMl)))
                    appendLine(plan.timesText())
                    // Gdy nie wszystko się zmieści, ostatnia porcja nie „domyka celu" — nie obiecujemy tego.
                    if (plan.shortfallMl == 0) plan.finishAt?.let {
                        appendLine(str(R.string.n_full_last_portion, it.hhmm(), formatMl(state.goal)))
                    }
                    if (plan.status == PlanStatus.TIGHT && plan.shortfallMl > 0) {
                        appendLine(str(R.string.n_full_capped, formatMl(plan.shortfallMl)))
                    } else if (plan.status == PlanStatus.TIGHT) {
                        appendLine(str(R.string.n_full_tight, formatMl(state.settings.reminderGlassMl)))
                    }
                }
            }

            appendLine()
            if (snarky) {
                snarkGapJab(sinceLastMinutes, seed)?.let { appendLine(it) }
                append(snarkJab(state.level, intensity, seed))
                append(" ")
                append(pick(SNARK_CLOSERS, seed * 3))
            } else {
                append(state.selfCare)
            }
        }.trim()

        return ReminderMessage(title, short, full)
    }

    /** Tekst z zasobów w języku aplikacji; argumenty podstawiane w kolejności %1$s, %2$s… */
    private fun str(@androidx.annotation.StringRes id: Int, vararg args: Any): String = applicationContext.getString(id, *args)

    private fun hasNotificationPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
