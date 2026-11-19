package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitsTest {
    private fun intake(day: Long, hour: Int, ml: Int = 250) =
        Intake(id = day * 100 + hour, epochDay = day, hour = hour, minute = 0, ml = ml, drink = Drink.WATER, note = null, counted = ml)

    /** Przez [days] dni pije się o podanych godzinach. */
    private fun routine(days: Int, today: Long, hours: List<Int>) =
        (1..days).flatMap { back -> hours.map { intake(today - back, it) } }

    @Test
    fun tooLittleDataSuggestsNothing() {
        val r = Habits.learn(routine(3, 100, listOf(8, 9, 10, 11)), 100, 8, 22)
        assertTrue(r.gaps.isEmpty())
        assertEquals(3, r.daysUsed)
    }

    @Test
    fun findsTheUsualGapInTheMiddleOfTheDay() {
        // pije 8–13 i 17–21; luka 14–16 (3 godziny)
        val intakes = routine(14, 100, listOf(8, 9, 10, 11, 12, 13, 17, 18, 19, 20, 21))
        val r = Habits.learn(intakes, 100, 8, 22)
        assertEquals(listOf(HabitGap(14, 17)), r.gaps)
        assertEquals(1f, r.probability[9], 0.001f)
        assertEquals(0f, r.probability[15], 0.001f)
    }

    @Test
    fun aMorningWithoutDrinkingIsNotAGap() {
        val intakes = routine(14, 100, listOf(11, 12, 13, 14, 15))
        val r = Habits.learn(intakes, 100, 8, 22)
        // 8–10 to początek okna, a 16–21 to „reszta dnia" — ta ostatnia jest luką i ma sens
        assertTrue(r.gaps.none { it.startHour == 8 })
    }

    @Test
    fun aSingleQuietHourIsNotWorthAReminder() {
        val intakes = routine(14, 100, listOf(8, 9, 10, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21))
        assertTrue(Habits.learn(intakes, 100, 8, 22).gaps.isEmpty())
    }

    @Test
    fun todayDoesNotTeachTheModel() {
        val intakes = routine(14, 100, listOf(8, 9, 10, 11, 12, 13, 17, 18, 19, 20, 21)) + intake(100, 15)
        assertEquals(0f, Habits.learn(intakes, 100, 8, 22).probability[15], 0.001f)
    }

    @Test
    fun daysWithTooFewSipsAreNotCounted() {
        val thin = (1..14).map { intake(100L - it, 9) } // jeden łyk dziennie
        assertEquals(0, Habits.learn(thin, 100, 8, 22).daysUsed)
    }

    @Test
    fun reminderComesShortlyBeforeTheGapOnly() {
        val gaps = listOf(HabitGap(14, 17))
        assertEquals(gaps[0], Habits.gapAhead(gaps, 14 * 60 - 30, 60))
        assertNull(Habits.gapAhead(gaps, 14 * 60 - 120, 60)) // za wcześnie
        assertNull(Habits.gapAhead(gaps, 14 * 60 + 5, 60)) // już po początku
    }

    @Test
    fun noReminderWhenYouJustDrank() {
        val gaps = listOf(HabitGap(14, 17))
        assertNull(Habits.gapAhead(gaps, 14 * 60 - 20, 10))
        assertEquals(gaps[0], Habits.gapAhead(gaps, 14 * 60 - 20, null)) // nic dziś nie wypito
    }
}
