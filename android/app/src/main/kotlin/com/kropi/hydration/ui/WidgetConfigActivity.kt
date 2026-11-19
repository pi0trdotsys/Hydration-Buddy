package com.kropi.hydration.ui

import android.app.Activity
import com.kropi.hydration.widget.updateAllWidgets
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.kropi.hydration.data.ButtonSpec
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.WidgetConfig
import com.kropi.hydration.data.WidgetStyle
import kotlinx.coroutines.launch

private val SLOT_AMOUNTS = listOf(100, 150, 200, 250, 300, 330, 400, 500, 750, 1000)

/**
 * Konfiguracja jednego widgetu: styl kolorów i (dla głównego widgetu) co robi każdy
 * z czterech przycisków — sama ilość wody, napój wybrany na pasku albo stały „kawa 200".
 * Launcher otwiera ją przy dodawaniu (opcjonalnie) i z menu „Konfiguruj" po przytrzymaniu widgetu.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Anulowanie (np. cofnięcie) ma sprawić, że launcher nie doda widgetu.
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val repository = HydrationRepository(applicationContext)
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider?.className.orEmpty()
        // Przyciski dolewania ma tylko główny widget; pozostałe mają tylko styl.
        val hasButtons = provider.endsWith("HydrationWidgetReceiver")

        setContent {
            MaterialTheme {
                var config by remember { mutableStateOf<WidgetConfig?>(null) }
                LaunchedEffect(Unit) { config = repository.widgetConfigOnce(appWidgetId) }

                val current = config
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KropiColors.background)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                ) {
                    if (current == null) return@Column
                    ConfigBody(
                        config = current,
                        hasButtons = hasButtons,
                        defaultBottles = { repository },
                        onChange = { config = it },
                        onSave = {
                            lifecycleScope.launch {
                                repository.saveWidgetConfig(appWidgetId, it)
                                updateAllWidgets(applicationContext)
                                setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                                finish()
                            }
                        },
                        onCancel = { finish() },
                    )
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ConfigBody(
    config: WidgetConfig,
    hasButtons: Boolean,
    defaultBottles: () -> HydrationRepository,
    onChange: (WidgetConfig) -> Unit,
    onSave: (WidgetConfig) -> Unit,
    onCancel: () -> Unit,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Ustawienia widgetu", color = KropiColors.foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Text("Styl kolorów", color = KropiColors.mutedForeground, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetStyle.entries.forEach { style ->
                ChipButton(style.label, config.style == style) { onChange(config.copy(style = style)) }
            }
        }
        Text(
            config.style.hint + if (config.style == WidgetStyle.MATERIAL_YOU && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) " (Ten telefon ma starszy Android, więc zostanie paleta Kropi.)" else "",
            color = KropiColors.mutedForeground,
            fontSize = 11.sp,
        )

        if (hasButtons) {
            Text("Gotowe zestawy", color = KropiColors.mutedForeground, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                ChipButton("💧 Woda", false) {
                    scope.launch {
                        val bottles = defaultBottles().current().settings.bottlesMl
                        onChange(config.copy(slots = WidgetConfig.default(bottles).slots))
                    }
                }
                ChipButton("☕ Kawa i herbata", false) {
                    onChange(config.copy(slots = listOf(
                        ButtonSpec(Drink.COFFEE, 200), ButtonSpec(Drink.COFFEE, 300),
                        ButtonSpec(Drink.TEA, 250), ButtonSpec(Drink.TEA, 400),
                    )))
                }
                ChipButton("🧃 Mieszane", false) {
                    onChange(config.copy(slots = listOf(
                        ButtonSpec(Drink.WATER, 250), ButtonSpec(Drink.COFFEE, 200),
                        ButtonSpec(Drink.TEA, 250), ButtonSpec(Drink.JUICE, 200),
                    )))
                }
            }

            config.slots.forEachIndexed { index, spec ->
                SlotEditor(index + 1, spec) { updated ->
                    onChange(config.copy(slots = config.slots.mapIndexed { i, s -> if (i == index) updated else s }))
                }
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
            modifier = Modifier.weight(1f),
        ) { Text("Anuluj") }
        Button(
            onClick = { onSave(config) },
            colors = ButtonDefaults.buttonColors(containerColor = KropiColors.aqua, contentColor = KropiColors.background),
            modifier = Modifier.weight(2f),
        ) { Text("Zapisz", fontWeight = FontWeight.Bold) }
    }
}

/** Jeden przycisk widgetu: czym jest (napój z paska / woda / konkretny napój) i ile ml. */
@Composable
private fun SlotEditor(number: Int, spec: ButtonSpec, onChange: (ButtonSpec) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Przycisk $number", color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            ChipButton("Z paska", spec.drink == null) { onChange(spec.copy(drink = null)) }
            Drink.entries.forEach { d ->
                ChipButton("${d.emoji} ${d.label}", spec.drink == d) { onChange(spec.copy(drink = d)) }
            }
        }
        Text(
            if (spec.drink == null) "Zapisuje napój zaznaczony na pasku widgetu (domyślnie wodę)."
            else "Zawsze zapisuje: ${spec.drink.label.lowercase()}.",
            color = KropiColors.mutedForeground,
            fontSize = 11.sp,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepperButton("−") { onChange(spec.copy(ml = (spec.ml - 10).coerceAtLeast(ButtonSpec.MIN_ML))) }
            Text("${spec.ml} ml", color = KropiColors.foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
            StepperButton("+") { onChange(spec.copy(ml = (spec.ml + 10).coerceAtMost(ButtonSpec.MAX_ML))) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            SLOT_AMOUNTS.forEach { ml -> ChipButton("$ml", spec.ml == ml) { onChange(spec.copy(ml = ml)) } }
        }
    }
}
