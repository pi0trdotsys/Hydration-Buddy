package com.kropi.hydration.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kropi.hydration.backup.BackupFormat
import com.kropi.hydration.backup.CsvHistory
import com.kropi.hydration.export.ExportToday
import com.kropi.hydration.export.HydrationExport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.LocalTime
import kotlin.math.roundToInt

val Context.hydrationStore by preferencesDataStore(name = "hydration")

/**
 * Jeden łyk w postaci, z jaką pracuje UI. [ml] to ile wlano do szklanki, a
 * [counted] — ile z tego zalicza się do celu (zależy od napoju i od tego, czy
 * włączone jest liczenie wg współczynnika nawodnienia).
 */
data class Intake(
    val id: Long,
    val epochDay: Long,
    val hour: Int,
    val minute: Int,
    val ml: Int,
    val drink: Drink,
    val note: String?,
    val counted: Int,
) {
    val minuteOfDay: Int get() = hour * 60 + minute
}

fun IntakeEntity.toIntake(net: Boolean): Intake {
    val kind = Drink.of(drink)
    return Intake(
        id = id,
        epochDay = epochDay,
        hour = (minuteOfDay / 60).coerceIn(0, 23),
        minute = minuteOfDay % 60,
        ml = ml,
        drink = kind,
        note = note,
        counted = kind.counted(ml, net),
    )
}

/** Zamknięte dni z historii: suma „liczonych" ml z łyków i cel obowiązujący wtedy. */
object DayRecords {
    fun build(
        intakes: List<IntakeEntity>,
        goals: List<DayGoalEntity>,
        currentDay: Long,
        fallbackGoal: Int,
        net: Boolean,
    ): List<DayRecord> {
        val byDay = intakes.groupBy { it.epochDay }
        val goalByDay = goals.associate { it.epochDay to it.goal }
        return (byDay.keys + goalByDay.keys).filter { it < currentDay }.sorted().map { day ->
            val ml = byDay[day].orEmpty().sumOf { Drink.of(it.drink).counted(it.ml, net) }
            DayRecord(day, ml, goalByDay[day] ?: fallbackGoal)
        }
    }
}

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
    val hourlyProfile: List<Int>,
    val pokeSeed: Int,
    val settings: HydrationSettings,
    /** Epoch ms ostatniej zmiany dzisiejszej sumy/celu (0 = nieznane, sprzed eksportu). */
    val lastChangeEpochMs: Long = 0L,
    /** Ostatnio cofnięty łyk (widget pozwala go na chwilę przywrócić). */
    val undo: UndoInfo? = null,
    /** Ostatnio użyta ilość dla każdego napoju — „typowa porcja". */
    val drinkMemory: Map<Drink, Int> = emptyMap(),
    /** Napój wybrany na widgecie — następne dolanie z widgetu liczy się jako on, potem wraca woda. */
    val widgetDrink: Drink = Drink.WATER,
    /** Wszystkie łyki z bazy (do ~400 dni wstecz) — kalendarz miesiąca i statystyki napojów. */
    val allIntakes: List<Intake> = emptyList(),
) {
    /** Łyki z ostatnich ~30 dni (razem z dzisiejszymi) — do statystyk napojów. */
    val recentIntakes: List<Intake>
        get() = LocalDate.now().toEpochDay().let { today -> allIntakes.filter { it.epochDay >= today - 30 } }

    /** Typowa porcja napoju: ostatnio użyta ilość albo rozsądna wartość domyślna. */
    fun typicalMl(drink: Drink): Int = drinkMemory[drink] ?: drink.defaultMl

    /** Łyki jednego dnia, od najwcześniejszego. */
    fun intakesOn(epochDay: Long): List<Intake> =
        allIntakes.filter { it.epochDay == epochDay }.sortedWith(compareBy({ it.minuteOfDay }, { it.id }))

    /** Rekord dnia: dzisiejszy liczony w locie, wcześniejsze z historii, null = brak danych. */
    fun recordOn(epochDay: Long): DayRecord? =
        if (epochDay == LocalDate.now().toEpochDay()) today else records.firstOrNull { it.epochDay == epochDay }

    /** Najstarszy dzień, o którym Kropi coś wie (do ograniczenia nawigacji po miesiącach). */
    val firstKnownDay: Long get() = records.minOfOrNull { it.epochDay } ?: LocalDate.now().toEpochDay()

    val total: Int get() = intakes.sumOf { it.counted }
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
    /** Maskotka mówi w tym samym tonie, co powiadomienia. */
    val mascotLine: String
        get() = if (settings.notificationTone == NotificationTone.SNARKY) {
            pick(SNARK_MASCOT_LINES.getValue(level), seed * 5 + pokeSeed)
        } else {
            pick(MASCOT_LINES.getValue(level), seed * 5 + pokeSeed)
        }

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
    // Dawne miejsce łyków i historii (do 1.7) — czytane już tylko przy migracji do bazy.
    val INTAKES = stringPreferencesKey("intakes") // "h:m:ml,h:m:ml,..."
    val HISTORY = stringPreferencesKey("history") // "epochDay:ml:goal,..."
    val LAST_EPOCH_DAY = longPreferencesKey("last_epoch_day")
    val POKE_SEED = intPreferencesKey("poke_seed")
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
    val BOTTLES_ML = stringPreferencesKey("bottles_ml")
    val SNARK_INTENSITY = stringPreferencesKey("snark_intensity")
    val LAST_SUMMARY_EPOCH_DAY = longPreferencesKey("last_summary_epoch_day")
    val ADAPTIVE_PLAN = booleanPreferencesKey("adaptive_plan")
    val NET_HYDRATION = booleanPreferencesKey("net_hydration")
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    val WIDGET_DRINK = stringPreferencesKey("widget_drink")
    val UNDO_ML = intPreferencesKey("undo_ml")
    val UNDO_DRINK = stringPreferencesKey("undo_drink")
    val UNDO_DAY = longPreferencesKey("undo_day")
    val UNDO_MINUTE = intPreferencesKey("undo_minute")
    val UNDO_NOTE = stringPreferencesKey("undo_note")
    val UNDO_AT = longPreferencesKey("undo_at")
    val HOURLY_PROFILE = stringPreferencesKey("hourly_profile") // 24 liczby: ile zwykle pijesz o tej godzinie
    val LAST_CHANGE_EPOCH_MS = longPreferencesKey("last_change_epoch_ms") // dla eksportu do Szpili
}

