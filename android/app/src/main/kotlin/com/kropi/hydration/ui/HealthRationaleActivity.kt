package com.kropi.hydration.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Ekran „po co Kropi prosi o te uprawnienia" — Health Connect pokazuje go przy zgodach. */
class HealthRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize().background(KropiColors.background).padding(24.dp),
                ) {
                    Text("Dlaczego Health Connect?", color = KropiColors.foreground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "• Zapis nawodnienia: Kropi zapisuje wypite napoje, żeby inne aplikacje zdrowotne widziały to, co pijesz.\n" +
                            "• Odczyt wagi i kroków: z nich Kropi liczy dzienny cel nawodnienia (waga i poziom aktywności).\n\n" +
                            "Dane zostają na telefonie. Kropi nigdzie ich nie wysyła, a zgodę możesz w każdej chwili cofnąć w ustawieniach Health Connect.",
                        color = KropiColors.mutedForeground,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                    Button(
                        onClick = { finish() },
                        colors = ButtonDefaults.buttonColors(containerColor = KropiColors.aqua, contentColor = KropiColors.background),
                    ) { Text("Rozumiem", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
