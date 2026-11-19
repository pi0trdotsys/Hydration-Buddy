package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HealthMappingTest {
    @Test
    fun stepsMapToActivityLevels() {
        assertEquals(ActivityLevel.LOW, HealthMapping.activityFromSteps(0))
        assertEquals(ActivityLevel.LOW, HealthMapping.activityFromSteps(4_999))
        assertEquals(ActivityLevel.MEDIUM, HealthMapping.activityFromSteps(5_000))
        assertEquals(ActivityLevel.MEDIUM, HealthMapping.activityFromSteps(8_999))
        assertEquals(ActivityLevel.HIGH, HealthMapping.activityFromSteps(9_000))
        assertEquals(ActivityLevel.HIGH, HealthMapping.activityFromSteps(25_000))
    }

    @Test
    fun weightIsRoundedAndNonsenseRejected() {
        assertEquals(72f, HealthMapping.sanitizeWeight(72.4)!!, 0f)
        assertEquals(73f, HealthMapping.sanitizeWeight(72.5)!!, 0f)
        assertNull(HealthMapping.sanitizeWeight(null))
        assertNull(HealthMapping.sanitizeWeight(0.0))
        assertNull(HealthMapping.sanitizeWeight(12.0))
        assertNull(HealthMapping.sanitizeWeight(400.0))
    }

    @Test
    fun hydrationCountsOnlyWhatHydrates() {
        assertEquals(250, HealthMapping.hydrationMl(Drink.WATER, 250))
        assertEquals(200, HealthMapping.hydrationMl(Drink.COFFEE, 250))
        assertEquals(0, HealthMapping.hydrationMl(Drink.ALCOHOL, 500))
    }

    @Test
    fun betterActivityRaisesTheCalculatedGoal() {
        val low = GoalCalculator.calculate(70f, ActivityLevel.LOW, Temperature.MILD)
        val high = GoalCalculator.calculate(70f, ActivityLevel.HIGH, Temperature.MILD)
        assert(high > low)
    }
}
