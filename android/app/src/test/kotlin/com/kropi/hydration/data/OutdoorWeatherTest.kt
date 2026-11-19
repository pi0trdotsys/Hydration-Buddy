package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutdoorWeatherTest {
    private val hour = 3_600_000L

    @Test
    fun heatUsesTheForecastMaximumWhenThereIsOne() {
        assertEquals(31f, OutdoorReading.sanitized(18.0, 31.0, 14.0, 1, 1)!!.heatC, 0.001f)
        assertEquals(18f, OutdoorReading.sanitized(18.0, null, null, 1, 1)!!.heatC, 0.001f)
    }

    @Test
    fun nonsenseIsRejected() {
        assertNull(OutdoorReading.sanitized(null, 20.0, 10.0, 1, 1))
        assertNull(OutdoorReading.sanitized(Double.NaN, 20.0, 10.0, 1, 1))
        assertNull(OutdoorReading.sanitized(500.0, 20.0, 10.0, 1, 1))
        // max spoza zakresu nie psuje całego odczytu, tylko znika
        assertNull(OutdoorReading.sanitized(20.0, 999.0, 10.0, 1, 1)!!.maxC)
    }

    @Test
    fun freshnessIsMeasuredFromTheForecastTime() {
        val now = 100 * hour
        assertTrue(OutdoorReading(20f, 25f, 10f, now - 35 * hour, now).isFresh(now))
        assertFalse(OutdoorReading(20f, 25f, 10f, now - 37 * hour, now).isFresh(now))
    }

    @Test
    fun undatedForecastIsMeasuredFromWhenKropiReadIt() {
        val now = 100 * hour
        assertTrue(OutdoorReading(20f, 25f, 10f, 0, now - hour).isFresh(now))
        assertFalse(OutdoorReading(20f, 25f, 10f, 0, now - 40 * hour).isFresh(now))
    }
}
