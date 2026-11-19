package com.kropi.hydration.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.HydrationSettings
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.Intake
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Każdy klikalny element ma mieć tekst albo opis dla czytnika ekranu. Przyciski z samym symbolem
 * („−", „+", „✕", „‹") bez opisu TalkBack czyta jako „przycisk" — i nie wiadomo, do czego służy.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "en-w411dp-h2400dp-xhdpi")
class AccessibilityTest {
    @get:Rule val rule = createComposeRule()

    private fun ComposeContentTestRule.assertClickablesAreDescribed(where: String) {
        val offenders = onAllNodes(hasClickAction()).fetchSemanticsNodes().filter { node ->
            val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty()
            val description = node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString().orEmpty()
            text.isBlank() && description.isBlank()
        }
        assertTrue("$where: ${offenders.size} klikalnych elementów bez tekstu i opisu", offenders.isEmpty())
    }

    private fun state(): HydrationState {
        val today = LocalDate.now().toEpochDay()
        val intakes = listOf(Intake(1, today, 9, 5, 250, Drink.WATER, null, 250), Intake(2, today, 11, 0, 200, Drink.COFFEE, "x", 160))
        return HydrationState(2500, intakes, emptyList(), List(24) { 0 }, 0, HydrationSettings(), allIntakes = intakes)
    }

    @Test
    fun editorButtonsAreDescribed() {
        rule.setContent { MaterialTheme { IntakeEditorSheet(null, true, onSave = { _, _, _, _, _ -> }, onDelete = null, onDismiss = {}) } }
        rule.assertClickablesAreDescribed("edytor wpisu")
    }

    @Test
    fun onboardingButtonsAreDescribedOnEveryStep() {
        rule.setContent { MaterialTheme { OnboardingScreen(HydrationSettings(), {}, {}, {}, {}) } }
        rule.assertClickablesAreDescribed("powitanie, krok 0")
        rule.onNodeWithText("Next").performClick()
        rule.assertClickablesAreDescribed("powitanie, krok 1")
        rule.onNodeWithText("Next").performClick()
        rule.assertClickablesAreDescribed("powitanie, krok 2")
        rule.onNodeWithText("Next").performClick()
        rule.assertClickablesAreDescribed("powitanie, krok 3")
    }

    @Test
    fun historyButtonsAreDescribed() {
        rule.setContent { MaterialTheme { HistoryScreen(state(), onAddEntryOn = {}, onEditIntake = {}) } }
        rule.assertClickablesAreDescribed("historia")
    }

    @Test
    fun homeButtonsAreDescribed() {
        rule.setContent {
            MaterialTheme {
                HomeScreen(state(), onAdd = {}, onUndo = {}, onPoke = {}, onSetGoal = {}, onAddEntry = {}, onEditIntake = {}, onDeleteIntake = {})
            }
        }
        rule.assertClickablesAreDescribed("ekran główny")
    }
}
