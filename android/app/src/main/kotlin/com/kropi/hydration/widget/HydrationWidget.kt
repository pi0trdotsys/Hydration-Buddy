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
import androidx.glance.appwidget.GlanceAppWidgetManager
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
import androidx.glance.appwidget.action.actionStartActivity
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
import com.kropi.hydration.data.ButtonSpec
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.WidgetConfig
import com.kropi.hydration.data.HydrationPlan
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.Level
import com.kropi.hydration.data.PlanStatus
import com.kropi.hydration.data.formatMl
import com.kropi.hydration.data.hhmm
import com.kropi.hydration.data.plan
import java.time.LocalTime
import kotlinx.coroutines.launch

private val Aqua: ColorProvider @androidx.compose.runtime.Composable get() = LocalPalette.current.accent
private val AquaMuted: ColorProvider @androidx.compose.runtime.Composable get() = LocalPalette.current.accentMuted
private val AquaSoft: ColorProvider @androidx.compose.runtime.Composable get() = LocalPalette.current.accentSoft
private val Foreground: ColorProvider @androidx.compose.runtime.Composable get() = LocalPalette.current.foreground
private val Muted: ColorProvider @androidx.compose.runtime.Composable get() = LocalPalette.current.muted
private val Secondary: ColorProvider @androidx.compose.runtime.Composable get() = LocalPalette.current.secondary
private val CardBg: ColorProvider @androidx.compose.runtime.Composable get() = LocalPalette.current.card
private val ScreenBg: ColorProvider @androidx.compose.runtime.Composable get() = LocalPalette.current.screen
private val Warn: ColorProvider @androidx.compose.runtime.Composable get() = LocalPalette.current.warn

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
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        // Przewinięcie doby (i zasiew przy pierwszym uruchomieniu) robimy raz,
        // przed kompozycją — to jedyny zapis, jaki widget wykonuje sam z siebie.
        val initial = repository.current()
        provideContent {
            // Stan i konfigurację zbieramy jako Flow WEWNĄTRZ kompozycji. Wcześniej stan
            // był czytany jednorazowo przed provideContent, więc kolejne update() trafiały
            // na zamrożoną wartość: drugie dolanie wody pod rząd nie zmieniało już nic.
            // Teraz każdy zapis do DataStore (łącznie ze zmianą przycisków) odświeża widget.
            val state by repository.state.collectAsState(initial = initial)
            val config by remember(appWidgetId) { repository.widgetConfig(appWidgetId) }
                .collectAsState(initial = WidgetConfig.default(initial.settings.bottlesMl))
            WidgetTheme(config.style) { HydrationWidgetContent(state, config) }
        }
    }
}

class HydrationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HydrationWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Widget bez uruchomionej aplikacji też ma się wyzerować o północy.
        com.kropi.hydration.notifications.MidnightRollWorker.schedule(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            HydrationRepository(context).removeWidgetConfigs(appWidgetIds)
        }
    }
}

@androidx.compose.runtime.Composable
private fun HydrationWidgetContent(state: HydrationState, config: WidgetConfig) {
    val size = LocalSize.current
    val variant = when {
        size.height < 120.dp && size.width < 180.dp -> Variant.TINY
        size.height < 150.dp -> Variant.BAR
        size.height < 290.dp -> Variant.COMPACT
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
            Variant.BAR -> BarContent(state, config)
            Variant.COMPACT -> CompactContent(state, config, size.width, size.height)
            Variant.FULL -> FullContent(state, config, size.width, size.height, pad)
        }
    }
}

