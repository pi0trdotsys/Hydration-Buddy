package com.kropi.hydration.data

/** Sesja treningowa z Health Connect, bez zależności od jego klas. */
data class WorkoutSession(val id: String, val startMs: Long, val endMs: Long, val title: String? = null) {
    val minutes: Int get() = ((endMs - startMs) / 60_000L).toInt().coerceAtLeast(0)
}

/**
 * Trening a nawodnienie. ACSM (2007) szacuje pocenie na 0,5–1,0 l na godzinę umiarkowanego wysiłku
 * i zaleca nawadniać zarówno w trakcie, jak i po nim. Nie znamy intensywności ani warunków, więc
 * bierzemy środek widełek — [ML_PER_HOUR] — liczone od czasu trwania sesji (od [MIN_MINUTES] minut,
 * maksymalnie [MAX_ML_PER_DAY] dziennie).
 */
object Workout {
    const val ML_PER_HOUR = 600
    const val MIN_MINUTES = 15
    const val MAX_ML_PER_DAY = 1800

    /** Po tylu minutach od końca treningu przypomnienie przestaje mieć sens. */
    const val NUDGE_WINDOW_MINUTES = 90

    /** Dodatek do dzisiejszego celu za sesje [sessions] zakończone do [nowMs] (trwające liczone do teraz). */
    fun bonusMl(sessions: List<WorkoutSession>, nowMs: Long): Int {
        val minutes = sessions.sumOf { s ->
            val end = minOf(s.endMs, nowMs)
            val m = ((end - s.startMs) / 60_000L).toInt()
            if (m >= MIN_MINUTES) m else 0
        }
        val ml = minutes * ML_PER_HOUR / 60
        return (Math.round(ml / 50f) * 50).coerceAtMost(MAX_ML_PER_DAY)
    }

    /** Ile wypić po konkretnym treningu: tyle, ile ten trening „dołożył" do celu, ale nie więcej niż 750 ml naraz. */
    fun afterWorkoutMl(session: WorkoutSession): Int =
        (Math.round(session.minutes * ML_PER_HOUR / 60 / 50f) * 50).coerceIn(250, 750)

    /** Trening zakończony niedawno i wart przypomnienia (dość długi, jeszcze nieprzypomniany). */
    fun nudgeCandidate(sessions: List<WorkoutSession>, nowMs: Long, alreadyNotified: String?): WorkoutSession? =
        sessions
            .filter { it.id != alreadyNotified && it.endMs <= nowMs && it.minutes >= MIN_MINUTES }
            .filter { (nowMs - it.endMs) / 60_000L <= NUDGE_WINDOW_MINUTES }
            .maxByOrNull { it.endMs }
}
