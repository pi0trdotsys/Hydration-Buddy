package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyMigrationTest {
    private val today = 20_800L

    @Test
    fun todaysIntakesKeepTheirTimes() {
        val r = LegacyMigration.parse("7:20:250,9:5:330", null, today)
        assertEquals(2, r.intakes.size)
        assertEquals(7 * 60 + 20, r.intakes[0].minuteOfDay)
        assertEquals(250, r.intakes[0].ml)
        assertEquals(today, r.intakes[0].epochDay)
        assertEquals(9 * 60 + 5, r.intakes[1].minuteOfDay)
        assertTrue(r.intakes.all { it.drink == Drink.WATER.name })
    }

    @Test
    fun historyBecomesOneSummaryIntakePerDayAndKeepsGoal() {
        val r = LegacyMigration.parse(null, "20770:2100:2500,20771:0:2400", today)
        assertEquals(listOf(DayGoalEntity(20770, 2500), DayGoalEntity(20771, 2400)), r.dayGoals)
        // Dzień z 0 ml nie dostaje pustego łyka, ale cel zostaje.
        assertEquals(1, r.intakes.size)
        assertEquals(2100, r.intakes[0].ml)
        assertEquals(LegacyMigration.SUMMARY_MINUTE, r.intakes[0].minuteOfDay)
        assertEquals(LegacyMigration.SUMMARY_NOTE, r.intakes[0].note)
    }

    @Test
    fun brokenEntriesAreSkippedNotFatal() {
        val r = LegacyMigration.parse("x:y:z,8:0:200,1:2", "oops,20770:abc:2500,20771:900:2500", today)
        assertEquals(2, r.intakes.size) // 8:00 i zbiorczy z 20771
        assertEquals(listOf(20771L), r.dayGoals.map { it.epochDay })
    }

    @Test
    fun noCurrentDayMeansNoTodaysIntakes() {
        val r = LegacyMigration.parse("7:20:250", null, null)
        assertTrue(r.intakes.isEmpty())
    }

    @Test
    fun emptyInputGivesEmptyResult() {
        val r = LegacyMigration.parse("", "", today)
        assertTrue(r.intakes.isEmpty())
        assertTrue(r.dayGoals.isEmpty())
    }

    @Test
    fun duplicateHistoryDaysKeepFirstGoal() {
        val r = LegacyMigration.parse(null, "20770:100:2000,20770:200:3000", today)
        assertEquals(1, r.dayGoals.size)
        assertEquals(2000, r.dayGoals[0].goal)
    }
}
