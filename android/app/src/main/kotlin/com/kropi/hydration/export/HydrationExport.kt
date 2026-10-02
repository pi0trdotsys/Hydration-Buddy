package com.kropi.hydration.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.kropi.hydration.data.DayRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Kontrakt eksportu nawodnienia dla innych aplikacji tego samego autora
 * (Szpila, app.lovable.glow_habit_widget). Tylko do odczytu, chroniony
 * uprawnieniem o poziomie `signature`.
 *
 * - `content://com.kropi.hydration.export/today` — jeden wiersz (dziś)
 * - `content://com.kropi.hydration.export/days` — dziś, potem poprzednie dni
 *   z danymi, od najnowszego, maksymalnie [DAYS_LIMIT] dni wstecz
 *
 * Kolumny: [COLUMNS]. Po każdej zmianie dzisiejszej sumy lub celu leci jawny
 * broadcast [ACTION_HYDRATION_CHANGED] do Szpili (zob. [notifyChanged]).
 */
object HydrationExportContract {
    const val AUTHORITY = "com.kropi.hydration.export"
    const val PERMISSION_READ = "com.kropi.hydration.permission.READ_HYDRATION"
    const val ACTION_HYDRATION_CHANGED = "com.kropi.hydration.action.HYDRATION_CHANGED"
    const val TARGET_PACKAGE = "app.lovable.glow_habit_widget"

    const val PATH_TODAY = "today"
    const val PATH_DAYS = "days"

    const val COL_DATE = "date"
    const val COL_ML = "ml"
    const val COL_GOAL = "goal"
    const val COL_INTAKES = "intakes"
    const val COL_UPDATED = "updated"
    val COLUMNS = arrayOf(COL_DATE, COL_ML, COL_GOAL, COL_INTAKES, COL_UPDATED)

    const val EXTRA_DATE = "date"
    const val EXTRA_ML = "ml"
    const val EXTRA_GOAL = "goal"

    /** Ile dni (licząc dzisiejszy) obejmuje `/days`. */
    const val DAYS_LIMIT = 60

    val TODAY_URI: Uri get() = "content://$AUTHORITY/$PATH_TODAY".toUri()
    val DAYS_URI: Uri get() = "content://$AUTHORITY/$PATH_DAYS".toUri()
}

/** Dzisiejszy stan w postaci, jaką widzi eksport. */
data class ExportToday(
    val epochDay: Long,
    val ml: Int,
    val goal: Int,
    val intakes: Int,
    val updatedEpochMs: Long,
)

/** Jeden wiersz kursora: kolejność pól = [HydrationExportContract.COLUMNS]. */
data class ExportRow(
    val date: String,
    val ml: Int,
    val goal: Int,
    val intakes: Int,
    val updated: Long,
) {
    fun toCursorRow(): Array<Any> = arrayOf(date, ml, goal, intakes, updated)
}

/** Czysta logika budowania wierszy — bez Androida, testowana na JVM. */
object HydrationExportRows {
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun formatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(DATE_FORMAT)

    fun todayRow(today: ExportToday): ExportRow =
        ExportRow(formatDate(today.epochDay), today.ml, today.goal, today.intakes, today.updatedEpochMs)

    /**
     * Dziś na początku, potem zamknięte dni z historii, które mają cokolwiek
     * wypite (dni-luki zapisane jako 0 ml pomijamy), od najnowszego. Okno to
     * [limitDays] dni kalendarzowych licząc dzisiejszy, więc wierszy nigdy nie
     * jest więcej niż [limitDays].
     */
    fun daysRows(
        today: ExportToday,
        history: List<DayRecord>,
        limitDays: Int = HydrationExportContract.DAYS_LIMIT,
    ): List<ExportRow> {
        if (limitDays <= 0) return emptyList()
        val oldestAllowed = today.epochDay - (limitDays - 1)
        val previous = history
            .asSequence()
            .filter { it.epochDay < today.epochDay && it.epochDay >= oldestAllowed && it.ml > 0 }
            .distinctBy { it.epochDay }
            .sortedByDescending { it.epochDay }
            .map { ExportRow(formatDate(it.epochDay), it.ml, it.goal, 0, 0L) }
            .toList()
        return listOf(todayRow(today)) + previous
    }

    /** Extras broadcastu o zmianie — klucze jak w [HydrationExportContract]. */
    fun changedExtras(today: ExportToday): Map<String, Any> = mapOf(
        HydrationExportContract.EXTRA_DATE to formatDate(today.epochDay),
        HydrationExportContract.EXTRA_ML to today.ml,
        HydrationExportContract.EXTRA_GOAL to today.goal,
    )
}

/** Jedno miejsce, z którego repozytorium ogłasza zmianę dzisiejszego nawodnienia. */
object HydrationExport {
    private const val TAG = "KropiExport"

    fun changedIntent(today: ExportToday): Intent {
        val extras = HydrationExportRows.changedExtras(today)
        return Intent(HydrationExportContract.ACTION_HYDRATION_CHANGED)
            .setPackage(HydrationExportContract.TARGET_PACKAGE)
            .putExtra(HydrationExportContract.EXTRA_DATE, extras[HydrationExportContract.EXTRA_DATE] as String)
            .putExtra(HydrationExportContract.EXTRA_ML, extras[HydrationExportContract.EXTRA_ML] as Int)
            .putExtra(HydrationExportContract.EXTRA_GOAL, extras[HydrationExportContract.EXTRA_GOAL] as Int)
    }

    /** Nigdy nie rzuca — brak Szpili czy odmowa systemu nie może psuć dolewania wody. */
    fun notifyChanged(context: Context, today: ExportToday) {
        val app = context.applicationContext ?: context
        try {
            app.sendBroadcast(changedIntent(today), HydrationExportContract.PERMISSION_READ)
        } catch (t: Throwable) {
            Log.w(TAG, "Hydration broadcast failed", t)
        }
        try {
            app.contentResolver.notifyChange(HydrationExportContract.TODAY_URI, null)
            app.contentResolver.notifyChange(HydrationExportContract.DAYS_URI, null)
        } catch (t: Throwable) {
            Log.w(TAG, "Hydration notifyChange failed", t)
        }
    }
}
