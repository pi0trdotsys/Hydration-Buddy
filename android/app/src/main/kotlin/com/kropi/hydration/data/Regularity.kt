package com.kropi.hydration.data

/** Jedno okno czasu (domyślnie 2 godziny) w aktywnych godzinach picia i to, ile w nim wypito. */
data class RegularityBlock(
    val startMinute: Int,
    val endMinute: Int,
    val countedMl: Int,
    /** Okno już się skończyło. */
    val finished: Boolean,
    /** „Teraz" wypada w tym oknie. */
    val current: Boolean,
) {
    val hit: Boolean get() = countedMl >= Regularity.MIN_BLOCK_ML
}

/** Wynik regularności z całych okien: ile z zakończonych miało choć trochę picia. */
data class RegularitySummary(val hits: Int, val finishedBlocks: Int) {
    val percent: Int? get() = if (finishedBlocks == 0) null else Math.round(hits * 100f / finishedBlocks)
}

/**
 * Regularność picia. Badania i zalecenia (m.in. ACSM) są zgodne co do jednego: lepiej pić regularnie,
 * małymi porcjami, niż rzadko i dużo — jelito wchłania do ok. 1 l/h, a nerki wydalają 0,8–1 l/h, więc
 * nadwyżka „na zapas" po prostu przepływa. Dlatego poza sumą dnia patrzymy na to, czy w każdym oknie
 * czasu cokolwiek wypito, jaka była najdłuższa przerwa i czy ktoś nie wlał w siebie litra naraz.
 */
object Regularity {
    const val BLOCK_MINUTES = 120

    /** Tyle wypite w oknie wystarcza, żeby uznać je za „pite". */
    const val MIN_BLOCK_ML = 100

    /** Powyżej tego w ciągu godziny ostrzegamy: to więcej, niż organizm zdąży sensownie wykorzystać. */
    const val BURST_LIMIT_ML = 1000

    /** Dłuższa przerwa niż ta w aktywnych godzinach jest już „dziurą" w nawodnieniu. */
    const val LONG_GAP_MINUTES = 180

    fun blocks(intakes: List<Intake>, startHour: Int, endHour: Int, nowMinute: Int, blockMinutes: Int = BLOCK_MINUTES): List<RegularityBlock> {
        val start = startHour * 60
        val end = endHour * 60
        if (end <= start) return emptyList()
        val result = mutableListOf<RegularityBlock>()
        var s = start
        while (s < end) {
            val e = minOf(s + blockMinutes, end)
            val counted = intakes.filter { it.minuteOfDay in s until e }.sumOf { it.counted }
            result += RegularityBlock(s, e, counted, finished = e <= nowMinute, current = nowMinute in s until e)
            s = e
        }
        return result
    }

    fun summary(blocks: List<RegularityBlock>): RegularitySummary {
        val finished = blocks.filter { it.finished }
        return RegularitySummary(finished.count { it.hit }, finished.size)
    }

    /**
     * Najdłuższa przerwa w oknie picia do chwili [nowMinute]: od startu okna do pierwszego łyka, między
     * kolejnymi łykami i od ostatniego do teraz. Przed otwarciem okna — 0.
     */
    fun longestGapMinutes(intakes: List<Intake>, startHour: Int, endHour: Int, nowMinute: Int): Int {
        val start = startHour * 60
        val end = endHour * 60
        val until = minOf(nowMinute, end)
        if (until <= start) return 0
        val points = intakes.map { it.minuteOfDay }.filter { it in start..until }.sorted()
        var previous = start
        var longest = 0
        for (p in points) { longest = maxOf(longest, p - previous); previous = p }
        return maxOf(longest, until - previous)
    }

    /** Ile „liczonych" ml wypito w ostatnich [windowMinutes] minutach. */
    fun recentMl(intakes: List<Intake>, nowMinute: Int, windowMinutes: Int = 60): Int =
        intakes.filter { it.minuteOfDay in (nowMinute - windowMinutes + 1)..nowMinute }.sumOf { it.counted }

    fun isBurst(intakes: List<Intake>, nowMinute: Int): Boolean = recentMl(intakes, nowMinute) > BURST_LIMIT_ML

    /** Procent okien z piciem w całym, zakończonym dniu — do trendów. */
    fun dayPercent(dayIntakes: List<Intake>, startHour: Int, endHour: Int): Int? {
        val blocks = blocks(dayIntakes, startHour, endHour, nowMinute = 24 * 60)
        return summary(blocks).percent
    }
}
