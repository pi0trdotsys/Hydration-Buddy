package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrinkTest {
    @Test
    fun waterCountsFully() {
        assertEquals(250, Drink.WATER.counted(250))
    }

    @Test
    fun coffeeCountsEightyPercentWhenNet() {
        assertEquals(200, Drink.COFFEE.counted(250))
        assertEquals(80, Drink.COFFEE.percent)
    }

    @Test
    fun alcoholNeverCounts() {
        assertEquals(0, Drink.ALCOHOL.counted(500))
    }

    @Test
    fun netOffCountsEverythingOneToOne() {
        Drink.entries.forEach { assertEquals(330, it.counted(330, net = false)) }
    }

    @Test
    fun unknownNameFallsBackToWater() {
        assertEquals(Drink.WATER, Drink.of("NIEZNANY"))
        assertEquals(Drink.WATER, Drink.of(null))
        assertEquals(Drink.TEA, Drink.of("TEA"))
    }

    @Test
    fun factorsStayInRange() {
        assertTrue(Drink.entries.all { it.factor in 0f..1f })
    }
}
