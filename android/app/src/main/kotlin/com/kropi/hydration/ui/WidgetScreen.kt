package com.kropi.hydration.ui

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
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column {
                Text(
                    "Widget — skalowalny",
                    color = KropiColors.foreground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Jeden komponent, trzy rozmiary. Ten sam stan co na ekranie głównym.",
                    color = KropiColors.mutedForeground,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        item {
            Button(
                onClick = onPin,
                colors = ButtonDefaults.buttonColors(containerColor = KropiColors.aqua, contentColor = KropiColors.background),
            ) {
                Text("Dodaj widget do ekranu głównego", fontWeight = FontWeight.Bold)
            }
        }
        item {
            Text(
                "Powinieneś/aś już wypić: ${state.targetSoFar} ml",
                color = if (state.isBehindSchedule) Color(0xFFFF8A65) else KropiColors.aqua,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        item { SizeLabel("Mały (1×1)") }
        item { WidgetCard(state, WidgetVariant.SMALL, onAdd, onUndo, onPoke, Modifier.size(140.dp)) }
        item { SizeLabel("Średni (2×1)") }
        item { WidgetCard(state, WidgetVariant.MEDIUM, onAdd, onUndo, onPoke, Modifier.fillMaxWidth().height(150.dp)) }
        item { SizeLabel("Duży (2×2)") }
        item { WidgetCard(state, WidgetVariant.LARGE, onAdd, onUndo, onPoke, Modifier.fillMaxWidth().height(320.dp)) }
    }
}
