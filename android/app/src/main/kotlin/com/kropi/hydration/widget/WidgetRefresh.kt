package com.kropi.hydration.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/**
 * Odświeża wszystkie widgety Kropi (główny, „dolej szklankę" i tygodniowy). Wywoływane po każdej
 * zmianie danych; błąd jednego widgetu nie może przerwać zapisu ani odświeżenia pozostałych.
 */
suspend fun updateAllWidgets(context: Context) {
    runCatching { HydrationWidget().updateAll(context) }
    runCatching { GlassWidget().updateAll(context) }
    runCatching { WeekWidget().updateAll(context) }
}
