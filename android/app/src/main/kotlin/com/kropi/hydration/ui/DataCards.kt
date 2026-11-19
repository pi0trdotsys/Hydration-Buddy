package com.kropi.hydration.ui

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.kropi.hydration.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import com.kropi.hydration.widget.updateAllWidgets
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kropi.hydration.backup.BackupFormat
import com.kropi.hydration.backup.BackupStore
import com.kropi.hydration.backup.CsvHistory
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.notifications.NotificationScheduler
import com.kropi.hydration.update.UpdateLogic
import com.kropi.hydration.update.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val WHEN: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")

/**
 * Kopie zapasowe jak w Szpili: ręczna kopia na żądanie, kopia dzienna (ostatnie 14),
 * lista z przywracaniem/kasowaniem oraz „Przywróć z pliku…" — konieczne po
 * odinstalowaniu, bo nowa instalacja nie widzi plików poprzedniej w MediaStore.
 * Plik .csv z eksportu historii też da się wczytać (dopisze brakujące dni).
 */
@Composable
fun DataBackupCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { HydrationRepository(context.applicationContext) }

    var entries by remember { mutableStateOf(emptyList<BackupStore.Entry>()) }
    var auto by remember { mutableStateOf(BackupStore.autoEnabled(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<Pair<String, BackupFormat.Snapshot>?>(null) }

    fun refresh() {
        scope.launch { entries = withContext(Dispatchers.IO) { BackupStore.list(context) } }
    }

    suspend fun applyRestore(snapshot: BackupFormat.Snapshot) {
        repo.restore(snapshot)
        updateAllWidgets(context)
        NotificationScheduler.schedule(context)
        message = context.getString(R.string.backup_restored)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) { runCatching { BackupStore.readUri(context, uri) }.getOrNull() }
            when {
                text == null -> message = context.getString(R.string.backup_read_failed)
                CsvHistory.looksLikeCsv(text) -> {
                    val added = repo.importHistory(CsvHistory.parse(text))
                    message = if (added > 0) context.resources.getQuantityString(R.plurals.csv_days_added, added, added) else context.getString(R.string.csv_nothing_new)
                }
                else -> {
                    val snapshot = BackupFormat.decode(text)
                    if (snapshot == null) message = context.getString(R.string.backup_not_ours)
                    else pending = context.getString(R.string.backup_from_file) to snapshot
                }
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    pending?.let { (label, snapshot) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.data_przywrocic_kopie)) },
            text = { Text(stringResource(R.string.data_dane_zastapia_to_co_jest, label)) },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch { applyRestore(snapshot) }
                }) { Text(stringResource(R.string.data_przywroc)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.data_anuluj)) } },
        )
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.data_kopie_zapasowe), color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.data_pliki_laduja_w_pobrane_kropi),
            color = KropiColors.mutedForeground,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    scope.launch {
                        message = runCatching { withContext(Dispatchers.IO) { BackupStore.saveManual(context, repo) } }
                            .fold({ context.getString(R.string.backup_saved, it) }, { context.getString(R.string.backup_save_failed, it.message ?: "") })
                        refresh()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = KropiColors.aqua, contentColor = KropiColors.background),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.data_zapisz_kopie), fontWeight = FontWeight.Bold) }
            Button(
                onClick = { picker.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain", "*/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.data_przywroc_z_pliku)) }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.data_kopia_codziennie), color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.data_automatycznie_ostatnie_dni), color = KropiColors.mutedForeground, fontSize = 11.sp)
            }
            val switchLabel1 = stringResource(R.string.data_automatycznie_ostatnie_dni)
            Switch(
                modifier = Modifier.semantics { contentDescription = switchLabel1 },
                checked = auto,
                onCheckedChange = {
                    auto = it
                    BackupStore.setAutoEnabled(context, it)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = KropiColors.aqua, checkedThumbColor = KropiColors.background),
            )
        }

        message?.let { Text(it, color = KropiColors.aqua, fontSize = 12.sp, lineHeight = 17.sp) }

        if (entries.isEmpty()) {
            Text(stringResource(R.string.data_brak_kopii_w_tej_instalacji), color = KropiColors.mutedForeground, fontSize = 12.sp)
        }
        entries.take(6).forEach { entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(KropiColors.secondary.copy(alpha = 0.5f))
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            if (entry.auto) R.string.backup_entry_auto else R.string.backup_entry_manual,
                            Instant.ofEpochMilli(entry.modifiedMs).atZone(ZoneId.systemDefault()).format(WHEN),
                        ),
                        color = KropiColors.foreground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text("${entry.size / 1024 + 1} kB", color = KropiColors.mutedForeground, fontSize = 10.sp)
                }
                TextButton(onClick = {
                    scope.launch {
                        val text = withContext(Dispatchers.IO) { runCatching { BackupStore.read(context, entry.id) }.getOrNull() }
                        val snapshot = text?.let { BackupFormat.decode(it) }
                        if (snapshot == null) message = context.getString(R.string.backup_corrupt)
                        else pending = context.getString(R.string.backup_from_date, Instant.ofEpochMilli(entry.modifiedMs).atZone(ZoneId.systemDefault()).format(WHEN)) to snapshot
                    }
                }) { Text(stringResource(R.string.data_przywroc), color = KropiColors.aqua, fontSize = 12.sp) }
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { BackupStore.delete(context, entry.id) }
                        refresh()
                    }
                }) { Text(stringResource(R.string.data_usun), color = KropiColors.mutedForeground, fontSize = 12.sp) }
            }
        }
        if (entries.size > 6) {
            Text(stringResource(R.string.data_i_starszych_w_pobrane_kropi, entries.size - 6), color = KropiColors.mutedForeground, fontSize = 11.sp)
        }
    }
}