private const val LAST_ML_PREFIX = "last_ml_"
private const val WIDGET_CONFIG_PREFIX = "widget_cfg_"

private fun lastMlKey(drink: Drink) = intPreferencesKey(LAST_ML_PREFIX + drink.name)
private fun widgetConfigKey(appWidgetId: Int) = stringPreferencesKey(WIDGET_CONFIG_PREFIX + appWidgetId)

private fun readDrinkMemory(prefs: Preferences): Map<Drink, Int> =
    Drink.entries.mapNotNull { d -> prefs[lastMlKey(d)]?.let { d to it } }.toMap()

private fun readUndo(prefs: Preferences): UndoInfo? {
    val ml = prefs[Keys.UNDO_ML] ?: return null
    val at = prefs[Keys.UNDO_AT] ?: return null
    return UndoInfo(ml, Drink.of(prefs[Keys.UNDO_DRINK]), at)
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
        adaptivePlan = prefs[Keys.ADAPTIVE_PLAN] ?: defaults.adaptivePlan,
        netHydration = prefs[Keys.NET_HYDRATION] ?: defaults.netHydration,
        snarkIntensity = prefs[Keys.SNARK_INTENSITY]?.let { runCatching { SnarkIntensity.valueOf(it) }.getOrNull() }
            ?: defaults.snarkIntensity,
        bottlesMl = prefs[Keys.BOTTLES_ML]?.split(",")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 4 }
            ?: defaults.bottlesMl,
    )
}

private const val DEFAULT_GOAL = 2500

/** Ile zamkniętych dni trzymamy — rok z zapasem, a to i tak kilka kB tekstu. */
private const val HISTORY_LIMIT_DAYS = 400

/**
 * Jak mocno stary profil godzinowy ustępuje nowym dniom. 0.85 daje pamięć
 * rzędu dwóch tygodni: zmiana rytmu dnia przebija się w kilka dni, ale jeden
 * nietypowy dzień nie wywraca planu.
 */
private const val PROFILE_DECAY = 0.85f

private fun decodeProfile(raw: String?): List<Int> {
    val parsed = raw?.split(",")?.mapNotNull { it.toIntOrNull() }
    return if (parsed != null && parsed.size == 24) parsed else List(24) { 0 }
}


