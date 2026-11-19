package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreaksTest {
    private fun day(d: Long, reached: Boolean) = DayRecord(d, if (reached) 2000 else 500, 2000)
    private fun run(vararg r: Boolean, today: Boolean = false): StreakInfo {
        val closed = r.mapIndexed { i, ok -> day(i.toLong(), ok) }
        return Streaks.compute(closed, day(r.size.toLong(), today))
    }

    @Test
    fun noHistoryMeansNoStreakAndOneShield() {
        val info = Streaks.compute(emptyList(), day(0, false))
        assertEquals(0, info.days)
        assertEquals(1, info.shields)
        assertFalse(info.atRisk)
    }

    @Test
    fun consecutiveDaysCount() {
        assertEquals(3, run(true, true, true).days)
        assertEquals(4, run(true, true, true, today = true).days)
    }

    @Test
    fun oneMissedDayIsAbsorbedByTheShield() {
        val info = run(true, true, false, true)
        assertEquals(3, info.days) // uratowany dzień nie dolicza się, ale nie przerywa
        assertEquals(0, info.shields)
        assertEquals(1, info.savedDays)
    }

    @Test
    fun secondMissWithoutShieldResetsTheStreak() {
        val info = run(true, true, false, false, true)
        assertEquals(1, info.days)
        assertEquals(0, info.savedDays)
    }

    @Test
    fun sevenDaysInARowEarnAnotherShield() {
        val info = run(true, true, true, true, true, true, true)
        assertEquals(7, info.days)
        assertEquals(2, info.shields)
    }

    @Test
    fun shieldsAreCappedAtTwo() {
        val info = Streaks.compute(List(30) { day(it.toLong(), true) }, day(30, true))
        assertEquals(Streaks.MAX_SHIELDS, info.shields)
        assertEquals(31, info.days)
    }

    @Test
    fun missWithNoStreakDoesNotSpendTheShield() {
        val info = run(false, false, true, true)
        assertEquals(2, info.days)
        assertEquals(1, info.shields)
    }

    @Test
    fun todayInProgressIsAtRiskOnlyWhenThereIsAStreak() {
        assertTrue(run(true, true, today = false).atRisk)
        assertFalse(run(false, false, today = false).atRisk)
        assertFalse(run(true, true, today = true).atRisk)
    }

    @Test
    fun daysWithoutAnyRecordCountAsMisses() {
        // dni 0,1 zaliczone, 2–4 brak danych (aplikacja nieużywana), dziś = 5
        val info = Streaks.compute(listOf(day(0, true), day(1, true)), day(5, true))
        assertEquals(0 + 1, info.days) // tarcza ratuje pierwszy brakujący dzień, drugi zeruje serię, dziś zaczyna nową
        assertEquals(0, info.shields)
    }
}
