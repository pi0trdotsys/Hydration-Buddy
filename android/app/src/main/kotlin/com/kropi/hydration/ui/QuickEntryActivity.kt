package com.kropi.hydration.ui

import com.kropi.hydration.R
import androidx.compose.ui.res.stringResource
import android.os.Bundle
import com.kropi.hydration.widget.updateAllWidgets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.data.Intake
import kotlinx.coroutines.launch

/**
 * Edytor otwierany z widgetu, w przezroczystym oknie nad ekranem głównym.
 *  - „＋ wpis": pusty edytor (ilość, napój, dzień, godzina, notatka), z napojem wybranym na widgecie.
 *  - dotknięcie wykresu ([EXTRA_DAY_LIST]): lista dzisiejszych wpisów — każdy można poprawić albo
 *    usunąć, można też dopisać nowy.
 */
class QuickEntryActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DAY_LIST = "day_list"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = HydrationRepository(applicationContext)
        val startWithList = intent?.getBooleanExtra(EXTRA_DAY_LIST, false) == true

        fun refreshWidgets() = lifecycleScope.launch { updateAllWidgets(applicationContext) }

        setContent {
            MaterialTheme {
                val loaded by repository.state.collectAsState(initial = null)
                // Najpierw lista (jeśli tak otwarto), a edytor wjeżdża na jej miejsce.
                var showList by remember { mutableStateOf(startWithList) }
                var editing by remember { mutableStateOf<Intake?>(null) }
                var adding by remember { mutableStateOf(!startWithList) }

                val state = loaded ?: return@MaterialTheme

                when {
                    editing != null || adding -> IntakeEditorSheet(
                        initial = editing,
                        netHydration = state.settings.netHydration,
                        defaultDrink = state.widgetDrink,
                        typicalMl = { state.typicalMl(it) },
                        onSave = { ml, kind, note, day, minute ->
                            lifecycleScope.launch {
                                val target = editing
                                if (target == null) {
                                    repository.addIntake(ml, kind, note, day, minute)
                                    // Napój wybrany na widgecie obowiązuje na jeden wpis, jak przy butelkach.
                                    if (repository.widgetDrink() != com.kropi.hydration.data.Drink.WATER) {
                                        repository.setWidgetDrink(com.kropi.hydration.data.Drink.WATER)
                                    }
                                } else {
                                    repository.updateIntake(target.id, ml, kind, note, day, minute)
                                }
                                refreshWidgets().join()
                                if (showList) { editing = null; adding = false } else finish()
                            }
                        },
                        onDelete = editing?.let { old ->
                            {
                                lifecycleScope.launch {
                                    repository.deleteIntake(old.id)
                                    refreshWidgets().join()
                                    editing = null
                                    if (!showList) finish()
                                }
                            }
                        },
                        onDismiss = { if (showList) { editing = null; adding = false } else finish() },
                    )
                    showList -> DayEntriesSheet(
                        state = state,
                        onEdit = { editing = it },
                        onAdd = { adding = true },
                        onDismiss = { finish() },
                    )
                }
            }
        }
    }
}

/** Dzisiejsze wpisy od najnowszego: dotknięcie otwiera edytor, na dole „＋ Dodaj wpis". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayEntriesSheet(state: HydrationState, onEdit: (Intake) -> Unit, onAdd: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = KropiColors.card,
        contentColor = KropiColors.foreground,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.quick_dzisiejsze_wpisy), color = KropiColors.foreground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.quick_ml_dotknij_wpis_zeby_go, state.total, state.goal),
                color = KropiColors.mutedForeground,
                fontSize = 12.sp,
            )
            if (state.intakes.isEmpty()) {
                Text(stringResource(R.string.history_brak_wpisow), color = KropiColors.mutedForeground, fontSize = 13.sp)
            }
            state.intakes.sortedWith(compareByDescending<Intake> { it.minuteOfDay }.thenByDescending { it.id }).forEach { intake ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(KropiColors.secondary)
                        .clickable { onEdit(intake) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(intake.drink.emoji, fontSize = 16.sp, modifier = Modifier.padding(end = 10.dp))
                    Text(
                        String.format("%02d:%02d", intake.hour, intake.minute),
                        color = KropiColors.mutedForeground,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(
                        listOfNotNull(
                            if (intake.counted != intake.ml) stringResource(R.string.history_liczy_sie, intake.counted) else null,
                            intake.note,
                        ).joinToString(" · "),
                        color = KropiColors.mutedForeground,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${intake.ml} ml", color = KropiColors.foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = KropiColors.aqua, contentColor = KropiColors.background),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.home_dodaj_wpis), fontWeight = FontWeight.Bold) }
        }
    }
}