/**
 * Aktualizacje jak w Szpili: ostatnie wydanie GitHuba to kanał; aplikacja sama
 * sprawdza (raz na parę godzin, także w tle z powiadomieniem), pobiera APK,
 * weryfikuje sumę, pakiet, wersję i podpis, po czym oddaje go instalatorowi systemu.
 */
@Composable
fun UpdateCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var release by remember { mutableStateOf<UpdateLogic.Release?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Float?>(null) }
    val hint = remember { UpdateManager.hint(context) }

    fun check(silent: Boolean) {
        scope.launch {
            busy = true
            if (!silent) status = context.getString(R.string.update_checking)
            runCatching { withContext(Dispatchers.IO) { UpdateManager.checkNow(context) } }
                .onSuccess {
                    release = it
                    status = when {
                        it != null -> context.getString(R.string.update_new, it.version)
                        silent -> null
                        else -> context.getString(R.string.update_latest)
                    }
                }
                .onFailure { if (!silent) status = context.getString(R.string.update_check_failed) }
            busy = false
        }
    }

    fun install(r: UpdateLogic.Release) {
        if (!UpdateManager.canInstall(context)) {
            status = context.getString(R.string.update_allow_install)
            UpdateManager.openInstallPermission(context)
            return
        }
        scope.launch {
            busy = true
            progress = 0f
            status = context.getString(R.string.update_downloading, r.apkName)
            val outcome = runCatching {
                withContext(Dispatchers.IO) {
                    val apk = UpdateManager.download(context, r) { received, total ->
                        progress = if (total > 0) (received.toFloat() / total).coerceIn(0f, 1f) else null
                    }
                    UpdateManager.verify(context, apk, r.sha256) to apk
                }
            }
            outcome.onSuccess { (problem, apk) ->
                if (problem == null) {
                    status = context.getString(R.string.update_ready)
                    UpdateManager.install(context, apk)
                } else {
                    status = problem
                }
            }.onFailure { status = context.getString(R.string.update_download_failed, it.message ?: "") }
            progress = null
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        if (System.currentTimeMillis() - UpdateManager.lastCheckedMs(context) > 3L * 60 * 60 * 1000) check(silent = true)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.data_aktualizacje), color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.data_zainstalowana_wersja, UpdateManager.installedVersionName(context)) +
                if (hint.isNotEmpty() && release == null) stringResource(R.string.update_waiting_suffix, hint) else "",
            color = KropiColors.mutedForeground,
            fontSize = 12.sp,
        )

        release?.let { r ->
            Text(r.title, color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (r.notes.isNotBlank()) {
                Text(
                    r.notes.trim().take(500) + if (r.notes.length > 500) "…" else "",
                    color = KropiColors.mutedForeground,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        progress?.let { LinearProgressIndicator(progress = { it }, color = KropiColors.aqua, modifier = Modifier.fillMaxWidth().height(4.dp)) }
        status?.let { Text(it, color = KropiColors.aqua, fontSize = 12.sp, lineHeight = 17.sp) }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            release?.let { r ->
                Button(
                    onClick = { install(r) },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = KropiColors.aqua, contentColor = KropiColors.background),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.data_pobierz_i_zainstaluj), fontWeight = FontWeight.Bold) }
            }
            Button(
                onClick = { check(silent = false) },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.data_sprawdz_aktualizacje)) }
        }
    }
}

/**
 * Health Connect: zapis wypitych napojów jako nawodnienie oraz waga i kroki, z których liczy się
 * cel. Przełączniki działają od razu (jak karta kopii), a uprawnienia prosi system.
 */
