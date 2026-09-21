package com.kropi.hydration.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.kropi.hydration.data.HydrationRepository

val MlKey = ActionParameters.Key<Int>("ml")

/**
 * Zapis idzie do DataStore, a widget odświeża się sam — jego kompozycja zbiera
 * stan jako Flow (patrz [HydrationWidget.provideGlance]). [updateAll] zostaje
 * dla instancji, które akurat nie mają żywej sesji Glance, i żeby wszystkie
 * kafelki na ekranie głównym pokazywały to samo, a nie tylko ten dotknięty.
 */
private suspend fun refreshWidgets(context: Context) {
    runCatching { HydrationWidget().updateAll(context) }
}

class AddWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val ml = parameters[MlKey] ?: return
        HydrationRepository(context).addWater(ml)
        refreshWidgets(context)
    }
}

class UndoWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        HydrationRepository(context).undoLast()
        refreshWidgets(context)
    }
}

class PokeMascotAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        HydrationRepository(context).poke()
        refreshWidgets(context)
    }
}
