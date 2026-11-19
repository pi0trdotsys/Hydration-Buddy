package com.kropi.hydration.data

import kotlin.math.roundToInt

/** Czysta logika zamiany danych z Health Connect na ustawienia celu. */
object HealthMapping {
    /** Średnia dzienna liczba kroków -> poziom aktywności używany w kalkulatorze celu. */
    fun activityFromSteps(averageDailySteps: Int): ActivityLevel = when {
        averageDailySteps < 5_000 -> ActivityLevel.LOW
        averageDailySteps < 9_000 -> ActivityLevel.MEDIUM
        else -> ActivityLevel.HIGH
    }

    /** Waga w sensownym zakresie (30–200 kg) zaokrąglona do 1 kg; nonsens z czujnika odrzucamy. */
    fun sanitizeWeight(kg: Double?): Float? =
        kg?.takeIf { it in 30.0..200.0 }?.roundToInt()?.toFloat()

    /** Ile mililitrów zapisać do Health Connect: liczy się „nawodnienie", więc alkohol daje 0 i jest pomijany. */
    fun hydrationMl(drink: Drink, ml: Int): Int = drink.counted(ml, net = true)
}