@androidx.compose.runtime.Composable
private fun TinyContent(state: HydrationState) {
    val pct = (state.progress * 100).toInt()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RingWithMascot(progress = state.progress.toFloat(), level = state.level, ringSize = 84.dp, mascotSize = 38.dp, behind = state.isBehindSchedule && state.remaining > 0)
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
private fun BarContent(state: HydrationState, config: WidgetConfig) {
    val plan = state.plan()
    val behind = state.isBehindSchedule && state.remaining > 0
    val bottles = config.slots
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxSize()) {
        RingWithPercent(progress = state.progress.toFloat(), pct = (state.progress * 100).toInt(), ringSize = 46.dp, behind = behind)
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
private fun FixedBottleChip(spec: ButtonSpec, size: Dp) {
    Box(
        modifier = GlanceModifier
            .width(size)
            .height(size)
            .background(Secondary)
            .cornerRadius(14.dp)
            .clickable(actionRunCallback<AddWaterAction>(addParams(spec))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            (if (spec.isPlain) "+" else spec.drink!!.emoji) + spec.ml,
            style = TextStyle(color = Foreground, fontSize = 11.sp, fontWeight = FontWeight.Bold),
        )
    }
}

/** Parametry dolania: ilość i — dla przycisku z przypisanym napojem — ten napój. */
private fun addParams(spec: ButtonSpec): androidx.glance.action.ActionParameters =
    if (spec.drink == null) actionParametersOf(MlKey to spec.ml)
    else actionParametersOf(MlKey to spec.ml, DrinkKey to spec.drink.name)

@androidx.compose.runtime.Composable
private fun CompactContent(state: HydrationState, config: WidgetConfig, width: Dp, height: Dp) {
    val plan = state.plan()
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Header(state, plan, ringSize = 58.dp, titleSize = 14.sp, showMascot = false)
        Spacer(GlanceModifier.height(6.dp))
        StatusRow(state, plan)
        Spacer(GlanceModifier.defaultWeight())
        // Napoje i wpis z przeszłości — tylko gdy kafelek jest dość wysoki, żeby się zmieściły.
        if (height >= 210.dp) {
            ToolRow(state, width - 28.dp)
            Spacer(GlanceModifier.height(6.dp))
        }
        BottleRow(height = 50.dp, slots = config.slots, undo = state.undo)
    }
}

/**
 * Pełny kafelek. Wysokości sekcji są stałe, a wykres dostaje całą resztę —
 * dzięki temu na 4×4 nie zostaje pas pustki, a na mniejszym kafelku wykres
 * kurczy się zamiast wypychać stopkę poza krawędź.
 */
@androidx.compose.runtime.Composable
private fun FullContent(state: HydrationState, config: WidgetConfig, width: Dp, height: Dp, pad: Dp) {
    val plan = state.plan()

    val headerHeight = 74.dp
    val statusHeight = 30.dp
    val bottlesHeight = 54.dp
    val toolsHeight = 30.dp
    // Stopka jest najmniej ważna: pokazujemy ją dopiero, gdy po pasku napojów wykresowi
    // zostaje jeszcze porządnie dużo miejsca (min. ~94 dp po odjęciu stopki).
    val baseReserved = headerHeight + statusHeight + bottlesHeight + toolsHeight + 8.dp * 4 + pad * 2
    val showFooter = height - baseReserved >= 120.dp
    val footerExtra = if (showFooter) 18.dp + 8.dp else 0.dp
    val chartHeight = (height - baseReserved - footerExtra).coerceIn(64.dp, 190.dp)

    // Odstępy to padding, nie Spacer: kontener Glance mieści maksymalnie 10 dzieci,
    // a sześć sekcji przedzielonych pięcioma Spacerami dawało 11 — stopka znikała.
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Header(state, plan, ringSize = headerHeight, titleSize = 20.sp, showMascot = width >= 300.dp)
        Box(modifier = GlanceModifier.padding(top = 8.dp)) { StatusRow(state, plan) }
        Box(modifier = GlanceModifier.padding(top = 8.dp)) { BottleRow(height = bottlesHeight, slots = config.slots, undo = state.undo) }
        Box(modifier = GlanceModifier.padding(top = 8.dp)) { ToolRow(state, width - pad * 2) }
        Box(modifier = GlanceModifier.padding(top = 8.dp)) { ChartPanel(state, width = width - pad * 2, height = chartHeight) }
        if (showFooter) {
            Box(modifier = GlanceModifier.padding(top = 8.dp)) { Footer(state, plan) }
        }
    }
}

