package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CheckInRulesTest {
    private fun h(vararg f: Feeling) = f.mapIndexed { i, x -> CheckInEntry(i.toLong(), x) }

    @Test
    fun darkAfterReachingTheGoalRaisesIt() {
        assertEquals(3, CheckInRules.nextPct(0, emptyList(), Feeling.DARK, 1.0f))
        assertEquals(6, CheckInRules.nextPct(3, emptyList(), Feeling.DARK, 0.9f))
    }

    @Test
    fun darkWithoutReachingTheGoalChangesNothing() {
        assertEquals(0, CheckInRules.nextPct(0, emptyList(), Feeling.DARK, 0.5f))
    }

    @Test
    fun correctionNeverLeavesTheTenPercentBand() {
        assertEquals(10, CheckInRules.nextPct(9, emptyList(), Feeling.DARK, 1f))
        assertEquals(-10, CheckInRules.nextPct(-9, h(Feeling.CLEAR, Feeling.CLEAR), Feeling.CLEAR, 1f))
    }

    @Test
    fun veryPaleNeedsThreeInARowBeforeLoweringTheGoal() {
        assertEquals(0, CheckInRules.nextPct(0, emptyList(), Feeling.CLEAR, 1f))
        assertEquals(0, CheckInRules.nextPct(0, h(Feeling.CLEAR), Feeling.CLEAR, 1f))
        assertEquals(-3, CheckInRules.nextPct(0, h(Feeling.CLEAR, Feeling.CLEAR), Feeling.CLEAR, 1f))
    }

    @Test
    fun aBreakInTheRunResetsTheCount() {
        assertEquals(0, CheckInRules.nextPct(0, h(Feeling.CLEAR, Feeling.GOOD, Feeling.CLEAR), Feeling.CLEAR, 1f))
    }

    @Test
    fun veryPaleBelowTheGoalNeverLowersIt() {
        assertEquals(0, CheckInRules.nextPct(0, h(Feeling.CLEAR, Feeling.CLEAR), Feeling.CLEAR, 0.8f))
    }

    @Test
    fun goodAndSkippedLeaveTheCorrectionAlone() {
        assertEquals(3, CheckInRules.nextPct(3, emptyList(), Feeling.GOOD, 1f))
        assertEquals(3, CheckInRules.nextPct(3, emptyList(), Feeling.SKIPPED, 1f))
    }

    @Test
    fun historyRoundTripsAndKeepsOnlyThirtyDays() {
        val entries = (1L..40L).map { CheckInEntry(it, Feeling.GOOD) }
        val decoded = CheckInRules.decode(CheckInRules.encode(entries))
        assertEquals(30, decoded.size)
        assertEquals(40L, decoded.last().epochDay)
        assertEquals(emptyList<CheckInEntry>(), CheckInRules.decode(null))
        assertEquals(emptyList<CheckInEntry>(), CheckInRules.decode("garbage,1:X,:G"))
    }

    @Test
    fun adviceFollowsTheAnswerAndTheProgress() {
        assertEquals("drink_more", CheckInRules.adviceKey(Feeling.DARK, 0.4f))
        assertEquals("goal_up", CheckInRules.adviceKey(Feeling.DARK, 1f))
        assertEquals("no_more", CheckInRules.adviceKey(Feeling.CLEAR, 1.2f))
        assertEquals("fine", CheckInRules.adviceKey(Feeling.GOOD, 0.2f))
        assertNull(Feeling.of('?'))
    }
}
