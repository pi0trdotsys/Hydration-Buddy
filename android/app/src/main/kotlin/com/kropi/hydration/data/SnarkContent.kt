package com.kropi.hydration.data

/**
 * Drugi charakter Kropi — zaczepny.
 *
 * Domyślna baza ([SELF_CARE], [MASCOT_LINES]) mówi miękko i wspierająco.
 * Tutaj ten sam stan dnia komentuje wersja złośliwa: dogryza, kpi i nie
 * owija w bawełnę. Liczby i plan picia zostają identyczne — zmienia się
 * wyłącznie ton, wybierany przełącznikiem w Ustawieniach
 * ([HydrationSettings.notificationTone]).
 */
enum class NotificationTone(val label: String, val emoji: String, val hint: String) {
    CARING("Wspierający", "🤍", "Spokojnie, z self-care"),
    SNARKY("Zaczepny", "😈", "Z docinkami i bez owijania"),
}

/** Tytuły powiadomień zależne od tego, jak daleko jesteś od celu. */
val SNARK_TITLES: Map<Level, List<String>> = mapOf(
    Level.LOW to listOf(
        "💧 Serio? Tyle wody?",
        "💧 Twoje nerki składają skargę",
        "💧 Kaktus pije więcej",
        "💧 Halo, jest tam ktoś?",
        "💧 No i klops",
        "💧 Susza. Rekordowa",
        "💧 Robisz z siebie suszoną śliwkę",
    ),
    Level.MID to listOf(
        "💧 Nieźle. Bez szału, ale nieźle",
        "💧 Połowa. Jak twoje ambicje",
        "💧 Może być. Ledwo",
        "💧 Remis z samym sobą",
        "💧 Dobra, coś tam pijesz",
    ),
    Level.HIGH to listOf(
        "💧 O, jednak umiesz",
        "💧 Prawie jak dorosły człowiek",
        "💧 Blisko. Nie zepsuj tego",
        "💧 Zaskakujesz mnie",
    ),
    Level.DONE to listOf(
        "💧 No proszę, cud",
        "💧 Dobra, tym razem ci odpuszczę",
        "💧 Zaliczone. Nie przyzwyczajaj mnie",
    ),
)

/** Gdy jesteś wyraźnie pod kreską planu — ostrzejszy rejestr. */
val SNARK_TITLES_BEHIND: List<String> = listOf(
    "💧 Do dupy ten wynik",
    "💧 Lecisz na oparach",
    "💧 Kropi traci cierpliwość",
    "💧 To ma być nawodnienie?",
    "💧 Serio, tak trudno?",
)

val SNARK_TITLES_AFTER_HOURS: List<String> = listOf(
    "💧 Dzień się kończy, plan nie",
    "💧 Zdążyłeś? Jasne, że nie",
    "💧 Podsumujmy tę klęskę",
)

/** Docinek dopasowany do poziomu nawodnienia — odpowiednik self-care. */
val SNARK_JABS: Map<Level, List<String>> = mapOf(
    Level.LOW to listOf(
        "Rośliny w twoim mieszkaniu mają lepszy grafik picia. A ty je podlewasz raz na miesiąc.",
        "Twoja krew powoli osiąga konsystencję dżemu. Gratulacje.",
        "Wielbłąd by się zawstydził. Znaczy — on by dał radę. Ty nie.",
        "Woda leci z kranu za darmo, a ty i tak wybierasz cierpienie.",
        "Ile razy mam to pisać: kawa. się. nie. liczy.",
        "Twój organizm w 60% składa się z wody. Dziś głównie z wymówek.",
        "Nawet zupka chińska dostaje więcej wody niż ty.",
        "Za taki wynik w szkole byłaby jedynka i wezwanie rodziców.",
        "Ty nie jesteś zajęty. Ty po prostu nie pijesz.",
    ),
    Level.MID to listOf(
        "Połowa to nie sukces, to remis z samym sobą.",
        "Hasło „nie jest źle” nie zapisuje się w historii.",
        "Jeszcze trochę i będę musiał cię pochwalić. Nie zmuszaj mnie.",
        "Średnio. Jak twoje postanowienia noworoczne.",
        "Idzie ci. Powoli. Bardzo powoli.",
        "Jesteś w połowie drogi donikąd. Dokończ to.",
    ),
    Level.HIGH to listOf(
        "Dobra, dobra. Tylko nie rób z tego osobowości.",
        "Blisko. Nie zepsuj tego teraz, jak zwykle.",
        "Widzisz? Da się. I to bez płaczu.",
        "Jeszcze parę łyków i przestanę się czepiać. Tylko dziś.",
    ),
    Level.DONE to listOf(
        "Zrobione. Szok, niedowierzanie, brawa.",
        "Cel dowieziony. Jutro pewnie znowu susza, ale dziś — okej.",
        "Nawodniony i zadowolony. Nie przywiązuj się, jutro zaczynamy od zera.",
    ),
)

/** Komentarz do przerwy od ostatniego łyka; dobierany po liczbie minut. */
val SNARK_GAP_JABS: List<String> = listOf(
    "Cztery godziny bez wody. Twoje nerki wypełniły właśnie wniosek o urlop bezpłatny.",
    "Trzy godziny. Kran jest dwa metry stąd, a nie na Marsie.",
    "Dwie godziny sucho. Ratujesz świat czy po prostu zapomniałeś?",
    "Ostatni łyk był tak dawno, że pewnie zdążył wyparować.",
    "Tyle czasu bez wody, że mógłbyś już mieć nowy dowód osobisty.",
)

/** Zamknięcie powiadomienia — krótkie kopnięcie do działania. */
val SNARK_CLOSERS: List<String> = listOf(
    "Wstawaj i pij. Nie będę powtarzał.",
    "Butelka. Usta. Przechył. To nie jest skomplikowane.",
    "Zrób to, zanim znowu zacznę marudzić.",
    "Idź. Teraz. Zaraz sprawdzę.",
    "I nie, herbata z trzema cukrami to nie to samo.",
    "Nie musisz mnie lubić. Musisz się napić.",
)

/**
 * Docinek o przerwie, dobrany do jej długości — im dłużej sucho, tym ostrzej.
 * Zwraca null, gdy przerwa jest jeszcze na tyle krótka, że nie ma się czepiać.
 */
fun snarkGapJab(minutesSinceLast: Long, seed: Int): String? = when {
    minutesSinceLast >= 240 -> SNARK_GAP_JABS[0]
    minutesSinceLast >= 180 -> SNARK_GAP_JABS[1]
    minutesSinceLast >= 120 -> SNARK_GAP_JABS[2]
    minutesSinceLast >= 75 -> pick(SNARK_GAP_JABS.drop(3), seed)
    else -> null
}