/**
 * Stan aplikacji: ustawienia i drobne liczniki w DataStore, a łyki oraz
 * historia dni w bazie Room (od 1.8 — wcześniej wszystko było tekstem w
 * DataStore, stąd [migrateLegacyIfNeeded]).
 *
 * Dzień „zamyka się" przy pierwszym odczycie po północy ([current]): jego cel
 * ląduje w `day_goal`, a łyki zostają w bazie, więc da się je potem oglądać i
 * poprawiać wstecz.
 */
class HydrationRepository(private val context: Context) {

    private val dao: IntakeDao get() = KropiDatabase.get(context).dao()

    val state: Flow<HydrationState> = combine(
        context.hydrationStore.data,
        dao.observeIntakes(),
        dao.observeDayGoals(),
    ) { prefs, intakes, goals -> assemble(prefs, intakes, goals) }
        .onStart { migrateLegacyIfNeeded() }

    private fun assemble(prefs: Preferences, intakes: List<IntakeEntity>, goals: List<DayGoalEntity>): HydrationState {
        val settings = readSettings(prefs)
        val goal = prefs[Keys.GOAL] ?: DEFAULT_GOAL
        val currentDay = prefs[Keys.LAST_EPOCH_DAY] ?: LocalDate.now().toEpochDay()
        val net = settings.netHydration
        return HydrationState(
            goal = goal,
            intakes = intakes.filter { it.epochDay == currentDay }.map { it.toIntake(net) },
            records = DayRecords.build(intakes, goals, currentDay, goal, net),
            hourlyProfile = decodeProfile(prefs[Keys.HOURLY_PROFILE]),
            pokeSeed = prefs[Keys.POKE_SEED] ?: 0,
            settings = settings,
            lastChangeEpochMs = prefs[Keys.LAST_CHANGE_EPOCH_MS] ?: 0L,
            widgetDrink = Drink.of(prefs[Keys.WIDGET_DRINK]),
            undo = readUndo(prefs),
            drinkMemory = readDrinkMemory(prefs),
            allIntakes = intakes.map { it.toIntake(net) },
        )
    }

    companion object {
        private val migrationMutex = Mutex()

        /** Dzisiejszy stan w kształcie eksportu (provider dla Szpili). */
        fun exportToday(state: HydrationState): ExportToday = ExportToday(
            epochDay = state.today.epochDay,
            ml = state.total,
            goal = state.goal,
            intakes = state.intakes.size,
            updatedEpochMs = state.lastChangeEpochMs,
        )
    }

    /**
     * Przenosi łyki i historię z dawnego tekstu w DataStore do bazy. Idempotentne:
     * jeśli baza ma już dane (np. przerwana migracja), tylko sprząta stare klucze.
     */
    suspend fun migrateLegacyIfNeeded() = migrationMutex.withLock {
        val prefs = context.hydrationStore.data.first()
        val rawIntakes = prefs[Keys.INTAKES]
        val rawHistory = prefs[Keys.HISTORY]
        if (rawIntakes == null && rawHistory == null) return@withLock
        if (dao.intakeCount() == 0 && dao.dayGoalCount() == 0) {
            val parsed = LegacyMigration.parse(rawIntakes, rawHistory, prefs[Keys.LAST_EPOCH_DAY])
            dao.replaceAll(parsed.intakes, parsed.dayGoals)
        }
        context.hydrationStore.edit {
            it.remove(Keys.INTAKES)
            it.remove(Keys.HISTORY)
        }
    }

    /** To, co widzi eksport — porównujemy przed i po zmianie, żeby wiedzieć, czy ogłosić ją Szpili. */
    private data class ExportSnapshot(val epochDay: Long?, val ml: Int, val goal: Int, val intakes: Int, val allCount: Int, val allMl: Long)

    private suspend fun exportSnapshot(): ExportSnapshot {
        val prefs = context.hydrationStore.data.first()
        val day = prefs[Keys.LAST_EPOCH_DAY]
        val net = readSettings(prefs).netHydration
        val today = if (day != null) dao.intakesOfDay(day) else emptyList()
        return ExportSnapshot(
            epochDay = day,
            ml = today.sumOf { Drink.of(it.drink).counted(it.ml, net) },
            goal = prefs[Keys.GOAL] ?: DEFAULT_GOAL,
            intakes = today.size,
            allCount = dao.intakeCount(),
            allMl = dao.totalMl(),
        )
    }

