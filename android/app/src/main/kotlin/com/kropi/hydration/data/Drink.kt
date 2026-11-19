package com.kropi.hydration.data

/**
 * Rodzaje napojów i to, w jakim stopniu „liczą się" do dziennego celu.
 *
 * Współczynniki to uproszczenie (kawa czy herbata w praktyce prawie w pełni
 * nawadniają), ale pozwalają odróżnić szklankę wody od szklanki soku czy piwa.
 * Użytkownik może je wyłączyć w Ustawieniach — wtedy każdy napój liczy się 1:1.
 */
enum class Drink(val label: String, val emoji: String, val factor: Float) {
    WATER("Woda", "💧", 1.0f),
    TEA("Herbata", "🍵", 0.9f),
    COFFEE("Kawa", "☕", 0.8f),
    JUICE("Sok", "🧃", 0.85f),
    MILK("Mleko", "🥛", 0.9f),
    SODA("Napój gazowany", "🥤", 0.7f),
    ALCOHOL("Alkohol", "🍺", 0.0f);

    /** „liczy się w 80%" — do podpisów w UI. */
    val percent: Int get() = Math.round(factor * 100)

    /** Ile mililitrów z [ml] zalicza się do celu. */
    fun counted(ml: Int, net: Boolean = true): Int = if (net) Math.round(ml * factor) else ml

    companion object {
        fun of(name: String?): Drink = entries.firstOrNull { it.name == name } ?: WATER
    }
}