@androidx.compose.runtime.Composable
private fun Header(state: HydrationState, plan: HydrationPlan, ringSize: Dp, titleSize: TextUnit, showMascot: Boolean) {
    val behind = state.isBehindSchedule && state.remaining > 0 && plan.status != PlanStatus.AFTER_HOURS
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
        RingWithPercent(progress = state.progress.toFloat(), pct = (state.progress * 100).toInt(), ringSize = ringSize, behind = behind)
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
                when {
                    state.remaining <= 0 -> "Cel osiągnięty 🎉"
                    // Za planem widget przestaje być tylko tablicą wyników i mówi, co zrobić teraz.
                    behind -> "Wypij teraz ${formatMl(plan.portionMl)}"
                    else -> "Zostało ${formatMl(state.remaining)}"
                },
                style = TextStyle(color = if (behind) Warn else Aqua, fontSize = 11.sp, fontWeight = FontWeight.Medium),
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
            // Dotknięcie wykresu otwiera listę dzisiejszych wpisów: można je poprawić, usunąć albo dopisać nowy.
            .clickable(actionStartActivity(quickEntryIntent(androidx.glance.LocalContext.current, day = true)))
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
            "🔥 ${state.streak} dni  ·  $lastText",
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

    val graphics = LocalPalette.current.graphics
    val bitmap = remember(state.intakes, state.goal, widthPx, heightPx, nowMinutes, graphics) {
        intakeChartBitmap(
            widthPx = widthPx,
            heightPx = heightPx,
            density = density,
            intakes = state.intakes,
            goalMl = state.goal,
            startHour = state.settings.activeStartHour,
            endHour = state.settings.activeEndHour,
            nowMinutes = nowMinutes,
            palette = graphics,
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
private fun RingWithMascot(progress: Float, level: Level, ringSize: Dp, mascotSize: Dp, behind: Boolean) {
    val pal = LocalPalette.current
    val ringColors = if (behind) pal.warnGraphics else pal.graphics
    val ringPx = dpToPx(ringSize)
    val mascotPx = dpToPx(mascotSize)
    Box(modifier = GlanceModifier.size(ringSize), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(
                remember(ringPx, progress, ringColors) { WidgetGraphics.progressRing(ringPx, ringPx * 0.09f, progress, ringColors) },
            ),
            contentDescription = null,
            modifier = GlanceModifier.size(ringSize),
        )
        Image(
            provider = ImageProvider(remember(mascotPx, level, pal.graphics) { WidgetGraphics.mascot(mascotPx, level, pal.graphics) }),
            contentDescription = "Kropi",
            modifier = GlanceModifier.size(mascotSize).clickable(actionRunCallback<PokeMascotAction>()),
        )
    }
}

@androidx.compose.runtime.Composable
private fun RingWithPercent(progress: Float, pct: Int, ringSize: Dp, behind: Boolean) {
    val pal = LocalPalette.current
    val ringColors = if (behind) pal.warnGraphics else pal.graphics
    val ringPx = dpToPx(ringSize)
    Box(modifier = GlanceModifier.size(ringSize), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(
                remember(ringPx, progress, ringColors) { WidgetGraphics.progressRing(ringPx, ringPx * 0.11f, progress, ringColors) },
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
    val graphics = LocalPalette.current.graphics
    Image(
        provider = ImageProvider(remember(px, level, graphics) { WidgetGraphics.mascot(px, level, graphics) }),
        contentDescription = "Kropi",
        modifier = GlanceModifier.size(sizeDp).clickable(actionRunCallback<PokeMascotAction>()),
    )
}

@androidx.compose.runtime.Composable
private fun BottleRow(height: Dp, slots: List<ButtonSpec>, undo: com.kropi.hydration.data.UndoInfo?) {
    Row(modifier = GlanceModifier.fillMaxWidth().height(height), horizontalAlignment = Alignment.Start) {
        for (spec in slots) {
            BottleChip(spec, height)
            Spacer(GlanceModifier.width(6.dp))
        }
        UndoChip(height, undo?.takeIf { it.isFresh() })
    }
}

@androidx.compose.runtime.Composable
private fun RowScope.BottleChip(spec: ButtonSpec, height: Dp) {
    val fill = (spec.ml / 750f).coerceIn(0.15f, 1f)
    Box(
        modifier = GlanceModifier
            .defaultWeight()
            .height(height)
            .background(if (spec.isPlain) Secondary else AquaSoft)
            .cornerRadius(14.dp)
            .clickable(actionRunCallback<AddWaterAction>(addParams(spec)))
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (spec.isPlain) {
                BottleGlyph(fill)
            } else {
                // Przycisk z przypisanym napojem: emoji zamiast butelki, podświetlone tło.
                Text(spec.drink!!.emoji, style = TextStyle(fontSize = 20.sp))
            }
            Text("${spec.ml}", style = TextStyle(color = Foreground, fontSize = 9.sp, fontWeight = FontWeight.Bold))
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
private fun UndoChip(height: Dp, undone: com.kropi.hydration.data.UndoInfo?) {
    // Zaraz po cofnięciu strzałka zmienia się w „przywróć" — pomyłkowe dotknięcie nic nie kosztuje.
    val restoring = undone != null
    Box(
        modifier = GlanceModifier
            .width(38.dp)
            .height(height)
            .background(if (restoring) AquaSoft else Secondary)
            .cornerRadius(14.dp)
            .clickable(if (restoring) actionRunCallback<RestoreUndoAction>() else actionRunCallback<UndoWaterAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (restoring) "↻" else "↺",
            style = TextStyle(color = if (restoring) Aqua else Muted, fontSize = 17.sp),
        )
    }
}

/** Intent otwierający edytor: pojedynczy wpis albo (day = true) listę dzisiejszych wpisów. */
private fun quickEntryIntent(context: Context, day: Boolean): android.content.Intent =
    android.content.Intent(context, com.kropi.hydration.ui.QuickEntryActivity::class.java)
        .putExtra(com.kropi.hydration.ui.QuickEntryActivity.EXTRA_DAY_LIST, day)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)

/**
 * Pasek napojów i „＋ wpis". Szeroki kafelek pokazuje wszystkie napoje naraz (dotknięcie
 * zaznacza, ponowne cofa do wody), wąski — jeden przycisk, który przełącza kolejne.
 * Zaznaczony napój obowiązuje na najbliższe dolanie butelką; „＋ wpis" otwiera pełny
 * edytor, w którym można też wpisać łyk z przeszłości.
 */
@androidx.compose.runtime.Composable
private fun ToolRow(state: HydrationState, width: Dp) {
    val selected = state.widgetDrink
    val undone = state.undo?.takeIf { it.isFresh() }
    if (undone != null) {
        UndoBanner(undone)
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxWidth().height(30.dp),
    ) {
        if (width >= 280.dp) {
            // Odstęp jako padding, nie Spacer: kontener Glance mieści maksymalnie 10 dzieci,
            // a 7 pigułek + 7 odstępów + „＋ wpis" to 15 — nadmiar byłby po cichu ucinany.
            Drink.entries.forEach { drink ->
                Box(
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight().padding(end = 4.dp),
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(if (drink == selected) AquaSoft else Secondary)
                            .cornerRadius(10.dp)
                            .clickable(actionRunCallback<SetDrinkAction>(actionParametersOf(DrinkKey to drink.name))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(drink.emoji, style = TextStyle(fontSize = 13.sp))
                    }
                }
            }
        } else {
            Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight().padding(end = 4.dp)) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(if (selected == Drink.WATER) Secondary else AquaSoft)
                        .cornerRadius(10.dp)
                        .clickable(actionRunCallback<CycleDrinkAction>()),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${selected.emoji} ${selected.label}  ⟳",
                        style = TextStyle(color = if (selected == Drink.WATER) Muted else Foreground, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                    )
                }
            }
        }
        if (selected != Drink.WATER) {
            // „Typowa porcja" wybranego napoju — jedno dotknięcie zapisuje ostatnio użytą ilość.
            val typical = state.typicalMl(selected)
            Box(modifier = GlanceModifier.width(60.dp).fillMaxHeight().padding(end = 4.dp)) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(AquaSoft)
                        .cornerRadius(10.dp)
                        .clickable(actionRunCallback<AddWaterAction>(actionParametersOf(MlKey to typical))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("＋$typical", style = TextStyle(color = Foreground, fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                }
            }
        }
        Box(
            modifier = GlanceModifier
                .width(64.dp)
                .fillMaxHeight()
                .background(Secondary)
                .cornerRadius(10.dp)
                .clickable(actionStartActivity(quickEntryIntent(androidx.glance.LocalContext.current, day = false))),
            contentAlignment = Alignment.Center,
        ) {
            Text("＋ wpis", style = TextStyle(color = Aqua, fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        }
    }
}

/** Chwilowy pasek po cofnięciu: co zniknęło i jak to przywrócić. */
@androidx.compose.runtime.Composable
private fun UndoBanner(undone: com.kropi.hydration.data.UndoInfo) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxWidth().height(30.dp).background(AquaSoft).cornerRadius(10.dp).padding(horizontal = 10.dp),
    ) {
        Text(
            "↺ Cofnięto ${undone.drink.emoji} ${undone.ml} ml",
            style = TextStyle(color = Foreground, fontSize = 11.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            "PRZYWRÓĆ",
            style = TextStyle(color = Aqua, fontSize = 11.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = GlanceModifier.clickable(actionRunCallback<RestoreUndoAction>()).padding(start = 8.dp),
        )
    }
}
