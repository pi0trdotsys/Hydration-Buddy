package com.kropi.hydration.ui

import com.kropi.hydration.R
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import com.kropi.hydration.widget.updateAllWidgets
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.NfcTag
import com.kropi.hydration.data.formatMl
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Bezokienkowa trampolina dla skrótów spod ikony aplikacji (`res/xml/shortcuts.xml`) i dla tagów NFC:
 * dolewa łyk i od razu się zamyka, żeby zapisanie go nie wymagało przechodzenia przez cały ekran główny.
 *
 * Adres: `kropi://add/500` (ilość w ostatnim segmencie) albo `kropi://add?ml=250&drink=COFFEE`
 * (patrz [NfcTag]). Bez ilości dolewamy szklankę z ustawień, więc skrót nadąża za zmianą jej rozmiaru.
 * Opcjonalne `at` i `note` (np. z Drink Trackera) zapisują wpis wstecz i z notatką.
 * Zawartość tagu jest nieufna: ilość jest przycinana, a nieznany napój to woda.
 */
class QuickAddActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val raw = intent?.data?.toString()
        val request = NfcTag.parse(raw)
        val fromTag = intent?.action == NfcAdapter.ACTION_NDEF_DISCOVERED

        lifecycleScope.launch {
            val repository = HydrationRepository(applicationContext)
            val state = repository.current()
            val ml = request?.ml ?: state.settings.reminderGlassMl
            val drink = request?.drink ?: Drink.WATER

            // Tag przyłożony dwa razy z rzędu (albo trzymany przy telefonie) zapisuje jeden łyk.
            if (fromTag && raw != null && repository.nfcIsRepeat(raw)) {
                Toast.makeText(this@QuickAddActivity, getString(R.string.nfc_already_added), Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            // Wpis z innej aplikacji może nieść czas wypicia (np. piwo zalogowane wstecz) – wtedy trafia
            // na właściwy dzień i godzinę zamiast na „teraz”.
            val at = request?.atMs?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
            repository.addIntake(
                ml = ml,
                drink = drink,
                note = request?.note,
                epochDay = at?.toLocalDate()?.toEpochDay(),
                minuteOfDay = at?.let { it.hour * 60 + it.minute },
            )
            updateAllWidgets(applicationContext)
            if (fromTag) buzz()

            // Napój inny niż woda liczy się wg współczynnika, więc „po" bierzemy z odświeżonego stanu.
            val after = repository.current()
            Toast.makeText(
                this@QuickAddActivity,
                getString(R.string.quickadd_toast, formatMl(ml), formatMl(after.total), formatMl(after.goal)),
                Toast.LENGTH_SHORT,
            ).show()
            finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun buzz() {
        runCatching {
            val vibrator = getSystemService(Vibrator::class.java) ?: return
            vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
