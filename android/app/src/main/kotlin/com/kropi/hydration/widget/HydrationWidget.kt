package com.kropi.hydration.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.Intake
import com.kropi.hydration.data.Level

private val SizeSmall = DpSize(120.dp, 120.dp)
private val SizeMedium = DpSize(250.dp, 130.dp)
private val SizeLarge = DpSize(250.dp, 280.dp)

private val Aqua = ColorProvider(com.kropi.hydration.ui.KropiColors.aqua)
private val AquaMuted = ColorProvider(com.kropi.hydration.ui.KropiColors.aqua.copy(alpha = 0.55f))
private val Foreground = ColorProvider(com.kropi.hydration.ui.KropiColors.foreground)
private val Muted = ColorProvider(com.kropi.hydration.ui.KropiColors.mutedForeground)
private val Secondary = ColorProvider(com.kropi.hydration.ui.KropiColors.secondary)
private val CardBg = ColorProvider(com.kropi.hydration.ui.KropiColors.card)

class HydrationWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SizeSmall, SizeMedium, SizeLarge))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = HydrationRepository(context).current()
        provideContent {
            HydrationWidgetContent(state)
        }
    }
}

class HydrationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HydrationWidget()
}

@androidx.compose.runtime.Composable
private fun HydrationWidgetContent(state: HydrationState) {
    val size = LocalSize.current
    val variant = when {
        size.width < 160.dp -> "sm"
        size.height < 200.dp -> "md"
        else -> "lg"
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(CardBg)
            .cornerRadius(28.dp)
            .padding(if (variant == "sm") 10.dp else 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (variant) {
            "sm" -> SmallContent(state)
            "md" -> MediumLargeContent(state, large = false)
            else -> MediumLargeContent(state, large = true)
        }
    }
}

@androidx.compose.runtime.Composable
private fun SmallContent(state: HydrationState) {
    val pct = (state.progress * 100).toInt()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RingWithMascot(progress = state.progress.toFloat(), level = state.level, ringSize = 84.dp, mascotSize = 38.dp)
        Spacer(GlanceModifier.height(2.dp))
        Text("$pct%", style = TextStyle(color = Foreground, fontSize = 16.sp, fontWeight = FontWeight.Bold))
        Text(
            "${state.total} / ${state.goal} ml",
            style = TextStyle(color = Muted, fontSize = 9.sp),
        )
    }
}

@androidx.compose.runtime.Composable
private fun MediumLargeContent(state: HydrationState, large: Boolean) {
    val pct = (state.progress * 100).toInt()

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            RingWithPercent(progress = state.progress.toFloat(), pct = pct, ringSize = if (large) 74.dp else 58.dp)
            Spacer(GlanceModifier.width(12.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    "Nawodnienie • ${state.streak} dni",
                    style = TextStyle(color = Muted, fontSize = 9.sp),
                    maxLines = 1,
                )
                Text(
                    "${state.total} / ${state.goal} ml",
                    style = TextStyle(color = Foreground, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Text(
                    if (state.remaining > 0) "Zostało ${state.remaining} ml" else "Cel osiągnięty 🎉",
                    style = TextStyle(color = Aqua, fontSize = 9.sp),
                    maxLines = 1,
                )
                if (large) {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        state.selfCare,
                        style = TextStyle(color = Foreground, fontSize = 10.sp),
                        maxLines = 2,
                    )
                }
            }
            if (large) {
                Spacer(GlanceModifier.width(8.dp))
                MascotImage(level = state.level, sizeDp = 56.dp)
            }
        }

        Spacer(GlanceModifier.height(if (large) 10.dp else 6.dp))
        BottleRow(compact = !large)

        if (large) {
            Spacer(GlanceModifier.height(10.dp))
            HourlyChart(state.intakes, state.goal)
            Spacer(GlanceModifier.height(8.dp))
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(Secondary)
                    .cornerRadius(14.dp)
                    .padding(8.dp),
            ) {
                Text(
                    "Czy wiesz, że… ${state.fact}",
                    style = TextStyle(color = Muted, fontSize = 9.sp),
                    maxLines = 3,
                )
            }
        }
    }
}

/** Glance has no compose-ui LocalDensity; widgets only ever run on the device, so convert by hand. */
@androidx.compose.runtime.Composable
private fun dpToPx(dp: androidx.compose.ui.unit.Dp): Int {
    val ctx = androidx.glance.LocalContext.current
    return (dp.value * ctx.resources.displayMetrics.density).toInt()
}

