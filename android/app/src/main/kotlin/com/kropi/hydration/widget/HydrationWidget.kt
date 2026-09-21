package com.kropi.hydration.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.glance.layout.ContentScale
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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.Level
import com.kropi.hydration.data.paceLine
import com.kropi.hydration.data.plan
import com.kropi.hydration.data.summaryLine
import java.time.LocalTime

private val SizeSmall = DpSize(120.dp, 120.dp)
private val SizeMedium = DpSize(250.dp, 130.dp)
private val SizeLarge = DpSize(250.dp, 280.dp)

private val Aqua = ColorProvider(com.kropi.hydration.ui.KropiColors.aqua)
private val AquaMuted = ColorProvider(com.kropi.hydration.ui.KropiColors.aqua.copy(alpha = 0.55f))
private val Foreground = ColorProvider(com.kropi.hydration.ui.KropiColors.foreground)
private val Muted = ColorProvider(com.kropi.hydration.ui.KropiColors.mutedForeground)
private val Secondary = ColorProvider(com.kropi.hydration.ui.KropiColors.secondary)
private val CardBg = ColorProvider(com.kropi.hydration.ui.KropiColors.card)
private val Warn = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFF8A65))

class HydrationWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SizeSmall, SizeMedium, SizeLarge))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = HydrationRepository(context)
        // Przewinięcie doby (i zasiew przy pierwszym uruchomieniu) robimy raz,
        // przed kompozycją — to jedyny zapis, jaki widget wykonuje sam z siebie.
        val initial = repository.current()
        provideContent {
            // Stan zbieramy jako Flow WEWNĄTRZ kompozycji. Wcześniej był czytany
            // jednorazowo przed provideContent, więc kolejne update() trafiały na
            // tę samą, zamrożoną wartość: drugie dolanie wody pod rząd nie
            // zmieniało już nic na ekranie głównym. Teraz każdy zapis do
            // DataStore odświeża widget sam z siebie.
            val state by repository.state.collectAsState(initial = initial)
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
            .padding(if (variant == "sm") 10.dp else 14.dp),
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
    val plan = state.plan()

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            RingWithPercent(progress = state.progress.toFloat(), pct = pct, ringSize = if (large) 70.dp else 58.dp)
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
                Text(
                    state.paceLine(plan),
                    style = TextStyle(
                        color = if (state.isBehindSchedule) Warn else Muted,
                        fontSize = 9.sp,
                        fontWeight = if (state.isBehindSchedule) FontWeight.Bold else FontWeight.Normal,
                    ),
                    maxLines = 1,
                )
                if (large) {
                    Text(
                        state.selfCare,
                        style = TextStyle(color = Muted, fontSize = 9.sp),
                        maxLines = 2,
                    )
                }
            }
            if (large) {
                Spacer(GlanceModifier.width(8.dp))
                MascotImage(level = state.level, sizeDp = 52.dp)
            }
        }

        Spacer(GlanceModifier.height(if (large) 8.dp else 6.dp))
        BottleRow(compact = !large)

        if (large) {
            Spacer(GlanceModifier.height(8.dp))
            IntakeChartImage(state, heightDp = 56.dp)
            Text(
                "━ wypite   ┈ plan dnia",
                style = TextStyle(color = Muted, fontSize = 8.sp),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                plan.summaryLine(),
                style = TextStyle(color = Aqua, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                maxLines = 2,
            )
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
private fun IntakeChartImage(state: HydrationState, heightDp: androidx.compose.ui.unit.Dp) {
    val density = androidx.glance.LocalContext.current.resources.displayMetrics.density
    // LocalSize w trybie Responsive zwraca rozmiar kubełka, nie fizyczną
    // szerokość — bitmapa jest potem dociągana do boksu przez FillBounds.
    val widthPx = ((LocalSize.current.width - 28.dp).value * density).toInt()
    val heightPx = (heightDp.value * density).toInt()
    val now = LocalTime.now()
    val nowMinutes = now.hour * 60 + now.minute

    val bitmap = remember(state.intakes, state.goal, widthPx, nowMinutes) {
        intakeChartBitmap(
            widthPx = widthPx,
            heightPx = heightPx,
            density = density,
            intakes = state.intakes,
            goalMl = state.goal,
            startHour = state.settings.activeStartHour,
            endHour = state.settings.activeEndHour,
            nowMinutes = nowMinutes,
        )
    }
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = "Wypita woda w czasie: ${state.total} ml z ${state.goal} ml",
        contentScale = ContentScale.FillBounds,
        modifier = GlanceModifier.fillMaxWidth().height(heightDp),
    )
}

@androidx.compose.runtime.Composable
private fun RingWithMascot(progress: Float, level: Level, ringSize: androidx.compose.ui.unit.Dp, mascotSize: androidx.compose.ui.unit.Dp) {
    val ringPx = dpToPx(ringSize)
    val mascotPx = dpToPx(mascotSize)
    Box(modifier = GlanceModifier.size(ringSize), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(
                remember(ringPx, progress) { WidgetGraphics.progressRing(ringPx, ringPx * 0.09f, progress) },
            ),
            contentDescription = null,
            modifier = GlanceModifier.size(ringSize),
        )
        Image(
            provider = ImageProvider(remember(mascotPx, level) { WidgetGraphics.mascot(mascotPx, level) }),
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
            provider = ImageProvider(
                remember(ringPx, progress) { WidgetGraphics.progressRing(ringPx, ringPx * 0.1f, progress) },
            ),
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
        provider = ImageProvider(remember(px, level) { WidgetGraphics.mascot(px, level) }),
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
        UndoChip(compact)
    }
}

@androidx.compose.runtime.Composable
private fun RowScope.BottleChip(ml: Int, compact: Boolean) {
    val fill = (ml / 750f).coerceIn(0.15f, 1f)
    Box(
        modifier = GlanceModifier
            .defaultWeight()
            .height(if (compact) 52.dp else 54.dp)
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
private fun UndoChip(compact: Boolean) {
    Box(
        modifier = GlanceModifier
            .width(32.dp)
            .height(if (compact) 52.dp else 54.dp)
            .background(Secondary)
            .cornerRadius(14.dp)
            .clickable(actionRunCallback<UndoWaterAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Text("↺", style = TextStyle(color = Muted, fontSize = 16.sp))
    }
}
