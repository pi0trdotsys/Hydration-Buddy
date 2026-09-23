package com.kropi.hydration.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.LocalTime
import kotlin.math.roundToInt

val Context.hydrationStore by preferencesDataStore(name = "hydration")

data class Intake(val hour: Int, val minute: Int, val ml: Int)

/** Zamknięty dzień: ile wyszło i jaki cel wtedy obowiązywał. */
data class DayRecord(val epochDay: Long, val ml: Int, val goal: Int) {
    val reached: Boolean get() = goal > 0 && ml >= goal
    val pct: Float get() = if (goal > 0) (ml.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
}

data class HydrationState(
    val goal: Int,
    val intakes: List<Intake>,
    val records: List<DayRecord>,
    val pokeSeed: Int,
    val settings: HydrationSettings,
) {
    val total: Int get() = intakes.sumOf { it.ml }
    val progress: Double get() = (total.toDouble() / goal).coerceIn(0.0, 1.0)
    val remaining: Int get() = (goal - total).coerceAtLeast(0)
    val level: Level get() = levelFor(total.toDouble() / goal)
    val daypart: Daypart get() = daypartFor(LocalTime.now().hour)

    /** How much you *should* have drunk by now, given the active drinking window. */
    val targetSoFar: Int get() = targetSoFarAt(LocalTime.now())

    fun targetSoFarAt(time: LocalTime): Int {
        val startMin = settings.activeStartHour * 60
        val endMin = settings.activeEndHour * 60
        val nowMin = time.hour * 60 + time.minute
        if (endMin <= startMin) return goal
        val fraction = ((nowMin - startMin).toFloat() / (endMin - startMin)).coerceIn(0f, 1f)
        return (goal * fraction).roundToInt()
    }

    val isBehindSchedule: Boolean get() = total < targetSoFar - settings.reminderGlassMl / 2

    private val seed: Int get() = intakes.size + Math.round(total / 100f)
    val selfCare: String get() = pick(SELF_CARE.getValue(level), seed)
    val selfCareAlt: String get() = pick(SELF_CARE.getValue(level), seed + 1)
    val fact: String get() = pick(FACTS, seed * 3 + 1)
    val dayNote: String get() = pick(DAYPART_NOTES.getValue(daypart), seed)
    val mascotLine: String get() = pick(MASCOT_LINES.getValue(level), seed * 5 + pokeSeed)

    /** Dzisiaj jako rekord — dzień jeszcze się nie zamknął, więc liczymy go w locie. */
    val today: DayRecord get() = DayRecord(LocalDate.now().toEpochDay(), total, goal)

    /**
     * Ostatnie [days] dni, od najstarszego do dziś, z zerami w dniach bez
     * żadnego wpisu. Zastąpiło statyczny mock z `use-hydration-mock.ts`.
     */
    fun lastDays(days: Int): List<DayRecord> {
        val todayEpoch = LocalDate.now().toEpochDay()
        val byDay = records.associateBy { it.epochDay }
        return (days - 1 downTo 0).map { back ->
            val epochDay = todayEpoch - back
            when {
                epochDay == todayEpoch -> today
                else -> byDay[epochDay] ?: DayRecord(epochDay, 0, goal)
            }
        }
    }

    val week: List<WeekDay>
        get() = lastDays(7).map { WeekDay(dayInitials(it.date), it.pct) }

    val history: List<HistoryDay>
        get() = lastDays(7).map { record ->
            HistoryDay(
                day = dayInitials(record.date),
                date = record.date.format(DateTimeFormatter.ofPattern("dd.MM")),
                ml = record.ml,
                goal = record.goal,
                pct = record.pct,
                reached = record.reached,
            )
        }

    /**
     * Seria liczona z realnej historii, a nie z osobnego licznika: idziemy
     * wstecz dopóki dni mają zaliczony cel. Dzisiaj wlicza się dopiero po jego
     * osiągnięciu, żeby seria nie migała w trakcie dnia.
     */
    val streak: Int
        get() {
            val byDay = records.associateBy { it.epochDay }
            val todayEpoch = LocalDate.now().toEpochDay()
            var count = 0
            var day = todayEpoch
            if (today.reached) {
                count = 1
                day -= 1
            } else {
                day -= 1
            }
            while (true) {
                val record = byDay[day] ?: break
                if (!record.reached) break
                count++
                day--
            }
            return count
        }
}

private val POLISH = java.util.Locale.forLanguageTag("pl-PL")

/** „Pn", „Wt", … — dwuliterowy skrót dnia, jak w makiecie. */
private fun dayInitials(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, POLISH)
        .replaceFirstChar { it.uppercase(POLISH) }
        .take(2)

private object Keys {
    val GOAL = intPreferencesKey("goal")
    val INTAKES = stringPreferencesKey("intakes") // "h:m:ml,h:m:ml,..."
    val HISTORY = stringPreferencesKey("history") // "epochDay:ml:goal,..."
    val LAST_EPOCH_DAY = longPreferencesKey("last_epoch_day")
    val POKE_SEED = intPreferencesKey("poke_seed")
    val LAST_INTAKE_EPOCH_MINUTE = longPreferencesKey("last_intake_epoch_minute")
    val SNOOZE_UNTIL_EPOCH_MINUTE = longPreferencesKey("snooze_until_epoch_minute")

