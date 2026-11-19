package com.kropi.hydration.weather

import android.content.Context
import android.net.Uri
import com.kropi.hydration.data.OutdoorReading

/**
 * Czyta temperaturę z Homebrew Weather przez jego provider (tylko liczby, bez lokalizacji).
 * Wersja z Play/GitHuba ma authority `dev.pi0trdotsys.homebrewweather.export`, build debug
 * `…debug.export`; próbujemy obu, wydanie pierwsze. Brak aplikacji, brak widgetu z prognozą albo błąd
 * oznaczają po prostu null — wtedy zostaje ręczny wybór temperatury.
 */
object OutdoorWeatherSource {
    val AUTHORITIES = listOf(
        "dev.pi0trdotsys.homebrewweather.export",
        "dev.pi0trdotsys.homebrewweather.debug.export",
    )

    fun read(context: Context, nowMs: Long = System.currentTimeMillis()): OutdoorReading? {
        for (authority in AUTHORITIES) {
            val reading = runCatching { query(context, authority, nowMs) }.getOrNull()
            if (reading != null) return reading
        }
        return null
    }

    /** Czy któryś provider w ogóle istnieje (do podpisu w Ustawieniach: „nie znaleziono" vs „brak prognozy"). */
    fun isInstalled(context: Context): Boolean = AUTHORITIES.any { context.packageManager.resolveContentProvider(it, 0) != null }

    private fun query(context: Context, authority: String, nowMs: Long): OutdoorReading? {
        val uri = Uri.parse("content://$authority/outdoor")
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (!c.moveToFirst()) return null
            val now = c.getColumnIndex("temp_c").takeIf { it >= 0 && !c.isNull(it) }?.let { c.getDouble(it) }
            val max = c.getColumnIndex("max_c").takeIf { it >= 0 && !c.isNull(it) }?.let { c.getDouble(it) }
            val min = c.getColumnIndex("min_c").takeIf { it >= 0 && !c.isNull(it) }?.let { c.getDouble(it) }
            val updated = c.getColumnIndex("updated_at").takeIf { it >= 0 && !c.isNull(it) }?.let { c.getLong(it) } ?: 0L
            return OutdoorReading.sanitized(now, max, min, updated, nowMs)
        }
        return null
    }
}
