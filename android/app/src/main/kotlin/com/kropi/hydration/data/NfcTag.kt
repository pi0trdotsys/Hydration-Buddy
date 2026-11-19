package com.kropi.hydration.data

import java.net.URI
import java.net.URLDecoder

/**
 * Zawartość tagu NFC „dolej to, co w tym kubku": zwykły adres `kropi://add?ml=250&drink=COFFEE`.
 * Ten sam adres obsługują skróty spod ikony ([com.kropi.hydration.ui.QuickAddActivity]), więc tag
 * nie potrzebuje niczego poza adresem — i da się go zapisać dowolną aplikacją do tagów.
 */
data class QuickAdd(val ml: Int?, val drink: Drink?) {
    companion object {
        const val MIN_ML = 10
        const val MAX_ML = 1500
    }
}

object NfcTag {
    const val SCHEME = "kropi"
    const val HOST = "add"

    /** Dwa przyłożenia tego samego tagu w tym czasie liczymy jako jedno. */
    const val DEBOUNCE_MS = 6_000L

    fun uri(ml: Int, drink: Drink): String =
        "$SCHEME://$HOST?ml=${ml.coerceIn(QuickAdd.MIN_ML, QuickAdd.MAX_ML)}&drink=${drink.name}"

    /**
     * Rozbiera adres z tagu albo skrótu. Akceptuje `kropi://add/500` (stare skróty), `kropi://add?ml=…`
     * i parametr `drink`. Wartości z tagu są nieufne: ilość przycinamy do [QuickAdd.MIN_ML]…[QuickAdd.MAX_ML],
     * a nieznany napój to brak napoju (woda).
     */
    fun parse(raw: String?): QuickAdd? {
        if (raw.isNullOrBlank()) return null
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        if (uri.scheme != SCHEME || uri.host != HOST) return null
        val query = uri.rawQuery.orEmpty().split("&").mapNotNull { pair ->
            val i = pair.indexOf('=')
            if (i <= 0) null else URLDecoder.decode(pair.substring(0, i), "UTF-8") to URLDecoder.decode(pair.substring(i + 1), "UTF-8")
        }.toMap()
        val ml = (query["ml"] ?: uri.path.orEmpty().trim('/').takeIf { it.isNotEmpty() })
            ?.toIntOrNull()?.coerceIn(QuickAdd.MIN_ML, QuickAdd.MAX_ML)
        val drink = query["drink"]?.let { name -> Drink.entries.firstOrNull { it.name == name.uppercase() } }
        return QuickAdd(ml, drink)
    }

    /** Czy to powtórne przyłożenie tego samego tagu zaraz po poprzednim. */
    fun isRepeat(lastUri: String?, lastAtMs: Long, uri: String, nowMs: Long): Boolean =
        lastUri == uri && nowMs - lastAtMs in 0 until DEBOUNCE_MS
}
