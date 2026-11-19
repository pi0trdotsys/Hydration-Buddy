package com.kropi.hydration.ui

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.notifications.NotificationHelper
import com.kropi.hydration.notifications.NotificationScheduler
import com.kropi.hydration.widget.HydrationWidget
import kotlinx.coroutines.launch

// Host app. Mirrors the whole Lovable web plan under src/routes: a bottom-nav
// shell with the home dashboard, the widget size preview, history, and the
// content browser -- plus a Settings tab the web mockup didn't need (goal
// calculator, reminders), all sharing one HydrationRepository-backed state
// like the web's single useHydrationMock() instance.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = HydrationRepository(applicationContext)
        NotificationHelper.ensureChannel(applicationContext)
        NotificationScheduler.schedule(applicationContext)

        setContent {
            val notificationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { /* no-op, reminders simply do not fire without the permission */ }

            fun askNotificationPermission() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // null = jeszcze sprawdzamy; powitanie ma własny moment na pytanie o powiadomienia.
            var onboarding by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) {
                onboarding = repo.needsOnboarding()
                if (onboarding == false) askNotificationPermission()
            }

            MaterialTheme {
                val state by repo.state.collectAsState(initial = null)
                val scope = rememberCoroutineScope()
                LaunchedEffect(Unit) { repo.current() }
                var tab by remember {
                    mutableStateOf(Tab.entries.firstOrNull { it.name == intent?.getStringExtra(NotificationHelper.EXTRA_OPEN_TAB) } ?: Tab.HOME)
                }

                var editor by remember { mutableStateOf<EditorTarget?>(null) }

                val current = state
                if (onboarding == null) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().background(KropiColors.background))
                    return@MaterialTheme
                }
                if (onboarding == true && current != null) {
                    OnboardingScreen(
                        initial = current.settings,
                        onRequestNotifications = { askNotificationPermission() },
                        onPinWidget = { pinWidget() },
                        onFinish = { settings ->
                            scope.launch {
                                repo.completeOnboarding(settings)
                                HydrationWidget().updateAll(applicationContext)
                                NotificationScheduler.schedule(applicationContext)
                            }
                            onboarding = false
                        },
                        onSkip = {
                            scope.launch { repo.completeOnboarding(null) }
                            onboarding = false
                        },
                    )
                    return@MaterialTheme
                }
                editor?.let { target ->
                    IntakeEditorSheet(
                        initial = target.intake,
                        defaultEpochDay = target.epochDay,
                        typicalMl = { drink -> current?.typicalMl(drink) ?: drink.defaultMl },
                        netHydration = current?.settings?.netHydration ?: true,
                        onSave = { ml, drink, note, day, minute ->
                            scope.launch {
                                if (target.intake == null) repo.addIntake(ml, drink, note, day, minute)
                                else repo.updateIntake(target.intake.id, ml, drink, note, day, minute)
                                HydrationWidget().updateAll(applicationContext)
                            }
                            editor = null
                        },
                        onDelete = target.intake?.let { old ->
                            {
                                scope.launch { repo.deleteIntake(old.id); HydrationWidget().updateAll(applicationContext) }
                                editor = null
                            }
                        },
                        onDismiss = { editor = null },
                    )
                }
                Scaffold(
                    containerColor = KropiColors.background,
                    bottomBar = {
                        NavigationBar(containerColor = KropiColors.card) {
                            Tab.entries.forEach { t ->
                                NavigationBarItem(
                                    selected = tab == t,
                                    onClick = { tab = t },
                                    icon = { Text(t.emoji, fontSize = 18.sp) },
                                    label = { Text(t.label, fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = KropiColors.aqua,
                                        selectedTextColor = KropiColors.aqua,
                                        unselectedIconColor = KropiColors.mutedForeground,
                                        unselectedTextColor = KropiColors.mutedForeground,
                                        indicatorColor = KropiColors.secondary,
                                    ),
                                )
                            }
                        }
                    },
                ) { padding ->
                    if (current != null) {
                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                            when (tab) {
                                Tab.HOME -> HomeScreen(
                                    state = current,
                                    onAdd = { ml -> scope.launch { repo.addWater(ml); HydrationWidget().updateAll(applicationContext) } },
                                    onUndo = { scope.launch { repo.undoLast(); HydrationWidget().updateAll(applicationContext) } },
                                    onPoke = { scope.launch { repo.poke() } },
                                    onSetGoal = { ml -> scope.launch { repo.setGoal(ml); HydrationWidget().updateAll(applicationContext) } },
                                    onAddEntry = { editor = EditorTarget(null, null) },
                                    onEditIntake = { intake -> editor = EditorTarget(intake, null) },
                                    onDeleteIntake = { id -> scope.launch { repo.deleteIntake(id); HydrationWidget().updateAll(applicationContext) } },
                                )
                                Tab.WIDGET -> WidgetScreen(
                                    state = current,
                                    onAdd = { ml -> scope.launch { repo.addWater(ml); HydrationWidget().updateAll(applicationContext) } },
                                    onUndo = { scope.launch { repo.undoLast(); HydrationWidget().updateAll(applicationContext) } },
                                    onPoke = { scope.launch { repo.poke() } },
                                    onPin = { pinWidget() },
                                )
                                Tab.HISTORY -> HistoryScreen(
                                    state = current,
                                    onAddEntryOn = { day -> editor = EditorTarget(null, day) },
                                    onEditIntake = { intake -> editor = EditorTarget(intake, null) },
                                )
                                Tab.INSIGHTS -> InsightsScreen()
                                Tab.SETTINGS -> SettingsScreen(
                                    initial = current.settings,
                                    onSave = { settings ->
                                        scope.launch {
                                            repo.saveSettings(settings)
                                            HydrationWidget().updateAll(applicationContext)
                                            NotificationScheduler.schedule(applicationContext)
                                        }
                                    },
                                    onTestNotification = { NotificationScheduler.sendTestNotification(applicationContext) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun pinWidget() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val appWidgetManager = getSystemService(AppWidgetManager::class.java)
        val provider = ComponentName(this, com.kropi.hydration.widget.HydrationWidgetReceiver::class.java)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(provider, null, null)
        }
    }
}

/** Cel edytora: null w [intake] = nowy wpis. */
private class EditorTarget(val intake: com.kropi.hydration.data.Intake?, val epochDay: Long?)

private enum class Tab(val label: String, val emoji: String) {
    HOME("Główny", "🏠"),
    WIDGET("Widget", "🔲"),
    HISTORY("Historia", "📅"),
    INSIGHTS("Treści", "💡"),
    SETTINGS("Ustawienia", "⚙️"),
}
