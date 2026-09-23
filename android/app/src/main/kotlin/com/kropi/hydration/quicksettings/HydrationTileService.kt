package com.kropi.hydration.quicksettings

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.glance.appwidget.updateAll
import com.kropi.hydration.R
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.HydrationState
import com.kropi.hydration.widget.HydrationWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Kafelek w rozwijanym pasku: jedno dotknięcie dolewa szklankę bez
 * odblokowywania telefonu i bez otwierania aplikacji, a podtytuł pokazuje
 * bieżący bilans dnia.
 */
class HydrationTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { render(HydrationRepository(applicationContext).current()) }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val repository = HydrationRepository(applicationContext)
            val glassMl = repository.current().settings.reminderGlassMl
            repository.addWater(glassMl)
            HydrationWidget().updateAll(applicationContext)
            render(repository.current())
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun render(state: HydrationState) {
        val tile = qsTile ?: return
        tile.state = if (state.remaining > 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "+${state.settings.reminderGlassMl} ml"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_notification)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (state.remaining > 0) {
                "${state.total} / ${state.goal} ml"
            } else {
                "Cel zrobiony 🎉"
            }
        }
        tile.updateTile()
    }
}
