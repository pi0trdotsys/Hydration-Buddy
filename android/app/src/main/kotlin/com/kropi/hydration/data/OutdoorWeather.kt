package com.kropi.hydration.data

/**
 * Temperatura na zewnątrz z Homebrew Weather. Aplikacja udostępnia przez provider tylko liczby
 * (bez miejsca): bieżącą temperaturę, dzisiejsze maksimum i minimum oraz czas zapisu prognozy.
 * Do celu bierzemy prognozowane MAKSIMUM dnia, bo to ono mówi o obciążeniu cieplnym, a nie
 * temperatura z chwili odczytu (rano zwykle chłodniejsza).
 */
data class OutdoorReading(val nowC: Float, val maxC: Float?, val minC: Float?, val updatedAtMs: Long, val readAtMs: Long) {
    /** Do liczenia upału: maksimum dnia, a gdy go brak — temperatura bieżąca. */
    val heatC: Float get() = maxC ?: nowC

    companion object {
        /** Odczyt starszy niż to nie jest już wiarygodny (np. Homebrew Weather od dawna nie odświeża). */
        const val MAX_AGE_MS = 36L * 60 * 60 * 1000

        /** Zakres rozsądnych temperatur zewnętrznych; wszystko poza nim to błąd danych. */
        val SANE_RANGE = -60f..60f

        fun sanitized(nowC: Double?, maxC: Double?, minC: Double?, updatedAtMs: Long, readAtMs: Long): OutdoorReading? {
            val n = nowC?.toFloat()?.takeIf { it.isFinite() && it in SANE_RANGE } ?: return null
            val mx = maxC?.toFloat()?.takeIf { it.isFinite() && it in SANE_RANGE }
            val mn = minC?.toFloat()?.takeIf { it.isFinite() && it in SANE_RANGE }
            return OutdoorReading(n, mx, mn, updatedAtMs, readAtMs)
        }
    }

    /**
     * Czy odczyt dalej się nadaje. Liczy się wiek prognozy (kiedy Homebrew Weather ją pobrał); stary zapis bez
     * znacznika czasu ma 0, więc wtedy liczymy od chwili, w której Kropi go odczytał.
     */
    fun isFresh(nowMs: Long): Boolean {
        val basis = if (updatedAtMs > 0) updatedAtMs else readAtMs
        return nowMs - basis in 0..MAX_AGE_MS
    }
}
