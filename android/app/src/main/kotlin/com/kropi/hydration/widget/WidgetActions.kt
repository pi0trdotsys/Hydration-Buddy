package com.kropi.hydration.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.kropi.hydration.data.HydrationRepository

val MlKey = ActionParameters.Key<Int>("ml")

class AddWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val ml = parameters[MlKey] ?: return
        HydrationRepository(context).addWater(ml)
        HydrationWidget().update(context, glanceId)
    }
}

class UndoWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        HydrationRepository(context).undoLast()
        HydrationWidget().update(context, glanceId)
    }
}

class PokeMascotAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        HydrationRepository(context).poke()
        HydrationWidget().update(context, glanceId)
    }
}
