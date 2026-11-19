package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalCalculatorTest {
    private fun goal(kg: Float, a: ActivityLevel = ActivityLevel.LOW, t: Temperature = Temperature.MILD, hours: Int = 14) =
        GoalCalculator.calculate(kg, a, t, hours)

    @Test
    fun typicalAdultMatchesEfsaBeverageShare() {
        // 70 kg, mało ruchu: 28 ml/kg = 1960 → 1950. EFSA: 2,0–2,5 l wody łącznie, z czego ~80% z napojów.
        assertEquals(1950, goal(70f))
        // 60 kg i 85 kg (kobieta/mężczyzna "typowi") mieszczą się w widełkach 1,6–2,4 l z napojów.
        assertTrue(goal(60f) in 1600..1800)
        assertTrue(goal(85f) in 2300..2500)
    }

    @Test
    fun activityAndHeatAddProportionallyToWeight() {
        assertEquals(2250, goal(70f, ActivityLevel.MEDIUM))
        assertEquals(2500, goal(70f, ActivityLevel.HIGH))
        assertEquals(2450, goal(70f, ActivityLevel.LOW, Temperature.HOT))
        assertEquals(3000, goal(70f, ActivityLevel.HIGH, Temperature.HOT))
    }

    @Test
    fun coldDoesNotLowerTheGoal() {
        assertEquals(goal(70f, t = Temperature.MILD), goal(70f, t = Temperature.COLD))
    }

    @Test
    fun newDefaultIsNoLongerTheOldInflatedNumber() {
        // Dawniej 70 kg, średnia aktywność, umiarkowana temperatura = 2660 ml wody z napojów — za dużo.
        assertTrue(goal(70f, ActivityLevel.MEDIUM) < 2660)
    }

    @Test
    fun goalIsRoundedToFiftyMl() {
        for (kg in 40..120) for (a in ActivityLevel.entries) {
            assertEquals("${kg}kg $a", 0, goal(kg.toFloat(), a) % 50)
        }
    }

    @Test
    fun veryLightPeopleGetTheFloor() {
        val b = GoalCalculator.breakdown(35f, ActivityLevel.LOW, Temperature.MILD)
        assertEquals(GoalCalculator.FLOOR_ML, b.goalMl)
        assertEquals(GoalCap.FLOOR, b.cappedBy)
    }

    @Test
    fun heavyHotActiveGoalHitsTheCeiling() {
        val b = GoalCalculator.breakdown(150f, ActivityLevel.HIGH, Temperature.HOT)
        assertEquals(GoalCalculator.CEILING_ML, b.goalMl)
        assertEquals(GoalCap.CEILING, b.cappedBy)
    }

    @Test
    fun shortActiveWindowCapsTheGoalToARealisticPace() {
        // 8 h okna × 300 ml/h = 2400 ml, a wzór dałby 3000.
        val b = GoalCalculator.breakdown(70f, ActivityLevel.HIGH, Temperature.HOT, activeHours = 8)
        assertEquals(2400, b.goalMl)
        assertEquals(GoalCap.PACE, b.cappedBy)
    }

    @Test
    fun windowShorterThanSixHoursIsTreatedAsSix() {
        assertEquals(goal(70f, ActivityLevel.HIGH, Temperature.HOT, hours = 6), goal(70f, ActivityLevel.HIGH, Temperature.HOT, hours = 2))
    }

    @Test
    fun breakdownAddsUp() {
        val b = GoalCalculator.breakdown(70f, ActivityLevel.MEDIUM, Temperature.HOT)
        assertTrue(kotlin.math.abs(b.rawMl - (b.baseMl + b.activityMl + b.heatMl)) <= 1)
        assertNull(b.cappedBy)
        assertEquals(1960, b.baseMl)
        assertEquals(280, b.activityMl)
        assertEquals(490, b.heatMl)
    }

    @Test
    fun settingsUseTheirOwnWindowLength() {
        val wide = HydrationSettings(weightKg = 70f, activity = ActivityLevel.HIGH, temperature = Temperature.HOT, activeStartHour = 7, activeEndHour = 23)
        val narrow = wide.copy(activeStartHour = 12, activeEndHour = 20)
        assertTrue(narrow.calculatedGoalMl < wide.calculatedGoalMl)
        assertEquals(8, narrow.activeHours)
    }

    // --- 1.13: pogoda, korekta osobista, trening, alkohol ---

    private fun inputs(
        kg: Float = 70f, a: ActivityLevel = ActivityLevel.LOW, heat: Float = 0f, hours: Int = 14,
        personal: Int = 0, workout: Int = 0, alcohol: Int = 0,
    ) = GoalInputs(kg, a, heat, hours, personal, workout, alcohol)

    @Test
    fun heatGrowsSmoothlyWithTheForecastMaximum() {
        assertEquals(0f, GoalCalculator.heatFraction(20f), 0.001f)
        assertEquals(0f, GoalCalculator.heatFraction(GoalCalculator.HEAT_FROM_C), 0.001f)
        assertEquals(0.5f, GoalCalculator.heatFraction(28f), 0.001f)
        assertEquals(1f, GoalCalculator.heatFraction(GoalCalculator.HEAT_FULL_C), 0.001f)
        assertEquals(1f, GoalCalculator.heatFraction(41f), 0.001f)
        val warm = GoalCalculator.breakdown(inputs(heat = GoalCalculator.heatFraction(28f))).goalMl
        val hot = GoalCalculator.breakdown(inputs(heat = GoalCalculator.heatFraction(34f))).goalMl
        assertTrue(warm in 2000..2400 && hot > warm)
    }

    @Test
    fun manualHotEqualsAFullForecastHeat() {
        assertEquals(
            GoalCalculator.breakdown(70f, ActivityLevel.LOW, Temperature.HOT).goalMl,
            GoalCalculator.breakdown(inputs(heat = GoalCalculator.heatFraction(35f))).goalMl,
        )
    }

    @Test
    fun personalCorrectionMovesTheGoalByThatPercent() {
        val base = GoalCalculator.breakdown(inputs()).goalMl // 1950
        assertEquals(2050, GoalCalculator.breakdown(inputs(personal = 5)).goalMl) // 1960 × 1,05 = 2058
        assertTrue(GoalCalculator.breakdown(inputs(personal = -5)).goalMl < base)
        assertEquals(GoalCalculator.breakdown(inputs(personal = 10)).goalMl, GoalCalculator.breakdown(inputs(personal = 99)).goalMl)
        assertEquals(196, GoalCalculator.breakdown(inputs(personal = 10)).personalMl)
    }

    @Test
    fun workoutAndAlcoholComeAfterTheFeasibilityCap() {
        val capped = GoalCalculator.breakdown(inputs(kg = 110f, a = ActivityLevel.HIGH, heat = 1f, hours = 10))
        assertEquals(GoalCap.PACE, capped.cappedBy)
        val withExtras = GoalCalculator.breakdown(inputs(kg = 110f, a = ActivityLevel.HIGH, heat = 1f, hours = 10, workout = 600, alcohol = 150))
        assertEquals(capped.goalMl + 750, withExtras.goalMl)
        assertEquals(600, withExtras.workoutMl)
        assertEquals(150, withExtras.alcoholMl)
    }

    @Test
    fun totalNeverExceedsTheHardCeiling() {
        val b = GoalCalculator.breakdown(inputs(kg = 150f, a = ActivityLevel.HIGH, heat = 1f, hours = 16, workout = 1800, alcohol = 600))
        assertEquals(GoalCalculator.HARD_CEILING_ML, b.goalMl)
    }

    @Test
    fun extrasAreRoundedToFiftyAndNegativesIgnored() {
        val b = GoalCalculator.breakdown(inputs(workout = 333, alcohol = -40))
        assertEquals(350, b.workoutMl)
        assertEquals(0, b.alcoholMl)
    }

    @Test
    fun outdoorTemperatureIsRememberedInTheBreakdown() {
        val b = GoalCalculator.breakdown(GoalInputs(70f, ActivityLevel.LOW, 0.5f, 14, outdoorMaxC = 28f))
        assertEquals(28f, b.outdoorMaxC!!, 0.001f)
        assertNull(GoalCalculator.breakdown(inputs()).outdoorMaxC)
    }
}
