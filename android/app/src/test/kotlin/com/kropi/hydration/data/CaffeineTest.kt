package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaffeineTest {
    private fun e(minute: Int, ml: Int, d: Drink) = RawIntake(minute, ml, d)

    @Test
    fun moderateCoffeeCountsByItsOwnFactor() {
        // 3 × 200 ml = 270 mg < 400 mg → bez kary, współczynnik kawy 0,8.
        val counted = Caffeine.countedForDay(List(3) { e(600 + it * 60, 200, Drink.COFFEE) }, net = true)
        assertEquals(listOf(160, 160, 160), counted)
    }

    @Test
    fun caffeineOverTheLimitCountsHalf() {
        // 5 × 200 ml kawy = 450 mg: piąta filiżanka przekracza 400 mg o 50 mg z 90 → część nadwyżki w połowie.
        val counted = Caffeine.countedForDay(List(5) { e(480 + it * 30, 200, Drink.COFFEE) }, net = true)
        assertEquals(listOf(160, 160, 160, 160), counted.take(4))
        assertTrue(counted[4] < 160 && counted[4] > 80)
    }

    @Test
    fun everyCupAfterTheLimitIsHalved() {
        val counted = Caffeine.countedForDay(List(7) { e(480 + it * 30, 200, Drink.COFFEE) }, net = true)
        assertEquals(80, counted[6])
    }

    @Test
    fun orderIsByTimeOfDayNotByListPosition() {
        // Ta sama kawa wpisana „na końcu listy", ale rano — nie jest tą, która przekracza limit.
        val late = List(5) { e(700 + it, 200, Drink.COFFEE) }
        val morning = e(300, 200, Drink.COFFEE)
        val counted = Caffeine.countedForDay(late + morning, net = true)
        assertEquals(160, counted.last())
        assertTrue(counted[4] < 160)
    }

    @Test
    fun netOffIgnoresCaffeineEntirely() {
        val counted = Caffeine.countedForDay(List(8) { e(480 + it, 200, Drink.COFFEE) }, net = false)
        assertTrue(counted.all { it == 200 })
    }

    @Test
    fun waterNeverGetsAPenalty() {
        val entries = List(8) { e(480 + it, 200, Drink.COFFEE) } + e(900, 500, Drink.WATER)
        assertEquals(500, Caffeine.countedForDay(entries, net = true).last())
    }

    @Test
    fun caffeineEstimates() {
        assertEquals(90, Caffeine.mg(Drink.COFFEE, 200))
        assertEquals(0, Caffeine.mg(Drink.WATER, 1000))
        assertEquals(33, Caffeine.mg(Drink.SODA, 330))
    }

    @Test
    fun alcoholAddsHalfItsVolumeToTheGoalUpToTheCap() {
        assertEquals(165, Alcohol.compensationMl(330))
        assertEquals(0, Alcohol.compensationMl(0))
        assertEquals(Alcohol.ALCOHOL_COMPENSATION_CAP_ML, Alcohol.compensationMl(5000))
    }

    @Test
    fun entitiesAreCountedPerDayAndKeepTheirOrder() {
        fun ent(day: Long, minute: Int, ml: Int, d: Drink) = IntakeEntity(epochDay = day, minuteOfDay = minute, ml = ml, drink = d.name)
        val list = List(6) { ent(1, 480 + it * 10, 200, Drink.COFFEE) } + ent(2, 480, 200, Drink.COFFEE)
        val counted = list.countedList(net = true)
        assertEquals(7, counted.size)
        assertEquals(160, counted[6]) // inny dzień — kofeina liczy się od zera
        assertTrue(counted[5] < 160) // szósta kawa pierwszego dnia
    }
}
