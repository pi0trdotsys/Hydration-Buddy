package com.kropi.hydration.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
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
    updateAllWidgets(context)
}

class AddWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val ml = parameters[MlKey] ?: return
        val repository = HydrationRepository(context)
        val fixed = parameters[DrinkKey]
        if (fixed != null) {
            // Przycisk z przypisanym napojem („kawa 200") zapisuje dokładnie to, nie rusza wyboru na pasku.
            repository.addIntake(ml, Drink.of(fixed))
        } else {
            // Wybrany na widgecie napój obowiązuje na jedno dolanie; potem znów woda,
            // żeby późniejsze „250" nie zapisało się przypadkiem jako kawa.
            val drink = repository.widgetDrink()
            repository.addIntake(ml, drink)
            if (drink != Drink.WATER) repository.setWidgetDrink(Drink.WATER)
        }
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
        // Cofnięty łyk zostaje na chwilę w pamięci: widget pokazuje „przywróć", a po
        // kilku sekundach zadanie w tle zapomina go i przywraca zwykły wygląd.
        if (HydrationRepository(context).undoLastKeeping()) UndoExpiryWorker.schedule(context)
        refreshWidgets(context)
    }
}

class RestoreUndoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        HydrationRepository(context).restoreUndone()
        refreshWidgets(context)
    }
}

/** Po kilku sekundach od cofnięcia chowa „przywróć" z widgetu. */
class UndoExpiryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        HydrationRepository(applicationContext).clearUndo(olderThanMs = EXPIRY_MS - 1_000L)
        updateAllWidgets(applicationContext)
        return Result.success()
    }

    companion object {
        const val EXPIRY_MS = 8_000L

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<UndoExpiryWorker>()
                .setInitialDelay(EXPIRY_MS, TimeUnit.MILLISECONDS)
                .build()
            // REPLACE: kolejne cofnięcie przedłuża okno „przywróć" o nowe 8 sekund.
            WorkManager.getInstance(context).enqueueUniqueWork("undo_expiry", ExistingWorkPolicy.REPLACE, request)
        }
    }
}

class PokeMascotAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        HydrationRepository(context).poke()
        refreshWidgets(context)
    }
}
