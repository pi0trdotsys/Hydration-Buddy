package com.kropi.hydration.data

import java.time.DayOfWeek
import java.time.LocalDate

/** Zestaw liczb do karty „Trendy" w Historii; null = za mało danych na wniosek. */
data class Trends(
    val weekAvgMl: Int?,
    val prevWeekAvgMl: Int?,
    val monthAvgMl: Int?,
    val prevMonthAvgMl: Int?,
    /** Średnia godzina pierwszego łyka (minuta doby) z ostatnich 30 dni. */
    val avgFirstSipMinute: Int?,
    /** Godzina z największą / najmniejszą średnią objętością w oknie picia. */
    val strongestHour: Int?,
    val weakestHour: Int?,
    val bestWeekday: DayOfWeek?,
    val worstWeekday: DayOfWeek?,
) {
    /** Zmiana tydzień do tygodnia w procentach (np. +12 albo -8). */
    val weekDeltaPct: Int? get() = deltaPct(weekAvgMl, prevWeekAvgMl)
    val monthDeltaPct: Int? get() = deltaPct(monthAvgMl, prevMonthAvgMl)

    val hasAnything: Boolean
        get() = weekAvgMl != null || monthAvgMl != null || avgFirstSipMinute != null

    private fun deltaPct(now: Int?, before: Int?): Int? =
        if (now == null || before == null || before <= 0) null else Math.round((now - before) * 100f / before)
}

object TrendCalculator {
    /** Ile dni z zapisami potrzeba, żeby uczciwie wskazać najlepszy/najgorszy dzień tygodnia i godziny. */
    private const val MIN_DAYS_FOR_PATTERNS = 7

    fun compute(
        records: List<DayRecord>,
        intakes: List<Intake>,
        today: LocalDate,
        windowStartHour: Int,
        windowEndHour: Int,
    ): Trends {
        val todayEpoch = today.toEpochDay()
        // Tylko dni, w których cokolwiek wypito — puste dni to „nie używałem aplikacji", nie „piłem 0".
        val tracked = records.filter { it.ml > 0 }.associateBy { it.epochDay }

        fun avg(fromBack: Int, toBack: Int): Int? {
            val days = (fromBack..toBack).mapNotNull { tracked[todayEpoch - it]?.ml }
            return if (days.size >= 2) days.sum() / days.size else null
        }

        val recent = intakes.filter { it.epochDay in (todayEpoch - 29)..todayEpoch }
        val byDay = recent.groupBy { it.epochDay }

        val firstSips = byDay.values.map { list -> list.minOf { it.minuteOfDay } }
        val avgFirst = if (firstSips.size >= 3) firstSips.sum() / firstSips.size else null

        var strongest: Int? = null
        var weakest: Int? = null
        if (byDay.size >= MIN_DAYS_FOR_PATTERNS && windowEndHour > windowStartHour) {
            val hours = (windowStartHour until windowEndHour).associateWith { h ->
                byDay.values.sumOf { day -> day.filter { it.hour == h }.sumOf { it.counted } }
            }
            if (hours.values.any { it > 0 }) {
                strongest = hours.maxByOrNull { it.value }?.key
                weakest = hours.minByOrNull { it.value }?.key
                if (strongest == weakest) { strongest = null; weakest = null }
            }
        }

        var best: DayOfWeek? = null
        var worst: DayOfWeek? = null
        val last60 = tracked.filterKeys { it in (todayEpoch - 59)..todayEpoch }
        if (last60.size >= MIN_DAYS_FOR_PATTERNS) {
            val perWeekday = last60.entries
                .groupBy { LocalDate.ofEpochDay(it.key).dayOfWeek }
                .mapValues { (_, v) -> v.sumOf { it.value.ml } / v.size }
            if (perWeekday.size >= 3) {
                best = perWeekday.maxByOrNull { it.value }?.key
                worst = perWeekday.minByOrNull { it.value }?.key
                if (best == worst) { best = null; worst = null }
            }
        }

        return Trends(
            weekAvgMl = avg(0, 6),
            prevWeekAvgMl = avg(7, 13),
            monthAvgMl = avg(0, 29),
            prevMonthAvgMl = avg(30, 59),
            avgFirstSipMinute = avgFirst,
            strongestHour = strongest,
            weakestHour = weakest,
            bestWeekday = best,
            worstWeekday = worst,
        )
    }
}