    /**
     * Każda zmiana danych dnia (dolanie, edycja, cofnięcie, nowy cel, nowy dzień)
     * idzie tędy: jeśli zmieniła dzisiejszą sumę, cel, liczbę łyków albo cokolwiek
     * w historii, zapisujemy znacznik czasu i ogłaszamy to Szpili. Dzięki temu
     * widget, kafelek, szybkie dolewanie, powiadomienia i UI są objęte automatycznie.
     */
    private suspend fun <T> mutate(block: suspend () -> T): T {
        val before = exportSnapshot()
        val result = block()
        val after = exportSnapshot()
        if (after != before) {
            val now = System.currentTimeMillis()
            context.hydrationStore.edit { it[Keys.LAST_CHANGE_EPOCH_MS] = now }
            HydrationExport.notifyChanged(
                context,
                ExportToday(
                    epochDay = after.epochDay ?: LocalDate.now().toEpochDay(),
                    ml = after.ml,
                    goal = after.goal,
                    intakes = after.intakes,
                    updatedEpochMs = now,
                ),
            )
        }
        return result
    }

    suspend fun current(): HydrationState = rollDayIfNeeded()

    /**
     * Zamyka poprzedni dzień: zapisuje jego cel (łyki już leżą w bazie), dolicza go
     * do profilu godzinowego i pilnuje, żeby dni bez używania aplikacji też trafiły
     * do historii — pustą, inaczej luka udawałaby, że cel został zaliczony.
     */
    private suspend fun rollDayIfNeeded(): HydrationState {
        migrateLegacyIfNeeded()
        val today = LocalDate.now().toEpochDay()
        mutate {
            val prefs = context.hydrationStore.data.first()
            val lastDay = prefs[Keys.LAST_EPOCH_DAY]
            if (lastDay == null) {
                context.hydrationStore.edit {
                    it[Keys.LAST_EPOCH_DAY] = today
                    it[Keys.GOAL] = it[Keys.GOAL] ?: readSettings(it).effectiveGoalMl
                }
                return@mutate
            }
            if (lastDay >= today) return@mutate

            val goal = prefs[Keys.GOAL] ?: DEFAULT_GOAL
            val net = readSettings(prefs).netHydration

            // Profil „o której zwykle pijesz" — z niego plan dnia dobiera godziny.
            val profile = decodeProfile(prefs[Keys.HOURLY_PROFILE])
                .map { (it * PROFILE_DECAY).toInt() }
                .toIntArray()
            dao.intakesOfDay(lastDay).forEach {
                profile[(it.minuteOfDay / 60).coerceIn(0, 23)] += Drink.of(it.drink).counted(it.ml, net)
            }

            dao.insertDayGoals(listOf(DayGoalEntity(lastDay, goal)))
            for (day in (lastDay + 1) until today) dao.insertDayGoalIfAbsent(DayGoalEntity(day, goal))
            dao.pruneIntakes(today - HISTORY_LIMIT_DAYS)
            dao.pruneDayGoals(today - HISTORY_LIMIT_DAYS)

            context.hydrationStore.edit {
                it[Keys.HOURLY_PROFILE] = profile.joinToString(",")
                it[Keys.LAST_EPOCH_DAY] = today
            }
        }
        return state.first()
    }

