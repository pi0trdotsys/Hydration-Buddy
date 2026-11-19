package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NextSipCountdownTest {
    private fun h(hour: Int, minute: Int = 0) = hour * 60 + minute

    @Test
    fun halfwayBetweenLastSipAndNext() {
        // ostatni łyk 12:00, następny 14:00, jest 13:00
        assertEquals(0.5f, NextSipCountdown.fraction(h(13), h(12), h(8), h(14)), 0.001f)
    }

    @Test
    fun justAfterASipTheBarIsEmpty() {
        assertEquals(0f, NextSipCountdown.fraction(h(12), h(12), h(8), h(14)), 0.001f)
    }

    @Test
    fun withoutAnySipItCountsFromWindowStart() {
        // okno od 8:00, następna porcja 12:00, jest 10:00
        assertEquals(0.5f, NextSipCountdown.fraction(h(10), null, h(8), h(12)), 0.001f)
    }

    @Test
    fun overdueSipFillsTheBar() {
        assertEquals(1f, NextSipCountdown.fraction(h(15), h(12), h(8), h(14)), 0.001f)
    }

    @Test
    fun noRoomBetweenAnchorAndNextMeansFull() {
        assertEquals(1f, NextSipCountdown.fraction(h(12), h(14), h(8), h(12)), 0.001f)
    }

    @Test
    fun beforeTheWindowOpensStaysEmpty() {
        assertEquals(0f, NextSipCountdown.fraction(h(6), null, h(8), h(8)), 0.001f)
    }
}
