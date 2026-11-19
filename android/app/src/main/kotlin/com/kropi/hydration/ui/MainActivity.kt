package com.kropi.hydration.ui

import com.kropi.hydration.data.formatMl
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Snackbar
import androidx.compose.ui.res.stringResource
import com.kropi.hydration.R
import android.Manifest
import com.kropi.hydration.widget.updateAllWidgets
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
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.notifications.NotificationHelper
import com.kropi.hydration.notifications.NotificationScheduler
import kotlinx.coroutines.launch

// Host app. Mirrors the whole Lovable web plan under src/routes: a bottom-nav
// shell with the home dashboard, the widget size preview, history, and the
// content browser -- plus a Settings tab the web mockup didn't need (goal
// calculator, reminders), all sharing one HydrationRepository-backed state
// like the web's single useHydrationMock() instance.
class MainActivity : ComponentActivity() {

    /** Zakładka z intentu przychodzącego, gdy aplikacja już działa (np. dotknięcie widgetu tygodniowego). */
    private var requestedTab by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        requestedTab = intent.getStringExtra(NotificationHelper.EXTRA_OPEN_TAB)
    }

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
                // Zakładka przeżywa obrót ekranu i zmianę języka (rememberSaveable zapisuje indeks).
                var tabIndex by androidx.compose.runtime.saveable.rememberSaveable {
                    mutableIntStateOf((Tab.entries.firstOrNull { it.name == intent?.getStringExtra(NotificationHelper.EXTRA_OPEN_TAB) } ?: Tab.HOME).ordinal)
                }
                val tab = Tab.entries[tabIndex]
                // „Wstecz" z dowolnej zakładki wraca na ekran główny, a dopiero stamtąd zamyka aplikację.
                androidx.activity.compose.BackHandler(enabled = tab != Tab.HOME) { tabIndex = Tab.HOME.ordinal }

                val snackbar = remember { SnackbarHostState() }
                val haptics = LocalHapticFeedback.current
                val undoLabel = stringResource(R.string.snackbar_undo)
                val restoreLabel = stringResource(R.string.snackbar_restore)

                /** Dolewka z ekranu: wibracja, krótkie potwierdzenie i „Cofnij" — jak na widgecie. */
                fun addWithFeedback(ml: Int) {
                    scope.launch {
                        repo.addWater(ml)
                        updateAllWidgets(applicationContext)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        snackbar.currentSnackbarData?.dismiss()
                        val result = snackbar.showSnackbar(
                            message = getString(R.string.snackbar_added, formatMl(ml)),
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            repo.undoLast()
                            updateAllWidgets(applicationContext)
                        }
                    }
                }

                /** Usunięcie wpisu z możliwością przywrócenia, zamiast kasowania bez śladu. */
                fun deleteWithFeedback(id: Long, ml: Int) {
                    scope.launch {
                        if (!repo.deleteIntakeKeeping(id)) return@launch
                        updateAllWidgets(applicationContext)
                        snackbar.currentSnackbarData?.dismiss()
                        val result = snackbar.showSnackbar(
                            message = getString(R.string.snackbar_deleted, formatMl(ml)),
                            actionLabel = restoreLabel,
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            repo.restoreUndone()
                            updateAllWidgets(applicationContext)
                        }
                    }
                }

                var editor by remember { mutableStateOf<EditorTarget?>(null) }

                LaunchedEffect(requestedTab) {
                    Tab.entries.firstOrNull { it.name == requestedTab }?.let { tabIndex = it.ordinal }
                    requestedTab = null
                }

                val current = state
                if (onboarding == null) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().background(KropiColors.background))
                    return@MaterialTheme
                }
                if (onboarding == true && current != null) {
                    OnboardingScreen(
                        initial = current.settings,
                        onRequestNotifications = { askNotificationPermission() },
                        onPinWidget = { pinWidget(com.kropi.hydration.widget.HydrationWidgetReceiver::class.java) },
                        onFinish = { settings ->
                            scope.launch {
                                repo.completeOnboarding(settings)
                                updateAllWidgets(applicationContext)
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
                                updateAllWidgets(applicationContext)
                            }
                            editor = null
                        },
                        onDelete = target.intake?.let { old ->
                            {
                                deleteWithFeedback(old.id, old.ml)
                                editor = null
                            }
                        },
                        onDismiss = { editor = null },
                    )
                }
                Scaffold(
                    containerColor = KropiColors.background,
                    snackbarHost = {
                        SnackbarHost(snackbar) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = KropiColors.secondary,
                                contentColor = KropiColors.foreground,
                                actionColor = KropiColors.aqua,
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar(containerColor = KropiColors.card) {
                            Tab.entries.forEach { t ->
                                NavigationBarItem(
                                    selected = tab == t,
                                    onClick = { tabIndex = t.ordinal },
                                    icon = { Text(t.emoji, fontSize = 18.sp) },
                                    label = { Text(stringResource(t.labelRes), fontSize = 10.sp) },
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
                                    onAdd = { ml -> addWithFeedback(ml) },
                                    onUndo = { scope.launch { repo.undoLast(); updateAllWidgets(applicationContext) } },
                                    onPoke = { scope.launch { repo.poke() } },
                                    onSetGoal = { ml -> scope.launch { repo.setGoal(ml); updateAllWidgets(applicationContext) } },
                                    onAddEntry = { editor = EditorTarget(null, null) },
                                    onEditIntake = { intake -> editor = EditorTarget(intake, null) },
                                    onDeleteIntake = { id -> deleteWithFeedback(id, current.intakes.firstOrNull { it.id == id }?.ml ?: 0) },
                                    onOpenSettings = { tabIndex = Tab.SETTINGS.ordinal },
                                )
                                Tab.WIDGET -> WidgetScreen(
                                    state = current,
                                    onAdd = { ml -> addWithFeedback(ml) },
                                    onUndo = { scope.launch { repo.undoLast(); updateAllWidgets(applicationContext) } },
                                    onPoke = { scope.launch { repo.poke() } },
                                    onPin = { pinWidget(com.kropi.hydration.widget.HydrationWidgetReceiver::class.java) },
                                    onPinGlass = { pinWidget(com.kropi.hydration.widget.GlassWidgetReceiver::class.java) },
                                    onPinWeek = { pinWidget(com.kropi.hydration.widget.WeekWidgetReceiver::class.java) },
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
                                            updateAllWidgets(applicationContext)
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

    private fun pinWidget(receiver: Class<*>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val appWidgetManager = getSystemService(AppWidgetManager::class.java)
        val provider = ComponentName(this, receiver)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(provider, null, null)
        }
    }
}

/** Cel edytora: null w [intake] = nowy wpis. */
private class EditorTarget(val intake: com.kropi.hydration.data.Intake?, val epochDay: Long?)

private enum class Tab(@androidx.annotation.StringRes val labelRes: Int, val emoji: String) {
    HOME(R.string.tab_home, "🏠"),
    WIDGET(R.string.tab_widget, "🔲"),
    HISTORY(R.string.tab_history, "📅"),
    INSIGHTS(R.string.tab_insights, "💡"),
    SETTINGS(R.string.tab_settings, "⚙️"),
}
