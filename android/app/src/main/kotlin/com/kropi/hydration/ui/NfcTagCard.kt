package com.kropi.hydration.ui

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kropi.hydration.R
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.QuickAdd
import com.kropi.hydration.data.formatMl
import com.kropi.hydration.nfc.NfcWriter

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Tagi NFC: naklejka na kubku albo butelce, która po przyłożeniu telefonu dolewa dokładnie to, co w niej
 * zapisano (np. „kawa 200 ml"). Programuje się ją tutaj; odczyt nie wymaga żadnej aplikacji ani uprawnień.
 */
@Composable
fun NfcTagCard() {
    val activity = LocalContext.current.findActivity()
    val hardware = remember(activity) { activity?.let { NfcWriter.hardware(it) } ?: NfcWriter.Hardware.MISSING }
    var showDialog by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.nfc_title), color = KropiColors.foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.nfc_desc), color = KropiColors.mutedForeground, fontSize = 12.sp, lineHeight = 17.sp)
        when (hardware) {
            NfcWriter.Hardware.MISSING -> Text(stringResource(R.string.nfc_no_hardware), color = KropiColors.mutedForeground, fontSize = 12.sp)
            NfcWriter.Hardware.DISABLED -> {
                Text(stringResource(R.string.nfc_disabled), color = KropiColors.mutedForeground, fontSize = 12.sp)
                Button(
                    onClick = { activity?.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                    colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.nfc_open_settings)) }
            }
            NfcWriter.Hardware.READY -> Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = KropiColors.aqua, contentColor = KropiColors.background),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.nfc_button), fontWeight = FontWeight.Bold) }
        }
    }
    if (showDialog && activity != null) NfcWriteDialog(activity) { showDialog = false }
}

@Composable
private fun NfcWriteDialog(activity: Activity, onDismiss: () -> Unit) {
    var drink by remember { mutableStateOf(Drink.WATER) }
    var ml by remember { mutableIntStateOf(250) }
    // null = czeka na tag; inaczej wynik ostatniej próby.
    var result by remember { mutableStateOf<NfcWriter.Result?>(null) }
    var done by remember { mutableStateOf(false) }

    // Zmiana napoju zmienia też typową porcję (kawa 200, a nie 250), dopóki nikt nie ustawił ilości ręcznie.
    DisposableEffect(drink, ml) {
        val message = NfcWriter.message(ml, drink, activity.packageName)
        NfcWriter.listen(activity) { tag ->
            val outcome = NfcWriter.write(tag, message)
            activity.runOnUiThread {
                result = outcome
                done = outcome == NfcWriter.Result.OK
            }
        }
        onDispose { NfcWriter.stopListening(activity) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KropiColors.card,
        title = { Text(stringResource(R.string.nfc_dialog_title), color = KropiColors.foreground, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.nfc_pick_drink), color = KropiColors.mutedForeground, fontSize = 12.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Drink.entries.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { d ->
                                ChipButton("${d.emoji} ${d.label}", drink == d) {
                                    drink = d
                                    ml = d.defaultMl
                                    result = null; done = false
                                }
                            }
                        }
                    }
                }
                Text(stringResource(R.string.nfc_amount, formatMl(ml)), color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StepperButton("−", stringResource(R.string.nfc_amount_less)) { ml = (ml - 50).coerceAtLeast(QuickAdd.MIN_ML.coerceAtLeast(50)); result = null; done = false }
                    StepperButton("+", stringResource(R.string.nfc_amount_more)) { ml = (ml + 50).coerceAtMost(QuickAdd.MAX_ML); result = null; done = false }
                }
                Text(
                    when {
                        done -> stringResource(R.string.nfc_ok, formatMl(ml), drink.label.lowercase())
                        result == NfcWriter.Result.TOO_SMALL -> stringResource(R.string.nfc_err_small)
                        result == NfcWriter.Result.READ_ONLY -> stringResource(R.string.nfc_err_readonly)
                        result == NfcWriter.Result.UNSUPPORTED -> stringResource(R.string.nfc_err_unsupported)
                        result == NfcWriter.Result.FAILED -> stringResource(R.string.nfc_err_failed)
                        else -> stringResource(R.string.nfc_hint_waiting)
                    },
                    color = if (done) KropiColors.aqua else KropiColors.mutedForeground,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.nfc_close), color = KropiColors.aqua) } },
    )
}
