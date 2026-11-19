package com.kropi.hydration.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kropi.hydration.data.ActivityLevel
import com.kropi.hydration.data.GoalMode
import com.kropi.hydration.data.HydrationSettings
import com.kropi.hydration.data.NotificationTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "pl-w411dp-h2400dp-xhdpi")
class OnboardingFlowTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun walkingThroughAllStepsReturnsChosenSettings() {
        var finished: HydrationSettings? = null
        var notificationAsks = 0
        rule.setContent {
            MaterialTheme {
                OnboardingScreen(
                    initial = HydrationSettings(),
                    onRequestNotifications = { notificationAsks++ },
                    onPinWidget = {},
                    onFinish = { finished = it },
                    onSkip = {},
                )
            }
        }
        rule.onNodeWithText("Cześć, jestem Kropi").assertExists()
        rule.onNodeWithText("Dalej").performClick()

        // Krok 1: cel liczony z wagi, aktywności i temperatury.
        rule.onNodeWithText("Twój cel").assertExists()
        rule.onNodeWithText("🏃 Wysoka").performClick()
        rule.onNodeWithText("Dalej").performClick()

        // Krok 2: godziny, porcja, ton.
        rule.onNodeWithText("Twój dzień").assertExists()
        rule.onNodeWithText("🤍 Wspierający").performClick()
        rule.onNodeWithText("300").performClick()
        rule.onNodeWithText("Dalej").performClick()

        // Krok 3: przypomnienia — „Zaczynamy" prosi o zgodę na powiadomienia i kończy.
        rule.onNodeWithText("Ostatnie szlify").assertExists()
        rule.onNodeWithText("Zaczynamy").performClick()

        val settings = finished
        assertNotNull(settings)
        assertEquals(ActivityLevel.HIGH, settings!!.activity)
        assertEquals(NotificationTone.CARING, settings.notificationTone)
        assertEquals(300, settings.reminderGlassMl)
        assertEquals(GoalMode.AUTO, settings.goalMode)
        assertTrue(settings.remindersEnabled)
        assertTrue(notificationAsks >= 1)
    }

    @Test
    fun skipLeavesDefaultsUntouched() {
        var skipped = false
        var finished: HydrationSettings? = null
        rule.setContent {
            MaterialTheme {
                OnboardingScreen(HydrationSettings(), {}, {}, { finished = it }, { skipped = true })
            }
        }
        rule.onNodeWithText("Pomiń").performClick()
        assertEquals(true, skipped)
        assertEquals(null, finished)
    }
}
