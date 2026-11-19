package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayRecordsTest {
    private fun intake(day: Long, ml: Int, drink: Drink = Drink.WATER) =
        IntakeEntity(epochDay = day, minuteOfDay = 600, ml = ml, drink = drink.name)

    @Test
    fun sumsCountedMlPerClosedDayAndSkipsToday() {
        val records = DayRecords.build(
            intakes = listOf(intake(10, 500), intake(10, 250, Drink.COFFEE), intake(11, 300), intake(12, 999)),
            goals = listOf(DayGoalEntity(10, 2000), DayGoalEntity(11, 2200)),
            currentDay = 12,
            fallbackGoal = 2500,
            net = true,
        )
        assertEquals(listOf(10L, 11L), records.map { it.epochDay })
        assertEquals(700, records[0].ml) // 500 + 250*0.8
        assertEquals(2000, records[0].goal)
        assertEquals(300, records[1].ml)
    }

    @Test
    fun netOffCountsPouredMl() {
        val r = DayRecords.build(listOf(intake(1, 250, Drink.COFFEE)), emptyList(), 5, 2500, net = false)
        assertEquals(250, r.single().ml)
    }

    @Test
    fun dayWithoutGoalUsesFallback() {
        val r = DayRecords.build(listOf(intake(1, 100)), emptyList(), 5, 2500, net = true)
        assertEquals(2500, r.single().goal)
    }

    @Test
    fun dayWithGoalButNoIntakesIsStillListed() {
        val r = DayRecords.build(emptyList(), listOf(DayGoalEntity(3, 2000)), 5, 2500, net = true)
        assertEquals(0, r.single().ml)
        assertTrue(!r.single().reached)
    }

    @Test
    fun reachedWhenMlMeetsGoal() {
        assertTrue(DayRecord(1, 2000, 2000).reached)
        assertTrue(!DayRecord(1, 1999, 2000).reached)
    }
}
