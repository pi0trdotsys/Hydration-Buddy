package com.kropi.hydration.data

import kotlin.math.roundToInt

enum class Temperature(val label: String, val emoji: String) {
    COLD("Chłodno", "❄️"),
    MILD("Umiarkowanie", "🌤️"),
    HOT("Upał", "🔥"),
}

enum class ActivityLevel(val label: String, val emoji: String) {
    LOW("Niska", "🛋️"),
    MEDIUM("Średnia", "🚶"),
    HIGH("Wysoka", "🏃"),
}

enum class GoalMode { AUTO, MANUAL }

data class HydrationSettings(
    val weightKg: Float = 70f,
    val temperature: Temperature = Temperature.MILD,
    val activity: ActivityLevel = ActivityLevel.MEDIUM,
    val goalMode: GoalMode = GoalMode.AUTO,
    val manualGoalMl: Int = 2500,
    val activeStartHour: Int = 8,
    val activeEndHour: Int = 22,
    val reminderGlassMl: Int = 250,
    val remindersEnabled: Boolean = true,
) {
    val calculatedGoalMl: Int get() = GoalCalculator.calculate(weightKg, activity, temperature)
    val effectiveGoalMl: Int get() = if (goalMode == GoalMode.AUTO) calculatedGoalMl else manualGoalMl
}

/**
 * Rough but sane daily water target: ~33 ml per kg of body weight (the same
 * "zasada kciuka" already quoted in FACTS), plus a bump for activity and heat.
 */
object GoalCalculator {
    fun calculate(weightKg: Float, activity: ActivityLevel, temperature: Temperature): Int {
        val base = weightKg * 33f
        val activityBonus = when (activity) {
            ActivityLevel.LOW -> 0f
            ActivityLevel.MEDIUM -> 350f
            ActivityLevel.HIGH -> 700f
        }
        val temperatureBonus = when (temperature) {
            Temperature.COLD -> -150f
            Temperature.MILD -> 0f
            Temperature.HOT -> 500f
        }
        return (base + activityBonus + temperatureBonus).roundToInt().coerceIn(1200, 5000)
    }
}
