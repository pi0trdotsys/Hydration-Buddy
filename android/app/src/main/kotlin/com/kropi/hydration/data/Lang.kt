package com.kropi.hydration.data

import java.util.Locale

/**
 * Język treści w warstwie danych, która nie ma Contextu (plany, komunikaty maskotki, ciekawostki).
 * Aplikacja jest domyślnie po polsku; gdy system (albo język aplikacji w ustawieniach Androida) to
 * angielski, dostajemy angielskie bliźniaki tych samych tabel. Napisy interfejsu siedzą w zasobach
 * `strings.xml` — tu zostają tylko dłuższe tabele treści.
 */
object Lang {
    val isEnglish: Boolean get() = Locale.getDefault().language == "en"

    /** Locale do nazw dni i miesięcy: polska, chyba że język aplikacji to angielski. */
    val locale: Locale get() = if (isEnglish) Locale.ENGLISH else Locale.forLanguageTag("pl-PL")
}

/** Polski albo angielski wariant krótkiego napisu. */
fun tr(pl: String, en: String): String = if (Lang.isEnglish) en else pl
