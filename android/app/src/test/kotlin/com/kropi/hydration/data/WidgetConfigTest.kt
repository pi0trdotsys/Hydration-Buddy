package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class WidgetConfigTest {
    private val bottles = listOf(100, 250, 500, 750)

    @Test
    fun noStoredConfigUsesPlainBottlesFromSettings() {
        val cfg = WidgetConfig.decode(null, bottles)
        assertEquals(WidgetStyle.KROPI, cfg.style)
        assertEquals(bottles, cfg.slots.map { it.ml })
        assertTrue(cfg.slots.all { it.drink == null && it.isPlain })
    }

    @Test
    fun roundTripKeepsDrinksAmountsAndStyle() {
        val cfg = WidgetConfig(
            slots = listOf(
                ButtonSpec(Drink.COFFEE, 200),
                ButtonSpec(Drink.TEA, 300),
                ButtonSpec(null, 500),
                ButtonSpec(Drink.WATER, 750),
            ),
            style = WidgetStyle.MATERIAL_YOU,
        )
        assertEquals(cfg, WidgetConfig.decode(cfg.encode(), bottles))
    }

    @Test
    fun brokenSlotsFallBackToDefaultsButKeepStyle() {
        val cfg = WidgetConfig.decode("MATERIAL_YOU|COFFEE:200,xx", bottles)
        assertEquals(WidgetStyle.MATERIAL_YOU, cfg.style)
        assertEquals(bottles, cfg.slots.map { it.ml })
    }

    @Test
    fun unknownStyleAndGarbageAreHarmless() {
        assertEquals(WidgetStyle.KROPI, WidgetConfig.decode("ALIEN|-:100,-:250,-:500,-:750", bottles).style)
        assertEquals(bottles, WidgetConfig.decode("???", bottles).slots.map { it.ml })
    }

    @Test
    fun specRejectsAbsurdAmountsAndUnknownDrinks() {
        assertNull(ButtonSpec.decode("COFFEE:5"))
        assertNull(ButtonSpec.decode("COFFEE:99999"))
        assertNull(ButtonSpec.decode("BEER:200"))
        assertEquals(ButtonSpec(null, 250), ButtonSpec.decode("-:250"))
    }

    @Test
    fun alwaysExactlyFourSlots() {
        assertEquals(4, WidgetConfig.default(listOf(100)).slots.size)
        assertEquals(4, WidgetConfig.default(listOf(1, 2, 3, 4, 5, 6)).slots.size)
    }

    @Test
    fun plainMeansNoDrinkOrWater() {
        assertTrue(ButtonSpec(null, 100).isPlain)
        assertTrue(ButtonSpec(Drink.WATER, 100).isPlain)
        assertFalse(ButtonSpec(Drink.TEA, 100).isPlain)
    }

    @Test
    fun undoIsFreshOnlyBriefly() {
        val undo = UndoInfo(250, Drink.COFFEE, atEpochMs = 1_000_000L)
        assertTrue(undo.isFresh(nowMs = 1_000_000L))
        assertTrue(undo.isFresh(nowMs = 1_000_000L + UndoInfo.UNDO_WINDOW_MS))
        assertFalse(undo.isFresh(nowMs = 1_000_000L + UndoInfo.UNDO_WINDOW_MS + 1))
        assertFalse(undo.isFresh(nowMs = 999_000L)) // zegar cofnął się — nie pokazujemy
    }

    @Test
    fun midnightDelayPointsJustPastNextMidnight() {
        val evening = LocalDateTime.of(2026, 11, 19, 23, 59, 0)
        assertEquals(65_000L, MidnightSchedule.delayMs(evening)) // 60 s + 5 s zapasu
        val justAfter = LocalDateTime.of(2026, 11, 20, 0, 0, 1)
        assertEquals(24 * 3600_000L + 4_000L, MidnightSchedule.delayMs(justAfter))
    }

    @Test
    fun typicalPortionPrefersMemoryOverDefault() {
        val state = HydrationState(2500, emptyList(), emptyList(), List(24) { 0 }, 0, HydrationSettings(), drinkMemory = mapOf(Drink.COFFEE to 330))
        assertEquals(330, state.typicalMl(Drink.COFFEE))
        assertEquals(Drink.TEA.defaultMl, state.typicalMl(Drink.TEA))
    }
}

class WidgetStyleTest {
    @Test
    fun darkStyleSurvivesRoundTripAndDefaultsStaySystemFollowing() {
        val bottles = listOf(100, 250, 500, 750)
        val cfg = WidgetConfig.default(bottles, WidgetStyle.KROPI_DARK)
        assertEquals(WidgetStyle.KROPI_DARK, WidgetConfig.decode(cfg.encode(), bottles).style)
        assertEquals(WidgetStyle.KROPI, WidgetConfig.decode(null, bottles).style)
    }
}
