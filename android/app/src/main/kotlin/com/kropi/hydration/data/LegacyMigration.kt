package com.kropi.hydration.data

/**
 * Do wersji 1.7 łyki dnia i historia leżały w DataStore jako tekst:
 * `intakes = "7:20:250,9:5:330"` i `history = "20770:2500:2500,…"`.
 * Ten obiekt zamienia je na wiersze bazy — czysta logika, bez Androida.
 */
object LegacyMigration {
    /** Wpis z historii bez pojedynczych łyków dostaje jeden zbiorczy łyk o tej porze. */
    const val SUMMARY_MINUTE = 12 * 60
    const val SUMMARY_NOTE = "z historii sprzed 1.8"

    data class Result(val intakes: List<IntakeEntity>, val dayGoals: List<DayGoalEntity>)

    fun parse(intakesRaw: String?, historyRaw: String?, currentEpochDay: Long?): Result {
        val intakes = mutableListOf<IntakeEntity>()
        val goals = mutableListOf<DayGoalEntity>()

        historyRaw?.takeIf { it.isNotBlank() }?.split(",")?.forEach { entry ->
            val p = entry.split(":")
            if (p.size != 3) return@forEach
            val day = p[0].toLongOrNull() ?: return@forEach
            val ml = p[1].toIntOrNull() ?: return@forEach
            val goal = p[2].toIntOrNull() ?: return@forEach
            goals += DayGoalEntity(day, goal)
            if (ml > 0) intakes += IntakeEntity(epochDay = day, minuteOfDay = SUMMARY_MINUTE, ml = ml, note = SUMMARY_NOTE)
        }

        if (currentEpochDay != null) {
            intakesRaw?.takeIf { it.isNotBlank() }?.split(",")?.forEach { entry ->
                val p = entry.split(":")
                if (p.size != 3) return@forEach
                val h = p[0].toIntOrNull() ?: return@forEach
                val m = p[1].toIntOrNull() ?: return@forEach
                val ml = p[2].toIntOrNull() ?: return@forEach
                intakes += IntakeEntity(epochDay = currentEpochDay, minuteOfDay = h.coerceIn(0, 23) * 60 + m.coerceIn(0, 59), ml = ml)
            }
        }
        return Result(intakes, goals.distinctBy { it.epochDay })
    }
}
