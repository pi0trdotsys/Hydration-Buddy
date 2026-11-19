package com.kropi.hydration.ui

import com.kropi.hydration.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kropi.hydration.data.HydrationState

/** Mirrors src/routes/widget.tsx — the same component, in every size, sharing one state. */
@Composable
fun WidgetScreen(
    state: HydrationState,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onPoke: () -> Unit,
    onPin: () -> Unit,
    onPinGlass: () -> Unit,
    onPinWeek: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column {
                Text(
                    stringResource(R.string.widget_widget_skalowalny),
                    color = KropiColors.foreground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.widget_jeden_komponent_trzy_rozmiary_ten),
                    color = KropiColors.mutedForeground,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPin,
                    colors = ButtonDefaults.buttonColors(containerColor = KropiColors.aqua, contentColor = KropiColors.background),
                ) {
                    Text(stringResource(R.string.widget_dodaj_widget_do_ekranu_glownego), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onPinGlass,
                    colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
                ) { Text(stringResource(R.string.widget_dodaj_widget_dolej_szklanke)) }
                Button(
                    onClick = onPinWeek,
                    colors = ButtonDefaults.buttonColors(containerColor = KropiColors.secondary, contentColor = KropiColors.foreground),
                ) { Text(stringResource(R.string.widget_dodaj_widget_tygodniowy)) }
                Text(
                    stringResource(R.string.widget_przytrzymaj_widget_na_ekranie_glownym),
                    color = KropiColors.mutedForeground,
                    fontSize = 11.sp,
                )
            }
        }
        item {
            Text(
                stringResource(R.string.widget_powinienes_as_juz_wypic_ml, state.targetSoFar),
                color = if (state.isBehindSchedule) Color(0xFFFF8A65) else KropiColors.aqua,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        item { SizeLabel(stringResource(R.string.widget_maly)) }
        item { WidgetCard(state, WidgetVariant.SMALL, onAdd, onUndo, onPoke, Modifier.size(140.dp)) }
        item { SizeLabel(stringResource(R.string.widget_sredni)) }
        item { WidgetCard(state, WidgetVariant.MEDIUM, onAdd, onUndo, onPoke, Modifier.fillMaxWidth().height(216.dp)) }
        item { SizeLabel(stringResource(R.string.widget_duzy)) }
        item { WidgetCard(state, WidgetVariant.LARGE, onAdd, onUndo, onPoke, Modifier.fillMaxWidth().height(400.dp)) }
    }
}
