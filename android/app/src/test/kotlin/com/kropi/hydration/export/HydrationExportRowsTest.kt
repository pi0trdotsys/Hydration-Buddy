package com.kropi.hydration.export

import com.kropi.hydration.data.DayRecord
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HydrationExportRowsTest {

    private val todayEpoch = LocalDate.of(2026, 10, 2).toEpochDay()
    private val today = ExportToday(
        epochDay = todayEpoch,
        ml = 1250,
        goal = 2500,
        intakes = 5,
        updatedEpochMs = 1_790_000_000_000L,
    )

    @Test
    fun contractMatchesSzpilaSide() {
        assertEquals("com.kropi.hydration.export", HydrationExportContract.AUTHORITY)
        assertEquals("com.kropi.hydration.permission.READ_HYDRATION", HydrationExportContract.PERMISSION_READ)
        assertEquals("com.kropi.hydration.action.HYDRATION_CHANGED", HydrationExportContract.ACTION_HYDRATION_CHANGED)
        assertEquals("app.lovable.glow_habit_widget", HydrationExportContract.TARGET_PACKAGE)
        assertEquals("today", HydrationExportContract.PATH_TODAY)
        assertEquals("days", HydrationExportContract.PATH_DAYS)
        assertArrayEquals(arrayOf("date", "ml", "goal", "intakes", "updated"), HydrationExportContract.COLUMNS)
        assertEquals(60, HydrationExportContract.DAYS_LIMIT)
    }

    @Test
    fun dateIsZeroPaddedIsoLocalDate() {
        assertEquals("2026-10-02", HydrationExportRows.formatDate(todayEpoch))
        assertEquals("2026-01-09", HydrationExportRows.formatDate(LocalDate.of(2026, 1, 9).toEpochDay()))
    }

    @Test
    fun todayRowCarriesAllColumnsInContractOrder() {
        val row = HydrationExportRows.todayRow(today)
        assertEquals(ExportRow("2026-10-02", 1250, 2500, 5, 1_790_000_000_000L), row)
        assertArrayEquals(arrayOf<Any>("2026-10-02", 1250, 2500, 5, 1_790_000_000_000L), row.toCursorRow())
        assertEquals(HydrationExportContract.COLUMNS.size, row.toCursorRow().size)
    }

    @Test
    fun daysPutsTodayFirstThenNewestFirstWithUnknownIntakesAndUpdated() {
        val history = listOf(
            DayRecord(todayEpoch - 3, 2000, 2500),
            DayRecord(todayEpoch - 1, 2600, 2500),
            DayRecord(todayEpoch - 2, 900, 2400),
        )
        val rows = HydrationExportRows.daysRows(today, history)
        assertEquals(listOf("2026-10-02", "2026-10-01", "2026-09-30", "2026-09-29"), rows.map { it.date })
        assertEquals(ExportRow("2026-10-01", 2600, 2500, 0, 0L), rows[1])
        assertEquals(ExportRow("2026-09-30", 900, 2400, 0, 0L), rows[2])
        assertEquals(today.intakes, rows[0].intakes)
    }

    @Test
    fun daysSkipsEmptyGapDaysAndIgnoresStaleTodayRecord() {
        val history = listOf(
            DayRecord(todayEpoch - 2, 0, 2500), // dzień, w którym aplikacja nie działała
            DayRecord(todayEpoch - 1, 1500, 2500),
            DayRecord(todayEpoch, 9999, 2500), // dziś bierzemy tylko z bieżącego stanu
        )
        val rows = HydrationExportRows.daysRows(today, history)
        assertEquals(listOf("2026-10-02", "2026-10-01"), rows.map { it.date })
        assertEquals(1250, rows[0].ml)
    }

    @Test
    fun daysAreCappedAtSixtyCalendarDaysIncludingToday() {
        val history = (1L..400L).map { back -> DayRecord(todayEpoch - back, 1000 + back.toInt(), 2500) }
        val rows = HydrationExportRows.daysRows(today, history)
        assertEquals(60, rows.size)
        assertEquals("2026-10-02", rows.first().date)
        assertEquals(HydrationExportRows.formatDate(todayEpoch - 59), rows.last().date)
        val epochs = rows.map { LocalDate.parse(it.date).toEpochDay() }
        assertEquals(epochs.sortedDescending(), epochs)
        assertTrue(epochs.all { it > todayEpoch - 60 })
    }

    @Test
    fun daysWithEmptyHistoryIsJustToday() {
        val rows = HydrationExportRows.daysRows(today, emptyList())
        assertEquals(listOf(HydrationExportRows.todayRow(today)), rows)
    }

    @Test
    fun duplicateHistoryDaysAppearOnce() {
        val history = listOf(DayRecord(todayEpoch - 1, 1500, 2500), DayRecord(todayEpoch - 1, 1500, 2500))
        assertEquals(2, HydrationExportRows.daysRows(today, history).size)
    }

    @Test
    fun broadcastExtrasCarryDateMlAndGoal() {
        val extras = HydrationExportRows.changedExtras(today)
        assertEquals(mapOf<String, Any>("date" to "2026-10-02", "ml" to 1250, "goal" to 2500), extras)
        assertTrue(extras["ml"] is Int)
        assertTrue(extras["goal"] is Int)
    }
}