@Composable
fun HealthConnectCard(settings: com.kropi.hydration.data.HydrationSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { HydrationRepository(context.applicationContext) }
    val availability = remember { com.kropi.hydration.health.HealthConnect.availability(context) }

    var granted by remember { mutableStateOf(emptySet<String>()) }
    var lastSync by remember { mutableStateOf<Pair<Long, Int>?>(null) }
    var profileNote by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        granted = com.kropi.hydration.health.HealthConnect.grantedPermissions(context)
        lastSync = repository.lastHealthSync()
    }
    LaunchedEffect(settings) { reload() }

    val permissionLauncher = rememberLauncherForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract(),
    ) { result ->
        granted = result
        scope.launch { reload() }
    }

    val allGranted = granted.containsAll(com.kropi.hydration.health.HealthConnect.PERMISSIONS)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.data_health_connect), color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        when (availability) {
            com.kropi.hydration.health.HealthConnect.Availability.UNSUPPORTED -> Text(
                stringResource(R.string.data_ten_telefon_nie_obsluguje_health),
                color = KropiColors.mutedForeground,
                fontSize = 12.sp,
            )
            com.kropi.hydration.health.HealthConnect.Availability.NEEDS_INSTALL_OR_UPDATE -> {
                Text(
                    stringResource(R.string.data_zainstaluj_lub_zaktualizuj_aplikacje_health),
                    color = KropiColors.mutedForeground,
                    fontSize = 12.sp,
                )
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata"),
                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.data_otworz_w_sklepie)) }
            }
            com.kropi.hydration.health.HealthConnect.Availability.AVAILABLE -> {
                Text(
                    stringResource(R.string.data_kropi_moze_zapisywac_to_co),
                    color = KropiColors.mutedForeground,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
                if (!allGranted) {
                    Button(
                        onClick = { permissionLauncher.launch(com.kropi.hydration.health.HealthConnect.PERMISSIONS) },
                        colors = ButtonDefaults.buttonColors(containerColor = KropiColors.aqua, contentColor = KropiColors.background),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.data_polacz_z_health_connect), fontWeight = FontWeight.Bold) }
                }

                HealthToggle(
                    title = stringResource(R.string.data_zapisuj_nawodnienie),
                    detail = lastSync?.let { (at, count) ->
                        stringResource(R.string.data_ostatnio_wpisow_z_dni, WHEN.format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())), context.entriesText(count))
                    } ?: stringResource(R.string.data_napoje_trafia_do_health_connect),
                    checked = settings.healthWriteHydration,
                    enabled = allGranted,
                ) { on ->
                    scope.launch {
                        repository.saveSettings(settings.copy(healthWriteHydration = on))
                        if (on) repository.syncHealth()
                        reload()
                    }
                }
                HealthToggle(
                    title = stringResource(R.string.data_waga_i_aktywnosc_z_health),
                    detail = profileNote ?: stringResource(R.string.data_ostatnia_waga_i_srednia_krokow),
                    checked = settings.healthSyncProfile,
                    enabled = allGranted,
                ) { on ->
                    scope.launch {
                        repository.saveSettings(settings.copy(healthSyncProfile = on))
                        if (on) profileNote = describeProfile(context, repository.refreshProfileFromHealth(force = true))
                    }
                }
                HealthToggle(
                    title = stringResource(R.string.health_workouts_title),
                    detail = stringResource(R.string.health_workouts_detail, com.kropi.hydration.data.Workout.ML_PER_HOUR),
                    checked = settings.healthWorkouts,
                    enabled = allGranted,
                ) { on ->
                    scope.launch {
                        repository.saveSettings(settings.copy(healthWorkouts = on))
                        repository.syncWorkouts()
                    }
                }
                if (allGranted && settings.healthSyncProfile) {
                    Button(
                        onClick = { scope.launch { profileNote = describeProfile(context, repository.refreshProfileFromHealth(force = true)) } },
                        colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.data_pobierz_teraz)) }
                }
            }
        }
    }
}

private fun describeProfile(context: android.content.Context, result: com.kropi.hydration.data.HealthProfileResult?): String = when {
    result == null -> context.getString(R.string.health_no_permission)
    result.weightKg == null && result.averageSteps == null -> context.getString(R.string.health_nothing_yet)
    else -> listOfNotNull(
        result.weightKg?.let { context.getString(R.string.health_weight, it.toInt().toString()) },
        result.averageSteps?.let {
            context.getString(R.string.health_steps, it.toString(), result.activity?.label?.lowercase() ?: "")
        },
    ).joinToString(", ").replaceFirstChar { it.uppercase() }
}

@Composable
private fun HealthToggle(title: String, detail: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = KropiColors.mutedForeground, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Switch(
            modifier = Modifier.semantics { contentDescription = title },
            checked = checked && enabled,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = KropiColors.aqua, checkedThumbColor = KropiColors.background),
        )
    }
}
