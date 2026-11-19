package com.kropi.hydration.data

/** Styl kolorów widgetu: własna paleta Kropi albo kolory z tapety (Material You). */
enum class WidgetStyle(val label: String, val hint: String) {
    KROPI("Kropi", "Własna paleta, jasna lub ciemna zależnie od systemu."),
    KROPI_DARK("Kropi ciemny", "Własna paleta, zawsze ciemna — tak wyglądał widget do tej pory."),
    MATERIAL_YOU("Material You", "Kolory z tapety (Android 12+); na starszych systemach paleta Kropi."),
}

/**
 * Jeden przycisk dolewania na widgecie. [drink] = null oznacza „napój wybrany na widgecie"
 * (domyślnie woda) — tak działają zwykłe butelki; wpisany napój to stały przycisk,
 * np. „kawa 200", który zapisuje dokładnie to jednym dotknięciem.
 */
data class ButtonSpec(val drink: Drink?, val ml: Int) {
    val isPlain: Boolean get() = drink == null || drink == Drink.WATER

    fun encode(): String = "${drink?.name ?: "-"}:$ml"

    companion object {
        fun decode(raw: String): ButtonSpec? {
            val parts = raw.split(":")
            if (parts.size != 2) return null
            val ml = parts[1].toIntOrNull()?.takeIf { it in MIN_ML..MAX_ML } ?: return null
            val drink = if (parts[0] == "-") null else Drink.entries.firstOrNull { it.name == parts[0] } ?: return null
            return ButtonSpec(drink, ml)
        }

        const val MIN_ML = 10
        const val MAX_ML = 3000
    }
}

/**
 * Ustawienia jednego widgetu na ekranie głównym: cztery przyciski i styl kolorów.
 * Zapisywane jako `STYL|drink:ml,drink:ml,drink:ml,drink:ml`; brak zapisu = domyślne
 * butelki z Ustawień, więc istniejące widgety działają bez konfiguracji.
 */
data class WidgetConfig(val slots: List<ButtonSpec>, val style: WidgetStyle = WidgetStyle.KROPI) {

    fun encode(): String = style.name + "|" + slots.joinToString(",") { it.encode() }

    companion object {
        const val SLOT_COUNT = 4

        fun default(bottlesMl: List<Int>, style: WidgetStyle = WidgetStyle.KROPI) =
            WidgetConfig(normalize(bottlesMl.map { ButtonSpec(null, it) }), style)

        /** Zawsze dokładnie [SLOT_COUNT] przycisków — brakujące uzupełniamy butelkami 250 ml. */
        private fun normalize(slots: List<ButtonSpec>): List<ButtonSpec> =
            (slots.take(SLOT_COUNT) + List((SLOT_COUNT - slots.size).coerceAtLeast(0)) { ButtonSpec(null, 250) })

        fun decode(raw: String?, bottlesMl: List<Int>): WidgetConfig {
            if (raw.isNullOrBlank()) return default(bottlesMl)
            val parts = raw.split("|", limit = 2)
            val style = WidgetStyle.entries.firstOrNull { it.name == parts[0] } ?: WidgetStyle.KROPI
            val slots = parts.getOrNull(1)?.split(",")?.mapNotNull { ButtonSpec.decode(it) }.orEmpty()
            return if (slots.size == SLOT_COUNT) WidgetConfig(slots, style) else default(bottlesMl, style)
        }
    }
}

/** Zapamiętany ostatnio cofnięty łyk, który widget pozwala przywrócić. */
data class UndoInfo(val ml: Int, val drink: Drink, val atEpochMs: Long) {
    /** Czy „przywróć" jest jeszcze na widgecie (cofnięcie sprzed chwili). */
    fun isFresh(nowMs: Long = System.currentTimeMillis()): Boolean = nowMs - atEpochMs in 0..UNDO_WINDOW_MS

    companion object {
        const val UNDO_WINDOW_MS = 10_000L
    }
}

/** Kiedy następne przewinięcie doby. */
object MidnightSchedule {
    /** Milisekundy do najbliższej północy plus kilka sekund zapasu, żeby już była „nowa" data. */
    fun delayMs(now: java.time.LocalDateTime = java.time.LocalDateTime.now()): Long =
        java.time.Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay().plusSeconds(5)).toMillis()
}
