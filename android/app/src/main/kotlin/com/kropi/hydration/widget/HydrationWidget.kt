package com.kropi.hydration.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
import androidx.glance.layout.fillMaxHeight
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
import com.kropi.hydration.data.HydrationPlan
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.Level
import com.kropi.hydration.data.PlanStatus
import com.kropi.hydration.data.formatMl
import com.kropi.hydration.data.hhmm
import com.kropi.hydration.data.plan
import java.time.LocalTime

private val Aqua = ColorProvider(com.kropi.hydration.ui.KropiColors.aqua)
private val AquaMuted = ColorProvider(com.kropi.hydration.ui.KropiColors.aqua.copy(alpha = 0.55f))
private val Foreground = ColorProvider(com.kropi.hydration.ui.KropiColors.foreground)
private val Muted = ColorProvider(com.kropi.hydration.ui.KropiColors.mutedForeground)
private val Secondary = ColorProvider(com.kropi.hydration.ui.KropiColors.secondary)
private val CardBg = ColorProvider(com.kropi.hydration.ui.KropiColors.card)
private val ScreenBg = ColorProvider(com.kropi.hydration.ui.KropiColors.background)
private val Warn = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFF8A65))

private enum class Variant { TINY, BAR, COMPACT, FULL }

class HydrationWidget : GlanceAppWidget() {

    /**
     * [SizeMode.Exact], nie Responsive: przy kubełkach Glance zwraca rozmiar
     * kubełka, a nie kafelka, więc wykres renderował się dla 250 dp i był
     * rozciągany na faktyczne ~370 dp (rozmyte podpisy), a dolne 30% widgetu
     * zostawało puste. Z Exact znamy prawdziwe wymiary i możemy rozdać
     * wysokość tam, gdzie coś wnosi — czyli wykresowi.
     */
    override val sizeMode = SizeMode.Exact

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
        size.height < 120.dp && size.width < 180.dp -> Variant.TINY
        size.height < 150.dp -> Variant.BAR
        size.height < 250.dp -> Variant.COMPACT
        else -> Variant.FULL
    }
    val pad = if (variant == Variant.TINY) 10.dp else 14.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(CardBg)
            .cornerRadius(28.dp)
            .padding(pad),
        contentAlignment = Alignment.Center,
    ) {
        when (variant) {
            Variant.TINY -> TinyContent(state)
            Variant.BAR -> BarContent(state)
            Variant.COMPACT -> CompactContent(state)
            Variant.FULL -> FullContent(state, size.width, size.height, pad)
        }
    }
}

@androidx.compose.runtime.Composable
private fun TinyContent(state: HydrationState) {
    val pct = (state.progress * 100).toInt()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RingWithMascot(progress = state.progress.toFloat(), level = state.level, ringSize = 84.dp, mascotSize = 38.dp)
        Spacer(GlanceModifier.height(2.dp))
        Text("$pct%", style = TextStyle(color = Foreground, fontSize = 16.sp, fontWeight = FontWeight.Bold))
        Text("${state.total} / ${state.goal} ml", style = TextStyle(color = Muted, fontSize = 9.sp))
    }
}

/**
 * Kafelek 2×1 / 4×1 — niski i szeroki. Nie mieści wykresu ani pigułek, więc
 * zostaje to, co realnie potrzebne pod ręką: postęp, najbliższa porcja i dwa
 * najczęstsze przyciski dolewania.
 */
@androidx.compose.runtime.Composable
private fun BarContent(state: HydrationState) {
    val plan = state.plan()
    val bottles = state.settings.bottlesMl
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxSize()) {
        RingWithPercent(progress = state.progress.toFloat(), pct = (state.progress * 100).toInt(), ringSize = 46.dp)
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                "${state.total} / ${state.goal} ml",
                style = TextStyle(color = Foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                when (plan.status) {
                    PlanStatus.DONE -> "Cel zrobiony 🎉"
                    else -> plan.next?.let { "nast. ${it.ml} ml o ${it.time.hhmm()}" } ?: "—"
                },
                style = TextStyle(color = if (state.isBehindSchedule) Warn else Muted, fontSize = 9.sp),
                maxLines = 1,
            )
        }
        FixedBottleChip(bottles[1], 46.dp)
        Spacer(GlanceModifier.width(6.dp))
        FixedBottleChip(bottles[2], 46.dp)
    }
}

@androidx.compose.runtime.Composable
private fun FixedBottleChip(ml: Int, size: Dp) {
    Box(
        modifier = GlanceModifier
            .width(size)
            .height(size)
            .background(Secondary)
            .cornerRadius(14.dp)
            .clickable(actionRunCallback<AddWaterAction>(actionParametersOf(MlKey to ml))),
        contentAlignment = Alignment.Center,
    ) {
        Text("+$ml", style = TextStyle(color = Foreground, fontSize = 11.sp, fontWeight = FontWeight.Bold))
    }
}

