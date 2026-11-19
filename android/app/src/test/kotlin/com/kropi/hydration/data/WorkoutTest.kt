package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutTest {
    private val min = 60_000L
    private fun s(id: String, startMin: Long, endMin: Long) = WorkoutSession(id, startMin * min, endMin * min)

    @Test
    fun sixHundredMlPerHourRoundedToFifty() {
        assertEquals(600, Workout.bonusMl(listOf(s("a", 0, 60)), 100 * min))
        assertEquals(450, Workout.bonusMl(listOf(s("a", 0, 45)), 100 * min))
        assertEquals(1050, Workout.bonusMl(listOf(s("a", 0, 60), s("b", 70, 115)), 200 * min))
    }

    @Test
    fun veryShortSessionsAreIgnored() {
        assertEquals(0, Workout.bonusMl(listOf(s("a", 0, 10)), 100 * min))
    }

    @Test
    fun anOngoingSessionCountsUpToNow() {
        assertEquals(300, Workout.bonusMl(listOf(s("a", 0, 120)), 30 * min))
    }

    @Test
    fun dailyBonusIsCapped() {
        assertEquals(Workout.MAX_ML_PER_DAY, Workout.bonusMl(listOf(s("a", 0, 600)), 1000 * min))
    }

    @Test
    fun afterWorkoutPortionIsBounded() {
        assertEquals(250, Workout.afterWorkoutMl(s("a", 0, 20)))
        assertEquals(750, Workout.afterWorkoutMl(s("a", 0, 240)))
    }

    @Test
    fun nudgeOnlyForRecentUnnotifiedFinishedSessions() {
        val now = 200 * min
        assertEquals("b", Workout.nudgeCandidate(listOf(s("a", 0, 60), s("b", 100, 160)), now, null)?.id)
        assertNull(Workout.nudgeCandidate(listOf(s("a", 0, 60)), now, null)) // skończony 140 min temu
        assertNull(Workout.nudgeCandidate(listOf(s("b", 100, 160)), now, "b")) // już przypomniano
        assertNull(Workout.nudgeCandidate(listOf(s("c", 150, 190)), 180 * min, null)) // jeszcze trwa
        assertNull(Workout.nudgeCandidate(listOf(s("d", 150, 160)), now, null)) // za krótki
    }
}
