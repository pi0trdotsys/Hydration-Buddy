package com.kropi.hydration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.kropi.hydration.data.HistoryDay
import com.kropi.hydration.data.Intake
import com.kropi.hydration.data.MonthCalendar
import com.kropi.hydration.data.TrendCalculator
import com.kropi.hydration.data.formatMl
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import com.kropi.hydration.data.HydrationState

/** Mirrors src/routes/history.tsx. */
@Composable
fun HistoryScreen(
    state: HydrationState,
    onAddEntryOn: (Long) -> Unit,
    onEditIntake: (Intake) -> Unit,
) {
    val history = state.history
    val total = history.sumOf { it.ml }
    val avg = total / history.size
    val reachedDays = history.count { it.reached }
    val best = history.maxBy { it.ml }
    // Skala do celu, nie do maksimum tygodnia: inaczej 250 ml w pustym tygodniu
    // rysowałoby się jako pełny słupek i wyglądało na dzień wzorowy.
    val max = maxOf(history.maxOfOrNull { it.ml } ?: 0, history.first().goal, 1)
    val range = "${history.first().date} – ${history.last().date}"

    // Miesiąc liczymy tylko z dni, które aplikacja faktycznie widziała — inaczej
    // świeża instalacja pokazywałaby 29 dni „zerowych" i średnią bliską zeru.
    val month = state.lastDays(30).filter { it.ml > 0 || it.epochDay == state.today.epochDay }
    val monthReached = month.count { it.reached }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) scope.launch { writeCsv(context, uri, state) }
    }

    val entriesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) scope.launch { writeEntriesCsv(context, uri, state) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column {
                Text(
                    "Tydzień $range",
                    color = KropiColors.mutedForeground,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                )
                Text(
                    "📅 Historia nawodnienia",
                    color = KropiColors.foreground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    "Podsumowanie wypitej wody w bieżącym tygodniu.",
                    color = KropiColors.mutedForeground,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile("Suma", "$total ml", modifier = Modifier.weight(1f))
                    StatTile("Średnio", "$avg ml", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile("Cel", "${history.first().goal} ml", modifier = Modifier.weight(1f))
                    StatTile(
                        "Dni z celem",
                        "$reachedDays / ${history.size}",
                        highlight = reachedDays > 0,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item { WeekHistoryChart(history, max) }

        item { MonthCard(state, onAddEntryOn, onEditIntake) }

        item { TrendsCard(state) }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Ostatnie 30 dni", color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (month.size <= 1) {
                    Text(
                        "Kropi zbiera dane od dziś — za kilka dni pojawi się tu realna średnia i seria.",
                        color = KropiColors.mutedForeground,
                        fontSize = 12.sp,
                    )
                } else {
                    Text(
                        "Średnio ${month.sumOf { it.ml } / month.size} ml dziennie przez ${month.size} dni z zapisami. " +
                            "Cel zaliczony $monthReached razy.",
                        color = KropiColors.mutedForeground,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
                Text(
                    "Seria: ${state.streak} dni z rzędu z osiągniętym celem.",
                    color = if (state.streak > 0) KropiColors.aqua else KropiColors.mutedForeground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        item {
            Button(
                onClick = { exportLauncher.launch("kropi-nawodnienie-${LocalDate.now()}.csv") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = KropiColors.secondary,
                    contentColor = KropiColors.foreground,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Eksportuj dni do CSV")
            }
        }

        item {
            Button(
                onClick = { entriesLauncher.launch("kropi-wpisy-${LocalDate.now()}.csv") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = KropiColors.secondary,
                    contentColor = KropiColors.foreground,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Eksportuj wpisy do CSV (godzina, napój, notatka)")
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Szczegóły dni", color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                history.forEach { d -> DayRow(d) }
            }
        }

        item { DrinkBreakdownCard(state) }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Najlepszy dzień", color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(KropiColors.aqua.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🏆", fontSize = 18.sp)
                    }
                    Column {
                        Text("${best.day}, ${best.date}", color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${best.ml} ml — " + if (best.reached) "cel osiągnięty" else "blisko celu",
                            color = KropiColors.mutedForeground,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MascotCanvas(level = state.level, modifier = Modifier.size(56.dp))
                    Text(
                        "${state.mascotLine} ${state.dayNote}",
                        color = KropiColors.mutedForeground,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

/** Ile czego wypito w ostatnich ~30 dniach: udział napojów w wypitej objętości. */
@Composable
private fun DrinkBreakdownCard(state: HydrationState) {
    val rows = state.recentIntakes
        .groupBy { it.drink }
        .map { (drink, list) -> Triple(drink, list.sumOf { it.ml }, list.sumOf { it.counted }) }
        .sortedByDescending { it.second }
    val totalMl = rows.sumOf { it.second }.coerceAtLeast(1)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Co pijesz (30 dni)", color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (rows.isEmpty()) {
            Text("Brak wpisów.", color = KropiColors.mutedForeground, fontSize = 12.sp)
            return@GlassCard
        }
        rows.forEach { (drink, ml, counted) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${drink.emoji} ${drink.label}", color = KropiColors.foreground, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(
                        if (counted != ml) "$ml ml (liczy się $counted)" else "$ml ml",
                        color = KropiColors.mutedForeground,
                        fontSize = 12.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(KropiColors.secondary),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ml.toFloat() / totalMl)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(KropiColors.aqua.copy(alpha = 0.8f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, highlight: Boolean = false, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Text(label, color = KropiColors.mutedForeground, fontSize = 11.sp)
        Text(
            value,
            color = if (highlight) KropiColors.aqua else KropiColors.foreground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun WeekHistoryChart(history: List<HistoryDay>, max: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Nawodnienie w tym tygodniu", color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("ml / dzień", color = KropiColors.mutedForeground, fontSize = 11.sp)
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            history.forEachIndexed { index, d ->
                val isToday = index == history.lastIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(KropiColors.secondary),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((d.ml.toFloat() / max * 118).coerceAtLeast(8f).dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (d.reached) KropiColors.aqua else KropiColors.aqua.copy(alpha = 0.5f)),
                        )
                    }
                    Text(d.day, color = if (isToday) KropiColors.aqua else KropiColors.foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(d.date, color = KropiColors.mutedForeground, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun DayRow(d: HistoryDay) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KropiColors.secondary.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (d.reached) KropiColors.aqua else KropiColors.secondary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                d.day,
                color = if (d.reached) KropiColors.background else KropiColors.mutedForeground,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(d.date, color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("${d.ml} / ${d.goal} ml", color = KropiColors.mutedForeground, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${(d.pct * 100).toInt()}%", color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (d.reached) {
                Text("Cel osiągnięty", color = KropiColors.aqua, fontSize = 10.sp)
            }
        }
    }
}

/**
 * Eksport przez SAF: użytkownik sam wybiera miejsce zapisu, więc nie trzeba
 * ani uprawnień do pamięci, ani FileProvidera. Separator średnikowy, bo
 * polski Excel tak otwiera CSV bez kreatora importu.
 */
private suspend fun writeCsv(context: android.content.Context, uri: android.net.Uri, state: HydrationState) {
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.appendLine("data;wypite_ml;cel_ml;procent;cel_osiagniety")
                (state.records + state.today).sortedBy { it.epochDay }.forEach { record ->
                    writer.appendLine(
                        "${record.date};${record.ml};${record.goal};" +
                            "${(record.pct * 100).toInt()};${if (record.reached) "tak" else "nie"}",
                    )
                }
            }
        }
    }
}


private suspend fun writeEntriesCsv(context: android.content.Context, uri: android.net.Uri, state: HydrationState) {
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(com.kropi.hydration.backup.CsvEntries.build(state.allIntakes))
            }
        }
    }
}

private val POLISH_LOCALE = Locale.forLanguageTag("pl-PL")
private val WEEKDAYS = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")

/**
 * Kalendarz miesiąca: każdy dzień zabarwiony wg realizacji celu, strzałki do przeglądania
 * wcześniejszych miesięcy, a po dotknięciu dnia — jego łyki, które można poprawiać albo
 * dopisywać wstecz.
 */
@Composable
private fun MonthCard(state: HydrationState, onAddEntryOn: (Long) -> Unit, onEditIntake: (Intake) -> Unit) {
    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableLongStateOf(today.toEpochDay()) }

    val minMonth = YearMonth.from(LocalDate.ofEpochDay(state.firstKnownDay))
    val currentMonth = YearMonth.from(today)
    val byDay = remember(state.records, state.total, state.goal) {
        (state.records + state.today).associateBy { it.epochDay }
    }
    val cells = MonthCalendar.cells(month, byDay, today)
    val summary = MonthCalendar.summary(cells)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ChipButton("‹", false) { if (month > minMonth) month = month.minusMonths(1) }
            Text(
                month.month.getDisplayName(TextStyle.FULL_STANDALONE, POLISH_LOCALE)
                    .replaceFirstChar { it.uppercase(POLISH_LOCALE) } + " ${month.year}",
                color = KropiColors.foreground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            ChipButton("›", false) { if (month < currentMonth) month = month.plusMonths(1) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            WEEKDAYS.forEach {
                Text(it, color = KropiColors.mutedForeground, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MonthCalendar.weeks(month, cells).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        if (cell == null) {
                            Box(modifier = Modifier.weight(1f).height(40.dp))
                        } else {
                            DayCell(
                                day = cell.date.dayOfMonth,
                                pct = cell.record?.takeIf { it.ml > 0 }?.pct,
                                reached = cell.record?.takeIf { it.ml > 0 }?.reached == true,
                                isToday = cell.date == today,
                                isSelected = cell.date.toEpochDay() == selected,
                                enabled = !cell.date.isAfter(today),
                                onClick = { selected = cell.date.toEpochDay() },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        Text(
            if (summary.trackedDays == 0) "W tym miesiącu nie ma jeszcze zapisów."
            else "Średnio ${summary.averageMl} ml przez ${summary.trackedDays} dni z zapisami, cel zaliczony ${summary.reachedDays} razy.",
            color = KropiColors.mutedForeground,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )

        DayDetail(state, selected, onAddEntryOn, onEditIntake)
    }
}

@Composable
private fun DayCell(
    day: Int,
    pct: Float?,
    reached: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill = when {
        pct == null -> KropiColors.secondary.copy(alpha = if (enabled) 0.6f else 0.25f)
        reached -> KropiColors.aqua
        else -> KropiColors.aqua.copy(alpha = 0.2f + 0.5f * pct)
    }
    val border = when {
        isSelected -> Color.White
        isToday -> KropiColors.aqua
        else -> Color.Transparent
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .border(if (isSelected || isToday) 2.dp else 0.dp, border, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            "$day",
            color = when {
                reached -> KropiColors.background
                enabled -> KropiColors.foreground
                else -> KropiColors.mutedForeground.copy(alpha = 0.5f)
            },
            fontSize = 13.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun DayDetail(state: HydrationState, epochDay: Long, onAddEntryOn: (Long) -> Unit, onEditIntake: (Intake) -> Unit) {
    val date = LocalDate.ofEpochDay(epochDay)
    val record = state.recordOn(epochDay)
    val intakes = state.intakesOn(epochDay)
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, POLISH_LOCALE)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(KropiColors.secondary.copy(alpha = 0.5f))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$dayName, ${date.format(DateTimeFormatter.ofPattern("dd.MM"))}",
                    color = KropiColors.foreground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (record == null || record.ml == 0) "Brak zapisów"
                    else "${record.ml} / ${record.goal} ml · ${Math.round(record.pct * 100)}%",
                    color = if (record?.reached == true) KropiColors.aqua else KropiColors.mutedForeground,
                    fontSize = 12.sp,
                )
            }
            ChipButton("＋ Dopisz", selected = true) { onAddEntryOn(epochDay) }
        }
        intakes.forEach { intake ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onEditIntake(intake) }
                    .padding(vertical = 4.dp),
            ) {
                Text(intake.drink.emoji, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                Text(
                    String.format("%02d:%02d", intake.hour, intake.minute),
                    color = KropiColors.mutedForeground,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Text(
                    listOfNotNull(
                        if (intake.counted != intake.ml) "liczy się ${intake.counted}" else null,
                        intake.note,
                    ).joinToString(" · "),
                    color = KropiColors.mutedForeground,
                    fontSize = 11.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text("${intake.ml} ml", color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


/** Trendy: tydzień do tygodnia, miesiąc do miesiąca, pora pierwszej szklanki, mocne i słabe godziny i dni. */
@Composable
private fun TrendsCard(state: HydrationState) {
    val trends = remember(state.records, state.total, state.allIntakes, state.settings.activeStartHour, state.settings.activeEndHour) {
        TrendCalculator.compute(
            records = state.records + state.today,
            intakes = state.allIntakes,
            today = LocalDate.now(),
            windowStartHour = state.settings.activeStartHour,
            windowEndHour = state.settings.activeEndHour,
        )
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Trendy", color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (!trends.hasAnything) {
            Text(
                "Za mało danych na wnioski — po kilku dniach zapisów pojawią się tu porównania i wzorce.",
                color = KropiColors.mutedForeground,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
            return@GlassCard
        }
        trends.weekAvgMl?.let { avg ->
            TrendRow("Ten tydzień", "${formatMl(avg)} dziennie", trends.weekDeltaPct, "vs poprzedni tydzień")
        }
        trends.monthAvgMl?.let { avg ->
            TrendRow("Ostatnie 30 dni", "${formatMl(avg)} dziennie", trends.monthDeltaPct, "vs poprzednie 30 dni")
        }
        trends.avgFirstSipMinute?.let { minute ->
            TrendRow("Pierwsza szklanka", String.format("średnio o %d:%02d", minute / 60, minute % 60), null, null)
        }
        if (trends.strongestHour != null && trends.weakestHour != null) {
            TrendRow(
                "Godziny",
                "najwięcej ${hourRange(trends.strongestHour)} · najmniej ${hourRange(trends.weakestHour)}",
                null,
                null,
            )
        }
        if (trends.bestWeekday != null && trends.worstWeekday != null) {
            TrendRow(
                "Dni tygodnia",
                "najlepszy ${weekdayName(trends.bestWeekday)} · najsłabszy ${weekdayName(trends.worstWeekday)}",
                null,
                null,
            )
        }
    }
}

@Composable
private fun TrendRow(label: String, value: String, deltaPct: Int?, deltaLabel: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(POLISH_LOCALE), color = KropiColors.mutedForeground, fontSize = 9.sp, letterSpacing = 1.5.sp)
        Text(value, color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (deltaPct != null && deltaLabel != null) {
            val up = deltaPct >= 0
            Text(
                "${if (up) "▲ +" else "▼ "}$deltaPct% $deltaLabel",
                color = if (up) KropiColors.aqua else Color(0xFFFF8A65),
                fontSize = 11.sp,
            )
        }
    }
}

private fun hourRange(hour: Int): String = String.format("%02d–%02d", hour, (hour + 1) % 24)

private fun weekdayName(day: java.time.DayOfWeek): String = day.getDisplayName(TextStyle.FULL, POLISH_LOCALE)