@androidx.compose.runtime.Composable
private fun CompactContent(state: HydrationState) {
    val plan = state.plan()
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Header(state, ringSize = 58.dp, titleSize = 16.sp, showMascot = false)
        Spacer(GlanceModifier.height(6.dp))
        StatusRow(state, plan)
        Spacer(GlanceModifier.defaultWeight())
        BottleRow(height = 50.dp, bottles = state.settings.bottlesMl)
    }
}

/**
 * Pełny kafelek. Wysokości sekcji są stałe, a wykres dostaje całą resztę —
 * dzięki temu na 4×4 nie zostaje pas pustki, a na mniejszym kafelku wykres
 * kurczy się zamiast wypychać stopkę poza krawędź.
 */
@androidx.compose.runtime.Composable
private fun FullContent(state: HydrationState, width: Dp, height: Dp, pad: Dp) {
    val plan = state.plan()

    val headerHeight = 74.dp
    val statusHeight = 30.dp
    val bottlesHeight = 54.dp
    val footerHeight = 18.dp
    val gaps = 8.dp * 4
    val reserved = headerHeight + statusHeight + bottlesHeight + footerHeight + gaps + pad * 2
    val chartHeight = (height - reserved).coerceIn(64.dp, 190.dp)

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Header(state, ringSize = headerHeight, titleSize = 20.sp, showMascot = width >= 300.dp)
        Spacer(GlanceModifier.height(8.dp))
        StatusRow(state, plan)
        Spacer(GlanceModifier.height(8.dp))
        BottleRow(height = bottlesHeight, bottles = state.settings.bottlesMl)
        Spacer(GlanceModifier.height(8.dp))
        ChartPanel(state, width = width - pad * 2, height = chartHeight)
        Spacer(GlanceModifier.height(8.dp))
        Footer(state, plan)
    }
}

