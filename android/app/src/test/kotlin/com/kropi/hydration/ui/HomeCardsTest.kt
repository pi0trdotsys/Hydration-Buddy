package com.kropi.hydration.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kropi.hydration.data.CheckInState
import com.kropi.hydration.data.DayRecord
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.Feeling
import com.kropi.hydration.data.HydrationSettings
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.Intake
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "en-w411dp-h2400dp-xhdpi")
class HomeCardsTest {
    @get:Rule val rule = createComposeRule()

    private val today = LocalDate.now().toEpochDay()

    private fun state(
        intakes: List<Intake> = emptyList(),
        records: List<DayRecord> = emptyList(),
        checkIn: CheckInState = CheckInState(),
        goal: Int = 2000,
    ) = HydrationState(goal, intakes, records, List(24) { 0 }, 0, HydrationSettings(), allIntakes = intakes, checkIn = checkIn)

    private fun intake(id: Long, ml: Int, drink: Drink, counted: Int = ml) = Intake(id, today, 10, 0, ml, drink, null, counted)

    @Test
    fun checkInAsksFromNoonAndReportsTheAnswer() {
        var answered: Feeling? = null
        rule.setContent { MaterialTheme { CheckInCard(state(), { answered = it }, nowHour = 15) } }
        rule.onNodeWithText("HOW ARE YOU TODAY?").assertExists()
        rule.onNodeWithText("Dark, or thirsty", substring = true).performClick()
        assertEquals(Feeling.DARK, answered)
    }

    @Test
    fun checkInStaysQuietInTheMorning() {
        rule.setContent { MaterialTheme { CheckInCard(state(), {}, nowHour = 9) } }
        rule.onNodeWithText("HOW ARE YOU TODAY?").assertDoesNotExist()
    }

    @Test
    fun anAnsweredCheckInShowsAdviceAndTomorrowsChange() {
        val s = state(
            intakes = listOf(intake(1, 2000, Drink.WATER)),
            checkIn = CheckInState(today = Feeling.DARK, activePct = 0, pendingPct = 3),
        )
        rule.setContent { MaterialTheme { CheckInCard(s, {}, nowHour = 18) } }
        rule.onNodeWithText("Today: ", substring = true).assertExists()
        rule.onNodeWithText("your goal is too low", substring = true).assertExists()
        rule.onNodeWithText("From tomorrow the goal goes up by 3%", substring = true).assertExists()
    }

    @Test
    fun streakShowsShieldsAndTheRiskToday() {
        val records = (1L..3L).map { DayRecord(today - it, 2000, 2000) }
        rule.setContent { MaterialTheme { StreakCard(state(records = records)) } }
        rule.onNodeWithText("Streak: 3 days").assertExists()
        rule.onNodeWithText("🛡️").assertExists()
        rule.onNodeWithText("shield will save the streak", substring = true).assertExists()
    }

    @Test
    fun noStreakCardWithoutAStreak() {
        rule.setContent { MaterialTheme { StreakCard(state()) } }
        rule.onNodeWithText("Streak:", substring = true).assertDoesNotExist()
    }

    @Test
    fun alcoholWarningNamesTheCompensation() {
        val s = state(intakes = listOf(intake(1, 330, Drink.ALCOHOL, counted = 0)))
        rule.setContent { MaterialTheme { DrinkWarningsCard(s) } }
        rule.onNodeWithText("ALCOHOL").assertExists()
        rule.onNodeWithText("I added 165 ml", substring = true).assertExists()
    }

    @Test
    fun caffeineWarningStaysQuietBelowThreeHundredMg() {
        val near = state(intakes = List(3) { intake(it.toLong(), 200, Drink.COFFEE, 160) }) // 270 mg
        rule.setContent { MaterialTheme { DrinkWarningsCard(near) } }
        rule.onNodeWithText("CAFFEINE").assertDoesNotExist()
    }

    @Test
    fun caffeineWarningAppearsOverTheLimit() {
        val over = state(intakes = List(5) { intake(it.toLong(), 200, Drink.COFFEE, 160) }) // 450 mg
        rule.setContent { MaterialTheme { DrinkWarningsCard(over) } }
        rule.onNodeWithText("over the 400 mg limit", substring = true).assertExists()
    }

    @Test
    fun nothingToWarnAboutMeansNoCard() {
        rule.setContent { MaterialTheme { DrinkWarningsCard(state(intakes = listOf(intake(1, 250, Drink.WATER)))) } }
        rule.onNodeWithText("ALCOHOL").assertDoesNotExist()
    }

    @Test
    fun newCardsHaveDescribedButtons() {
        val s = state(checkIn = CheckInState())
        rule.setContent { MaterialTheme { CheckInCard(s, {}, nowHour = 15); NfcTagCard() } }
        val offenders = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().filter { node ->
            val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty()
            val description = node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString().orEmpty()
            text.isBlank() && description.isBlank()
        }
        assertTrue(offenders.isEmpty())
    }
}
