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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import com.kropi.hydration.data.HydrationState

/** Mirrors src/routes/history.tsx. */
@Composable
fun HistoryScreen(state: HydrationState) {
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
                Text("Eksportuj historię do CSV")
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Szczegóły dni", color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                history.forEach { d -> DayRow(d) }
            }
        }

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
