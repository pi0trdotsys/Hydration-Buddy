package com.kropi.hydration.data

import java.time.LocalDate
import java.time.YearMonth

/** Jeden dzień kalendarza miesiąca; [record] = null, gdy Kropi nic o tym dniu nie wie (albo to przyszłość). */
data class MonthCell(val date: LocalDate, val record: DayRecord?)

/** Podsumowanie miesiąca liczone tylko z dni, w których coś wypito. */
data class MonthSummary(val trackedDays: Int, val totalMl: Int, val reachedDays: Int) {
    val averageMl: Int get() = if (trackedDays == 0) 0 else totalMl / trackedDays
}

/** Czysta logika siatki kalendarza — tydzień zaczyna się w poniedziałek. */
object MonthCalendar {
    /** Ile pustych pól przed 1. dniem miesiąca (poniedziałek = 0). */
    fun leadingBlanks(month: YearMonth): Int = month.atDay(1).dayOfWeek.value - 1

    fun cells(month: YearMonth, byDay: Map<Long, DayRecord>, today: LocalDate): List<MonthCell> =
        (1..month.lengthOfMonth()).map { dom ->
            val date = month.atDay(dom)
            MonthCell(date, if (date.isAfter(today)) null else byDay[date.toEpochDay()])
        }

    fun summary(cells: List<MonthCell>): MonthSummary {
        val tracked = cells.mapNotNull { it.record }.filter { it.ml > 0 }
        return MonthSummary(tracked.size, tracked.sumOf { it.ml }, tracked.count { it.reached })
    }

    /** Wiersze po 7 pól (null = puste pole przed/po miesiącu), gotowe do narysowania. */
    fun weeks(month: YearMonth, cells: List<MonthCell>): List<List<MonthCell?>> {
        val padded: List<MonthCell?> = List(leadingBlanks(month)) { null } + cells
        return padded.chunked(7).map { week -> week + List(7 - week.size) { null } }
    }
}
