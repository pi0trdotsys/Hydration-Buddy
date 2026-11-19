package com.kropi.hydration.data

import kotlin.math.roundToInt

enum class Temperature(private val labelPl: String, private val labelEn: String, val emoji: String) {
    COLD("Chłodno", "Cool", "❄️"),
    MILD("Umiarkowanie", "Mild", "🌤️"),
    HOT("Upał", "Hot", "🔥");

    val label: String get() = tr(labelPl, labelEn)
}

enum class ActivityLevel(private val labelPl: String, private val labelEn: String, val emoji: String) {
    LOW("Niska", "Low", "🛋️"),
    MEDIUM("Średnia", "Medium", "🚶"),
    HIGH("Wysoka", "High", "🏃");

    val label: String get() = tr(labelPl, labelEn)
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
    val notificationTone: NotificationTone = NotificationTone.SNARKY,
    val snarkIntensity: SnarkIntensity = SnarkIntensity.NORMAL,
    /** Rozkładaj porcje w godzinach, w których naprawdę pijesz, zamiast równo co tyle samo. */
    val adaptivePlan: Boolean = true,
    /** Liczy napoje wg współczynnika nawodnienia (kawa 80%, sok 85%…); wyłączone = każdy 1:1. */
    val netHydration: Boolean = true,
    /** Nie wysyłaj przypomnień, gdy telefon ma włączone „Nie przeszkadzać". */
    val respectDnd: Boolean = true,
    /** Nie wysyłaj przypomnień podczas spotkań z kalendarza (wymaga zgody na odczyt kalendarza). */
    val quietDuringMeetings: Boolean = false,
    /** Zapisuj wypite napoje do Health Connect (jako nawodnienie). */
    val healthWriteHydration: Boolean = false,
    /** Waga i aktywność (kroki) z Health Connect zamiast ręcznych ustawień celu. */
    val healthSyncProfile: Boolean = false,
    /** Cztery pojemności na kafelkach widgetu i w aplikacji — do przestawienia w Ustawieniach. */
    val bottlesMl: List<Int> = listOf(100, 250, 500, 750),
) {
    val activeHours: Int get() = (activeEndHour - activeStartHour).coerceAtLeast(1)
    val goalBreakdown: GoalBreakdown get() = GoalCalculator.breakdown(weightKg, activity, temperature, activeHours)
    val calculatedGoalMl: Int get() = goalBreakdown.goalMl
    val effectiveGoalMl: Int get() = if (goalMode == GoalMode.AUTO) calculatedGoalMl else manualGoalMl
}

/**
 * Z czego wzięła się liczba: baza z wagi, dodatek za aktywność i za upał, a na końcu ewentualne
 * przycięcie do tego, co da się realnie wypić. Pokazywane w Ustawieniach, żeby cel nie był czarną skrzynką.
 */
data class GoalBreakdown(
    val baseMl: Int,
    val activityMl: Int,
    val heatMl: Int,
    /** Suma przed zaokrągleniem i przycięciem. */
    val rawMl: Int,
    val goalMl: Int,
    val cappedBy: GoalCap?,
)

/** Dlaczego cel jest niższy albo wyższy niż suma składników. */
enum class GoalCap { FLOOR, CEILING, PACE }

/**
 * Dzienny cel NAPOJÓW (nie całej wody z jedzeniem).
 *
 * Podstawa — zalecenia dla dorosłych: EFSA (2010) podaje 2,0 l wody dziennie dla kobiet i 2,5 l dla
 * mężczyzn, NASEM (2004) odpowiednio 2,7 i 3,7 l. W przeliczeniu na masę ciała to ok. 35 ml/kg wody
 * łącznie. Mniej więcej 20% tej wody dostarcza jedzenie (zupy, owoce, warzywa), więc z napojów
 * potrzebujemy ok. 80% → [BASE_ML_PER_KG] = 28 ml/kg. (Wcześniejsze 33 ml/kg liczyło całą wodę, ale
 * przypisywało ją samym napojom — stąd cele rzędu 2,7 l, trudne do wypicia.)
 *
 * Dodatki: ruch (ACSM 2007: pot to 0,5–1 l na godzinę umiarkowanego wysiłku, a nawadniać trzeba
 * dzień po dniu) i upał (zapotrzebowanie rośnie o kilkanaście procent).
 *
 * „Realne do wykonania": cel nie przekracza tempa [MAX_PACE_ML_PER_HOUR] na godzinę aktywnego okna
 * (żeby nie wymagać pijackich porcji — nerki wydalają ok. 0,8–1 l/h, a więcej naraz zwiększa ryzyko
 * hiponatremii) ani sufitu [CEILING_ML]; zbyt niski wynik podnosimy do [FLOOR_ML].
 */
object GoalCalculator {
    const val BASE_ML_PER_KG = 28f
    private const val ACTIVITY_MEDIUM_ML_PER_KG = 4f
    private const val ACTIVITY_HIGH_ML_PER_KG = 8f
    private const val HEAT_ML_PER_KG = 7f
    const val FLOOR_ML = 1200
    const val CEILING_ML = 4000
    const val MAX_PACE_ML_PER_HOUR = 300

    /** Aktywne okno w godzinach: bez absurdalnie krótkich, bo plan i tak potrzebuje czasu na kilka porcji. */
    private const val MIN_WINDOW_HOURS = 6

    fun breakdown(weightKg: Float, activity: ActivityLevel, temperature: Temperature, activeHours: Int = 14): GoalBreakdown {
        val base = weightKg * BASE_ML_PER_KG
        val activityBonus = weightKg * when (activity) {
            ActivityLevel.LOW -> 0f
            ActivityLevel.MEDIUM -> ACTIVITY_MEDIUM_ML_PER_KG
            ActivityLevel.HIGH -> ACTIVITY_HIGH_ML_PER_KG
        }
        val heatBonus = if (temperature == Temperature.HOT) weightKg * HEAT_ML_PER_KG else 0f
        val raw = base + activityBonus + heatBonus
        val pace = activeHours.coerceAtLeast(MIN_WINDOW_HOURS) * MAX_PACE_ML_PER_HOUR
        val ceiling = minOf(CEILING_ML, pace)
        var capped: GoalCap? = null
        var goal = roundTo50(raw)
        if (goal > ceiling) {
            goal = ceiling - ceiling % 50
            capped = if (ceiling == pace && pace < CEILING_ML) GoalCap.PACE else GoalCap.CEILING
        } else if (goal < FLOOR_ML) {
            goal = FLOOR_ML
            capped = GoalCap.FLOOR
        }
        return GoalBreakdown(base.roundToInt(), activityBonus.roundToInt(), heatBonus.roundToInt(), raw.roundToInt(), goal, capped)
    }

    fun calculate(weightKg: Float, activity: ActivityLevel, temperature: Temperature, activeHours: Int = 14): Int =
        breakdown(weightKg, activity, temperature, activeHours).goalMl

    private fun roundTo50(ml: Float): Int = (Math.round(ml / 50f) * 50)
}