    val WEIGHT_KG = floatPreferencesKey("weight_kg")
    val TEMPERATURE = stringPreferencesKey("temperature")
    val ACTIVITY = stringPreferencesKey("activity")
    val GOAL_MODE = stringPreferencesKey("goal_mode")
    val MANUAL_GOAL = intPreferencesKey("manual_goal")
    val ACTIVE_START_HOUR = intPreferencesKey("active_start_hour")
    val ACTIVE_END_HOUR = intPreferencesKey("active_end_hour")
    val REMINDER_GLASS_ML = intPreferencesKey("reminder_glass_ml")
    val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
    val NOTIFICATION_TONE = stringPreferencesKey("notification_tone")
}

private fun readSettings(prefs: androidx.datastore.preferences.core.Preferences): HydrationSettings {
    val defaults = HydrationSettings()
    return HydrationSettings(
        weightKg = prefs[Keys.WEIGHT_KG] ?: defaults.weightKg,
        temperature = prefs[Keys.TEMPERATURE]?.let { runCatching { Temperature.valueOf(it) }.getOrNull() }
            ?: defaults.temperature,
        activity = prefs[Keys.ACTIVITY]?.let { runCatching { ActivityLevel.valueOf(it) }.getOrNull() }
            ?: defaults.activity,
        goalMode = prefs[Keys.GOAL_MODE]?.let { runCatching { GoalMode.valueOf(it) }.getOrNull() }
            ?: defaults.goalMode,
        manualGoalMl = prefs[Keys.MANUAL_GOAL] ?: defaults.manualGoalMl,
        activeStartHour = prefs[Keys.ACTIVE_START_HOUR] ?: defaults.activeStartHour,
        activeEndHour = prefs[Keys.ACTIVE_END_HOUR] ?: defaults.activeEndHour,
        reminderGlassMl = prefs[Keys.REMINDER_GLASS_ML] ?: defaults.reminderGlassMl,
        remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: defaults.remindersEnabled,
        notificationTone = prefs[Keys.NOTIFICATION_TONE]?.let { runCatching { NotificationTone.valueOf(it) }.getOrNull() }
            ?: defaults.notificationTone,
    )
}

private const val DEFAULT_GOAL = 2500

/** Ile zamkniętych dni trzymamy — rok z zapasem, a to i tak kilka kB tekstu. */
private const val HISTORY_LIMIT_DAYS = 400

private fun encodeRecords(records: List<DayRecord>): String =
    records.joinToString(",") { "${it.epochDay}:${it.ml}:${it.goal}" }

private fun decodeRecords(raw: String?): List<DayRecord> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(",").mapNotNull { entry ->
        val parts = entry.split(":")
        if (parts.size != 3) return@mapNotNull null
        val epochDay = parts[0].toLongOrNull() ?: return@mapNotNull null
        val ml = parts[1].toIntOrNull() ?: return@mapNotNull null
        val goal = parts[2].toIntOrNull() ?: return@mapNotNull null
        DayRecord(epochDay, ml, goal)
    }.sortedBy { it.epochDay }
}

private fun encodeIntakes(intakes: List<Intake>): String =
    intakes.joinToString(",") { "${it.hour}:${it.minute}:${it.ml}" }

private fun decodeIntakes(raw: String?): List<Intake> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(",").mapNotNull { entry ->
        val parts = entry.split(":")
        if (parts.size != 3) return@mapNotNull null
        val h = parts[0].toIntOrNull() ?: return@mapNotNull null
        val m = parts[1].toIntOrNull() ?: return@mapNotNull null
        val ml = parts[2].toIntOrNull() ?: return@mapNotNull null
        Intake(h, m, ml)
    }
}

/**
 * DataStore-backed replacement for the mockup's useHydrationMock hook.
 * Rolls the day over at midnight (archiving the streak) instead of the
 * mockup's static in-memory state.
 */
class HydrationRepository(private val context: Context) {

    val state: Flow<HydrationState> = context.hydrationStore.data.map { prefs ->
        HydrationState(
            goal = prefs[Keys.GOAL] ?: DEFAULT_GOAL,
            intakes = decodeIntakes(prefs[Keys.INTAKES]),
            records = decodeRecords(prefs[Keys.HISTORY]),
            pokeSeed = prefs[Keys.POKE_SEED] ?: 0,
            settings = readSettings(prefs),
        )
    }

    suspend fun current(): HydrationState = rollDayIfNeeded()

