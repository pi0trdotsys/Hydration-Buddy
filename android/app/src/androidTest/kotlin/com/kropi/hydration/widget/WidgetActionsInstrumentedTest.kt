package com.kropi.hydration.widget

import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.AppWidgetId
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kropi.hydration.backup.BackupFormat
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.HydrationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Zachowanie przycisków widgetu wywołane tak, jak robi to Glance: przez [androidx.glance.appwidget.action.ActionCallback]. */
@RunWith(AndroidJUnit4::class)
class WidgetActionsInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val widgetId = AppWidgetId(1)
    private lateinit var repo: HydrationRepository

    @Before
    fun freshState() = runBlocking {
        repo = HydrationRepository(context)
        repo.restore(BackupFormat.Snapshot(0, "test", listOf(BackupFormat.Entry("goal", BackupFormat.Type.INT, 2500))))
    }

    @Test
    fun plainButtonLogsWaterByDefault() = runBlocking {
        AddWaterAction().onAction(context, widgetId, actionParametersOf(MlKey to 250))
        val state = repo.current()
        assertEquals(250, state.total)
        assertEquals(Drink.WATER, state.intakes.single().drink)
    }

    @Test
    fun selectedDrinkAppliesToOneBottleTapThenResetsToWater() = runBlocking {
        repo.setWidgetDrink(Drink.COFFEE)
        AddWaterAction().onAction(context, widgetId, actionParametersOf(MlKey to 250))
        AddWaterAction().onAction(context, widgetId, actionParametersOf(MlKey to 250))
        val drinks = repo.current().intakes.sortedBy { it.id }.map { it.drink }
        assertEquals(listOf(Drink.COFFEE, Drink.WATER), drinks)
        assertEquals(Drink.WATER, repo.widgetDrink())
    }

    @Test
    fun fixedDrinkButtonLogsThatDrinkAndLeavesSelectionAlone() = runBlocking {
        repo.setWidgetDrink(Drink.TEA)
        AddWaterAction().onAction(context, widgetId, actionParametersOf(MlKey to 200, DrinkKey to Drink.COFFEE.name))
        val state = repo.current()
        assertEquals(Drink.COFFEE, state.intakes.single().drink)
        assertEquals(Drink.TEA, repo.widgetDrink())
    }

    @Test
    fun tappingSelectedDrinkAgainFallsBackToWater() = runBlocking {
        SetDrinkAction().onAction(context, widgetId, actionParametersOf(DrinkKey to Drink.JUICE.name))
        assertEquals(Drink.JUICE, repo.widgetDrink())
        SetDrinkAction().onAction(context, widgetId, actionParametersOf(DrinkKey to Drink.JUICE.name))
        assertEquals(Drink.WATER, repo.widgetDrink())
    }

    @Test
    fun cyclingVisitsEveryDrinkAndWrapsAround() = runBlocking {
        val seen = mutableListOf<Drink>()
        repeat(Drink.entries.size) {
            CycleDrinkAction().onAction(context, widgetId, actionParametersOf())
            seen += repo.widgetDrink()
        }
        assertEquals(Drink.entries.size, seen.toSet().size)
        assertEquals(Drink.WATER, seen.last())
    }

    @Test
    fun undoThenRestoreActionsRoundTrip() = runBlocking {
        AddWaterAction().onAction(context, widgetId, actionParametersOf(MlKey to 330))
        UndoWaterAction().onAction(context, widgetId, actionParametersOf())
        val afterUndo = repo.current()
        assertEquals(0, afterUndo.total)
        assertNotNull(afterUndo.undo)

        RestoreUndoAction().onAction(context, widgetId, actionParametersOf())
        val afterRestore = repo.current()
        assertEquals(330, afterRestore.total)
        assertNull(afterRestore.undo)
    }
}
