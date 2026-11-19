package com.kropi.hydration.widget

import com.kropi.hydration.R
import androidx.glance.semantics.semantics
import androidx.glance.semantics.contentDescription
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.WidgetConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget 1×1 „dolej szklankę": pierścień z procentem i jedno wielkie dotknięcie, które zapisuje
 * szklankę wody (rozmiar z Ustawień). Dla kogoś, kto nie potrzebuje wykresu ani planu.
 */
class GlassWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = HydrationRepository(context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initial = repository.current()
        provideContent {
            val state by repository.state.collectAsState(initial = initial)
            val config by remember(appWidgetId) { repository.widgetConfig(appWidgetId) }
                .collectAsState(initial = WidgetConfig.default(initial.settings.bottlesMl))
            WidgetTheme(config.style) { GlassContent(state) }
        }
    }
}

class GlassWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlassWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        com.kropi.hydration.notifications.MidnightRollWorker.schedule(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        CoroutineScope(Dispatchers.IO).launch { HydrationRepository(context).removeWidgetConfigs(appWidgetIds) }
    }
}

@Composable
private fun GlassContent(state: HydrationState) {
    val size = LocalSize.current
    val glass = state.settings.reminderGlassMl
    val behind = state.isBehindSchedule && state.remaining > 0
    val rootDescription = str(R.string.a11y_glass_widget, glass, (state.progress * 100).toInt())
    // Pierścień dostaje całe miejsce poza paddingiem i linijką „+250 ml" pod spodem.
    val ringSize = (minOf(size.width, size.height) - 20.dp - 16.dp).coerceAtLeast(36.dp)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(CardBg)
            .cornerRadius(24.dp)
            .padding(10.dp)
            // Cały kafelek to jeden przycisk: szklanka wody, bez względu na napój wybrany w głównym widgecie.
            .clickable(actionRunCallback<AddWaterAction>(actionParametersOf(MlKey to glass, DrinkKey to Drink.WATER.name)))
            .semantics { contentDescription = rootDescription },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RingWithPercent(
                progress = state.progress.toFloat(),
                pct = (state.progress * 100).toInt(),
                ringSize = ringSize,
                behind = behind,
            )
            Text(
                "+$glass ml",
                style = TextStyle(color = if (behind) Warn else Aqua, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }
    }
}