    /**
     * Zamyka poprzedni dzień: dopisuje go do historii (to z niej liczą się
     * potem statystyki i seria) i zeruje dzisiejsze łyki. Dni, w których
     * aplikacja w ogóle nie działała, zapisujemy jako zerowe — inaczej luka
     * udawałaby, że tamtego dnia cel został zaliczony.
     */
    private suspend fun rollDayIfNeeded(): HydrationState {
        val today = LocalDate.now().toEpochDay()
        context.hydrationStore.edit { prefs ->
            val lastDay = prefs[Keys.LAST_EPOCH_DAY]
            if (lastDay == null) {
                prefs[Keys.LAST_EPOCH_DAY] = today
                prefs[Keys.GOAL] = prefs[Keys.GOAL] ?: readSettings(prefs).effectiveGoalMl
                return@edit
            }
            if (lastDay == today) return@edit

            val goal = prefs[Keys.GOAL] ?: DEFAULT_GOAL
            val total = decodeIntakes(prefs[Keys.INTAKES]).sumOf { it.ml }

            val closedDays = buildList {
                add(DayRecord(lastDay, total, goal))
                // Przerwa w używaniu aplikacji: dni pomiędzy też się zamknęły, tyle że pusto.
                for (day in (lastDay + 1) until today) add(DayRecord(day, 0, goal))
            }
            val merged = (decodeRecords(prefs[Keys.HISTORY]).filter { it.epochDay < lastDay } + closedDays)
                .takeLast(HISTORY_LIMIT_DAYS)
            prefs[Keys.HISTORY] = encodeRecords(merged)

            prefs[Keys.LAST_EPOCH_DAY] = today
            prefs[Keys.INTAKES] = encodeIntakes(emptyList())
        }
        return state.first()
    }

    suspend fun addWater(ml: Int) {
        rollDayIfNeeded()
        context.hydrationStore.edit { prefs ->
            val now = LocalTime.now()
            val updated = decodeIntakes(prefs[Keys.INTAKES]) + Intake(now.hour, now.minute, ml)
            prefs[Keys.INTAKES] = encodeIntakes(updated)
            prefs[Keys.LAST_INTAKE_EPOCH_MINUTE] = java.time.LocalDateTime.now()
                .atZone(java.time.ZoneId.systemDefault()).toEpochSecond() / 60
        }
    }

    /** Minutes since the last recorded sip, or null if none today. */
    suspend fun minutesSinceLastIntake(): Long? {
        val prefs = context.hydrationStore.data.first()
        val last = prefs[Keys.LAST_INTAKE_EPOCH_MINUTE] ?: return null
        val nowMinute = java.time.LocalDateTime.now()
            .atZone(java.time.ZoneId.systemDefault()).toEpochSecond() / 60
        return (nowMinute - last).coerceAtLeast(0)
    }

    private fun nowEpochMinute(): Long =
        java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() / 60

    suspend fun snoozeFor(minutes: Long) {
        context.hydrationStore.edit { prefs ->
            prefs[Keys.SNOOZE_UNTIL_EPOCH_MINUTE] = nowEpochMinute() + minutes
        }
    }

    suspend fun isSnoozed(): Boolean {
        val until = context.hydrationStore.data.first()[Keys.SNOOZE_UNTIL_EPOCH_MINUTE] ?: return false
        return nowEpochMinute() < until
    }

    suspend fun saveSettings(settings: HydrationSettings) {
        context.hydrationStore.edit { prefs ->
            prefs[Keys.WEIGHT_KG] = settings.weightKg
            prefs[Keys.TEMPERATURE] = settings.temperature.name
            prefs[Keys.ACTIVITY] = settings.activity.name
            prefs[Keys.GOAL_MODE] = settings.goalMode.name
            prefs[Keys.MANUAL_GOAL] = settings.manualGoalMl
            prefs[Keys.ACTIVE_START_HOUR] = settings.activeStartHour
            prefs[Keys.ACTIVE_END_HOUR] = settings.activeEndHour
            prefs[Keys.REMINDER_GLASS_ML] = settings.reminderGlassMl
            prefs[Keys.REMINDERS_ENABLED] = settings.remindersEnabled
            prefs[Keys.NOTIFICATION_TONE] = settings.notificationTone.name
            prefs[Keys.GOAL] = settings.effectiveGoalMl
        }
    }

    /** Usuwa jeden konkretny wpis — pomyłkowe dolanie nie wymaga cofania całej reszty. */
    suspend fun removeIntakeAt(index: Int) {
        context.hydrationStore.edit { prefs ->
            val current = decodeIntakes(prefs[Keys.INTAKES])
            if (index !in current.indices) return@edit
            prefs[Keys.INTAKES] = encodeIntakes(current.filterIndexed { i, _ -> i != index })
        }
    }

    suspend fun undoLast() {
        context.hydrationStore.edit { prefs ->
            val updated = decodeIntakes(prefs[Keys.INTAKES]).dropLast(1)
            prefs[Keys.INTAKES] = encodeIntakes(updated)
        }
    }

    suspend fun poke() {
        context.hydrationStore.edit { prefs ->
            prefs[Keys.POKE_SEED] = (prefs[Keys.POKE_SEED] ?: 0) + 1
        }
    }

    suspend fun setGoal(ml: Int) {
        context.hydrationStore.edit { prefs -> prefs[Keys.GOAL] = ml }
    }
}
