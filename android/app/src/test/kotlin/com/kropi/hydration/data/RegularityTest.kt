package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegularityTest {
    private var id = 1L
    private fun sip(hour: Int, minute: Int = 0, ml: Int = 250) =
        Intake(id++, 0, hour, minute, ml, Drink.WATER, null, ml)

    private fun h(hour: Int, minute: Int = 0) = hour * 60 + minute

    @Test
    fun windowIsSplitIntoTwoHourBlocks() {
        val blocks = Regularity.blocks(emptyList(), 8, 22, nowMinute = h(23))
        assertEquals(7, blocks.size)
        assertEquals(h(8) to h(10), blocks.first().startMinute to blocks.first().endMinute)
        assertEquals(h(20) to h(22), blocks.last().startMinute to blocks.last().endMinute)
    }

    @Test
    fun blockCountsOnlyItsOwnSipsAndNetHydration() {
        val coffee = Intake(99, 0, 9, 0, 250, Drink.COFFEE, null, Drink.COFFEE.counted(250))
        val blocks = Regularity.blocks(listOf(sip(8, 30), coffee, sip(10, 0)), 8, 22, h(12, 30))
        assertEquals(250 + 200, blocks[0].countedMl)
        assertEquals(250, blocks[1].countedMl) // 10:00 należy już do drugiego okna
        assertTrue(blocks[0].hit)
    }

    @Test
    fun finishedAndCurrentBlocksAreMarked() {
        val blocks = Regularity.blocks(emptyList(), 8, 22, h(11))
        assertTrue(blocks[0].finished); assertFalse(blocks[0].current)
        assertFalse(blocks[1].finished); assertTrue(blocks[1].current)
        assertFalse(blocks[2].finished); assertFalse(blocks[2].current)
    }

    @Test
    fun summaryCountsOnlyFinishedBlocks() {
        val blocks = Regularity.blocks(listOf(sip(8, 30), sip(14, 30)), 8, 22, h(15))
        val summary = Regularity.summary(blocks)
        // zakończone: 8–10, 10–12, 12–14; piło się tylko w pierwszym
        assertEquals(RegularitySummary(1, 3), summary)
        assertEquals(33, summary.percent)
    }

    @Test
    fun noFinishedBlocksMeansNoPercent() {
        assertNull(Regularity.summary(Regularity.blocks(emptyList(), 8, 22, h(8, 30))).percent)
    }

    @Test
    fun longestGapIncludesStartAndNow() {
        val sips = listOf(sip(10), sip(11), sip(14))
        // od 8:00 do 10:00 = 120, 11→14 = 180, 14→16:30 = 150
        assertEquals(180, Regularity.longestGapMinutes(sips, 8, 22, h(16, 30)))
    }

    @Test
    fun noSipsMeansTheWholeElapsedWindowIsOneGap() {
        assertEquals(h(11) - h(8), Regularity.longestGapMinutes(emptyList(), 8, 22, h(11)))
    }

    @Test
    fun beforeWindowThereIsNoGap() {
        assertEquals(0, Regularity.longestGapMinutes(emptyList(), 8, 22, h(7)))
    }

    @Test
    fun burstIsMoreThanALitreInTheLastHour() {
        val sips = listOf(sip(14, 5, 500), sip(14, 20, 500), sip(14, 40, 250))
        assertTrue(Regularity.isBurst(sips, h(14, 50)))
        assertFalse(Regularity.isBurst(sips, h(16, 30))) // ten litr był ponad godzinę temu
        assertFalse(Regularity.isBurst(listOf(sip(14, 5, 500), sip(14, 20, 500)), h(14, 50))) // dokładnie litr to jeszcze nie ostrzeżenie
    }

    @Test
    fun dayPercentOverAFullDay() {
        val day = listOf(sip(8, 30), sip(10, 30), sip(12, 30), sip(14, 30), sip(16, 30), sip(18, 30), sip(20, 30))
        assertEquals(100, Regularity.dayPercent(day, 8, 22))
        assertEquals(14, Regularity.dayPercent(listOf(sip(8, 30)), 8, 22))
    }
}
