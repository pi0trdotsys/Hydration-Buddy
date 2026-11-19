package com.kropi.hydration.ui

import com.kropi.hydration.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kropi.hydration.data.DAYPART_NOTES
import com.kropi.hydration.data.Daypart
import com.kropi.hydration.data.FACTS
import com.kropi.hydration.data.Level
import com.kropi.hydration.data.MASCOT_LINES
import com.kropi.hydration.data.SELF_CARE

private val LEVEL_LABEL = mapOf(
    Level.LOW to R.string.insights_level_low,
    Level.MID to R.string.insights_level_mid,
    Level.HIGH to R.string.insights_level_high,
    Level.DONE to R.string.insights_level_done,
)

private val DAYPART_LABEL = mapOf(
    Daypart.MORNING to R.string.insights_part_morning,
    Daypart.DAY to R.string.insights_part_day,
    Daypart.EVENING to R.string.insights_part_evening,
)

/** Mirrors src/routes/insights.tsx — the full deterministic content browser. */
@Composable
fun InsightsScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column {
                Text(stringResource(R.string.insights_baza_tresci), color = KropiColors.foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.insights_wszystkie_komunikaty_ktore_aplikacja_losuje),
                    color = KropiColors.mutedForeground,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item { SectionTitle(stringResource(R.string.insights_section_selfcare)) }
        items(Level.entries.toList()) { level ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(LEVEL_LABEL.getValue(level)).uppercase(),
                    color = KropiColors.aqua,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                SELF_CARE.getValue(level).forEach { line -> BulletLine(line) }
            }
        }

        item { SectionTitle(stringResource(R.string.insights_section_daypart)) }
        items(Daypart.entries.toList()) { part ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(DAYPART_LABEL.getValue(part)).uppercase(),
                    color = KropiColors.aqua,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                DAYPART_NOTES.getValue(part).forEach { line -> BulletLine(line) }
            }
        }

        item { SectionTitle(stringResource(R.string.insights_section_facts, FACTS.size)) }
        itemsIndexed(FACTS) { i, fact ->
            InsightCard(eyebrow = "#${(i + 1).toString().padStart(2, '0')}", body = fact, modifier = Modifier.fillMaxWidth())
        }

        item { SectionTitle(stringResource(R.string.insights_section_mascot)) }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Level.entries.forEach { level ->
                    MASCOT_LINES.getValue(level).forEach { line ->
                        Text(stringResource(R.string.quote_wrap, line), color = KropiColors.mutedForeground, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = KropiColors.foreground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun BulletLine(text: String) {
    Text("•  $text", color = KropiColors.mutedForeground, fontSize = 13.sp, lineHeight = 18.sp)
}
