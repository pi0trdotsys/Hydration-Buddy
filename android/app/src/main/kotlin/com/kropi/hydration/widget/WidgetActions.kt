package com.kropi.hydration.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.HydrationRepository

val MlKey = ActionParameters.Key<Int>("ml")
val DrinkKey = ActionParameters.Key<String>("drink")

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
        val repository = HydrationRepository(context)
        // Wybrany na widgecie napój obowiązuje na jedno dolanie; potem znów woda,
        // żeby późniejsze „250" nie zapisało się przypadkiem jako kawa.
        val drink = repository.widgetDrink()
        repository.addIntake(ml, drink)
        if (drink != Drink.WATER) repository.setWidgetDrink(Drink.WATER)
        refreshWidgets(context)
    }
}

class SetDrinkAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val picked = Drink.of(parameters[DrinkKey])
        val repository = HydrationRepository(context)
        // Drugie dotknięcie wybranego napoju cofa wybór do wody.
        repository.setWidgetDrink(if (repository.widgetDrink() == picked) Drink.WATER else picked)
        refreshWidgets(context)
    }
}

class CycleDrinkAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repository = HydrationRepository(context)
        val all = Drink.entries
        repository.setWidgetDrink(all[(all.indexOf(repository.widgetDrink()) + 1) % all.size])
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