    private fun nowMinuteOfDay(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

    /** Dolewka „teraz" — to, co robią widget, kafelek, skróty i powiadomienia. */
    suspend fun addWater(ml: Int) {
        addIntake(ml)
    }

    /**
     * Dodaje wpis. Domyślnie „teraz" i dziś; [epochDay] i [minuteOfDay] pozwalają dopisać
     * łyk wstecz. Zwraca id nowego wpisu.
     */
    suspend fun addIntake(
        ml: Int,
        drink: Drink = Drink.WATER,
        note: String? = null,
        epochDay: Long? = null,
        minuteOfDay: Int? = null,
    ): Long {
        rollDayIfNeeded()
        rememberAmount(drink, ml)
        return mutate {
            val prefs = context.hydrationStore.data.first()
            val currentDay = prefs[Keys.LAST_EPOCH_DAY] ?: LocalDate.now().toEpochDay()
            val day = epochDay ?: currentDay
            if (day < currentDay) {
                // Dzień z przeszłości bez zapisanego celu dostaje obecny, żeby trafił do historii.
                dao.insertDayGoalIfAbsent(DayGoalEntity(day, prefs[Keys.GOAL] ?: DEFAULT_GOAL))
            }
            dao.insert(
                IntakeEntity(
                    epochDay = day,
                    minuteOfDay = (minuteOfDay ?: nowMinuteOfDay()).coerceIn(0, 24 * 60 - 1),
                    ml = ml.coerceAtLeast(1),
                    drink = drink.name,
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
        }
    }

    /** Zmienia istniejący wpis (ilość, napój, notatkę, godzinę, dzień). */
    suspend fun updateIntake(
        id: Long,
        ml: Int,
        drink: Drink,
        note: String?,
        epochDay: Long,
        minuteOfDay: Int,
    ) {
        rememberAmount(drink, ml)
        mutate {
            val existing = dao.intakeById(id) ?: return@mutate
            val currentDay = context.hydrationStore.data.first()[Keys.LAST_EPOCH_DAY] ?: LocalDate.now().toEpochDay()
            if (epochDay < currentDay) {
                dao.insertDayGoalIfAbsent(DayGoalEntity(epochDay, context.hydrationStore.data.first()[Keys.GOAL] ?: DEFAULT_GOAL))
            }
            dao.update(
                existing.copy(
                    epochDay = epochDay,
                    minuteOfDay = minuteOfDay.coerceIn(0, 24 * 60 - 1),
                    ml = ml.coerceAtLeast(1),
                    drink = drink.name,
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
        }
    }

    /** Zapamiętuje ilość jako typową porcję tego napoju (podpowiedź w edytorze i na widgecie). */
    private suspend fun rememberAmount(drink: Drink, ml: Int) {
        context.hydrationStore.edit { it[lastMlKey(drink)] = ml.coerceAtLeast(1) }
    }

    suspend fun deleteIntake(id: Long) {
        mutate { dao.delete(id) }
    }

    /** Cofa ostatnio dodany łyk dzisiejszego dnia (strzałka na widgecie). */
    suspend fun undoLast() {
        mutate {
            val day = context.hydrationStore.data.first()[Keys.LAST_EPOCH_DAY] ?: LocalDate.now().toEpochDay()
            dao.newestIdOfDay(day)?.let { dao.delete(it) }
        }
    }

    /**
     * Cofnięcie z widgetu: usuwa ostatni łyk dnia, ale zostawia go w pamięci, żeby
     * widget mógł pokazać „Cofnięto … · przywróć". Zwraca, czy coś usunięto.
     */
    suspend fun undoLastKeeping(): Boolean {
        rollDayIfNeeded()
        var removed = false
        mutate {
            val day = context.hydrationStore.data.first()[Keys.LAST_EPOCH_DAY] ?: LocalDate.now().toEpochDay()
            val id = dao.newestIdOfDay(day) ?: return@mutate
            val entry = dao.intakeById(id) ?: return@mutate
            dao.delete(id)
            context.hydrationStore.edit {
                it[Keys.UNDO_ML] = entry.ml
                it[Keys.UNDO_DRINK] = entry.drink
                it[Keys.UNDO_DAY] = entry.epochDay
                it[Keys.UNDO_MINUTE] = entry.minuteOfDay
                if (entry.note != null) it[Keys.UNDO_NOTE] = entry.note else it.remove(Keys.UNDO_NOTE)
                it[Keys.UNDO_AT] = System.currentTimeMillis()
            }
            removed = true
        }
        return removed
    }

    /** Odtwarza łyk cofnięty z widgetu, o ile nie minęło za dużo czasu. */
    suspend fun restoreUndone() {
        val prefs = context.hydrationStore.data.first()
        val ml = prefs[Keys.UNDO_ML] ?: return
        val at = prefs[Keys.UNDO_AT] ?: return
        val fresh = System.currentTimeMillis() - at <= 60_000L
        if (fresh) {
            mutate {
                dao.insert(
                    IntakeEntity(
                        epochDay = prefs[Keys.UNDO_DAY] ?: LocalDate.now().toEpochDay(),
                        minuteOfDay = prefs[Keys.UNDO_MINUTE] ?: nowMinuteOfDay(),
                        ml = ml,
                        drink = prefs[Keys.UNDO_DRINK] ?: Drink.WATER.name,
                        note = prefs[Keys.UNDO_NOTE],
                    ),
                )
            }
        }
        clearUndo()
    }

    /** Zapomina cofnięty łyk; [olderThanMs] > 0 oznacza „tylko jeśli już się przeterminował". */
    suspend fun clearUndo(olderThanMs: Long = 0L) {
        context.hydrationStore.edit {
            val at = it[Keys.UNDO_AT] ?: return@edit
            if (olderThanMs > 0 && System.currentTimeMillis() - at < olderThanMs) return@edit
            it.remove(Keys.UNDO_ML)
            it.remove(Keys.UNDO_DRINK)
            it.remove(Keys.UNDO_DAY)
            it.remove(Keys.UNDO_MINUTE)
            it.remove(Keys.UNDO_NOTE)
            it.remove(Keys.UNDO_AT)
        }
    }

    /** Konfiguracja jednego widgetu; bez zapisu — domyślne butelki z Ustawień. */
    fun widgetConfig(appWidgetId: Int): Flow<WidgetConfig> =
        context.hydrationStore.data.map { prefs ->
            WidgetConfig.decode(prefs[widgetConfigKey(appWidgetId)], readSettings(prefs).bottlesMl)
        }

    suspend fun widgetConfigOnce(appWidgetId: Int): WidgetConfig = widgetConfig(appWidgetId).first()

    suspend fun saveWidgetConfig(appWidgetId: Int, config: WidgetConfig) {
        context.hydrationStore.edit { it[widgetConfigKey(appWidgetId)] = config.encode() }
    }

    suspend fun removeWidgetConfigs(appWidgetIds: IntArray) {
        context.hydrationStore.edit { prefs -> appWidgetIds.forEach { prefs.remove(widgetConfigKey(it)) } }
    }

    /** Minuty od ostatniego dzisiejszego łyka (wg godziny wpisu) albo null, gdy nic dziś nie wypito. */
    suspend fun minutesSinceLastIntake(): Long? {
        val day = context.hydrationStore.data.first()[Keys.LAST_EPOCH_DAY] ?: return null
        val last = dao.lastMinuteOfDay(day) ?: return null
        return (nowMinuteOfDay() - last).coerceAtLeast(0).toLong()
    }

    private fun nowEpochMinute(): Long =
        java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() / 60

    /** Wieczorne podsumowanie wysyłamy raz na dobę — tu pilnujemy, że już poszło. */
    suspend fun summarySentDay(): Long? =
        context.hydrationStore.data.first()[Keys.LAST_SUMMARY_EPOCH_DAY]

    suspend fun markSummarySent(epochDay: Long) {
        context.hydrationStore.edit { prefs -> prefs[Keys.LAST_SUMMARY_EPOCH_DAY] = epochDay }
    }

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
        mutate {
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
                prefs[Keys.BOTTLES_ML] = settings.bottlesMl.joinToString(",")
                prefs[Keys.SNARK_INTENSITY] = settings.snarkIntensity.name
                prefs[Keys.ADAPTIVE_PLAN] = settings.adaptivePlan
                prefs[Keys.NET_HYDRATION] = settings.netHydration
                prefs[Keys.GOAL] = settings.effectiveGoalMl
            }
        }
    }

    /** Napój wybrany na widgecie (woda, jeśli nic nie wybrano). */
    suspend fun widgetDrink(): Drink = Drink.of(context.hydrationStore.data.first()[Keys.WIDGET_DRINK])

    /** Chwilowy wybór napoju na widgecie — to nie zmiana danych, więc bez ogłaszania Szpili. */
    suspend fun setWidgetDrink(drink: Drink) {
        context.hydrationStore.edit { it[Keys.WIDGET_DRINK] = drink.name }
    }

    suspend fun poke() {
        context.hydrationStore.edit { prefs ->
            prefs[Keys.POKE_SEED] = (prefs[Keys.POKE_SEED] ?: 0) + 1
        }
    }

    suspend fun setGoal(ml: Int) {
        mutate { context.hydrationStore.edit { prefs -> prefs[Keys.GOAL] = ml } }
    }

    /**
     * Pełna kopia danych: DataStore (poza chwilowym odroczeniem przypomnienia) oraz
     * wszystkie łyki i cele dni z bazy — to trafia do pliku kopii zapasowej.
     */
    suspend fun snapshot(): BackupFormat.Snapshot {
        migrateLegacyIfNeeded()
        val prefs = context.hydrationStore.data.first()
        val entries = prefs.asMap().mapNotNull { (key, value) ->
            if (key.name == Keys.SNOOZE_UNTIL_EPOCH_MINUTE.name) null else BackupFormat.entryOf(key.name, value)
        }
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
        return BackupFormat.Snapshot(
            createdAtMs = System.currentTimeMillis(),
            appVersion = version,
            entries = entries,
            intakes = dao.allIntakes().map { BackupFormat.IntakeRow(it.epochDay, it.minuteOfDay, it.ml, it.drink, it.note) },
            dayGoals = dao.allDayGoals().map { BackupFormat.DayGoalRow(it.epochDay, it.goal) },
        )
    }

    /**
     * Zastępuje całe dane tym, co w kopii. Kopia z wersji 1.x niesie łyki jako
     * tekst w ustawieniach — wtedy [migrateLegacyIfNeeded] przenosi je do bazy.
     * Jeśli kopia jest sprzed paru dni, `current()` zamknie brakujące dni.
     */
    suspend fun restore(snapshot: BackupFormat.Snapshot) {
        mutate {
            dao.replaceAll(
                snapshot.intakes.map { IntakeEntity(0, it.epochDay, it.minuteOfDay, it.ml, it.drink, it.note) },
                snapshot.dayGoals.map { DayGoalEntity(it.epochDay, it.goal) },
            )
            context.hydrationStore.edit { prefs ->
                prefs.clear()
                snapshot.entries.forEach { e ->
                    when (e.type) {
                        BackupFormat.Type.INT -> prefs[intPreferencesKey(e.name)] = e.value as Int
                        BackupFormat.Type.LONG -> prefs[longPreferencesKey(e.name)] = e.value as Long
                        BackupFormat.Type.FLOAT -> prefs[floatPreferencesKey(e.name)] = e.value as Float
                        BackupFormat.Type.STRING -> prefs[stringPreferencesKey(e.name)] = e.value as String
                        BackupFormat.Type.BOOL -> prefs[booleanPreferencesKey(e.name)] = e.value as Boolean
                    }
                }
            }
        }
        // Kopia z wcześniejszej wersji nie zna powitania, a jej właściciel nie potrzebuje go drugi raz.
        context.hydrationStore.edit { it[Keys.ONBOARDING_DONE] = true }
        migrateLegacyIfNeeded()
        rollDayIfNeeded()
    }

    /**
     * Czy pokazać powitanie: tylko przy świeżej instalacji bez żadnych danych. Aktualizacja
     * starszej wersji, istniejące wpisy albo zapisane ustawienia oznaczają, że to nie pierwszy raz.
     */
    suspend fun needsOnboarding(): Boolean {
        val prefs = context.hydrationStore.data.first()
        prefs[Keys.ONBOARDING_DONE]?.let { return !it }
        val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        val updated = info != null && info.lastUpdateTime > info.firstInstallTime
        val hasData = dao.intakeCount() > 0 || prefs[Keys.WEIGHT_KG] != null ||
            prefs[Keys.INTAKES] != null || prefs[Keys.HISTORY] != null
        val needs = !updated && !hasData
        if (!needs) context.hydrationStore.edit { it[Keys.ONBOARDING_DONE] = true }
        return needs
    }

    /** Kończy powitanie; [settings] = null oznacza „pomiń" i zostawia domyślne ustawienia. */
    suspend fun completeOnboarding(settings: HydrationSettings?) {
        if (settings != null) saveSettings(settings)
        context.hydrationStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    /**
     * Dopisuje do historii dni z CSV, których jeszcze w niej nie ma (istniejące
     * zostają, dzisiejszy i przyszłe pomijamy). Zwraca, ile dni dodano.
     */
    suspend fun importHistory(rows: List<CsvHistory.Row>): Int {
        rollDayIfNeeded()
        val today = context.hydrationStore.data.first()[Keys.LAST_EPOCH_DAY] ?: LocalDate.now().toEpochDay()
        val known = dao.allDayGoals().map { it.epochDay }.toSet() + dao.allIntakes().map { it.epochDay }.toSet()
        val fresh = rows.filter { it.epochDay < today && it.epochDay !in known }.distinctBy { it.epochDay }
        if (fresh.isEmpty()) return 0
        mutate {
            dao.insertDayGoals(fresh.map { DayGoalEntity(it.epochDay, it.goal) })
            dao.insertAll(
                fresh.filter { it.ml > 0 }.map {
                    IntakeEntity(
                        epochDay = it.epochDay,
                        minuteOfDay = LegacyMigration.SUMMARY_MINUTE,
                        ml = it.ml,
                        note = "z importu CSV",
                    )
                },
            )
        }
        return fresh.size
    }
}
