package com.kropi.hydration.data

/** Typowa „dziura" w piciu: przedział godzin w oknie aktywności, w którym zwykle nic nie pijesz. */
data class HabitGap(val startHour: Int, val endHour: Int) {
    val hours: Int get() = endHour - startHour
}

/**
 * Model Twoich zwyczajów z ostatnich tygodni. Liczy, w jakiej części dni piłeś w danej godzinie, i z tego
 * wyciąga godziny, w których zwykle nie pijesz — spotkania, jazda, skupienie w pracy. Przypomnienie
 * wysyłane tuż PRZED taką przerwą ma sens, bo w samej przerwie i tak go nie zobaczysz albo zlekceważysz.
 */
object Habits {
    /** Ile ostatnich dni patrzymy wstecz. */
    const val LOOKBACK_DAYS = 21

    /** Poniżej tylu dni z danymi model niczego nie sugeruje. */
    const val MIN_DAYS = 7

    /** Dzień „z danymi" ma co najmniej tyle wpisów. */
    const val MIN_INTAKES_PER_DAY = 3

    /** Godzina z mniejszą szansą picia niż ta jest „cicha". */
    const val QUIET_PROBABILITY = 0.2f

    /** Najkrótsza przerwa warta przypomnienia. */
    const val MIN_GAP_HOURS = 2

    /** Przypomnienie wychodzi tak wcześnie przed przerwą. */
    const val LEAD_MINUTES = 45

    /** Jeśli dopiero co piłeś, nie przypominamy. */
    const val RECENT_SIP_MINUTES = 30

    data class Result(val probability: List<Float>, val gaps: List<HabitGap>, val daysUsed: Int)

    /**
     * [intakes] to wpisy z poprzednich dni (dzisiejszy dzień nie wchodzi do modelu, żeby model nie uczył
     * się z niedokończonego dnia). Zwraca pusty wynik, gdy danych jest za mało.
     */
    fun learn(intakes: List<Intake>, today: Long, startHour: Int, endHour: Int): Result {
        val recent = intakes.filter { it.epochDay in (today - LOOKBACK_DAYS) until today }
        val days = recent.groupBy { it.epochDay }.filterValues { it.size >= MIN_INTAKES_PER_DAY }
        if (days.size < MIN_DAYS) return Result(List(24) { 0f }, emptyList(), days.size)
        val probability = List(24) { hour ->
            days.values.count { day -> day.any { it.hour == hour } }.toFloat() / days.size
        }
        val gaps = mutableListOf<HabitGap>()
        var runStart: Int? = null
        for (h in startHour until endHour) {
            val quiet = probability[h] < QUIET_PROBABILITY
            if (quiet && runStart == null) runStart = h
            if ((!quiet || h == endHour - 1) && runStart != null) {
                val end = if (quiet && h == endHour - 1) endHour else h
                // Przerwa na samym początku okna to po prostu poranek bez picia — nie ma przed czym ostrzegać.
                if (end - runStart >= MIN_GAP_HOURS && runStart > startHour) gaps += HabitGap(runStart, end)
                runStart = null
            }
        }
        return Result(probability, gaps, days.size)
    }

    /**
     * Przerwa, tuż przed którą jesteśmy (w ciągu [LEAD_MINUTES] przed jej początkiem), albo null.
     * [minutesSinceLast] = null oznacza, że dziś nic jeszcze nie wypito.
     */
    fun gapAhead(gaps: List<HabitGap>, nowMinute: Int, minutesSinceLast: Long?): HabitGap? {
        if (minutesSinceLast != null && minutesSinceLast < RECENT_SIP_MINUTES) return null
        return gaps.firstOrNull { g ->
            val start = g.startHour * 60
            nowMinute in (start - LEAD_MINUTES) until start
        }
    }
}
