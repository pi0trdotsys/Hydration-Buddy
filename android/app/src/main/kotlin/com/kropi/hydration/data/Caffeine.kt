package com.kropi.hydration.data

/**
 * Kofeina i alkohol: dwie rzeczy, które sam współczynnik napoju opisuje zbyt grubo.
 *
 * Kofeina — umiarkowane ilości (do ok. 4 filiżanek kawy dziennie) nie odwadniają w sposób mierzalny
 * (Killer i in. 2014), dlatego kawa i herbata liczą się według współczynnika z [Drink]. Dopiero powyżej
 * 400 mg dziennie (granica uznana przez EFSA 2015 za bezpieczną dla dorosłych) działanie moczopędne
 * zaczyna się liczyć, więc nadwyżka kofeiny zalicza się do celu tylko w połowie ([EXCESS_FACTOR]).
 *
 * Alkohol nie nawadnia (współczynnik 0), a do tego zwiększa diurezę. Badania nad rehydratacją
 * (Shirreffs i Maughan 1997) mówią, że żeby wyrównać straty, trzeba wypić wyraźnie więcej niż się
 * straciło. Dlatego każdy mililitr alkoholu podnosi dzisiejszy cel o pół mililitra wody
 * ([ALCOHOL_COMPENSATION]), do [ALCOHOL_COMPENSATION_CAP_ML]. To przybliżenie, nie pomiar.
 */
object Caffeine {
    const val DAILY_LIMIT_MG = 400
    const val EXCESS_FACTOR = 0.5f

    /** Orientacyjna zawartość kofeiny w 100 ml (parzona kawa, czarna herbata, cola). */
    fun mgPer100ml(drink: Drink): Int = when (drink) {
        Drink.COFFEE -> 45
        Drink.TEA -> 20
        Drink.SODA -> 10
        else -> 0
    }

    fun mg(drink: Drink, ml: Int): Int = ml * mgPer100ml(drink) / 100

    /**
     * Ile mililitrów z każdego wpisu jednej doby zalicza się do celu — wynik w tej samej kolejności co [entries].
     * Kolejność liczenia kofeiny to godzina wpisu (przy remisie kolejność na liście).
     */
    fun countedForDay(entries: List<RawIntake>, net: Boolean): List<Int> {
        val result = IntArray(entries.size)
        var running = 0
        for (index in entries.indices.sortedWith(compareBy({ entries[it].minuteOfDay }, { it }))) {
            val e = entries[index]
            val base = e.drink.counted(e.ml, net)
            val mg = mg(e.drink, e.ml)
            if (!net || mg <= 0) {
                result[index] = base
                continue
            }
            val over = (running + mg - DAILY_LIMIT_MG).coerceIn(0, mg)
            val share = over.toFloat() / mg
            result[index] = Math.round(base * (1f - share * (1f - EXCESS_FACTOR)))
            running += mg
        }
        return result.toList()
    }

    fun totalMg(entries: List<RawIntake>): Int = entries.sumOf { mg(it.drink, it.ml) }
}

/** Wpis w postaci potrzebnej do liczenia „ile się liczy" bez zależności od bazy i UI. */
data class RawIntake(val minuteOfDay: Int, val ml: Int, val drink: Drink)

object Alcohol {
    const val ALCOHOL_COMPENSATION = 0.5f
    const val ALCOHOL_COMPENSATION_CAP_ML = 600

    fun compensationMl(alcoholMl: Int): Int =
        (alcoholMl * ALCOHOL_COMPENSATION).toInt().coerceIn(0, ALCOHOL_COMPENSATION_CAP_ML)
}

/**
 * „Liczone ml" dla każdego wpisu z listy (w tej samej kolejności), z uwzględnieniem kofeiny ponad 400 mg
 * dziennie. Wpisy mogą pochodzić z wielu dni; każdy dzień liczy się osobno.
 */
fun List<IntakeEntity>.countedList(net: Boolean): List<Int> {
    val out = IntArray(size)
    indices.groupBy { this[it].epochDay }.values.forEach { dayIdx ->
        val counted = Caffeine.countedForDay(dayIdx.map { RawIntake(this[it].minuteOfDay, this[it].ml, Drink.of(this[it].drink)) }, net)
        dayIdx.forEachIndexed { k, i -> out[i] = counted[k] }
    }
    return out.toList()
}
