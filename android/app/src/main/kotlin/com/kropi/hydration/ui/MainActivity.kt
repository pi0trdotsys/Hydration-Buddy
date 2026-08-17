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

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            MaterialTheme {
                val state by repo.state.collectAsState(initial = null)
                val scope = rememberCoroutineScope()
                LaunchedEffect(Unit) { repo.current() }
                var tab by remember { mutableStateOf(Tab.HOME) }

                val current = state
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
                                )
                                Tab.WIDGET -> WidgetScreen(
                                    state = current,
                                    onAdd = { ml -> scope.launch { repo.addWater(ml); HydrationWidget().updateAll(applicationContext) } },
                                    onUndo = { scope.launch { repo.undoLast(); HydrationWidget().updateAll(applicationContext) } },
                                    onPoke = { scope.launch { repo.poke() } },
                                    onPin = { pinWidget() },
                                )
                                Tab.HISTORY -> HistoryScreen(state = current)
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

private enum class Tab(val label: String, val emoji: String) {
    HOME("Główny", "🏠"),
    WIDGET("Widget", "🔲"),
    HISTORY("Historia", "📅"),
    INSIGHTS("Treści", "💡"),
    SETTINGS("Ustawienia", "⚙️"),
}
