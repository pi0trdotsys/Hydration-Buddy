package com.kropi.hydration.backup

import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.Intake
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Rule
import com.kropi.hydration.testutil.polish
import java.time.LocalDate

class CsvEntriesTest {
    @get:Rule val language = polish()

    private val day = LocalDate.of(2026, 11, 19).toEpochDay()

    private fun intake(id: Long, hour: Int, minute: Int, ml: Int, drink: Drink = Drink.WATER, note: String? = null, dayOffset: Long = 0) =
        Intake(id, day + dayOffset, hour, minute, ml, drink, note, drink.counted(ml))

    @Test
    fun headerAndRowsAreSortedByTime() {
        val csv = CsvEntries.build(listOf(intake(2, 15, 5, 250), intake(1, 8, 30, 330, Drink.COFFEE, "po bieganiu"), intake(3, 9, 0, 100, dayOffset = -1)))
        val lines = csv.trim().lines()
        assertEquals(CsvEntries.HEADER, lines[0])
        assertEquals("2026-11-18;09:00;Woda;100;100;", lines[1])
        assertEquals("2026-11-19;08:30;Kawa;330;264;po bieganiu", lines[2])
        assertEquals("2026-11-19;15:05;Woda;250;250;", lines[3])
    }

    @Test
    fun separatorsAndQuotesInNotesAreEscaped() {
        assertEquals("zwykła notatka", CsvEntries.field("zwykła notatka"))
        assertEquals("\"a;b\"", CsvEntries.field("a;b"))
        assertEquals("\"powiedział \"\"cześć\"\"\"", CsvEntries.field("powiedział \"cześć\""))
        assertEquals("\"dwie\nlinie\"", CsvEntries.field("dwie\nlinie"))
    }

    @Test
    fun emptyListGivesOnlyTheHeader() {
        assertEquals(CsvEntries.HEADER, CsvEntries.build(emptyList()).trim())
    }

    @Test
    fun entriesFileIsNotMistakenForDayHistoryOnImport() {
        assertEquals(false, CsvHistory.looksLikeCsv(CsvEntries.build(listOf(intake(1, 8, 0, 250)))))
    }
}