@androidx.compose.runtime.Composable
private fun RingWithMascot(progress: Float, level: Level, ringSize: androidx.compose.ui.unit.Dp, mascotSize: androidx.compose.ui.unit.Dp) {
    val ringPx = dpToPx(ringSize)
    val mascotPx = dpToPx(mascotSize)
    Box(modifier = GlanceModifier.size(ringSize), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(WidgetGraphics.progressRing(ringPx, ringPx * 0.09f, progress)),
            contentDescription = null,
            modifier = GlanceModifier.size(ringSize),
        )
        Image(
            provider = ImageProvider(WidgetGraphics.mascot(mascotPx, level)),
            contentDescription = "Kropi",
            modifier = GlanceModifier.size(mascotSize).clickable(actionRunCallback<PokeMascotAction>()),
        )
    }
}

@androidx.compose.runtime.Composable
private fun RingWithPercent(progress: Float, pct: Int, ringSize: androidx.compose.ui.unit.Dp) {
    val ringPx = dpToPx(ringSize)
    Box(modifier = GlanceModifier.size(ringSize), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(WidgetGraphics.progressRing(ringPx, ringPx * 0.1f, progress)),
            contentDescription = null,
            modifier = GlanceModifier.size(ringSize),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$pct%", style = TextStyle(color = Foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@androidx.compose.runtime.Composable
private fun MascotImage(level: Level, sizeDp: androidx.compose.ui.unit.Dp) {
    val px = dpToPx(sizeDp)
    Image(
        provider = ImageProvider(WidgetGraphics.mascot(px, level)),
        contentDescription = "Kropi",
        modifier = GlanceModifier.size(sizeDp).clickable(actionRunCallback<PokeMascotAction>()),
    )
}

private val WidgetBottles = listOf(100, 250, 500, 750)

@androidx.compose.runtime.Composable
private fun BottleRow(compact: Boolean) {
    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        for (ml in WidgetBottles) {
            BottleChip(ml, compact)
            Spacer(GlanceModifier.width(4.dp))
        }
        UndoChip()
    }
}

@androidx.compose.runtime.Composable
private fun RowScope.BottleChip(ml: Int, compact: Boolean) {
    val fill = (ml / 750f).coerceIn(0.15f, 1f)
    Box(
        modifier = GlanceModifier
            .defaultWeight()
            .height(if (compact) 52.dp else 58.dp)
            .background(Secondary)
            .cornerRadius(14.dp)
            .clickable(actionRunCallback<AddWaterAction>(actionParametersOf(MlKey to ml)))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BottleGlyph(fill)
            Text("$ml", style = TextStyle(color = Foreground, fontSize = 8.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@androidx.compose.runtime.Composable
private fun BottleGlyph(fill: Float) {
    Box(
        modifier = GlanceModifier.width(4.dp).height(3.dp).background(AquaMuted).cornerRadius(1.dp),
    ) {}
    Spacer(GlanceModifier.height(1.dp))
    Box(
        modifier = GlanceModifier.width(14.dp).height(24.dp).background(AquaMuted).cornerRadius(5.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(1.5.dp)
                .height((24 * fill).dp)
                .background(Aqua)
                .cornerRadius(4.dp),
        ) {}
    }
}

@androidx.compose.runtime.Composable
private fun UndoChip() {
    Box(
        modifier = GlanceModifier
            .width(32.dp)
            .height(58.dp)
            .background(Secondary)
            .cornerRadius(14.dp)
            .clickable(actionRunCallback<UndoWaterAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Text("↺", style = TextStyle(color = Muted, fontSize = 16.sp))
    }
}

@androidx.compose.runtime.Composable
private fun HourlyChart(intakes: List<Intake>, goal: Int) {
    val buckets = (0 until 8).map { i ->
        val hStart = 6 + i * 2
        val hEnd = hStart + 1
        intakes.filter { it.hour == hStart || it.hour == hEnd }.sumOf { it.ml }
    }
    val max = (goal / 4).coerceAtLeast(buckets.maxOrNull() ?: 1)
    Row(modifier = GlanceModifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.Bottom) {
        for (ml in buckets) {
            val h = ((ml.toFloat() / max) * 30f).coerceIn(2f, 30f)
            Box(modifier = GlanceModifier.defaultWeight().padding(horizontal = 1.dp), contentAlignment = Alignment.BottomCenter) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(h.dp)
                        .background(if (ml > 0) Aqua else Secondary)
                        .cornerRadius(3.dp),
                ) {}
            }
        }
    }
}
