package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class HydrationPlanTest {
    private fun state(goal: Int, drunk: Int, settings: HydrationSettings = HydrationSettings(adaptivePlan = false)): HydrationState {
        val intakes = if (drunk > 0) listOf(Intake(1, 0, 8, 0, drunk, Drink.WATER, null, drunk)) else emptyList()
        return HydrationState(goal, intakes, emptyList(), List(24) { 0 }, 0, settings)
    }

    @Test
    fun goalReachedMeansDoneAndNoSips() {
        val plan = state(2000, 2000).plan(LocalTime.of(12, 0))
        assertEquals(PlanStatus.DONE, plan.status)
        assertTrue(plan.sips.isEmpty())
    }

    @Test
    fun planCoversRemainingMlWithinWindow() {
        val plan = state(2500, 1000).plan(LocalTime.of(12, 0))
        assertEquals(PlanStatus.ON_TRACK, plan.status)
        assertEquals(1500, plan.remainingMl)
        assertTrue(plan.sips.sumOf { it.ml } >= 1500)
        assertTrue(plan.sips.all { it.time >= LocalTime.of(12, 10) && it.time < LocalTime.of(22, 0) })
    }

    @Test
    fun sipsAreSortedAndSpacedAtLeastTwentyMinutes() {
        val plan = state(3000, 0).plan(LocalTime.of(9, 0))
        val minutes = plan.sips.map { it.time.hour * 60 + it.time.minute }
        assertEquals(minutes.sorted(), minutes)
        minutes.zipWithNext().forEach { (a, b) -> assertTrue(b - a >= 20) }
    }

    @Test
    fun lateEveningWithBigGapIsTightWithBiggerPortions() {
        val plan = state(2500, 0).plan(LocalTime.of(20, 0))
        assertEquals(PlanStatus.TIGHT, plan.status)
        assertTrue(plan.portionMl > 250)
    }

    @Test
    fun afterWindowIsAfterHours() {
        val plan = state(2500, 500).plan(LocalTime.of(22, 30))
        assertEquals(PlanStatus.AFTER_HOURS, plan.status)
    }

    @Test
    fun beforeWindowStartsAtWindowOpening() {
        val plan = state(2500, 0).plan(LocalTime.of(6, 30))
        assertEquals(LocalTime.of(8, 0), plan.next!!.time)
    }

    @Test
    fun coffeeCountsOnlyItsShareTowardRemaining() {
        val intakes = listOf(Intake(1, 0, 8, 0, 500, Drink.COFFEE, null, Drink.COFFEE.counted(500)))
        val s = HydrationState(2000, intakes, emptyList(), List(24) { 0 }, 0, HydrationSettings())
        assertEquals(400, s.total)
        assertEquals(1600, s.remaining)
    }
}
