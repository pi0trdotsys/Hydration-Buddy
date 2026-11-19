package com.kropi.hydration.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.Intake
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "pl-w411dp-h2400dp-xhdpi")
class IntakeEditorTest {
    @get:Rule val rule = createComposeRule()

    private data class Saved(val ml: Int, val drink: Drink, val note: String?, val day: Long, val minute: Int)

    @Test
    fun choosingCoffeeShowsNetAmountAndSavesIt() {
        var saved: Saved? = null
        rule.setContent {
            MaterialTheme {
                IntakeEditorSheet(
                    initial = null,
                    netHydration = true,
                    onSave = { ml, drink, note, day, minute -> saved = Saved(ml, drink, note, day, minute) },
                    onDelete = null,
                    onDismiss = {},
                )
            }
        }
        rule.onNodeWithText("Liczy się: 250 ml").assertExists()
        rule.onNodeWithText("☕ Kawa").performClick()
        // Kawa podpowiada swoją typową porcję (200 ml) i liczy się w 80%.
        rule.onNodeWithText("Liczy się: 160 ml (80% z 200 ml)").assertExists()
        rule.onNodeWithText("Zapisz").performClick()

        val result = saved
        assertNotNull(result)
        assertEquals(200, result!!.ml)
        assertEquals(Drink.COFFEE, result.drink)
        assertNull(result.note)
    }

    @Test
    fun manualAmountIsNotOverriddenByDrinkSuggestion() {
        var saved: Saved? = null
        rule.setContent {
            MaterialTheme {
                IntakeEditorSheet(
                    initial = null,
                    netHydration = true,
                    onSave = { ml, drink, note, day, minute -> saved = Saved(ml, drink, note, day, minute) },
                    onDelete = null,
                    onDismiss = {},
                )
            }
        }
        rule.onNodeWithText("300").performClick()
        rule.onNodeWithText("🍵 Herbata").performClick()
        rule.onNodeWithText("Zapisz").performClick()
        assertEquals(300, saved!!.ml)
        assertEquals(Drink.TEA, saved!!.drink)
    }

    @Test
    fun editingExistingEntryOffersDeleteAndKeepsValues() {
        var deleted = false
        var saved: Saved? = null
        val existing = Intake(7, 20_000, 9, 15, 330, Drink.JUICE, "po treningu", Drink.JUICE.counted(330))
        rule.setContent {
            MaterialTheme {
                IntakeEditorSheet(
                    initial = existing,
                    netHydration = true,
                    onSave = { ml, drink, note, day, minute -> saved = Saved(ml, drink, note, day, minute) },
                    onDelete = { deleted = true },
                    onDismiss = {},
                )
            }
        }
        rule.onNodeWithText("Edytuj wpis").assertExists()
        rule.onNodeWithText("Usuń wpis").performClick()
        assertEquals(true, deleted)

        rule.onNodeWithText("Zapisz").performClick()
        assertEquals(Saved(330, Drink.JUICE, "po treningu", 20_000, 9 * 60 + 15), saved)
    }
}
