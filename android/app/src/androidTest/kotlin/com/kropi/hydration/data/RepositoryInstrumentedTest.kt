package com.kropi.hydration.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kropi.hydration.backup.BackupFormat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testy na prawdziwej bazie i DataStore (na urządzeniu): przepływy, które psuły się po cichu —
 * cofanie z przywracaniem, pamięć porcji napojów, konfiguracja widgetu i kopia zapasowa.
 */
@RunWith(AndroidJUnit4::class)
class RepositoryInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var repo: HydrationRepository

    @Before
    fun freshState() = runBlocking {
        repo = HydrationRepository(context)
        // Przywrócenie minimalnej kopii czyści bazę i ustawienia — czysty start dla każdego testu.
        repo.restore(BackupFormat.Snapshot(0, "test", listOf(BackupFormat.Entry("goal", BackupFormat.Type.INT, 2500))))
    }

    @Test
    fun addedCoffeeCountsNetAndIsRemembered() = runBlocking {
        repo.addIntake(250, Drink.COFFEE, "test")
        val state = repo.current()
        assertEquals(200, state.total)
        assertEquals(250, state.typicalMl(Drink.COFFEE))
        assertEquals(Drink.TEA.defaultMl, state.typicalMl(Drink.TEA))
    }

    @Test
    fun undoKeepsTheSipAndRestoreBringsItBack() = runBlocking {
        repo.addWater(250)
        assertEquals(250, repo.current().total)

        assertTrue(repo.undoLastKeeping())
        val afterUndo = repo.current()
        assertEquals(0, afterUndo.total)
        assertEquals(250, afterUndo.undo?.ml)

        repo.restoreUndone()
        val afterRestore = repo.current()
        assertEquals(250, afterRestore.total)
        assertNull(afterRestore.undo)
    }

    @Test
    fun undoOnEmptyDayDoesNothing() = runBlocking {
        assertEquals(false, repo.undoLastKeeping())
        assertNull(repo.current().undo)
    }

    @Test
    fun freshUndoIsNotClearedByExpiryCheckButIsByForce() = runBlocking {
        repo.addWater(100)
        repo.undoLastKeeping()
        repo.clearUndo(olderThanMs = 60_000)
        assertNotNull(repo.current().undo)
        repo.clearUndo()
        assertNull(repo.current().undo)
    }

    @Test
    fun widgetConfigIsStoredPerWidgetWithDefaultsFallback() = runBlocking {
        val custom = WidgetConfig(
            slots = listOf(ButtonSpec(Drink.COFFEE, 200), ButtonSpec(Drink.TEA, 300), ButtonSpec(null, 500), ButtonSpec(Drink.WATER, 750)),
            style = WidgetStyle.MATERIAL_YOU,
        )
        repo.saveWidgetConfig(42, custom)
        assertEquals(custom, repo.widgetConfigOnce(42))
        // Inny widget bez zapisu dostaje domyślne butelki z Ustawień.
        assertEquals(HydrationSettings().bottlesMl, repo.widgetConfigOnce(43).slots.map { it.ml })
        repo.removeWidgetConfigs(intArrayOf(42))
        assertEquals(WidgetStyle.KROPI, repo.widgetConfigOnce(42).style)
    }

    @Test
    fun backupRoundTripKeepsEntriesDrinksAndNotes() = runBlocking {
        repo.addIntake(300, Drink.JUICE, "po biegu")
        repo.addIntake(250, Drink.WATER)
        val snapshot = repo.snapshot()
        assertEquals(2, snapshot.intakes.size)

        repo.restore(BackupFormat.Snapshot(0, "test", listOf(BackupFormat.Entry("goal", BackupFormat.Type.INT, 2500))))
        assertEquals(0, repo.current().intakes.size)

        repo.restore(snapshot)
        val restored = repo.current()
        assertEquals(2, restored.intakes.size)
        assertTrue(restored.intakes.any { it.drink == Drink.JUICE && it.note == "po biegu" && it.ml == 300 })
    }

    @Test
    fun editingAndDeletingEntriesUpdatesTotals() = runBlocking {
        val id = repo.addIntake(200, Drink.WATER)
        repo.updateIntake(id, 500, Drink.WATER, null, repo.current().intakes.first().epochDay, 9 * 60)
        assertEquals(500, repo.current().total)
        repo.deleteIntake(id)
        assertEquals(0, repo.current().total)
    }

    @Test
    fun legacyHistoryAndOnboardingFlagsSurviveRestore() = runBlocking {
        // Kopia z wcześniejszej wersji nie zna powitania — po przywróceniu nie wolno go pokazać drugi raz.
        assertEquals(false, repo.needsOnboarding())
    }

    @Test
    fun oldAutoGoalIsRecalculatedOnceWithTheNewFormula() = runBlocking {
        // Kopia sprzed 1.11: cel 2660 policzony starym wzorem, tryb automatyczny, brak znacznika wersji wzoru.
        repo.restore(
            BackupFormat.Snapshot(
                0, "1.10.0",
                listOf(
                    BackupFormat.Entry("goal", BackupFormat.Type.INT, 2660),
                    BackupFormat.Entry("goal_mode", BackupFormat.Type.STRING, "AUTO"),
                    BackupFormat.Entry("weight_kg", BackupFormat.Type.FLOAT, 70f),
                    BackupFormat.Entry("activity", BackupFormat.Type.STRING, "MEDIUM"),
                    BackupFormat.Entry("temperature", BackupFormat.Type.STRING, "MILD"),
                ),
            ),
        )
        assertEquals(2250, repo.current().goal)
    }

    @Test
    fun manualGoalSurvivesTheFormulaChange() = runBlocking {
        repo.restore(
            BackupFormat.Snapshot(
                0, "1.10.0",
                listOf(
                    BackupFormat.Entry("goal", BackupFormat.Type.INT, 3100),
                    BackupFormat.Entry("goal_mode", BackupFormat.Type.STRING, "MANUAL"),
                    BackupFormat.Entry("manual_goal", BackupFormat.Type.INT, 3100),
                ),
            ),
        )
        assertEquals(3100, repo.current().goal)
    }

    // --- 1.13 ---

    private suspend fun restoreWith(vararg extra: BackupFormat.Entry) {
        repo.restore(
            BackupFormat.Snapshot(
                0, "test",
                listOf(
                    BackupFormat.Entry("goal", BackupFormat.Type.INT, 2500),
                    BackupFormat.Entry("weight_kg", BackupFormat.Type.FLOAT, 70f),
                    BackupFormat.Entry("activity", BackupFormat.Type.STRING, "LOW"),
                ) + extra,
            ),
        )
    }

    private fun f(name: String, v: Float) = BackupFormat.Entry(name, BackupFormat.Type.FLOAT, v)
    private fun l(name: String, v: Long) = BackupFormat.Entry(name, BackupFormat.Type.LONG, v)

    @Test
    fun forecastHeatRaisesTheAutoGoal() = runBlocking {
        val now = System.currentTimeMillis()
        restoreWith(f("outdoor_now_c", 30f), f("outdoor_max_c", 34f), l("outdoor_updated_ms", now), l("outdoor_read_ms", now))
        val state = repo.current()
        assertEquals(2450, state.goal) // 70 kg × 28 + pełne 7 ml/kg
        assertEquals(34f, state.breakdown!!.outdoorMaxC!!, 0.01f)
    }

    @Test
    fun aStaleForecastFallsBackToTheManualTemperature() = runBlocking {
        val now = System.currentTimeMillis()
        val old = now - 40L * 3_600_000L
        restoreWith(f("outdoor_now_c", 30f), f("outdoor_max_c", 34f), l("outdoor_updated_ms", old), l("outdoor_read_ms", now))
        assertEquals(1950, repo.current().goal) // ręcznie „umiarkowanie" → bez dodatku
    }

    @Test
    fun weatherCanBeSwitchedOffInSettings() = runBlocking {
        val now = System.currentTimeMillis()
        restoreWith(f("outdoor_now_c", 30f), f("outdoor_max_c", 34f), l("outdoor_updated_ms", now), l("outdoor_read_ms", now))
        val settings = repo.current().settings
        repo.saveSettings(settings.copy(weatherTemperature = false))
        assertEquals(1950, repo.current().goal)
    }

    @Test
    fun alcoholRaisesTodaysGoalAndDeletingItTakesItBack() = runBlocking {
        restoreWith()
        val before = repo.current().goal
        val id = repo.addIntake(330, Drink.ALCOHOL)
        val after = repo.current()
        assertEquals(before + 150, after.goal)
        assertEquals(0, after.total) // alkohol nie liczy się do celu
        assertEquals(330, after.alcoholMl)
        repo.deleteIntake(id)
        assertEquals(before, repo.current().goal)
    }

    @Test
    fun manualGoalIsNotTouchedByAlcohol() = runBlocking {
        restoreWith()
        repo.saveSettings(repo.current().settings.copy(goalMode = GoalMode.MANUAL, manualGoalMl = 2000))
        repo.addIntake(500, Drink.ALCOHOL)
        assertEquals(2000, repo.current().goal)
    }

    @Test
    fun darkCheckInAfterReachingTheGoalRaisesTheGoalFromTomorrowOnly() = runBlocking {
        restoreWith()
        val goal = repo.current().goal
        repo.addIntake(goal, Drink.WATER)
        assertEquals("goal_up", repo.answerCheckIn(Feeling.DARK))
        val state = repo.current()
        assertEquals(Feeling.DARK, state.checkIn.today)
        assertEquals(3, state.checkIn.pendingPct)
        assertEquals(0, state.checkIn.activePct)
        assertEquals(goal, state.goal) // dziś bez zmian
        assertNull(repo.answerCheckIn(Feeling.GOOD)) // jedna odpowiedź dziennie
        assertEquals(Feeling.DARK, repo.current().checkIn.today)
    }

    @Test
    fun checkInPromotedAtDayRollWhenPrefsSayTomorrowHasCome() = runBlocking {
        // Wczorajszy dzień z oczekującą korektą +3%: po rozpoczęciu nowego dnia obowiązuje.
        val yesterday = java.time.LocalDate.now().toEpochDay() - 1
        restoreWith(l("last_epoch_day", yesterday), BackupFormat.Entry("personal_pct_pending", BackupFormat.Type.INT, 3))
        val state = repo.current()
        assertEquals(3, state.checkIn.activePct)
        assertEquals(GoalCalculator.breakdown(GoalInputs(70f, ActivityLevel.LOW, 0f, 14, personalPct = 3)).goalMl, state.goal)
    }

    @Test
    fun newSettingsSurviveARoundTrip() = runBlocking {
        restoreWith()
        val s = repo.current().settings.copy(
            healthWorkouts = true, learnedReminders = false, anchorAfterCoffee = true, homeWifiSsid = "Dom 5G", weatherTemperature = false,
        )
        repo.saveSettings(s)
        val back = repo.current().settings
        assertTrue(back.healthWorkouts)
        assertEquals(false, back.learnedReminders)
        assertTrue(back.anchorAfterCoffee)
        assertEquals("Dom 5G", back.homeWifiSsid)
        assertEquals(false, back.weatherTemperature)
    }

    @Test
    fun tagTappedTwiceQuicklyIsLoggedOnce() = runBlocking {
        restoreWith()
        val uri = NfcTag.uri(250, Drink.COFFEE)
        assertEquals(false, repo.nfcIsRepeat(uri, nowMs = 10_000))
        assertTrue(repo.nfcIsRepeat(uri, nowMs = 12_000))
        assertEquals(false, repo.nfcIsRepeat(NfcTag.uri(250, Drink.WATER), nowMs = 12_500))
        assertEquals(false, repo.nfcIsRepeat(uri, nowMs = 12_500 + NfcTag.DEBOUNCE_MS))
    }

    @Test
    fun nudgeMarkersAreRememberedPerKind() = runBlocking {
        restoreWith()
        assertNull(repo.lastNudge("gap"))
        repo.markNudge("gap", "100:14")
        repo.markNudge("wifi", "5")
        assertEquals("100:14", repo.lastNudge("gap"))
        assertEquals("5", repo.lastNudge("wifi"))
    }

    @Test
    fun overTheCaffeineLimitCountsLessThanTheNaiveSum() = runBlocking {
        restoreWith()
        repeat(6) { repo.addIntake(200, Drink.COFFEE, minuteOfDay = 480 + it * 30) }
        val total = repo.current().total
        assertTrue("było $total", total < 6 * 160)
        assertTrue(repo.current().caffeineMg > Caffeine.DAILY_LIMIT_MG)
    }
}
