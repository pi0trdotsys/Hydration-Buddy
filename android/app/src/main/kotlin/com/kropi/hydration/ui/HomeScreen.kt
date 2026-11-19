package com.kropi.hydration.ui

import com.kropi.hydration.data.formatMl
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.minimumInteractiveComponentSize
import com.kropi.hydration.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.kropi.hydration.data.Intake
import com.kropi.hydration.data.detailText
import com.kropi.hydration.data.plan
import com.kropi.hydration.data.WeekDay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale


/** The app's landing tab. Mirrors src/routes/index.tsx (dashboard). */
@Composable
fun HomeScreen(
    state: HydrationState,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onPoke: () -> Unit,
    onSetGoal: (Int) -> Unit,
    onAddEntry: () -> Unit,
    onEditIntake: (Intake) -> Unit,
    onDeleteIntake: (Long) -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { HomeHeader(state.goal, state.settings.goalMode == com.kropi.hydration.data.GoalMode.AUTO, onSetGoal, onOpenSettings) }
        item {
            WidgetCard(
                state = state,
                variant = WidgetVariant.LARGE,
                onAdd = onAdd,
                onUndo = onUndo,
                onPoke = onPoke,
                modifier = Modifier.fillMaxWidth(),
                wrapHeight = true,
            )
        }
        item {
            // Ta sama kalkulacja, którą dostajesz w powiadomieniu — żeby plan dnia
            // dało się sprawdzić bez czekania na przypomnienie.
            InsightCard(eyebrow = stringResource(R.string.home_plan_na_reszte_dnia), body = state.plan().detailText(), modifier = Modifier.fillMaxWidth())
        }
        item { RegularityCard(state) }
        item { InsightCard(eyebrow = stringResource(R.string.home_self_care_na_dzis), body = state.selfCareAlt, modifier = Modifier.fillMaxWidth()) }
        item { InsightCard(eyebrow = stringResource(R.string.home_ciekawostka), body = state.fact, modifier = Modifier.fillMaxWidth()) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.home_dzisiejsze_lyki),
                        color = KropiColors.foreground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    ChipButton(stringResource(R.string.home_dodaj_wpis), selected = true, onClick = onAddEntry)
                }
                IntakeTimeline(state, onEditIntake, onDeleteIntake)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.home_tydzien), color = KropiColors.foreground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                WeekBar(week = state.week, todayPct = state.progress.toFloat())
            }
        }
        item { MascotSection(state) }
    }
}

@Composable
private fun HomeHeader(goal: Int, auto: Boolean, onSetGoal: (Int) -> Unit, onOpenSettings: () -> Unit) {
    val today = LocalDate.now()
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, com.kropi.hydration.data.Lang.locale)
        .replaceFirstChar { it.uppercase(com.kropi.hydration.data.Lang.locale) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(
                stringResource(R.string.home_dzis, dayName).uppercase(com.kropi.hydration.data.Lang.locale),
                color = KropiColors.mutedForeground,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            )
            Text(
                stringResource(R.string.home_twoje_nawodnienie),
                color = KropiColors.foreground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (auto) {
            // Tryb automatyczny: cel liczy się sam, więc suwak by tylko udawał, że coś zmienia.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(KropiColors.card)
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${stringResource(R.string.home_cel)}: $goal ml", color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.home_goal_auto_hint), color = KropiColors.mutedForeground, fontSize = 11.sp, lineHeight = 15.sp)
                }
                ChipButton(stringResource(R.string.home_goal_change), false, stringResource(R.string.home_goal_change_cd), onOpenSettings)
            }
        } else Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(KropiColors.card)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(stringResource(R.string.home_cel), color = KropiColors.mutedForeground, fontSize = 13.sp)
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
private fun IntakeTimeline(state: HydrationState, onEdit: (Intake) -> Unit, onDelete: (Long) -> Unit) {
    if (state.intakes.isEmpty()) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.home_brak_wpisow_kliknij_butelke_w),
                color = KropiColors.mutedForeground,
                fontSize = 13.sp,
            )
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.intakes.reversed().forEach { intake ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(KropiColors.card)
                    .clickable { onEdit(intake) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(intake.drink.emoji, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
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
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        "${intake.ml} ml",
                        color = KropiColors.foreground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    // Napój liczony poniżej 100% albo notatka — drobny druk pod ilością.
                    val sub = listOfNotNull(
                        if (intake.counted != intake.ml) stringResource(R.string.history_liczy_sie, intake.counted) else null,
                        intake.note,
                    ).joinToString(" · ")
                    if (sub.isNotEmpty()) {
                        Text(sub, color = KropiColors.mutedForeground, fontSize = 10.sp, maxLines = 1)
                    }
                }
                val deleteLabel = stringResource(R.string.a11y_delete_entry)
                Text(
                    "✕",
                    color = KropiColors.mutedForeground,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(role = Role.Button) { onDelete(intake.id) }
                        .semantics { contentDescription = deleteLabel }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
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
                stringResource(R.string.home_kropi_mowi),
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


/**
 * Regularność dnia: pasek okien czasu (każde to 2 godziny aktywnego picia), najdłuższa przerwa i — gdy
 * ktoś wlał w siebie za dużo naraz — ostrzeżenie. Suma dnia nie mówi, czy piło się równo.
 */
@Composable
private fun RegularityCard(state: HydrationState) {
    val now = java.time.LocalTime.now()
    val nowMinute = now.hour * 60 + now.minute
    val s = state.settings
    val blocks = remember(state.intakes, s.activeStartHour, s.activeEndHour, nowMinute) {
        com.kropi.hydration.data.Regularity.blocks(state.intakes, s.activeStartHour, s.activeEndHour, nowMinute)
    }
    if (blocks.isEmpty()) return
    val summary = com.kropi.hydration.data.Regularity.summary(blocks)
    val gap = com.kropi.hydration.data.Regularity.longestGapMinutes(state.intakes, s.activeStartHour, s.activeEndHour, nowMinute)
    val recent = com.kropi.hydration.data.Regularity.recentMl(state.intakes, nowMinute)
    val burst = recent > com.kropi.hydration.data.Regularity.BURST_LIMIT_ML
    val longGap = gap >= com.kropi.hydration.data.Regularity.LONG_GAP_MINUTES
    val barDescription = stringResource(R.string.reg_bar_cd, summary.hits, summary.finishedBlocks)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.reg_title).uppercase(),
            color = KropiColors.aqua,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = barDescription },
        ) {
            blocks.forEach { block ->
                val color = when {
                    block.hit -> KropiColors.aqua
                    block.finished -> Color(0xFFFF8A65).copy(alpha = 0.75f)
                    block.current -> KropiColors.aqua.copy(alpha = 0.25f)
                    else -> KropiColors.secondary
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(color),
                )
            }
        }
        Text(
            if (summary.finishedBlocks == 0) stringResource(R.string.reg_summary_none)
            else stringResource(R.string.reg_summary, summary.hits, summary.finishedBlocks),
            color = KropiColors.foreground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.reg_gap, durationText(gap)) + if (longGap) " — " + stringResource(R.string.reg_gap_long) else "",
            color = if (longGap) Color(0xFFFF8A65) else KropiColors.mutedForeground,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        if (burst) {
            Text(
                stringResource(R.string.reg_burst, formatMl(recent)),
                color = Color(0xFFFF8A65),
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        } else {
            Text(stringResource(R.string.reg_tip), color = KropiColors.mutedForeground, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
