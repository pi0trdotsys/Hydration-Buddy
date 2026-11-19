package com.kropi.hydration.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.kropi.hydration.data.HistoryDay
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.WidgetConfig
import com.kropi.hydration.notifications.NotificationHelper
import com.kropi.hydration.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget tygodniowy: siedem słupków z realizacją celu, seria i średnia. Dotknięcie otwiera
 * Historię w aplikacji. Niczego tu nie dolewa się — to tablica wyników na ekran główny.
 */
class WeekWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = HydrationRepository(context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initial = repository.current()
        provideContent {
            val state by repository.state.collectAsState(initial = initial)
            val config by remember(appWidgetId) { repository.widgetConfig(appWidgetId) }
                .collectAsState(initial = WidgetConfig.default(initial.settings.bottlesMl))
            WidgetTheme(config.style) { WeekContent(state) }
        }
    }
}

class WeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekWidget()

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
private fun WeekContent(state: HydrationState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val pad = 12.dp
    val days = state.history
    val tracked = days.filter { it.ml > 0 }
    val average = if (tracked.isEmpty()) 0 else tracked.sumOf { it.ml } / tracked.size
    val reached = days.count { it.reached }

    val headerHeight = 16.dp
    val footerHeight = 18.dp
    val chartHeight = (size.height - pad * 2 - headerHeight - footerHeight - 12.dp).coerceAtLeast(40.dp)
    val chartWidth = size.width - pad * 2

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(CardBg)
            .cornerRadius(28.dp)
            .padding(pad)
            .clickable(
                actionStartActivity(
                    Intent(context, MainActivity::class.java)
                        .putExtra(NotificationHelper.EXTRA_OPEN_TAB, "HISTORY")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ),
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth().height(headerHeight)) {
            Text(
                "OSTATNIE 7 DNI",
                style = TextStyle(color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                "🔥 ${state.streak} dni",
                style = TextStyle(color = if (state.streak > 0) Aqua else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }
        Box(modifier = GlanceModifier.padding(top = 6.dp)) {
            WeekBars(days, chartWidth, chartHeight)
        }
        Box(modifier = GlanceModifier.padding(top = 6.dp)) {
            Text(
                if (tracked.isEmpty()) "Za kilka dni pojawi się tu średnia."
                else "Średnio $average ml · cel zaliczony $reached / ${days.size}",
                style = TextStyle(color = Muted, fontSize = 9.sp),
                maxLines = 1,
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WeekBars(days: List<HistoryDay>, width: Dp, height: Dp) {
    val density = LocalContext.current.resources.displayMetrics.density
    val widthPx = (width.value * density).toInt()
    val heightPx = (height.value * density).toInt()
    val graphics = LocalPalette.current.graphics
    val bitmap = remember(days, widthPx, heightPx, graphics) {
        weekBarsBitmap(
            widthPx = widthPx,
            heightPx = heightPx,
            density = density,
            bars = days.mapIndexed { i, d -> WeekBar(d.day, d.pct, d.reached, isToday = i == days.lastIndex) },
            palette = graphics,
        )
    }
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = "Nawodnienie w ostatnich 7 dniach",
        contentScale = ContentScale.Fit,
        modifier = GlanceModifier.fillMaxWidth().height(height),
    )
}
