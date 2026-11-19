package com.kropi.hydration.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kropi.hydration.R
import com.kropi.hydration.data.Alcohol
import com.kropi.hydration.data.Caffeine
import com.kropi.hydration.data.CheckInRules
import com.kropi.hydration.data.Feeling
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.Streaks
import com.kropi.hydration.data.formatMl
import java.time.LocalTime

private val WARN = Color(0xFFFF8A65)

/**
 * Jedno pytanie dziennie: jak wygląda mocz (albo czy chce Ci się pić). Od południa, bo poranny jest
 * naturalnie ciemniejszy. Odpowiedź dopasowuje cel do Ciebie od następnego dnia (patrz [CheckInRules]).
 */
@Composable
fun CheckInCard(state: HydrationState, onAnswer: (Feeling) -> Unit, nowHour: Int = LocalTime.now().hour) {
    val answered = state.checkIn.today
    if (answered == null && nowHour < CheckInRules.ASK_FROM_HOUR) return
    val ratio = state.progress.toFloat()

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.checkin_title).uppercase(),
            color = KropiColors.aqua, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
        )
        if (answered == null) {
            Text(stringResource(R.string.checkin_sub), color = KropiColors.mutedForeground, fontSize = 12.sp, lineHeight = 17.sp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Feeling.CLEAR, Feeling.GOOD, Feeling.DARK).forEach { f ->
                    ChipButton("${f.emoji} ${f.label} — ${f.hint}", false) { onAnswer(f) }
                }
                ChipButton(stringResource(R.string.checkin_skip), false) { onAnswer(Feeling.SKIPPED) }
            }
        } else if (answered == Feeling.SKIPPED) {
            Text(stringResource(R.string.checkin_skipped), color = KropiColors.mutedForeground, fontSize = 12.sp)
        } else {
            Text(
                stringResource(R.string.checkin_answered, "${answered.emoji} ${answered.label}"),
                color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(
                    when (CheckInRules.adviceKey(answered, ratio)) {
                        "drink_more" -> R.string.checkin_adv_drink_more
                        "goal_up" -> R.string.checkin_adv_goal_up
                        "no_more" -> R.string.checkin_adv_no_more
                        else -> R.string.checkin_adv_fine
                    },
                ),
                color = KropiColors.mutedForeground, fontSize = 12.sp, lineHeight = 17.sp,
            )
            val c = state.checkIn
            if (c.pendingPct != c.activePct) {
                val delta = c.pendingPct - c.activePct
                Text(
                    stringResource(if (delta > 0) R.string.checkin_pending_up else R.string.checkin_pending_down, kotlin.math.abs(delta)),
                    color = KropiColors.aqua, fontSize = 12.sp, lineHeight = 17.sp,
                )
            }
        }
    }
}

/** Seria dni z tarczami; pokazuje się, gdy jest co pokazać. */
@Composable
fun StreakCard(state: HydrationState) {
    val info = state.streakInfo
    if (info.days == 0 && info.savedDays == 0) return
    val cd = stringResource(R.string.streak_cd, info.days, info.shields, info.savedDays)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().semantics { contentDescription = cd }) {
            Text(
                stringResource(R.string.streak_title, daysText(info.days)),
                color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "🛡️".repeat(info.shields).ifEmpty { "—" },
                fontSize = 15.sp,
            )
        }
        Text(
            when {
                info.atRisk && info.shields > 0 -> stringResource(R.string.streak_risk_shield, formatMl(state.remaining))
                info.atRisk -> stringResource(R.string.streak_risk_noshield, formatMl(state.remaining))
                else -> stringResource(R.string.streak_help, Streaks.DAYS_PER_SHIELD, Streaks.MAX_SHIELDS)
            },
            color = if (info.atRisk && info.shields == 0) WARN else KropiColors.mutedForeground,
            fontSize = 12.sp, lineHeight = 17.sp,
        )
    }
}

/** Alkohol i kofeina: dwie rzeczy, które sam współczynnik napoju opisuje zbyt grubo. */
@Composable
fun DrinkWarningsCard(state: HydrationState) {
    val alcohol = state.alcoholMl
    val caffeine = state.caffeineMg
    val showCaffeine = caffeine >= Caffeine.DAILY_LIMIT_MG * 3 / 4
    if (alcohol <= 0 && !showCaffeine) return
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        if (alcohol > 0) {
            Text(stringResource(R.string.warn_alcohol_title).uppercase(), color = WARN, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.warn_alcohol_text, formatMl(Alcohol.compensationMl(alcohol))),
                color = KropiColors.mutedForeground, fontSize = 12.sp, lineHeight = 17.sp,
            )
        }
        if (showCaffeine) {
            Text(stringResource(R.string.warn_caffeine_title).uppercase(), color = WARN, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text(
                if (caffeine > Caffeine.DAILY_LIMIT_MG) stringResource(R.string.warn_caffeine_over, caffeine, Caffeine.DAILY_LIMIT_MG)
                else stringResource(R.string.warn_caffeine_near, caffeine, Caffeine.DAILY_LIMIT_MG),
                color = KropiColors.mutedForeground, fontSize = 12.sp, lineHeight = 17.sp,
            )
        }
    }
}
