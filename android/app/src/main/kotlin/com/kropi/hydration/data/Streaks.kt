package com.kropi.hydration.data

/**
 * Seria dni z zaliczonym celem, z „tarczą".
 *
 * Seria bez żadnej wyrozumiałości kończy się pierwszą gorszą dobą, a to jest najczęstszy powód porzucenia
 * nawyku („i tak już przepadło"). Dlatego jeden opuszczony dzień nie zeruje serii, jeśli masz tarczę:
 * zaczynasz z jedną, a kolejną dostajesz za każde [DAYS_PER_SHIELD] dni z rzędu (maks. [MAX_SHIELDS]).
 * Dzień uratowany tarczą nie dolicza się do serii — tylko jej nie przerywa.
 */
data class StreakInfo(
    /** Dni z zaliczonym celem w bieżącej serii (dzisiejszy liczy się po osiągnięciu celu). */
    val days: Int,
    /** Ile tarcz zostało. */
    val shields: Int,
    /** Czy dzisiejszy dzień jest zagrożony: seria trwa, a cel jeszcze niezaliczony. */
    val atRisk: Boolean,
    /** Ile dni uratowały tarcze w tej serii (do wyświetlenia przy serii). */
    val savedDays: Int = 0,
) {
    companion object {
        val NONE = StreakInfo(0, Streaks.START_SHIELDS, false)
    }
}

object Streaks {
    const val START_SHIELDS = 1
    const val MAX_SHIELDS = 2
    const val DAYS_PER_SHIELD = 7

    /**
     * [closed] to zamknięte dni od najstarszego (luki zapełnia repozytorium zerami), [today] — dzień w toku.
     */
    fun compute(closed: List<DayRecord>, today: DayRecord): StreakInfo {
        var streak = 0
        var shields = START_SHIELDS
        var saved = 0
        var expected: Long? = null
        for (day in closed.sortedBy { it.epochDay }) {
            // Brakujący dzień (np. import z lukami) traktujemy jak niezaliczony.
            expected?.let { exp ->
                var gap = exp
                while (gap < day.epochDay) {
                    val r = miss(streak, shields, saved)
                    streak = r.first; shields = r.second; saved = r.third
                    gap++
                }
            }
            if (day.reached) {
                streak++
                if (streak % DAYS_PER_SHIELD == 0) shields = minOf(MAX_SHIELDS, shields + 1)
            } else {
                val r = miss(streak, shields, saved)
                streak = r.first; shields = r.second; saved = r.third
            }
            expected = day.epochDay + 1
        }
        // Dni między ostatnim zamkniętym a dziś (aplikacja nieużywana) też są pudłami.
        expected?.let { exp ->
            var gap = exp
            while (gap < today.epochDay) {
                val r = miss(streak, shields, saved)
                streak = r.first; shields = r.second; saved = r.third
                gap++
            }
        }
        val reachedToday = today.reached
        if (reachedToday) {
            streak++
            if (streak % DAYS_PER_SHIELD == 0) shields = minOf(MAX_SHIELDS, shields + 1)
        }
        return StreakInfo(
            days = streak,
            shields = shields,
            atRisk = !reachedToday && streak > 0,
            savedDays = saved,
        )
    }

    private fun miss(streak: Int, shields: Int, saved: Int): Triple<Int, Int, Int> =
        if (streak > 0 && shields > 0) Triple(streak, shields - 1, saved + 1)
        else Triple(0, shields, 0)
}
