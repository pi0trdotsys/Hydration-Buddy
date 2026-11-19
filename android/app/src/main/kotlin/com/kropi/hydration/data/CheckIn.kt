package com.kropi.hydration.data

/**
 * Codzienne pytanie „jak dziś?": jedyny sygnał zwrotny, jaki aplikacja może dostać o tym, czy cel pasuje
 * do konkretnego człowieka. Kolor moczu to prosty i sprawdzony marker nawodnienia (skala Armstronga —
 * jasnosłomkowy oznacza dobre nawodnienie, ciemny niedobór); pytamy nie o liczby, tylko o trzy odpowiedzi.
 *
 * Reguły są ostrożne, żeby nie gonić własnego ogona:
 *  - **ciemny** przy zrealizowanym (≥ 85%) celu oznacza, że cel jest dla Ciebie za niski → +[STEP_PCT]%;
 *    przy niezrealizowanym winna jest ilość, nie cel — nic nie zmieniamy;
 *  - **bardzo jasny** dopiero trzeci raz z rzędu (a cel był wypity) obniża cel o [STEP_PCT]%;
 *  - korekta nie wyjdzie poza ±[GoalCalculator.MAX_PERSONAL_PCT]% i zaczyna obowiązywać od następnego dnia,
 *    żeby nie można było „odpukać" dzisiejszego celu odpowiedzią.
 */
enum class Feeling(val code: Char, val emoji: String, private val labelPl: String, private val labelEn: String, private val hintPl: String, private val hintEn: String) {
    CLEAR('C', "🫧", "Bardzo jasny", "Very pale", "przezroczysty jak woda", "clear as water"),
    GOOD('G', "🙂", "Jasnożółty", "Pale yellow", "w sam raz", "just right"),
    DARK('D', "🥵", "Ciemny lub pragnienie", "Dark, or thirsty", "ciemnożółty, ból głowy, sucho w ustach", "dark yellow, headache, dry mouth"),
    SKIPPED('S', "—", "Pomiń", "Skip", "", "");

    val label: String get() = tr(labelPl, labelEn)
    val hint: String get() = tr(hintPl, hintEn)

    companion object {
        fun of(code: Char?): Feeling? = entries.firstOrNull { it.code == code }
    }
}

/** Jedna odpowiedź z dnia. */
data class CheckInEntry(val epochDay: Long, val feeling: Feeling)

object CheckInRules {
    const val STEP_PCT = 3
    const val CLEAR_STREAK_TO_LOWER = 3
    const val REACHED_RATIO = 0.85f

    /** Od tej godziny pytamy — poranny mocz jest naturalnie ciemniejszy i myli. */
    const val ASK_FROM_HOUR = 12

    /**
     * Nowa korekta po odpowiedzi [feeling]. [history] to wcześniejsze odpowiedzi (bez dzisiejszej),
     * [ratio] — jaka część dzisiejszego celu została wypita.
     */
    fun nextPct(currentPct: Int, history: List<CheckInEntry>, feeling: Feeling, ratio: Float): Int {
        val max = GoalCalculator.MAX_PERSONAL_PCT
        return when (feeling) {
            Feeling.DARK -> if (ratio >= REACHED_RATIO) (currentPct + STEP_PCT).coerceAtMost(max) else currentPct
            Feeling.CLEAR -> {
                var streak = 1
                for (e in history.sortedByDescending { it.epochDay }) {
                    if (e.feeling == Feeling.CLEAR) streak++ else if (e.feeling == Feeling.SKIPPED) continue else break
                }
                if (ratio >= 1f && streak % CLEAR_STREAK_TO_LOWER == 0) (currentPct - STEP_PCT).coerceAtLeast(-max) else currentPct
            }
            Feeling.GOOD, Feeling.SKIPPED -> currentPct
        }
    }

    fun encode(entries: List<CheckInEntry>): String = entries.sortedBy { it.epochDay }.takeLast(30)
        .joinToString(",") { "${it.epochDay}:${it.feeling.code}" }

    fun decode(raw: String?): List<CheckInEntry> = raw.orEmpty().split(",").mapNotNull { part ->
        val bits = part.split(":")
        val day = bits.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
        val feeling = Feeling.of(bits.getOrNull(1)?.firstOrNull()) ?: return@mapNotNull null
        CheckInEntry(day, feeling)
    }

    /** Podpowiedź pod odpowiedzią — co z niej wynika dla dzisiejszego dnia. */
    fun adviceKey(feeling: Feeling, ratio: Float): String = when {
        feeling == Feeling.DARK && ratio < REACHED_RATIO -> "drink_more"
        feeling == Feeling.DARK -> "goal_up"
        feeling == Feeling.CLEAR && ratio >= 1f -> "no_more"
        feeling == Feeling.CLEAR -> "fine"
        else -> "fine"
    }
}
