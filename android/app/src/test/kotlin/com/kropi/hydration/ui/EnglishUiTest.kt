package com.kropi.hydration.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kropi.hydration.data.HydrationSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Ten sam interfejs z angielskimi ustawieniami regionalnymi: zasoby `values-en` faktycznie się ładują. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "en-w411dp-h2400dp-xhdpi")
class EnglishUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun editorSpeaksEnglish() {
        rule.setContent {
            MaterialTheme {
                IntakeEditorSheet(initial = null, netHydration = true, onSave = { _, _, _, _, _ -> }, onDelete = null, onDismiss = {})
            }
        }
        rule.onNodeWithText("Add entry").assertExists()
        rule.onNodeWithText("Counts as: 250 ml").assertExists()
        rule.onNodeWithText("☕ Coffee").performClick()
        rule.onNodeWithText("Counts as: 160 ml (80% of 200 ml)").assertExists()
        rule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun onboardingSpeaksEnglish() {
        rule.setContent {
            MaterialTheme {
                OnboardingScreen(HydrationSettings(), {}, {}, {}, {})
            }
        }
        rule.onNodeWithText("Hi, I'm Kropi").assertExists()
        rule.onNodeWithText("Skip").assertExists()
        rule.onNodeWithText("Next").performClick()
        rule.onNodeWithText("Your goal").assertExists()
    }
}
