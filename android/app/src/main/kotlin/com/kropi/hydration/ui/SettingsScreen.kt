package com.kropi.hydration.ui

import androidx.compose.runtime.mutableIntStateOf
import com.kropi.hydration.data.formatMl
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.kropi.hydration.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.runtime.key
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
import com.kropi.hydration.data.NotificationTone
import com.kropi.hydration.data.SnarkIntensity
import com.kropi.hydration.data.Temperature

@Composable
fun SettingsScreen(
    initial: HydrationSettings,
    onSave: (HydrationSettings) -> Unit,
    onTestNotification: () -> Unit,
    state: com.kropi.hydration.data.HydrationState? = null,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column {
                Text(stringResource(R.string.settings_ustawienia), color = KropiColors.foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.settings_cel_dzienny_aktywne_godziny_i),
                    color = KropiColors.mutedForeground,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        // Najpierw to, po co się tu przychodzi (cel, przypomnienia), a techniczne karty na końcu.
        item {
            // Formularz zapisuje się sam, więc nie wolno go odtwarzać po własnym zapisie (zgubiłby trwający
            // gest na suwaku). Odtwarzamy go tylko, gdy ustawienia zmieniły się z zewnątrz — przywrócona
            // kopia, waga z Health Connect.
            var lastSaved by remember { mutableStateOf(initial) }
            var version by remember { mutableIntStateOf(0) }
            androidx.compose.runtime.LaunchedEffect(initial) {
                if (initial != lastSaved) { lastSaved = initial; version++ }
            }
            key(version) {
                SettingsSection(
                    initial = initial,
                    onSave = { lastSaved = it; onSave(it) },
                    onTestNotification = onTestNotification,
                    state = state,
                )
            }
        }
        item { SectionLabel(stringResource(R.string.settings_section_app)) }
        item { BackgroundRestrictionsCard() }
        item { HealthConnectCard(initial) }
        item { NfcTagCard() }
        item { DataBackupCard() }
        item { UpdateCard() }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = KropiColors.mutedForeground, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
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
        Text(stringResource(R.string.settings_szybsze_powiadomienia_i_widget), color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.settings_jesli_dolanie_wody_z_widgetu, if (isMiui) stringResource(R.string.settings_and_autostart) else ""),
            color = KropiColors.mutedForeground,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_bez_ograniczen_baterii), color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (ignoringBatteryOpt) stringResource(R.string.settings_wlaczone) else stringResource(R.string.settings_dotknij_aby_zezwolic),
                    color = if (ignoringBatteryOpt) KropiColors.aqua else KropiColors.mutedForeground,
                    fontSize = 11.sp,
                )
            }
            val switchLabel1 = stringResource(R.string.settings_bez_ograniczen_baterii)
            Switch(
                modifier = Modifier.semantics { contentDescription = switchLabel1 },
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
                Text(stringResource(R.string.settings_otworz_ustawienia_autostartu_miui))
            }
        }
    }
}

internal fun isProbablyMiui(): Boolean =
    Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) || Build.BRAND.equals("redmi", ignoreCase = true) ||
        Build.BRAND.equals("poco", ignoreCase = true)

internal fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

