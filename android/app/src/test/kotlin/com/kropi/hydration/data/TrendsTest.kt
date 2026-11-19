package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class TrendsTest {
    private val today = LocalDate.of(2026, 11, 19) // czwartek
    private val todayEpoch = today.toEpochDay()

    private fun record(back: Int, ml: Int) = DayRecord(todayEpoch - back, ml, 2500)

    private var nextId = 1L
    private fun intake(back: Int, hour: Int, ml: Int = 250, minute: Int = 0) =
        Intake(nextId++, todayEpoch - back, hour, minute, ml, Drink.WATER, null, ml)

    @Test
    fun emptyDataGivesNothing() {
        val t = TrendCalculator.compute(emptyList(), emptyList(), today, 8, 22)
        assertEquals(false, t.hasAnything)
        assertNull(t.weekDeltaPct)
        assertNull(t.strongestHour)
    }

    @Test
    fun weekOverWeekDeltaUsesOnlyTrackedDays() {
        val records = (0..6).map { record(it, 2000) } + (7..13).map { record(it, 1600) } + record(3, 0)
        val t = TrendCalculator.compute(records.distinctBy { it.epochDay }.let { it }, emptyList(), today, 8, 22)
        assertEquals(2000, t.weekAvgMl)
        assertEquals(1600, t.prevWeekAvgMl)
        assertEquals(25, t.weekDeltaPct)
    }

    @Test
    fun deltaIsNullWithoutPreviousPeriod() {
        val records = (0..6).map { record(it, 2000) }
        val t = TrendCalculator.compute(records, emptyList(), today, 8, 22)
        assertNull(t.prevWeekAvgMl)
        assertNull(t.weekDeltaPct)
    }

    @Test
    fun oneTrackedDayIsNotEnoughForAnAverage() {
        val t = TrendCalculator.compute(listOf(record(0, 1800)), emptyList(), today, 8, 22)
        assertNull(t.weekAvgMl)
    }

    @Test
    fun averageFirstSipNeedsAFewDays() {
        val two = listOf(intake(0, 8), intake(1, 9))
        assertNull(TrendCalculator.compute(emptyList(), two, today, 8, 22).avgFirstSipMinute)
        val three = two + intake(2, 10, 250, minute = 30)
        // 8:00, 9:00, 10:30 -> średnia 9:10
        assertEquals(8 * 60 + 70, TrendCalculator.compute(emptyList(), three, today, 8, 22).avgFirstSipMinute)
    }

    @Test
    fun strongestAndWeakestHourFromEnoughDays() {
        val intakes = (0..9).flatMap { d -> listOf(intake(d, 9, 300), intake(d, 15, 500), intake(d, 19, 100)) }
        val t = TrendCalculator.compute(emptyList(), intakes, today, 8, 22)
        assertEquals(15, t.strongestHour)
        // godziny bez żadnego łyka mają 0, więc to one są najsłabsze (pierwsza z najniższych)
        assertNotNull(t.weakestHour)
        assertTrue(t.weakestHour != 15)
    }

    @Test
    fun patternsNeedAWeekOfData() {
        val intakes = (0..3).map { intake(it, 12, 500) }
        val t = TrendCalculator.compute(emptyList(), intakes, today, 8, 22)
        assertNull(t.strongestHour)
        assertNull(t.weakestHour)
    }

    @Test
    fun bestAndWorstWeekday() {
        // Wtorki 3000 ml, poniedziałki 1000 ml, reszta 2000 ml, przez 8 tygodni.
        val records = (0..55).map { back ->
            val dow = LocalDate.ofEpochDay(todayEpoch - back).dayOfWeek
            record(back, when (dow) { DayOfWeek.TUESDAY -> 3000; DayOfWeek.MONDAY -> 1000; else -> 2000 })
        }
        val t = TrendCalculator.compute(records, emptyList(), today, 8, 22)
        assertEquals(DayOfWeek.TUESDAY, t.bestWeekday)
        assertEquals(DayOfWeek.MONDAY, t.worstWeekday)
    }

    @Test
    fun identicalWeekdaysGiveNoWinner() {
        val records = (0..20).map { record(it, 2000) }
        val t = TrendCalculator.compute(records, emptyList(), today, 8, 22)
        assertNull(t.bestWeekday)
        assertNull(t.worstWeekday)
    }
}
