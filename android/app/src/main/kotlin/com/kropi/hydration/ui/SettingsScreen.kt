package com.kropi.hydration.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.kropi.hydration.data.Temperature

@Composable
fun SettingsScreen(
    initial: HydrationSettings,
    onSave: (HydrationSettings) -> Unit,
    onTestNotification: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column {
                Text("Ustawienia", color = KropiColors.foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Cel dzienny, aktywne godziny i przypomnienia o piciu wody.",
                    color = KropiColors.mutedForeground,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        item { BackgroundRestrictionsCard() }
        item { SettingsSection(initial = initial, onSave = onSave, onTestNotification = onTestNotification) }
    }
}

/**
 * MIUI (and some other OEM skins) aggressively freeze/kill background apps,
 * which delays the broadcast that a widget tap or reminder relies on — the
 * water gets logged, but the widget/notification can lag behind until the
 * app is reopened. This card walks the user through the two switches that
 * actually fix it, since neither is reachable through a single system API.
 */
@Composable
private fun BackgroundRestrictionsCard() {
    val context = LocalContext.current
    val isMiui = remember { isProbablyMiui() }
    var ignoringBatteryOpt by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Szybsze powiadomienia i widget", color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            "Jeśli dolanie wody z widgetu widać dopiero po otwarciu aplikacji, to system usypia Kropi w tle. " +
                "Wyłącz dla niej optymalizację baterii" + (if (isMiui) " i włącz autostart" else "") + ", żeby widget odświeżał się od razu.",
            color = KropiColors.mutedForeground,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Bez ograniczeń baterii", color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (ignoringBatteryOpt) "Włączone" else "Dotknij, aby zezwolić",
                    color = if (ignoringBatteryOpt) KropiColors.aqua else KropiColors.mutedForeground,
                    fontSize = 11.sp,
                )
            }
            Switch(
                checked = ignoringBatteryOpt,
                onCheckedChange = {
                    if (!ignoringBatteryOpt) requestIgnoreBatteryOptimizations(context)
                    ignoringBatteryOpt = isIgnoringBatteryOptimizations(context)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = KropiColors.aqua, checkedThumbColor = KropiColors.background),
            )
        }

        if (isMiui) {
            Button(
                onClick = { openMiuiAutostartSettings(context) },
                colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Otwórz ustawienia autostartu (MIUI)")
            }
        }
    }
}

private fun isProbablyMiui(): Boolean =
    Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) || Build.BRAND.equals("redmi", ignoreCase = true) ||
        Build.BRAND.equals("poco", ignoreCase = true)

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    runCatching {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun openMiuiAutostartSettings(context: Context) {
    val candidates = listOf(
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivityNew"),
    )
    for (component in candidates) {
        val opened = runCatching {
            context.startActivity(
                Intent().apply {
                    this.component = component
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }.isSuccess
        if (opened) return
    }
    // Fallback: the app's own system settings page, where the user can still find
    // battery/autostart controls under "App info" on most OEM skins.
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}

@Composable
private fun SettingsSection(
    initial: HydrationSettings,
    onSave: (HydrationSettings) -> Unit,
    onTestNotification: () -> Unit,
) {
    var weightKg by remember { mutableStateOf(initial.weightKg) }
    var temperature by remember { mutableStateOf(initial.temperature) }
    var activity by remember { mutableStateOf(initial.activity) }
    var goalMode by remember { mutableStateOf(initial.goalMode) }
    var manualGoal by remember { mutableStateOf(initial.manualGoalMl.toFloat()) }
    var startHour by remember { mutableStateOf(initial.activeStartHour) }
    var endHour by remember { mutableStateOf(initial.activeEndHour) }
    var glassMl by remember { mutableStateOf(initial.reminderGlassMl) }
    var remindersEnabled by remember { mutableStateOf(initial.remindersEnabled) }

    val calculatedGoal = GoalCalculator.calculate(weightKg, activity, temperature)
    val effectiveGoal = if (goalMode == GoalMode.AUTO) calculatedGoal else manualGoal.toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(KropiColors.card)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // --- reminders toggle ---
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Przypomnienia", color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Fancy powiadomienie, gdy dawno nie piłeś/aś wody",
                    color = KropiColors.mutedForeground,
                    fontSize = 11.sp,
                )
            }
            Switch(
                checked = remindersEnabled,
                onCheckedChange = { remindersEnabled = it },
                colors = SwitchDefaults.colors(checkedTrackColor = KropiColors.aqua, checkedThumbColor = KropiColors.background),
            )
        }

        // --- goal mode ---
        Text("Cel dzienny", color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipButton("Automatyczny", goalMode == GoalMode.AUTO) { goalMode = GoalMode.AUTO }
            ChipButton("Ręczny", goalMode == GoalMode.MANUAL) { goalMode = GoalMode.MANUAL }
        }

        if (goalMode == GoalMode.AUTO) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Waga: ${weightKg.toInt()} kg", color = KropiColors.mutedForeground, fontSize = 12.sp)
                androidx.compose.material3.Slider(
                    value = weightKg,
                    onValueChange = { weightKg = it },
                    valueRange = 30f..150f,
                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = KropiColors.aqua, activeTrackColor = KropiColors.aqua),
                )
            }
            Text("Temperatura otoczenia", color = KropiColors.mutedForeground, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Temperature.entries.forEach { t ->
                    ChipButton("${t.emoji} ${t.label}", temperature == t) { temperature = t }
                }
            }
            Text("Aktywność fizyczna", color = KropiColors.mutedForeground, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityLevel.entries.forEach { a ->
                    ChipButton("${a.emoji} ${a.label}", activity == a) { activity = a }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Cel: ${manualGoal.toInt()} ml", color = KropiColors.mutedForeground, fontSize = 12.sp)
                androidx.compose.material3.Slider(
                    value = manualGoal,
                    onValueChange = { manualGoal = it },
                    valueRange = 1200f..5000f,
                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = KropiColors.aqua, activeTrackColor = KropiColors.aqua),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(KropiColors.secondary)
                .padding(10.dp),
        ) {
            Text(
                "Dzienny cel: $effectiveGoal ml" + if (goalMode == GoalMode.AUTO) " (wyliczony)" else "",
                color = KropiColors.aqua,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // --- active hours ---
        Text("Aktywne godziny picia", color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HourStepper("Od", startHour) { startHour = it.coerceIn(0, endHour - 1) }
            HourStepper("Do", endHour) { endHour = it.coerceIn(startHour + 1, 23) }
        }

        // --- reminder glass size ---
        Text("Rozmiar szklanki w przypomnieniach", color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(100, 250, 330, 500).forEach { ml ->
                ChipButton("$ml ml", glassMl == ml) { glassMl = ml }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    onSave(
                        HydrationSettings(
                            weightKg = weightKg,
                            temperature = temperature,
                            activity = activity,
                            goalMode = goalMode,
                            manualGoalMl = manualGoal.toInt(),
                            activeStartHour = startHour,
                            activeEndHour = endHour,
                            reminderGlassMl = glassMl,
                            remindersEnabled = remindersEnabled,
                        ),
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = KropiColors.aqua, contentColor = KropiColors.background),
                modifier = Modifier.weight(1f),
            ) {
                Text("Zapisz", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onTestNotification,
                colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
                modifier = Modifier.weight(1f),
            ) {
                Text("Testuj powiadomienie")
            }
        }
    }
}
