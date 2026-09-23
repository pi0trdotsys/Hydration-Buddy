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

/** Jak mocno Kropi ma przyciskać. Widoczne tylko przy tonie zaczepnym. */
enum class SnarkIntensity(val label: String, val emoji: String, val hint: String) {
    MILD("Delikatnie", "🙂", "Przytyk, ale bez ostrych słów"),
    NORMAL("Normalnie", "😏", "Dogryza i nie owija w bawełnę"),
    SAVAGE("Bezlitośnie", "🔥", "Bez taryfy ulgowej i bez cenzury"),
}

/** Łagodniejszy rejestr: zaczepka zamiast obelgi. */
val SNARK_JABS_MILD: List<String> = listOf(
    "Wiem, że dasz radę. Po prostu na razie tego nie robisz.",
    "Butelka stoi obok. Sama się nie wypije, sprawdzałem.",
    "Twoje nerki przesyłają delikatne, ale stanowcze pozdrowienia.",
    "Kawa to nie woda — wiesz o tym równie dobrze jak ja.",
    "Nie musisz nadrabiać wszystkiego naraz. Ale zacząć by wypadało.",
    "Jeszcze chwila i zacznę marudzić na poważnie.",
    "Roślinki podlewasz regularniej niż siebie. Trochę niesprawiedliwie.",
)

/** Bez taryfy ulgowej — świadomy wybór użytkownika w Ustawieniach. */
val SNARK_JABS_SAVAGE: List<String> = listOf(
    "Kurwa, to jest woda, a nie egzamin z fizyki kwantowej. Nalej i wypij.",
    "Twoje ciało wysyła sygnał SOS, a ty stwierdzasz, że później. Genialnie.",
    "Gdyby lenistwo nawadniało, byłbyś oceanem. Niestety nie nawadnia.",
    "Serio zamierzasz przegrać z zadaniem, które umie wykonać dwulatek?",
    "Suchy jak pieprz w młynku. I równie interesujący dla własnych nerek.",
    "Masz jedno ciało i traktujesz je jak wypożyczone auto przed oddaniem.",
    "Pij, do cholery, bo zaraz zacznę wysyłać to samo co pięć minut.",
    "Ten wynik to żenada i oboje o tym wiemy.",
)

/** Ostrzejsze tytuły dla trybu bezlitosnego. */
val SNARK_TITLES_SAVAGE: List<String> = listOf(
    "💧 Żenada, nie nawodnienie",
    "💧 Serio, ile można?",
    "💧 Twoje nerki mają dość",
    "💧 Kropi już nie prosi",
    "💧 Weź się w garść",
)

/**
 * Docinek dobrany do poziomu nawodnienia i ustawionej ostrości. Tryb łagodny
 * i bezlitosny mają własne, płaskie zestawy — poziom nawodnienia dobiera już
 * tytuł, więc rozbijanie ich jeszcze na cztery warianty niczego by nie wniosło.
 */
fun snarkJab(level: Level, intensity: SnarkIntensity, seed: Int): String = when (intensity) {
    SnarkIntensity.MILD -> pick(SNARK_JABS_MILD, seed)
    SnarkIntensity.NORMAL -> pick(SNARK_JABS.getValue(level), seed)
    SnarkIntensity.SAVAGE -> pick(SNARK_JABS_SAVAGE, seed)
}

/** Kwestie maskotki w aplikacji, gdy Kropi jest w trybie zaczepnym. */
val SNARK_MASCOT_LINES: Map<Level, List<String>> = mapOf(
    Level.LOW to listOf(
        "Patrzę na ten licznik i mam ochotę wyparować.",
        "Jestem kroplą wody. Ty jesteś kroplą rozczarowania.",
        "Zero postępu. Ale przynajmniej konsekwentnie.",
    ),
    Level.MID to listOf(
        "Połowa. Mógłbym być dumny, gdybym się nie znał na twoich popołudniach.",
        "Idzie ci. Nie psuj tego drzemką zamiast picia.",
        "Dobrze, dobrze. Nie rozpychaj się jeszcze tym sukcesem.",
    ),
    Level.HIGH to listOf(
        "Prawie. I to bez mojego krzyku — postęp.",
        "Widzisz? Da się. Szkoda, że dopiero po dziesięciu przypomnieniach.",
        "Jeszcze parę łyków i przestanę cię śledzić. Na dziś.",
    ),
    Level.DONE to listOf(
        "Zrobione. Nie przyzwyczajaj mnie do dobrych wiadomości.",
        "Cel zamknięty. Jutro pewnie znowu będę musiał być wredny.",
        "Brawo. Powiedziałem to raz, nie licz na powtórkę.",
    ),
)
