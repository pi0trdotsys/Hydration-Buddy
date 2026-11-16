package com.kropi.hydration.ui

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.glance.appwidget.updateAll
import com.kropi.hydration.backup.BackupFormat
import com.kropi.hydration.backup.BackupStore
import com.kropi.hydration.backup.CsvHistory
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.notifications.NotificationScheduler
import com.kropi.hydration.update.UpdateLogic
import com.kropi.hydration.update.UpdateManager
import com.kropi.hydration.widget.HydrationWidget
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
        HydrationWidget().updateAll(context)
        NotificationScheduler.schedule(context)
        message = "Przywrócono kopię. Widget i przypomnienia już ją widzą."
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) { runCatching { BackupStore.readUri(context, uri) }.getOrNull() }
            when {
                text == null -> message = "Nie udało się odczytać pliku."
                CsvHistory.looksLikeCsv(text) -> {
                    val added = repo.importHistory(CsvHistory.parse(text))
                    message = if (added > 0) "Dodano $added dni do historii z CSV." else "W historii jest już wszystko z tego CSV."
                }
                else -> {
                    val snapshot = BackupFormat.decode(text)
                    if (snapshot == null) message = "To nie jest kopia ani eksport Kropi."
                    else pending = "z wybranego pliku" to snapshot
                }
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    pending?.let { (label, snapshot) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Przywrócić kopię?") },
            text = { Text("Dane $label zastąpią to, co jest teraz w Kropi (łyki, historia, ustawienia).") },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch { applyRestore(snapshot) }
                }) { Text("Przywróć") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Anuluj") } },
        )
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Kopie zapasowe", color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            "Pliki lądują w Pobrane/Kropi/Kopie. Przed odinstalowaniem aplikacji zrób kopię, " +
                "a po instalacji wybierz ją przez „Przywróć z pliku…”.",
            color = KropiColors.mutedForeground,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    scope.launch {
                        message = runCatching { withContext(Dispatchers.IO) { BackupStore.saveManual(context, repo) } }
                            .fold({ "Zapisano: $it" }, { "Nie udało się zapisać kopii: ${it.message}" })
                        refresh()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = KropiColors.aqua, contentColor = KropiColors.background),
                modifier = Modifier.weight(1f),
            ) { Text("Zapisz kopię", fontWeight = FontWeight.Bold) }
            Button(
                onClick = { picker.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain", "*/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
                modifier = Modifier.weight(1f),
            ) { Text("Przywróć z pliku…") }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Kopia codziennie", color = KropiColors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Automatycznie, ostatnie 14 dni", color = KropiColors.mutedForeground, fontSize = 11.sp)
            }
            Switch(
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
            Text("Brak kopii w tej instalacji.", color = KropiColors.mutedForeground, fontSize = 12.sp)
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
                        (if (entry.auto) "Automatyczna · " else "Zapisana · ") +
                            Instant.ofEpochMilli(entry.modifiedMs).atZone(ZoneId.systemDefault()).format(WHEN),
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
                        if (snapshot == null) message = "Ta kopia jest uszkodzona."
                        else pending = "z ${Instant.ofEpochMilli(entry.modifiedMs).atZone(ZoneId.systemDefault()).format(WHEN)}" to snapshot
                    }
                }) { Text("Przywróć", color = KropiColors.aqua, fontSize = 12.sp) }
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { BackupStore.delete(context, entry.id) }
                        refresh()
                    }
                }) { Text("Usuń", color = KropiColors.mutedForeground, fontSize = 12.sp) }
            }
        }
        if (entries.size > 6) {
            Text("…i ${entries.size - 6} starszych w Pobrane/Kropi/Kopie", color = KropiColors.mutedForeground, fontSize = 11.sp)
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
            if (!silent) status = "Sprawdzam GitHuba…"
            runCatching { withContext(Dispatchers.IO) { UpdateManager.checkNow(context) } }
                .onSuccess {
                    release = it
                    status = when {
                        it != null -> "Jest nowsza wersja: ${it.version}."
                        silent -> null
                        else -> "Masz najnowszą wersję."
                    }
                }
                .onFailure { if (!silent) status = "Nie udało się sprawdzić (brak internetu?)." }
            busy = false
        }
    }

    fun install(r: UpdateLogic.Release) {
        if (!UpdateManager.canInstall(context)) {
            status = "Zezwól Kropi na instalowanie aplikacji, a potem dotknij ponownie."
            UpdateManager.openInstallPermission(context)
            return
        }
        scope.launch {
            busy = true
            progress = 0f
            status = "Pobieram ${r.apkName}…"
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
                    status = "Gotowe — potwierdź instalację w oknie systemu."
                    UpdateManager.install(context, apk)
                } else {
                    status = problem
                }
            }.onFailure { status = "Pobieranie nie powiodło się: ${it.message}" }
            progress = null
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        if (System.currentTimeMillis() - UpdateManager.lastCheckedMs(context) > 3L * 60 * 60 * 1000) check(silent = true)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Aktualizacje", color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            "Zainstalowana wersja: ${UpdateManager.installedVersionName(context)}" +
                if (hint.isNotEmpty() && release == null) " · czeka $hint" else "",
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
                ) { Text("Pobierz i zainstaluj", fontWeight = FontWeight.Bold) }
            }
            Button(
                onClick = { check(silent = false) },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
                modifier = Modifier.weight(1f),
            ) { Text("Sprawdź aktualizacje") }
        }
    }
}
