package com.kropi.hydration.data

import java.net.URI
import java.net.URLDecoder

/**
 * Zawartość tagu NFC „dolej to, co w tym kubku": zwykły adres `kropi://add?ml=250&drink=COFFEE`.
 * Ten sam adres obsługują skróty spod ikony ([com.kropi.hydration.ui.QuickAddActivity]), więc tag
 * nie potrzebuje niczego poza adresem — i da się go zapisać dowolną aplikacją do tagów.
 *
 * Inne aplikacje (np. Drink Tracker) mogą dodać `at` (czas wypicia, ms od epoki) i `note` (notatka),
 * żeby wpis trafił na właściwy dzień i godzinę: `kropi://add?ml=500&drink=ALCOHOL&at=…&note=Piwo`.
 */
data class QuickAdd(
    val ml: Int?,
    val drink: Drink?,
    /** Kiedy napój wypito (ms od epoki) – dla wpisów wstecznych, np. z Drink Trackera. `null` = teraz. */
    val atMs: Long? = null,
    /** Notatka do wpisu, np. nazwa trunku („Piwo”). */
    val note: String? = null,
) {
    companion object {
        const val MIN_ML = 10
        const val MAX_ML = 1500
        const val MAX_NOTE = 60

        /** Wpis wsteczny najwyżej sprzed tylu dni; starszy czas jest ignorowany (zapis „teraz”). */
        const val MAX_PAST_DAYS = 31L

        /** Tolerancja na rozjechane zegary: czas „z przyszłości” do tylu ms jest jeszcze przyjmowany. */
        const val MAX_FUTURE_MS = 5 * 60_000L
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
     * a nieznany napój to brak napoju (woda). Czas `at` spoza okna [QuickAdd.MAX_PAST_DAYS] wstecz
     * … [QuickAdd.MAX_FUTURE_MS] naprzód jest pomijany, a notatka przycinana do [QuickAdd.MAX_NOTE] znaków.
     */
    fun parse(raw: String?, nowMs: Long = System.currentTimeMillis()): QuickAdd? {
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
        val atMs = query["at"]?.toLongOrNull()?.takeIf {
            it in (nowMs - QuickAdd.MAX_PAST_DAYS * 24 * 3_600_000L)..(nowMs + QuickAdd.MAX_FUTURE_MS)
        }
        val note = query["note"]
            ?.filterNot { it.isISOControl() }
            ?.trim()
            ?.take(QuickAdd.MAX_NOTE)
            ?.takeIf { it.isNotEmpty() }
        return QuickAdd(ml, drink, atMs, note)
    }

    /** Czy to powtórne przyłożenie tego samego tagu zaraz po poprzednim. */
    fun isRepeat(lastUri: String?, lastAtMs: Long, uri: String, nowMs: Long): Boolean =
        lastUri == uri && nowMs - lastAtMs in 0 until DEBOUNCE_MS
}
