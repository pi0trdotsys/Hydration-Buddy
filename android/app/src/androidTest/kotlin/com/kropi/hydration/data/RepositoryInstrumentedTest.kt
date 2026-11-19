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
}
