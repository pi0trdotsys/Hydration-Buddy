package com.kropi.hydration.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.HydrationSettings
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.Intake
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h2400dp-xhdpi")
class HistoryCalendarTest {
    @get:Rule val rule = createComposeRule()

    private fun stateWithTodayEntries(): HydrationState {
        val today = LocalDate.now().toEpochDay()
        val intakes = listOf(
            Intake(1, today, 9, 5, 250, Drink.WATER, null, 250),
            Intake(2, today, 14, 40, 200, Drink.COFFEE, "po obiedzie", Drink.COFFEE.counted(200)),
        )
        return HydrationState(
            goal = 2500,
            intakes = intakes,
            records = emptyList(),
            hourlyProfile = List(24) { 0 },
            pokeSeed = 0,
            settings = HydrationSettings(),
            allIntakes = intakes,
        )
    }

    @Test
    fun tappingTodayShowsItsEntriesAndAddButton() {
        var addedFor: Long? = null
        var edited: Intake? = null
        rule.setContent {
            MaterialTheme {
                HistoryScreen(
                    state = stateWithTodayEntries(),
                    onAddEntryOn = { addedFor = it },
                    onEditIntake = { edited = it },
                )
            }
        }
        val dayOfMonth = LocalDate.now().dayOfMonth
        val list = rule.onAllNodes(hasScrollAction()).onFirst()
        list.performScrollToNode(hasText("＋ Dopisz"))

        rule.onNodeWithTag("day-$dayOfMonth").performClick()
        rule.onNodeWithText("＋ Dopisz").assertExists()
        rule.onNodeWithText("200 ml").assertExists()

        rule.onNodeWithText("＋ Dopisz").performClick()
        assertEquals(LocalDate.now().toEpochDay(), addedFor)

        rule.onNodeWithText("200 ml").performClick()
        assertEquals(2L, edited?.id)
    }
}
