package com.kropi.hydration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.WeekDay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val POLISH = Locale.forLanguageTag("pl-PL")

/** The app's landing tab. Mirrors src/routes/index.tsx (dashboard). */
@Composable
fun HomeScreen(
    state: HydrationState,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onPoke: () -> Unit,
    onSetGoal: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { HomeHeader(state.goal, onSetGoal) }
        item {
            WidgetCard(
                state = state,
                variant = WidgetVariant.LARGE,
                onAdd = onAdd,
                onUndo = onUndo,
                onPoke = onPoke,
                modifier = Modifier.fillMaxWidth().height(360.dp),
            )
        }
        item { InsightCard(eyebrow = "Self-care na dziś", body = state.selfCareAlt) }
        item { InsightCard(eyebrow = "Ciekawostka", body = state.fact) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "💧 Dzisiejsze łyki",
                    color = KropiColors.foreground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                IntakeTimeline(state)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tydzień", color = KropiColors.foreground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                WeekBar(week = state.week, todayPct = state.progress.toFloat())
            }
        }
        item { MascotSection(state) }
    }
}

@Composable
private fun HomeHeader(goal: Int, onSetGoal: (Int) -> Unit) {
    val today = LocalDate.now()
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, POLISH)
        .replaceFirstChar { it.uppercase(POLISH) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(
                "$dayName, dziś".uppercase(POLISH),
                color = KropiColors.mutedForeground,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            )
            Text(
                "Twoje nawodnienie",
                color = KropiColors.foreground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(KropiColors.card)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Cel", color = KropiColors.mutedForeground, fontSize = 13.sp)
            Slider(
                value = goal.toFloat(),
                onValueChange = { onSetGoal((it / 100).toInt() * 100) },
                valueRange = 1000f..4000f,
                colors = SliderDefaults.colors(thumbColor = KropiColors.aqua, activeTrackColor = KropiColors.aqua),
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            Text(
                "$goal ml",
                color = KropiColors.foreground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun IntakeTimeline(state: HydrationState) {
    if (state.intakes.isEmpty()) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Brak wpisów. Kliknij butelkę w widgecie.",
                color = KropiColors.mutedForeground,
                fontSize = 13.sp,
            )
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.intakes.asReversed().forEach { intake ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(KropiColors.card)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    String.format("%02d:%02d", intake.hour, intake.minute),
                    color = KropiColors.mutedForeground,
                    fontSize = 12.sp,
                    modifier = Modifier.width(48.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(KropiColors.secondary),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((intake.ml / 750f).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(999.dp))
                            .background(KropiColors.aqua.copy(alpha = 0.8f)),
                    )
                }
                Text(
                    "${intake.ml} ml",
                    color = KropiColors.foreground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
fun WeekBar(week: List<WeekDay>, todayPct: Float) {
    val days = week + WeekDay("Nd", todayPct)
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(KropiColors.card)
            .padding(16.dp),
    ) {
        days.forEachIndexed { index, d ->
            val isToday = index == days.lastIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(KropiColors.secondary),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((d.pct.coerceAtLeast(0.06f) * 72).dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (d.pct >= 1f) KropiColors.aqua else KropiColors.aqua.copy(alpha = 0.5f)),
                    )
                }
                Text(
                    d.day,
                    color = if (isToday) KropiColors.aqua else KropiColors.mutedForeground,
                    fontSize = 11.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun MascotSection(state: HydrationState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(KropiColors.card)
            .padding(20.dp),
    ) {
        MascotCanvas(level = state.level, modifier = Modifier.width(72.dp).height(72.dp))
        Column {
            Text(
                "Kropi mówi",
                color = KropiColors.aqua,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                state.mascotLine,
                color = KropiColors.mutedForeground,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                state.dayNote,
                color = KropiColors.mutedForeground.copy(alpha = 0.8f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
