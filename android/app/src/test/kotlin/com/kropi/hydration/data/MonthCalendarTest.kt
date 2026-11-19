package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthCalendarTest {
    private val nov2026 = YearMonth.of(2026, 11) // 1 listopada 2026 to niedziela
    private val today = LocalDate.of(2026, 11, 19)

    private fun record(day: Int, ml: Int, goal: Int = 2000) =
        DayRecord(LocalDate.of(2026, 11, day).toEpochDay(), ml, goal)

    @Test
    fun mondayFirstLeadingBlanks() {
        assertEquals(6, MonthCalendar.leadingBlanks(nov2026)) // niedziela
        assertEquals(0, MonthCalendar.leadingBlanks(YearMonth.of(2026, 6))) // poniedziałek
        assertEquals(2, MonthCalendar.leadingBlanks(YearMonth.of(2026, 7))) // środa
    }

    @Test
    fun oneCellPerDayAndFutureDaysHaveNoRecord() {
        val byDay = listOf(record(3, 1500), record(25, 999)).associateBy { it.epochDay }
        val cells = MonthCalendar.cells(nov2026, byDay, today)
        assertEquals(30, cells.size)
        assertEquals(1500, cells[2].record?.ml)
        assertNull(cells[24].record) // 25.11 jest po „dziś", nawet jeśli ktoś ma tam rekord
        assertNull(cells[0].record)
    }

    @Test
    fun summaryCountsOnlyDaysWithIntake() {
        val byDay = listOf(record(1, 0), record(2, 2000), record(3, 1000), record(4, 2500)).associateBy { it.epochDay }
        val s = MonthCalendar.summary(MonthCalendar.cells(nov2026, byDay, today))
        assertEquals(3, s.trackedDays)
        assertEquals(5500, s.totalMl)
        assertEquals(2, s.reachedDays)
        assertEquals(1833, s.averageMl)
    }

    @Test
    fun emptyMonthSummaryIsZero() {
        val s = MonthCalendar.summary(MonthCalendar.cells(nov2026, emptyMap(), today))
        assertEquals(0, s.trackedDays)
        assertEquals(0, s.averageMl)
    }

    @Test
    fun weeksAreSevenWideAndCoverTheWholeMonth() {
        val cells = MonthCalendar.cells(nov2026, emptyMap(), today)
        val weeks = MonthCalendar.weeks(nov2026, cells)
        assertTrue(weeks.all { it.size == 7 })
        assertEquals(30, weeks.flatten().count { it != null })
        assertNull(weeks[0][0])
        assertEquals(1, weeks[0][6]!!.date.dayOfMonth)
        assertEquals(6, weeks.size)
    }
}
