package com.kropi.hydration.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.kropi.hydration.backup.BackupFormat
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.HydrationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Przypomnienia „poza planem" na prawdziwym systemie: po powrocie do domu (Wi-Fi) i po kawie. Zmianę sieci
 * udajemy podając nazwę sieci, bo emulator nie zawsze łączy się z Wi-Fi.
 */
@RunWith(AndroidJUnit4::class)
class NudgesInstrumentedTest {
    @get:Rule
    val notifications: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private lateinit var repo: HydrationRepository

    @Before
    fun freshState() = runBlocking {
        repo = HydrationRepository(context)
        repo.restore(
            BackupFormat.Snapshot(
                0, "test",
                listOf(
                    BackupFormat.Entry("goal", BackupFormat.Type.INT, 2500),
                    // Okno 0–23, żeby test nie zależał od pory dnia.
                    BackupFormat.Entry("active_start_hour", BackupFormat.Type.INT, 0),
                    BackupFormat.Entry("active_end_hour", BackupFormat.Type.INT, 23),
                    BackupFormat.Entry("respect_dnd", BackupFormat.Type.BOOL, false),
                ),
            ),
        )
        manager.cancelAll()
    }

    private fun nudgeShown() = manager.activeNotifications.any { it.id == NotificationHelper.NUDGE_NOTIFICATION_ID }

    private suspend fun setHome(ssid: String) = repo.saveSettings(repo.current().settings.copy(homeWifiSsid = ssid))

    @Test
    fun withoutAHomeNetworkNothingHappens() = runBlocking {
        assertEquals(WifiOutcome.NO_HOME, handleWifiChange(context, repo) { "Dom" })
        assertFalse(nudgeShown())
    }

    @Test
    fun anotherNetworkIsIgnored() = runBlocking {
        setHome("Dom")
        assertEquals(WifiOutcome.OTHER_NETWORK, handleWifiChange(context, repo) { "Praca" })
        assertEquals(WifiOutcome.OTHER_NETWORK, handleWifiChange(context, repo) { null })
        assertFalse(nudgeShown())
    }

    @Test
    fun arrivingHomeSendsOneReminderAndThenWaits() = runBlocking {
        setHome("Dom")
        assertEquals(WifiOutcome.SENT, handleWifiChange(context, repo) { "Dom" })
        assertTrue(nudgeShown())
        assertEquals(WifiOutcome.COOLDOWN, handleWifiChange(context, repo) { "Dom" })
    }

    @Test
    fun aFreshSipSuppressesTheReminder() = runBlocking {
        setHome("Dom")
        repo.addWater(250)
        assertEquals(WifiOutcome.RECENT_SIP, handleWifiChange(context, repo) { "Dom" })
        assertFalse(nudgeShown())
    }

    @Test
    fun disabledRemindersSuppressIt() = runBlocking {
        setHome("Dom")
        repo.saveSettings(repo.current().settings.copy(remindersEnabled = false))
        assertEquals(WifiOutcome.HELD, handleWifiChange(context, repo) { "Dom" })
    }

    private fun coffeeWorker(minute: Int) =
        TestListenableWorkerBuilder<AnchorWorker>(context).setInputData(workDataOf("coffee_minute" to minute)).build()

    @Test
    fun afterCoffeeWithoutWaterAReminderArrives() = runBlocking {
        repo.saveSettings(repo.current().settings.copy(anchorAfterCoffee = true))
        repo.addIntake(200, Drink.COFFEE, minuteOfDay = 100) // wpis „z przeszłości" nie uruchamia odliczania, test robi to ręcznie
        assertEquals(ListenableWorker.Result.success(), coffeeWorker(100).doWork())
        assertTrue(nudgeShown())
    }

    @Test
    fun waterDrunkAfterTheCoffeeCancelsTheReminder() = runBlocking {
        repo.saveSettings(repo.current().settings.copy(anchorAfterCoffee = true))
        repo.addIntake(200, Drink.COFFEE, minuteOfDay = 100)
        repo.addIntake(250, Drink.WATER, minuteOfDay = 110)
        coffeeWorker(100).doWork()
        assertFalse(nudgeShown())
    }

    @Test
    fun theAnchorCanBeSwitchedOff() = runBlocking {
        coffeeWorker(100).doWork() // domyślnie wyłączone
        assertFalse(nudgeShown())
    }
}
