package com.kropi.hydration.ui

import com.kropi.hydration.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.Intake
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val QUICK_AMOUNTS = listOf(100, 150, 200, 250, 300, 330, 400, 500, 750, 1000)
private const val MAX_ML = 3000

/**
 * Dodawanie i edycja wpisu: ilość, napój, godzina, dzień (dziś/wczoraj) i notatka.
 * [initial] = null oznacza nowy wpis; wtedy domyślnie jest „teraz", woda i 250 ml.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeEditorSheet(
    initial: Intake?,
    netHydration: Boolean,
    /** Dzień dla nowego wpisu (np. wybrany w kalendarzu); null = dziś. */
    defaultEpochDay: Long? = null,
    /** Napój zaznaczony na starcie nowego wpisu (np. wybrany na widgecie). */
    defaultDrink: Drink = Drink.WATER,
    /** Typowa porcja napoju (ostatnio użyta ilość) — podpowiedź przy zmianie napoju w nowym wpisie. */
    typicalMl: (Drink) -> Int = { it.defaultMl },
    onSave: (ml: Int, drink: Drink, note: String?, epochDay: Long, minuteOfDay: Int) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now().toEpochDay() }
    val nowMinute = remember { LocalTime.now().let { it.hour * 60 + it.minute } }

    var ml by remember { mutableIntStateOf(initial?.ml ?: typicalMl(defaultDrink)) }
    // Dopóki nie ruszysz ilości sam/a, zmiana napoju podpowiada jego typową porcję.
    var amountTouched by remember { mutableStateOf(initial != null) }
    var drink by remember { mutableStateOf(initial?.drink ?: defaultDrink) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var epochDay by remember { mutableLongStateOf(initial?.epochDay ?: defaultEpochDay ?: today) }
    // Wpis dopisywany do minionego dnia startuje w południe, a nie o „teraz".
    var minuteOfDay by remember {
        mutableIntStateOf(initial?.minuteOfDay ?: if (epochDay < today) 12 * 60 else nowMinute)
    }

    // Dziś nie da się zalogować łyka z przyszłości.
    fun clampTime(minute: Int): Int {
        val upper = if (epochDay == today) nowMinute else 24 * 60 - 1
        return minute.coerceIn(0, upper)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = KropiColors.card,
        contentColor = KropiColors.foreground,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text(
                if (initial == null) stringResource(R.string.editor_dodaj_wpis) else stringResource(R.string.editor_edytuj_wpis),
                color = KropiColors.foreground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )

            // --- ilość ---
            Label(stringResource(R.string.editor_ilosc))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StepperButton("−", stringResource(R.string.editor_ilosc)) { amountTouched = true; ml = (ml - 10).coerceAtLeast(10) }
                Text(
                    "$ml ml",
                    color = KropiColors.foreground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(110.dp),
                )
                StepperButton("+", stringResource(R.string.editor_ilosc)) { amountTouched = true; ml = (ml + 10).coerceAtMost(MAX_ML) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                QUICK_AMOUNTS.forEach { amount -> ChipButton("$amount", ml == amount) { amountTouched = true; ml = amount } }
            }

            // --- napój ---
            Label(stringResource(R.string.editor_napoj))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Drink.entries.forEach { d -> ChipButton("${d.emoji} ${d.label}", drink == d) {
                    drink = d
                    if (!amountTouched) ml = typicalMl(d)
                } }
            }
            val counted = drink.counted(ml, netHydration)
            Text(
                if (!netHydration || drink.factor >= 1f) stringResource(R.string.editor_liczy_sie_ml, ml)
                else stringResource(R.string.editor_liczy_sie_ml_z_ml, counted, drink.percent, ml),
                color = KropiColors.aqua,
                fontSize = 12.sp,
            )

            // --- kiedy ---
            Label(stringResource(R.string.editor_kiedy))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipButton(stringResource(R.string.editor_dzis), epochDay == today) {
                    epochDay = today
                    minuteOfDay = clampTime(minuteOfDay)
                }
                ChipButton(stringResource(R.string.editor_wczoraj), epochDay == today - 1) { epochDay = today - 1 }
                if (epochDay < today - 1) {
                    ChipButton(LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd.MM")), true) {}
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StepperButton("−", stringResource(R.string.editor_hour)) { minuteOfDay = clampTime(minuteOfDay - 60) }
                Text(
                    String.format("%02d", minuteOfDay / 60),
                    color = KropiColors.foreground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                StepperButton("+", stringResource(R.string.editor_hour)) { minuteOfDay = clampTime(minuteOfDay + 60) }
                Text(":", color = KropiColors.mutedForeground, fontSize = 22.sp)
                StepperButton("−", stringResource(R.string.editor_minutes)) { minuteOfDay = clampTime(minuteOfDay - 5) }
                Text(
                    String.format("%02d", minuteOfDay % 60),
                    color = KropiColors.foreground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                StepperButton("+", stringResource(R.string.editor_minutes)) { minuteOfDay = clampTime(minuteOfDay + 5) }
                if (epochDay == today && initial == null) {
                    ChipButton(stringResource(R.string.editor_teraz), minuteOfDay == nowMinute) { minuteOfDay = nowMinute }
                }
            }

            // --- notatka ---
            Label(stringResource(R.string.editor_notatka_opcjonalnie))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(60) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.editor_np_po_bieganiu), color = KropiColors.mutedForeground) },
                keyboardOptions = KeyboardOptions.Default,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = KropiColors.foreground,
                    unfocusedTextColor = KropiColors.foreground,
                    focusedBorderColor = KropiColors.aqua,
                    unfocusedBorderColor = KropiColors.secondary,
                    cursorColor = KropiColors.aqua,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onSave(ml, drink, note.trim().ifEmpty { null }, epochDay, clampTime(minuteOfDay)) },
                    colors = ButtonDefaults.buttonColors(containerColor = KropiColors.aqua, contentColor = KropiColors.background),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.settings_zapisz), fontWeight = FontWeight.Bold) }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.data_anuluj)) }
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.editor_usun_wpis), color = Color(0xFFFF8A65))
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = KropiColors.mutedForeground, fontSize = 12.sp)
}
