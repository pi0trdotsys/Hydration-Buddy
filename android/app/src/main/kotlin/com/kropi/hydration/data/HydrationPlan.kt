package com.kropi.hydration.data

import java.time.LocalTime
import kotlin.math.ceil

/** Jedna zaplanowana porcja: "wypij tyle o tej godzinie". */
data class PlannedSip(val time: LocalTime, val ml: Int)

enum class PlanStatus {
    /** Cel na dziś już zrobiony — nie ma czego planować. */
    DONE,

    /** Reszta dnia mieści się w oknie picia przy ustawionej szklance. */
    ON_TRACK,

    /** Do końca okna trzeba pić większymi porcjami niż ustawiona szklanka. */
    TIGHT,

    /** Okno picia już się zamknęło — plan jest "po godzinach". */
    AFTER_HOURS,
}

/**
 * Konkretny rozkład picia do końca dnia: ile jeszcze porcji, po ile i o której,
 * żeby domknąć dzienny cel. To z tego powstaje treść powiadomienia
 * ("wypij szklankę o 15:10 i 16:25, a osiągniesz cel") oraz linijka
 * "następna szklanka" na widgecie.
 */
data class HydrationPlan(
    val sips: List<PlannedSip>,
    val totalMl: Int,
    val goalMl: Int,
    val remainingMl: Int,
    val windowEnd: LocalTime,
    val status: PlanStatus,
) {
    val next: PlannedSip? get() = sips.firstOrNull()
    val finishAt: LocalTime? get() = sips.lastOrNull()?.time
    val portionMl: Int get() = sips.firstOrNull()?.ml ?: 0
}

/** Odstęp, poniżej którego przypominanie o kolejnej szklance przestaje mieć sens. */
private const val MIN_SPACING_MINUTES = 20

/** Pierwsza porcja nie jest "natychmiast", tylko za chwilę — łatwiej ją zrealizować. */
private const val LEAD_MINUTES = 10

/** Ostatnia porcja przed samym końcem okna, żeby nie wypadła na styk. */
private const val TAIL_BUFFER_MINUTES = 10

private const val LAST_PLANNABLE_MINUTE = 23 * 60 + 55

private fun roundUpTo(value: Int, step: Int): Int = ((value + step - 1) / step) * step

/**
 * Rozkłada brakujące mililitry na równe porcje w pozostałym oknie picia.
 *
 * Domyślnie porcją jest szklanka z ustawień; jeśli tyle porcji nie zmieści się
 * już w oknie przy sensownym odstępie, plan skraca liczbę porcji i powiększa
 * każdą z nich (status [PlanStatus.TIGHT]), zamiast obiecywać cel, którego nie
 * da się zrealizować.
 */
fun HydrationState.plan(now: LocalTime = LocalTime.now()): HydrationPlan {
    val endHour = settings.activeEndHour.coerceIn(0, 23)
    val windowEnd = LocalTime.of(endHour, 0)
    if (remaining <= 0) {
        return HydrationPlan(emptyList(), total, goal, 0, windowEnd, PlanStatus.DONE)
    }

    val nowMinute = now.hour * 60 + now.minute
    val startMinute = settings.activeStartHour.coerceIn(0, 23) * 60
    val endMinute = endHour * 60
    // Przed otwarciem okna picia plan zaczyna się od jego startu, a nie "za 10 minut" —
    // inaczej o 6:30 rano Kropi kazałby pić o 6:40.
    val firstMinute = maxOf(roundUpTo(nowMinute + LEAD_MINUTES, 5), startMinute)
        .coerceAtMost(LAST_PLANNABLE_MINUTE)
    val afterHours = endMinute - firstMinute < MIN_SPACING_MINUTES
    val lastMinute = if (afterHours) {
        (firstMinute + 60).coerceAtMost(LAST_PLANNABLE_MINUTE)
    } else {
        (endMinute - TAIL_BUFFER_MINUTES).coerceIn(firstMinute, LAST_PLANNABLE_MINUTE)
    }
    val span = lastMinute - firstMinute

    val glassMl = settings.reminderGlassMl.coerceAtLeast(50)
    val maxSips = (span / MIN_SPACING_MINUTES) + 1
    val wantedSips = ceil(remaining.toDouble() / glassMl).toInt().coerceAtLeast(1)
    val tight = wantedSips > maxSips
    val sipCount = if (tight) maxSips.coerceAtLeast(1) else wantedSips

    val portionMl = if (sipCount == 1) {
        roundUpTo(remaining, 10)
    } else {
        roundUpTo(ceil(remaining.toDouble() / sipCount).toInt(), 50)
    }
    val step = if (sipCount == 1) 0 else span / (sipCount - 1)

    val sips = (0 until sipCount).map { index ->
        val minute = roundUpTo(firstMinute + step * index, 5).coerceAtMost(LAST_PLANNABLE_MINUTE)
        PlannedSip(LocalTime.of(minute / 60, minute % 60), portionMl)
    }

    val status = when {
        afterHours -> PlanStatus.AFTER_HOURS
        tight -> PlanStatus.TIGHT
        else -> PlanStatus.ON_TRACK
    }
    return HydrationPlan(sips, total, goal, remaining, windowEnd, status)
}