internal fun requestIgnoreBatteryOptimizations(context: Context) {
    runCatching {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

internal fun openMiuiAutostartSettings(context: Context) {
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
    state: com.kropi.hydration.data.HydrationState?,
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
    var tone by remember { mutableStateOf(initial.notificationTone) }
    var bottles by remember { mutableStateOf(initial.bottlesMl) }
    var intensity by remember { mutableStateOf(initial.snarkIntensity) }
    var adaptivePlan by remember { mutableStateOf(initial.adaptivePlan) }
    var netHydration by remember { mutableStateOf(initial.netHydration) }
    var respectDnd by remember { mutableStateOf(initial.respectDnd) }
    var quietMeetings by remember { mutableStateOf(initial.quietDuringMeetings) }
    var weatherTemp by remember { mutableStateOf(initial.weatherTemperature) }
    var learnedReminders by remember { mutableStateOf(initial.learnedReminders) }
    var anchorCoffee by remember { mutableStateOf(initial.anchorAfterCoffee) }
    var homeWifi by remember { mutableStateOf(initial.homeWifiSsid) }
    var wifiNote by remember { mutableStateOf<Int?>(null) }
    val calendarPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted -> quietMeetings = granted }
    val appContext = LocalContext.current
    val locationPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        // Android 12+ pozwala dać tylko przybliżoną lokalizację — a z nią nazwy sieci nie widać.
        if (result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            val ssid = com.kropi.hydration.notifications.WifiAnchor.currentSsid(appContext)
            if (ssid != null) { homeWifi = ssid; wifiNote = null } else wifiNote = R.string.settings_wifi_unreadable
        } else {
            wifiNote = R.string.settings_wifi_need_location
        }
    }
    // Na żywo: co teraz wstrzymałoby przypomnienie (spotkanie albo „Nie przeszkadzać").
    var holdNow by remember { mutableStateOf<com.kropi.hydration.notifications.ReminderGate.Reason?>(null) }
    androidx.compose.runtime.LaunchedEffect(respectDnd, quietMeetings) {
        holdNow = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.kropi.hydration.notifications.ReminderGate.currentHold(appContext, respectDnd, quietMeetings)
        }
    }

    // Szkic ustawień z tego, co jest w formularzu — zapisujemy go sami, po chwili bez zmian.
    val draft = initial.copy(
        weightKg = weightKg,
        temperature = temperature,
        activity = activity,
        goalMode = goalMode,
        manualGoalMl = manualGoal.toInt(),
        activeStartHour = startHour,
        activeEndHour = endHour,
        reminderGlassMl = glassMl,
        remindersEnabled = remindersEnabled,
        respectDnd = respectDnd,
        quietDuringMeetings = quietMeetings,
        notificationTone = tone,
        bottlesMl = bottles,
        snarkIntensity = intensity,
        adaptivePlan = adaptivePlan,
        netHydration = netHydration,
        weatherTemperature = weatherTemp,
        learnedReminders = learnedReminders,
        anchorAfterCoffee = anchorCoffee,
        homeWifiSsid = homeWifi,
    )
    var justSaved by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(draft) {
        if (draft != initial) {
            kotlinx.coroutines.delay(600)
            onSave(draft)
            justSaved = true
            kotlinx.coroutines.delay(2000)
            justSaved = false
        }
    }

    // Ta sama liczba co w repozytorium: upał z prognozy (jeśli świeża i włączona), korekta osobista, trening, alkohol.
    val outdoorNow = state?.outdoor?.takeIf { weatherTemp && it.isFresh(System.currentTimeMillis()) }
    val goalBreakdown = GoalCalculator.breakdown(
        com.kropi.hydration.data.GoalInputs(
            weightKg = weightKg,
            activity = activity,
            heat = outdoorNow?.let { GoalCalculator.heatFraction(it.heatC) } ?: GoalCalculator.heatFraction(temperature),
            activeHours = (endHour - startHour).coerceAtLeast(1),
            personalPct = state?.checkIn?.activePct ?: 0,
            workoutMl = state?.breakdown?.workoutMl ?: 0,
            alcoholMl = state?.breakdown?.alcoholMl ?: 0,
            outdoorMaxC = outdoorNow?.heatC,
        ),
    )
    val calculatedGoal = goalBreakdown.goalMl
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
                Text(stringResource(R.string.settings_przypomnienia), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.settings_fancy_powiadomienie_gdy_dawno_nie),
                    color = KropiColors.mutedForeground,
                    fontSize = 11.sp,
                )
            }
            val switchLabel2 = stringResource(R.string.settings_fancy_powiadomienie_gdy_dawno_nie)
            Switch(
                modifier = Modifier.semantics { contentDescription = switchLabel2 },
                checked = remindersEnabled,
                onCheckedChange = { remindersEnabled = it },
                colors = SwitchDefaults.colors(checkedTrackColor = KropiColors.aqua, checkedThumbColor = KropiColors.background),
            )
        }

        // --- cisza: Nie przeszkadzać i spotkania ---
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_szanuj_nie_przeszkadzac), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.settings_gdy_telefon_ma_wlaczony_ten),
                    color = KropiColors.mutedForeground,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
            val switchLabel3 = stringResource(R.string.settings_gdy_telefon_ma_wlaczony_ten)
            Switch(
                modifier = Modifier.semantics { contentDescription = switchLabel3 },
                checked = respectDnd,
                onCheckedChange = { respectDnd = it },
                colors = SwitchDefaults.colors(checkedTrackColor = KropiColors.aqua, checkedThumbColor = KropiColors.background),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_cisza_podczas_spotkan), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.settings_kropi_zaglada_do_kalendarza_na),
                    color = KropiColors.mutedForeground,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
            val switchLabel4 = stringResource(R.string.settings_kropi_zaglada_do_kalendarza_na)
            Switch(
                modifier = Modifier.semantics { contentDescription = switchLabel4 },
                checked = quietMeetings,
                onCheckedChange = { on ->
                    if (!on) {
                        quietMeetings = false
                    } else if (com.kropi.hydration.notifications.ReminderGate.hasCalendarPermission(appContext)) {
                        quietMeetings = true
                    } else {
                        calendarPermission.launch(android.Manifest.permission.READ_CALENDAR)
                    }
                },
                colors = SwitchDefaults.colors(checkedTrackColor = KropiColors.aqua, checkedThumbColor = KropiColors.background),
            )
        }
        if (respectDnd || quietMeetings) {
            Text(
                holdNow?.let { stringResource(R.string.settings_teraz_przypomnienia_czekaja, it.label) } ?: stringResource(R.string.settings_teraz_nic_nie_wstrzymuje_przypomnien),
                color = if (holdNow != null) KropiColors.aqua else KropiColors.mutedForeground,
                fontSize = 11.sp,
            )
        }

        // --- ton powiadomień ---
        Text(stringResource(R.string.settings_ton_powiadomien), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NotificationTone.entries.forEach { t ->
                ChipButton("${t.emoji} ${t.label}", tone == t) { tone = t }
            }
        }
        Text(
            when (tone) {
                NotificationTone.CARING -> stringResource(R.string.settings_kropi_przypomina_spokojnie_ze_zdaniem)
                NotificationTone.SNARKY -> stringResource(R.string.settings_kropi_dogryza_i_nie_owija)
            },
            color = KropiColors.mutedForeground,
            fontSize = 11.sp,
        )

        if (tone == NotificationTone.SNARKY) {
            Text(stringResource(R.string.settings_ostrosc_docinkow), color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SnarkIntensity.entries.forEach { level ->
                    ChipButton("${level.emoji} ${level.label}", intensity == level) { intensity = level }
                }
            }
            Text(intensity.hint, color = KropiColors.mutedForeground, fontSize = 11.sp)
        }

        // --- plan dopasowany do nawyków ---
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_plan_pod_twoje_godziny), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.settings_kropi_uczy_sie_o_ktorej),
                    color = KropiColors.mutedForeground,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
            val switchLabel5 = stringResource(R.string.settings_kropi_uczy_sie_o_ktorej)
            Switch(
                modifier = Modifier.semantics { contentDescription = switchLabel5 },
                checked = adaptivePlan,
                onCheckedChange = { adaptivePlan = it },
                colors = SwitchDefaults.colors(checkedTrackColor = KropiColors.aqua, checkedThumbColor = KropiColors.background),
            )
        }

        // --- przypomnienia uczone z nawyków ---
        ToggleRow(
            title = stringResource(R.string.settings_learned_title),
            detail = stringResource(R.string.settings_learned_desc),
            checked = learnedReminders,
        ) { learnedReminders = it }

        // --- skojarzenia z nawykami ---
        Text(stringResource(R.string.settings_anchors_title), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        ToggleRow(
            title = stringResource(R.string.settings_anchor_coffee_title),
            detail = stringResource(R.string.settings_anchor_coffee_desc, com.kropi.hydration.notifications.AnchorWorker.DELAY_MINUTES.toInt()),
            checked = anchorCoffee,
        ) { anchorCoffee = it }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.settings_anchor_wifi_title), color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                if (homeWifi.isBlank()) stringResource(R.string.settings_anchor_wifi_desc)
                else stringResource(R.string.settings_anchor_wifi_set, homeWifi),
                color = if (homeWifi.isBlank()) KropiColors.mutedForeground else KropiColors.aqua,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
            wifiNote?.let { Text(stringResource(it), color = KropiColors.mutedForeground, fontSize = 11.sp, lineHeight = 15.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipButton(stringResource(R.string.settings_wifi_set), false) {
                    if (com.kropi.hydration.notifications.WifiAnchor.hasLocationPermission(appContext)) {
                        val ssid = com.kropi.hydration.notifications.WifiAnchor.currentSsid(appContext)
                        if (ssid != null) { homeWifi = ssid; wifiNote = null } else wifiNote = R.string.settings_wifi_unreadable
                    } else {
                        locationPermission.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }
                if (homeWifi.isNotBlank()) ChipButton(stringResource(R.string.settings_wifi_clear), false) { homeWifi = ""; wifiNote = null }
            }
        }

        // --- napoje wg współczynnika nawodnienia ---
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_nawodnienie_netto), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.settings_kawa_sok_czy_napoj_gazowany),
                    color = KropiColors.mutedForeground,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
            val switchLabel6 = stringResource(R.string.settings_kawa_sok_czy_napoj_gazowany)
            Switch(
                modifier = Modifier.semantics { contentDescription = switchLabel6 },
                checked = netHydration,
                onCheckedChange = { netHydration = it },
                colors = SwitchDefaults.colors(checkedTrackColor = KropiColors.aqua, checkedThumbColor = KropiColors.background),
            )
        }

        // --- goal mode ---
        Text(stringResource(R.string.settings_cel_dzienny), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipButton(stringResource(R.string.settings_automatyczny), goalMode == GoalMode.AUTO) { goalMode = GoalMode.AUTO }
            ChipButton(stringResource(R.string.settings_reczny), goalMode == GoalMode.MANUAL) { goalMode = GoalMode.MANUAL }
        }

        if (goalMode == GoalMode.AUTO) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.settings_waga_kg, weightKg.toInt()), color = KropiColors.mutedForeground, fontSize = 12.sp)
                androidx.compose.material3.Slider(
                    value = weightKg,
                    onValueChange = { weightKg = it },
                    valueRange = 30f..150f,
                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = KropiColors.aqua, activeTrackColor = KropiColors.aqua),
                )
            }
            ToggleRow(
                title = stringResource(R.string.settings_weather_temp_title),
                detail = weatherStatus(state, weatherTemp),
                checked = weatherTemp,
            ) { weatherTemp = it }
            Text(
                stringResource(if (weatherTemp) R.string.settings_weather_manual_fallback else R.string.settings_temperatura_otoczenia),
                color = KropiColors.mutedForeground,
                fontSize = 12.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Temperature.entries.forEach { t ->
                    ChipButton("${t.emoji} ${t.label}", temperature == t) { temperature = t }
                }
            }
            Text(stringResource(R.string.settings_aktywnosc_fizyczna), color = KropiColors.mutedForeground, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityLevel.entries.forEach { a ->
                    ChipButton("${a.emoji} ${a.label}", activity == a) { activity = a }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.settings_cel_ml, manualGoal.toInt()), color = KropiColors.mutedForeground, fontSize = 12.sp)
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
                stringResource(R.string.settings_dzienny_cel_ml, effectiveGoal) + if (goalMode == GoalMode.AUTO) stringResource(R.string.settings_calculated_suffix) else "",
                color = KropiColors.aqua,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (goalMode == GoalMode.AUTO) GoalBreakdownCard(goalBreakdown)

        // --- active hours ---
        Text(stringResource(R.string.settings_aktywne_godziny_picia), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HourStepper("Od", startHour) { startHour = it.coerceIn(0, endHour - 1) }
            HourStepper("Do", endHour) { endHour = it.coerceIn(startHour + 1, 23) }
        }

        // --- pojemności na kafelkach ---
        Text(stringResource(R.string.settings_pojemnosci_na_widgecie), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.settings_cztery_przyciski_dolewania_na_widgecie),
            color = KropiColors.mutedForeground,
            fontSize = 11.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            bottles.forEachIndexed { index, ml ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("$ml ml", color = KropiColors.foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                        StepperButton("−", stringResource(R.string.wcfg_przycisk, index + 1)) {
                            bottles = bottles.toMutableList().also { it[index] = (ml - 50).coerceAtLeast(50) }
                        }
                        StepperButton("+", stringResource(R.string.wcfg_przycisk, index + 1)) {
                            bottles = bottles.toMutableList().also { it[index] = (ml + 50).coerceAtMost(2000) }
                        }
                    }
                }
            }
        }

        // --- reminder glass size ---
        Text(stringResource(R.string.settings_rozmiar_szklanki_w_przypomnieniach), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(100, 250, 330, 500).forEach { ml ->
                ChipButton("$ml ml", glassMl == ml) { glassMl = ml }
            }
        }

        Text(
            if (justSaved) stringResource(R.string.settings_saved) else stringResource(R.string.settings_autosave_hint),
            color = if (justSaved) KropiColors.aqua else KropiColors.mutedForeground,
            fontSize = 11.sp,
        )
        Button(
            onClick = onTestNotification,
            colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_testuj_powiadomienie))
        }
    }
}

