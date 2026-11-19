package com.kropi.hydration.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.widget.HydrationWidget
import kotlinx.coroutines.launch

/**
 * Pełny edytor wpisu otwierany z widgetu („＋ wpis"): ten sam arkusz co w aplikacji —
 * ilość, napój, dzień i godzina (łyk z przeszłości), notatka — ale bez wchodzenia
 * w całą aplikację. Okno jest przezroczyste, więc arkusz wysuwa się nad ekranem głównym.
 */
class QuickEntryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = HydrationRepository(applicationContext)

        setContent {
            MaterialTheme {
                var net by remember { mutableStateOf(true) }
                var drink by remember { mutableStateOf<Drink?>(null) }
                LaunchedEffect(Unit) {
                    val state = repository.current()
                    net = state.settings.netHydration
                    drink = state.widgetDrink
                }
                // Czekamy na napój wybrany na widgecie, żeby arkusz od razu go zaznaczył.
                drink?.let { picked ->
                    IntakeEditorSheet(
                        initial = null,
                        netHydration = net,
                        defaultDrink = picked,
                        onSave = { ml, kind, note, day, minute ->
                            lifecycleScope.launch {
                                repository.addIntake(ml, kind, note, day, minute)
                                if (repository.widgetDrink() != Drink.WATER) repository.setWidgetDrink(Drink.WATER)
                                HydrationWidget().updateAll(applicationContext)
                                finish()
                            }
                        },
                        onDelete = null,
                        onDismiss = { finish() },
                    )
                }
            }
        }
    }
}