/** "1 250 ml" — spacja jako separator tysięcy, tak jak w polskiej typografii. */
fun formatMl(ml: Int): String {
    val digits = ml.toString()
    if (digits.length <= 3) return "$digits ml"
    val head = digits.length % 3
    val groups = buildList {
        if (head > 0) add(digits.substring(0, head))
        for (i in head until digits.length step 3) add(digits.substring(i, i + 3))
    }
    return groups.joinToString(" ") + " ml"
}

fun LocalTime.hhmm(): String = String.format("%02d:%02d", hour, minute)

/** "15:10, 16:25 i 17:40" — z przycięciem, gdy porcji jest dużo. */
fun HydrationPlan.timesText(limit: Int = 5): String {
    if (sips.isEmpty()) return ""
    val shown = sips.take(limit).map { it.time.hhmm() }
    val truncated = sips.size > limit
    return when {
        truncated -> shown.joinToString(", ") + " … " + sips.last().time.hhmm()
        shown.size == 1 -> shown.first()
        else -> shown.dropLast(1).joinToString(", ") + " i " + shown.last()
    }
}


/**
 * Linijka w nagłówku widgetu: gdzie powinieneś być o tej porze i kiedy wypada
 * najbliższa porcja. Współdzielona przez widget Glance i jego podgląd w aplikacji.
 */
fun HydrationState.paceLine(plan: HydrationPlan): String = when (plan.status) {
    PlanStatus.DONE -> "Plan dnia wykonany"
    PlanStatus.AFTER_HOURS -> "Po godzinach • brakuje ${formatMl(remaining)}"
    else -> "Do teraz: ${formatMl(targetSoFar)} • nast. ${plan.next?.time?.hhmm()}"
}

/** Podpis pod wykresem: ile porcji, po ile i o której domykają cel. */
fun HydrationPlan.summaryLine(): String = when (status) {
    PlanStatus.DONE -> "Cel osiągnięty — dalej pij, ile masz ochotę 🎉"
    PlanStatus.AFTER_HOURS -> "Okno picia zamknięte. Brakuje ${formatMl(remainingMl)} — dopij, ile dasz radę."
    else -> "${sips.size} × ${formatMl(portionMl)} o ${timesText(limit = 2)} → cel ${formatMl(goalMl)}"
}

/** Pełny opis planu na kartę w aplikacji: ile porcji, o której i co to daje. */
fun HydrationPlan.detailText(): String = when (status) {
    PlanStatus.DONE ->
        "Cel ${formatMl(goalMl)} masz z głowy — wypite ${formatMl(totalMl)}. " +
            "Kolejne łyki liczą się już tylko na plus."
    PlanStatus.AFTER_HOURS ->
        "Okno picia zamknięte o ${windowEnd.hhmm()}, a do celu brakuje ${formatMl(remainingMl)}. " +
            "Dopij tyle, ile Ci pasuje — picie litra tuż przed snem i tak skończy się nocną pobudką."
    PlanStatus.TIGHT ->
        "Do ${windowEnd.hhmm()} zostało mało czasu, więc porcje są większe niż Twoja szklanka: " +
            "${sips.size} × ${formatMl(portionMl)} o ${timesText()}. " +
            "Pij spokojnie — to dalej domyka cel ${formatMl(goalMl)}."
    PlanStatus.ON_TRACK ->
        "Brakuje ${formatMl(remainingMl)}. Wypij ${sips.size} × ${formatMl(portionMl)} " +
            "o ${timesText()} — ostatnia porcja o ${finishAt?.hhmm()} domyka cel ${formatMl(goalMl)} " +
            "przed ${windowEnd.hhmm()}."
}