/**
 * Skąd się wziął automatyczny cel: składniki, ewentualne przycięcie do tego, co da się realnie wypić,
 * i źródła. Dzięki temu liczba nie jest czarną skrzynką.
 */
@Composable
private fun GoalBreakdownCard(b: com.kropi.hydration.data.GoalBreakdown) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KropiColors.secondary.copy(alpha = 0.5f))
            .padding(12.dp),
    ) {
        Text(stringResource(R.string.goal_how_title), color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        GoalLine(stringResource(R.string.goal_line_base), formatMl(b.baseMl))
        if (b.activityMl > 0) GoalLine(stringResource(R.string.goal_line_activity), "+" + formatMl(b.activityMl))
        if (b.heatMl > 0) {
            GoalLine(
                b.outdoorMaxC?.let { stringResource(R.string.goal_line_heat_outdoor, it.toInt().toString()) } ?: stringResource(R.string.goal_line_heat),
                "+" + formatMl(b.heatMl),
            )
        }
        if (b.personalMl != 0) GoalLine(stringResource(R.string.goal_line_personal), (if (b.personalMl > 0) "+" else "−") + formatMl(kotlin.math.abs(b.personalMl)))
        b.cappedBy?.let { cap ->
            Text(
                when (cap) {
                    com.kropi.hydration.data.GoalCap.FLOOR -> stringResource(R.string.goal_cap_floor, formatMl(b.goalMl))
                    com.kropi.hydration.data.GoalCap.CEILING -> stringResource(R.string.goal_cap_ceiling, formatMl(b.goalMl))
                    com.kropi.hydration.data.GoalCap.PACE -> stringResource(
                        R.string.goal_cap_pace, formatMl(b.goalMl), com.kropi.hydration.data.GoalCalculator.MAX_PACE_ML_PER_HOUR,
                    )
                },
                color = KropiColors.aqua,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
        if (b.workoutMl > 0) GoalLine(stringResource(R.string.goal_line_workout), "+" + formatMl(b.workoutMl))
        if (b.alcoholMl > 0) GoalLine(stringResource(R.string.goal_line_alcohol), "+" + formatMl(b.alcoholMl))
        GoalLine(stringResource(R.string.goal_line_total), formatMl(b.goalMl), bold = true)
        Text(stringResource(R.string.goal_sources), color = KropiColors.mutedForeground, fontSize = 10.sp, lineHeight = 14.sp)
        Text(stringResource(R.string.reg_tip), color = KropiColors.mutedForeground, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun GoalLine(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = KropiColors.mutedForeground, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = KropiColors.foreground,
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
    }
}


/** Wiersz z przełącznikiem: tytuł, opis i dostępny dla czytnika ekranu przełącznik. */
@Composable
internal fun ToggleRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = KropiColors.mutedForeground, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Switch(
            modifier = Modifier.semantics { contentDescription = title },
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = KropiColors.aqua, checkedThumbColor = KropiColors.background),
        )
    }
}

/** Co teraz wiemy o temperaturze z Homebrew Weather — albo dlaczego jej nie ma. */
@Composable
private fun weatherStatus(state: com.kropi.hydration.data.HydrationState?, enabled: Boolean): String {
    val context = LocalContext.current
    val installed = remember { com.kropi.hydration.weather.OutdoorWeatherSource.isInstalled(context) }
    val reading = state?.outdoor?.takeIf { it.isFresh(System.currentTimeMillis()) }
    return when {
        !enabled -> stringResource(
            R.string.settings_weather_temp_desc,
            GoalCalculator.HEAT_FROM_C.toInt(), GoalCalculator.HEAT_FULL_C.toInt(),
        )
        reading != null && reading.maxC != null ->
            stringResource(R.string.settings_weather_status_ok, reading.maxC!!.toInt().toString(), reading.nowC.toInt().toString())
        reading != null -> stringResource(R.string.settings_weather_status_now, reading.nowC.toInt().toString())
        !installed -> stringResource(R.string.settings_weather_status_missing)
        else -> stringResource(R.string.settings_weather_status_nodata)
    }
}