@androidx.compose.runtime.Composable
private fun Header(state: HydrationState, ringSize: Dp, titleSize: TextUnit, showMascot: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
        RingWithPercent(progress = state.progress.toFloat(), pct = (state.progress * 100).toInt(), ringSize = ringSize)
        Spacer(GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                "NAWODNIENIE",
                style = TextStyle(color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Text(
                "${state.total} / ${state.goal} ml",
                style = TextStyle(color = Foreground, fontSize = titleSize, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                if (state.remaining > 0) "Zostało ${formatMl(state.remaining)}" else "Cel osiągnięty 🎉",
                style = TextStyle(color = Aqua, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
        if (showMascot) {
            Spacer(GlanceModifier.width(8.dp))
            MascotImage(level = state.level, sizeDp = ringSize - 14.dp)
        }
    }
}

/** Dwie pigułki: gdzie powinieneś być o tej porze i co jest następne w planie. */
@androidx.compose.runtime.Composable
private fun StatusRow(state: HydrationState, plan: HydrationPlan) {
    Row(modifier = GlanceModifier.fillMaxWidth().height(30.dp)) {
        Pill(
            label = if (state.isBehindSchedule) "PONIŻEJ PLANU" else "PLAN NA TERAZ",
            value = formatMl(state.targetSoFar),
            accent = if (state.isBehindSchedule) Warn else Muted,
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(GlanceModifier.width(6.dp))
        Pill(
            label = when (plan.status) {
                PlanStatus.DONE -> "DZIŚ"
                PlanStatus.AFTER_HOURS -> "PO GODZINACH"
                else -> "NASTĘPNE"
            },
            value = when (plan.status) {
                PlanStatus.DONE -> "Cel zrobiony 🎉"
                else -> plan.next?.let { "${it.ml} ml o ${it.time.hhmm()}" } ?: "—"
            },
            accent = Aqua,
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}

@androidx.compose.runtime.Composable
private fun Pill(label: String, value: String, accent: ColorProvider, modifier: GlanceModifier) {
    // Bez fillMaxWidth() w środku: w Glance nadpisuje ono defaultWeight() rodzica
    // i pierwsza pigułka zabiera cały wiersz, wypychając drugą poza kafelek.
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(Secondary)
            .cornerRadius(10.dp)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            Text(label, style = TextStyle(color = Muted, fontSize = 7.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            Text(value, style = TextStyle(color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        }
    }
}

/** Wykres w „ekranie" — ciemniejsze tło odcina go od karty i porządkuje układ. */
@androidx.compose.runtime.Composable
private fun ChartPanel(state: HydrationState, width: Dp, height: Dp) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(height)
            .background(ScreenBg)
            .cornerRadius(16.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        IntakeChartImage(state, width = width - 16.dp, height = height - 12.dp)
    }
}

/** Mikro-stopka: seria, ostatni łyk i reszta planu jednym rzutem oka. */
@androidx.compose.runtime.Composable
private fun Footer(state: HydrationState, plan: HydrationPlan) {
    val last = state.intakes.lastOrNull()
    val lastText = last?.let { String.format("%02d:%02d · %d ml", it.hour, it.minute, it.ml) } ?: "brak wpisów"
    val planText = when (plan.status) {
        PlanStatus.DONE -> "plan dnia wykonany"
        PlanStatus.AFTER_HOURS -> "brakuje ${formatMl(plan.remainingMl)}"
        else -> "jeszcze ${plan.sips.size} × ${plan.portionMl} ml do ${plan.windowEnd.hhmm()}"
    }
    Row(modifier = GlanceModifier.fillMaxWidth().height(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "🔥 ${state.streak} dni  ·  ostatnio $lastText",
            style = TextStyle(color = Muted, fontSize = 9.sp),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(planText, style = TextStyle(color = Muted, fontSize = 9.sp), maxLines = 1)
    }
}

/** Glance has no compose-ui LocalDensity; widgets only ever run on the device, so convert by hand. */
@androidx.compose.runtime.Composable
private fun dpToPx(dp: Dp): Int {
    val ctx = androidx.glance.LocalContext.current
    return (dp.value * ctx.resources.displayMetrics.density).toInt()
}

@androidx.compose.runtime.Composable
private fun IntakeChartImage(state: HydrationState, width: Dp, height: Dp) {
    val density = androidx.glance.LocalContext.current.resources.displayMetrics.density
    val widthPx = (width.value * density).toInt()
    val heightPx = (height.value * density).toInt()
    val now = LocalTime.now()
    val nowMinutes = now.hour * 60 + now.minute

    val bitmap = remember(state.intakes, state.goal, widthPx, heightPx, nowMinutes) {
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
        contentScale = ContentScale.Fit,
        modifier = GlanceModifier.fillMaxSize(),
    )
}

@androidx.compose.runtime.Composable
private fun RingWithMascot(progress: Float, level: Level, ringSize: Dp, mascotSize: Dp) {
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
private fun RingWithPercent(progress: Float, pct: Int, ringSize: Dp) {
    val ringPx = dpToPx(ringSize)
    Box(modifier = GlanceModifier.size(ringSize), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(
                remember(ringPx, progress) { WidgetGraphics.progressRing(ringPx, ringPx * 0.11f, progress) },
            ),
            contentDescription = null,
            modifier = GlanceModifier.size(ringSize),
        )
        Text(
            "$pct%",
            style = TextStyle(
                color = Foreground,
                fontSize = if (ringSize >= 70.dp) 17.sp else 14.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@androidx.compose.runtime.Composable
private fun MascotImage(level: Level, sizeDp: Dp) {
    val px = dpToPx(sizeDp)
    Image(
        provider = ImageProvider(remember(px, level) { WidgetGraphics.mascot(px, level) }),
        contentDescription = "Kropi",
        modifier = GlanceModifier.size(sizeDp).clickable(actionRunCallback<PokeMascotAction>()),
    )
}

@androidx.compose.runtime.Composable
private fun BottleRow(height: Dp, bottles: List<Int>) {
    Row(modifier = GlanceModifier.fillMaxWidth().height(height), horizontalAlignment = Alignment.Start) {
        for (ml in bottles) {
            BottleChip(ml, height)
            Spacer(GlanceModifier.width(6.dp))
        }
        UndoChip(height)
    }
}

@androidx.compose.runtime.Composable
private fun RowScope.BottleChip(ml: Int, height: Dp) {
    val fill = (ml / 750f).coerceIn(0.15f, 1f)
    Box(
        modifier = GlanceModifier
            .defaultWeight()
            .height(height)
            .background(Secondary)
            .cornerRadius(14.dp)
            .clickable(actionRunCallback<AddWaterAction>(actionParametersOf(MlKey to ml)))
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BottleGlyph(fill)
            Text("$ml", style = TextStyle(color = Foreground, fontSize = 9.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@androidx.compose.runtime.Composable
private fun BottleGlyph(fill: Float) {
    Box(modifier = GlanceModifier.width(4.dp).height(3.dp).background(AquaMuted).cornerRadius(1.dp)) {}
    Spacer(GlanceModifier.height(1.dp))
    Box(
        modifier = GlanceModifier.width(14.dp).height(22.dp).background(AquaMuted).cornerRadius(5.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(1.5.dp)
                .height((22 * fill).dp)
                .background(Aqua)
                .cornerRadius(4.dp),
        ) {}
    }
}

@androidx.compose.runtime.Composable
private fun UndoChip(height: Dp) {
    Box(
        modifier = GlanceModifier
            .width(38.dp)
            .height(height)
            .background(Secondary)
            .cornerRadius(14.dp)
            .clickable(actionRunCallback<UndoWaterAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Text("↺", style = TextStyle(color = Muted, fontSize = 17.sp))
    }
}
