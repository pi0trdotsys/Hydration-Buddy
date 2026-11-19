package com.kropi.hydration.ui

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.kropi.hydration.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kropi.hydration.data.ActivityLevel
import com.kropi.hydration.data.GoalCalculator
import com.kropi.hydration.data.GoalMode
import com.kropi.hydration.data.HydrationSettings
import com.kropi.hydration.data.Level
import com.kropi.hydration.data.NotificationTone
import com.kropi.hydration.data.Temperature
import com.kropi.hydration.data.formatMl

private const val STEPS = 4
private val GLASS_CHOICES = listOf(150, 200, 250, 300, 400)

/**
 * Powitanie przy pierwszym uruchomieniu: cztery ekrany, po których cel, godziny i ton
 * przypomnień są ustawione, a powiadomienia — o ile ich chcesz — mają zgodę systemu.
 * [onFinish] dostaje gotowe ustawienia, [onSkip] zostawia domyślne.
 */
@Composable
fun OnboardingScreen(
    initial: HydrationSettings,
    onRequestNotifications: () -> Unit,
    onPinWidget: () -> Unit,
    onFinish: (HydrationSettings) -> Unit,
    onSkip: () -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var weightKg by remember { mutableStateOf(initial.weightKg) }
    var activity by remember { mutableStateOf(initial.activity) }
    var temperature by remember { mutableStateOf(initial.temperature) }
    var startHour by remember { mutableIntStateOf(initial.activeStartHour) }
    var endHour by remember { mutableIntStateOf(initial.activeEndHour) }
    var glassMl by remember { mutableIntStateOf(initial.reminderGlassMl) }
    var tone by remember { mutableStateOf(initial.notificationTone) }
    var reminders by remember { mutableStateOf(true) }

    val goal = GoalCalculator.calculate(weightKg, activity, temperature)

    Box(modifier = Modifier.fillMaxSize().background(KropiColors.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    repeat(STEPS) { i ->
                        Box(
                            modifier = Modifier
                                .size(width = if (i == step) 22.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(if (i <= step) KropiColors.aqua else KropiColors.secondary),
                        )
                    }
                }
                TextButton(onClick = onSkip) { Text(stringResource(R.string.onb_pomin), color = KropiColors.mutedForeground) }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp),
            ) {
                when (step) {
                    0 -> WelcomeStep()
                    1 -> BodyStep(weightKg, { weightKg = it }, activity, { activity = it }, temperature, { temperature = it }, goal)
                    2 -> DayStep(
                        startHour, endHour, glassMl, tone,
                        onStart = { startHour = it.coerceIn(0, endHour - 4) },
                        onEnd = { endHour = it.coerceIn(startHour + 4, 23) },
                        onGlass = { glassMl = it },
                        onTone = { tone = it },
                    )
                    else -> RemindersStep(reminders, { reminders = it }, onRequestNotifications, onPinWidget)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (step > 0) {
                    Button(
                        onClick = { step -= 1 },
                        colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text(stringResource(R.string.onb_wstecz)) }
                }
                Button(
                    onClick = {
                        if (step < STEPS - 1) {
                            step += 1
                        } else {
                            // Przełącznik jest domyślnie włączony, więc zgodę trzeba wyprosić tu, nie tylko przy jego przestawieniu.
                            if (reminders) onRequestNotifications()
                            onFinish(
                                initial.copy(
                                    weightKg = weightKg,
                                    activity = activity,
                                    temperature = temperature,
                                    goalMode = GoalMode.AUTO,
                                    activeStartHour = startHour,
                                    activeEndHour = endHour,
                                    reminderGlassMl = glassMl,
                                    notificationTone = tone,
                                    remindersEnabled = reminders,
                                ),
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KropiColors.aqua, contentColor = KropiColors.background),
                    modifier = Modifier.weight(2f).height(52.dp),
                ) { Text(if (step < STEPS - 1) stringResource(R.string.onb_dalej) else stringResource(R.string.onb_zaczynamy), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun StepHeader(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(eyebrow.uppercase(), color = KropiColors.aqua, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text(title, color = KropiColors.foreground, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = KropiColors.mutedForeground, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    ) {
        MascotCanvas(level = Level.MID, modifier = Modifier.size(140.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.onb_czesc_jestem_kropi), color = KropiColors.foreground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.onb_nie_przypominam_pij_wode_co),
                color = KropiColors.mutedForeground,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onb_zajmie_to_minute), color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.onb_cel_pod_twoja_wage_i), color = KropiColors.mutedForeground, fontSize = 13.sp)
            Text(stringResource(R.string.onb_godziny_w_ktorych_pijesz), color = KropiColors.mutedForeground, fontSize = 13.sp)
            Text(stringResource(R.string.onb_przypomnienia_i_widget), color = KropiColors.mutedForeground, fontSize = 13.sp)
        }
    }
}

@Composable
private fun BodyStep(
    weightKg: Float, onWeight: (Float) -> Unit,
    activity: ActivityLevel, onActivity: (ActivityLevel) -> Unit,
    temperature: Temperature, onTemperature: (Temperature) -> Unit,
    goal: Int,
) {
    StepHeader(stringResource(R.string.onb_krok_z), stringResource(R.string.onb_twoj_cel), stringResource(R.string.onb_liczymy_go_z_wagi_aktywnosci))
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.settings_waga_kg, weightKg.toInt()), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Slider(
            value = weightKg,
            onValueChange = { onWeight(Math.round(it).toFloat()) },
            valueRange = 30f..150f,
            colors = SliderDefaults.colors(thumbColor = KropiColors.aqua, activeTrackColor = KropiColors.aqua),
        )
    }
    Text(stringResource(R.string.settings_aktywnosc_fizyczna), color = KropiColors.mutedForeground, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActivityLevel.entries.forEach { a -> ChipButton("${a.emoji} ${a.label}", activity == a) { onActivity(a) } }
    }
    Text(stringResource(R.string.settings_temperatura_otoczenia), color = KropiColors.mutedForeground, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Temperature.entries.forEach { t -> ChipButton("${t.emoji} ${t.label}", temperature == t) { onTemperature(t) } }
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.onb_cel_na_dzien), color = KropiColors.mutedForeground, fontSize = 12.sp)
        Text(formatMl(goal), color = KropiColors.aqua, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DayStep(
    startHour: Int, endHour: Int, glassMl: Int, tone: NotificationTone,
    onStart: (Int) -> Unit, onEnd: (Int) -> Unit, onGlass: (Int) -> Unit, onTone: (NotificationTone) -> Unit,
) {
    StepHeader(stringResource(R.string.onb_krok_z_2), stringResource(R.string.onb_twoj_dzien), stringResource(R.string.onb_kropi_rozklada_picie_tylko_w))
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        HourRow(stringResource(R.string.history_pierwsza_szklanka), startHour, { onStart(startHour - 1) }, { onStart(startHour + 1) })
        HourRow(stringResource(R.string.onb_ostatnia_szklanka), endHour, { onEnd(endHour - 1) }, { onEnd(endHour + 1) })
    }
    Text(stringResource(R.string.onb_jedna_porcja), color = KropiColors.mutedForeground, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GLASS_CHOICES.forEach { ml -> ChipButton("$ml", glassMl == ml) { onGlass(ml) } }
    }
    Text(stringResource(R.string.onb_ton_przypomnien), color = KropiColors.mutedForeground, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NotificationTone.entries.forEach { t -> ChipButton("${t.emoji} ${t.label}", tone == t) { onTone(t) } }
    }
    Text(
        when (tone) {
            NotificationTone.CARING -> stringResource(R.string.settings_kropi_przypomina_spokojnie_ze_zdaniem)
            NotificationTone.SNARKY -> stringResource(R.string.settings_kropi_dogryza_i_nie_owija)
        },
        color = KropiColors.mutedForeground,
        fontSize = 12.sp,
    )
}

@Composable
private fun HourRow(label: String, hour: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = KropiColors.foreground, fontSize = 14.sp, modifier = Modifier.weight(1f))
        StepperButton("−", label, onMinus)
        Text(
            String.format("%02d:00", hour),
            color = KropiColors.foreground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        StepperButton("+", label, onPlus)
    }
}

@Composable
private fun RemindersStep(
    reminders: Boolean,
    onReminders: (Boolean) -> Unit,
    onRequestNotifications: () -> Unit,
    onPinWidget: () -> Unit,
) {
    val context = LocalContext.current
    var batteryOk by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    StepHeader(stringResource(R.string.onb_krok_z_3), stringResource(R.string.onb_ostatnie_szlify), stringResource(R.string.onb_zeby_przypomnienia_i_widget_dzialaly))
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_przypomnienia), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.onb_powiadomienie_gdy_jestes_za_planem), color = KropiColors.mutedForeground, fontSize = 11.sp)
            }
            val switchLabel1 = stringResource(R.string.onb_powiadomienie_gdy_jestes_za_planem)
            Switch(
                modifier = Modifier.semantics { contentDescription = switchLabel1 },
                checked = reminders,
                onCheckedChange = { on ->
                    onReminders(on)
                    if (on) onRequestNotifications()
                },
                colors = SwitchDefaults.colors(checkedTrackColor = KropiColors.aqua, checkedThumbColor = KropiColors.background),
            )
        }
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.settings_bez_ograniczen_baterii), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.onb_bez_tego_telefon_potrafi_uspic),
            color = KropiColors.mutedForeground,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        Button(
            onClick = {
                requestIgnoreBatteryOptimizations(context)
                batteryOk = isIgnoringBatteryOptimizations(context)
            },
            enabled = !batteryOk,
            colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (batteryOk) stringResource(R.string.onb_wlaczone) else stringResource(R.string.onb_zezwol)) }
        if (isProbablyMiui()) {
            Button(
                onClick = { openMiuiAutostartSettings(context) },
                colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.onb_otworz_autostart_miui)) }
        }
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.onb_widget_na_ekranie_glownym), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.onb_cztery_przyciski_woda_bez_otwierania),
            color = KropiColors.mutedForeground,
            fontSize = 12.sp,
        )
        Button(
            onClick = onPinWidget,
            colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.onb_dodaj_widget)) }
    }
}
