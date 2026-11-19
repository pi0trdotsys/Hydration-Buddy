package com.kropi.hydration.ui

import com.kropi.hydration.R
import android.os.Bundle
import com.kropi.hydration.widget.updateAllWidgets
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.formatMl
import kotlinx.coroutines.launch

/**
 * Bezokienkowa trampolina dla skrótów spod ikony aplikacji
 * (`res/xml/shortcuts.xml`): dolewa wodę i od razu się zamyka, żeby zapisanie
 * łyka nie wymagało przechodzenia przez cały ekran główny.
 *
 * Ilość bierzemy z ostatniego segmentu URI (`kropi://add/500`); `glass`
 * oznacza szklankę z ustawień, więc skrót nadąża za zmianą jej rozmiaru.
 */
class QuickAddActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requested = intent?.data?.lastPathSegment

        lifecycleScope.launch {
            val repository = HydrationRepository(applicationContext)
            val state = repository.current()
            val ml = requested?.toIntOrNull() ?: state.settings.reminderGlassMl

            repository.addWater(ml)
            updateAllWidgets(applicationContext)

            val total = state.total + ml
            Toast.makeText(
                this@QuickAddActivity,
                getString(R.string.quickadd_toast, formatMl(ml), formatMl(total), formatMl(state.goal)),
                Toast.LENGTH_SHORT,
            ).show()
            finish()
        }
    }
}
