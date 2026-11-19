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
enum class NotificationTone(
    private val labelPl: String, private val labelEn: String,
    val emoji: String,
    private val hintPl: String, private val hintEn: String,
) {
    CARING("Wspierający", "Supportive", "🤍", "Spokojnie, z self-care", "Calm, with self-care"),
    SNARKY("Zaczepny", "Snarky", "😈", "Z docinkami i bez owijania", "With jabs, no sugar-coating");

    val label: String get() = tr(labelPl, labelEn)
    val hint: String get() = tr(hintPl, hintEn)
}

/** Tytuły powiadomień zależne od tego, jak daleko jesteś od celu. */
private val SNARK_TITLES_PL: Map<Level, List<String>> = mapOf(
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
private val SNARK_TITLES_BEHIND_PL: List<String> = listOf(
    "💧 Do dupy ten wynik",
    "💧 Lecisz na oparach",
    "💧 Kropi traci cierpliwość",
    "💧 To ma być nawodnienie?",
    "💧 Serio, tak trudno?",
)

private val SNARK_TITLES_AFTER_HOURS_PL: List<String> = listOf(
    "💧 Dzień się kończy, plan nie",
    "💧 Zdążyłeś? Jasne, że nie",
    "💧 Podsumujmy tę klęskę",
)

/** Docinek dopasowany do poziomu nawodnienia — odpowiednik self-care. */
private val SNARK_JABS_PL: Map<Level, List<String>> = mapOf(
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
private val SNARK_GAP_JABS_PL: List<String> = listOf(
    "Cztery godziny bez wody. Twoje nerki wypełniły właśnie wniosek o urlop bezpłatny.",
    "Trzy godziny. Kran jest dwa metry stąd, a nie na Marsie.",
    "Dwie godziny sucho. Ratujesz świat czy po prostu zapomniałeś?",
    "Ostatni łyk był tak dawno, że pewnie zdążył wyparować.",
    "Tyle czasu bez wody, że mógłbyś już mieć nowy dowód osobisty.",
)

/** Zamknięcie powiadomienia — krótkie kopnięcie do działania. */
private val SNARK_CLOSERS_PL: List<String> = listOf(
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
enum class SnarkIntensity(
    private val labelPl: String, private val labelEn: String,
    val emoji: String,
    private val hintPl: String, private val hintEn: String,
) {
    MILD("Delikatnie", "Gentle", "🙂", "Przytyk, ale bez ostrych słów", "A dig, but no harsh words"),
    NORMAL("Normalnie", "Normal", "😏", "Dogryza i nie owija w bawełnę", "Teases and doesn't sugar-coat it"),
    SAVAGE("Ostro", "Savage", "🔥", "Bez taryfy ulgowej i bez cenzury", "No mercy and no censorship");

    val label: String get() = tr(labelPl, labelEn)
    val hint: String get() = tr(hintPl, hintEn)
}

/** Łagodniejszy rejestr: zaczepka zamiast obelgi. */
private val SNARK_JABS_MILD_PL: List<String> = listOf(
    "Wiem, że dasz radę. Po prostu na razie tego nie robisz.",
    "Butelka stoi obok. Sama się nie wypije, sprawdzałem.",
    "Twoje nerki przesyłają delikatne, ale stanowcze pozdrowienia.",
    "Kawa to nie woda — wiesz o tym równie dobrze jak ja.",
    "Nie musisz nadrabiać wszystkiego naraz. Ale zacząć by wypadało.",
    "Jeszcze chwila i zacznę marudzić na poważnie.",
    "Roślinki podlewasz regularniej niż siebie. Trochę niesprawiedliwie.",
)

/** Bez taryfy ulgowej — świadomy wybór użytkownika w Ustawieniach. */
private val SNARK_JABS_SAVAGE_PL: List<String> = listOf(
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
private val SNARK_TITLES_SAVAGE_PL: List<String> = listOf(
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
private val SNARK_MASCOT_LINES_PL: Map<Level, List<String>> = mapOf(
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

// ───────────────────────── English twins ─────────────────────────

private val SNARK_TITLES_EN: Map<Level, List<String>> = mapOf(
    Level.LOW to listOf(
        "💧 Seriously? That little water?",
        "💧 Your kidneys are filing a complaint",
        "💧 A cactus drinks more",
        "💧 Hello, anyone in there?",
        "💧 Well, that's a flop",
        "💧 Drought. Record-breaking",
        "💧 Turning yourself into a prune",
    ),
    Level.MID to listOf(
        "💧 Not bad. Nothing special, but not bad",
        "💧 Half. Like your ambitions",
        "💧 Acceptable. Barely",
        "💧 A draw with yourself",
        "💧 Okay, you do drink something",
    ),
    Level.HIGH to listOf(
        "💧 Oh, so you can",
        "💧 Almost like a grown-up",
        "💧 Close. Don't ruin it",
        "💧 You surprise me",
    ),
    Level.DONE to listOf(
        "💧 Well, well, a miracle",
        "💧 Fine, I'll let you off this time",
        "💧 Done. Don't spoil me",
    ),
)

private val SNARK_TITLES_BEHIND_EN: List<String> = listOf(
    "💧 This score is rubbish",
    "💧 Running on fumes",
    "💧 Kropi is losing patience",
    "💧 This is supposed to be hydration?",
    "💧 Seriously, is it that hard?",
)

private val SNARK_TITLES_AFTER_HOURS_EN: List<String> = listOf(
    "💧 The day is ending, the plan isn't",
    "💧 Made it? Of course you didn't",
    "💧 Let's tally up this disaster",
)

private val SNARK_JABS_EN: Map<Level, List<String>> = mapOf(
    Level.LOW to listOf(
        "The plants in your flat have a better drinking schedule. And you water them once a month.",
        "Your blood is slowly reaching the consistency of jam. Congratulations.",
        "A camel would be embarrassed. Well — it would manage. You wouldn't.",
        "Water comes out of the tap for free and you still choose suffering.",
        "How many times do I have to write this: coffee. does. not. count.",
        "Your body is 60% water. Today mostly excuses.",
        "Even instant noodles get more water than you.",
        "At school that score would get an F and a call to your parents.",
        "You're not busy. You just don't drink.",
    ),
    Level.MID to listOf(
        "Half isn't a success, it's a draw with yourself.",
        "“Not bad” never made it into the history books.",
        "A bit more and I'll have to praise you. Don't make me.",
        "Average. Like your New Year's resolutions.",
        "You're getting there. Slowly. Very slowly.",
        "You're halfway to nowhere. Finish it.",
    ),
    Level.HIGH to listOf(
        "Alright, alright. Just don't make it your whole personality.",
        "Close. Don't ruin it now, like you always do.",
        "See? It can be done. And without crying.",
        "A few more sips and I'll stop nagging. Today only.",
    ),
    Level.DONE to listOf(
        "Done. Shock, disbelief, applause.",
        "Goal delivered. Tomorrow will probably be a drought again, but today — okay.",
        "Hydrated and happy. Don't get attached, we start from zero tomorrow.",
    ),
)

private val SNARK_GAP_JABS_EN: List<String> = listOf(
    "Four hours without water. Your kidneys just filed for unpaid leave.",
    "Three hours. The tap is two metres away, not on Mars.",
    "Two hours dry. Saving the world, or did you simply forget?",
    "Your last sip was so long ago it has probably evaporated.",
    "So long without water you could have renewed your passport by now.",
)

private val SNARK_CLOSERS_EN: List<String> = listOf(
    "Get up and drink. I won't repeat myself.",
    "Bottle. Mouth. Tilt. It's not complicated.",
    "Do it before I start whining again.",
    "Go. Now. I'll check in a minute.",
    "And no, tea with three sugars isn't the same thing.",
    "You don't have to like me. You have to drink.",
)

private val SNARK_JABS_MILD_EN: List<String> = listOf(
    "I know you can do it. You're just not doing it yet.",
    "The bottle is right there. It won't drink itself, I checked.",
    "Your kidneys send gentle but firm regards.",
    "Coffee isn't water — you know that as well as I do.",
    "You don't have to catch up on everything at once. But starting would be nice.",
    "Any minute now and I'll start whining for real.",
    "You water your plants more regularly than yourself. A bit unfair.",
)

private val SNARK_JABS_SAVAGE_EN: List<String> = listOf(
    "Damn it, this is water, not a quantum physics exam. Pour it and drink it.",
    "Your body is sending an SOS and you decide it can wait. Brilliant.",
    "If laziness hydrated, you'd be an ocean. Sadly it doesn't.",
    "You're seriously going to lose to a task a two-year-old can do?",
    "Dry as pepper in a grinder. And just as interesting to your own kidneys.",
    "You have one body and you treat it like a rental car before returning it.",
    "Drink, for crying out loud, or I'll start sending this every five minutes.",
    "This score is pathetic and we both know it.",
)

private val SNARK_TITLES_SAVAGE_EN: List<String> = listOf(
    "💧 Pathetic, not hydration",
    "💧 Seriously, how long can this go on?",
    "💧 Your kidneys have had enough",
    "💧 Kropi isn't asking anymore",
    "💧 Pull yourself together",
)

private val SNARK_MASCOT_LINES_EN: Map<Level, List<String>> = mapOf(
    Level.LOW to listOf(
        "I look at this counter and feel like evaporating.",
        "I'm a drop of water. You're a drop of disappointment.",
        "Zero progress. But at least you're consistent.",
    ),
    Level.MID to listOf(
        "Half. I could be proud if I didn't know your afternoons.",
        "You're doing fine. Don't ruin it with a nap instead of drinking.",
        "Alright, alright. Don't get cocky with this success yet.",
    ),
    Level.HIGH to listOf(
        "Almost. And without me shouting — progress.",
        "See? It can be done. Shame it took ten reminders.",
        "A few more sips and I'll stop tracking you. For today.",
    ),
    Level.DONE to listOf(
        "Done. Don't get me used to good news.",
        "Goal closed. Tomorrow I'll probably have to be mean again.",
        "Bravo. I said it once, don't expect a repeat.",
    ),
)

val SNARK_TITLES: Map<Level, List<String>> get() = if (Lang.isEnglish) SNARK_TITLES_EN else SNARK_TITLES_PL
val SNARK_TITLES_BEHIND: List<String> get() = if (Lang.isEnglish) SNARK_TITLES_BEHIND_EN else SNARK_TITLES_BEHIND_PL
val SNARK_TITLES_AFTER_HOURS: List<String> get() = if (Lang.isEnglish) SNARK_TITLES_AFTER_HOURS_EN else SNARK_TITLES_AFTER_HOURS_PL
val SNARK_JABS: Map<Level, List<String>> get() = if (Lang.isEnglish) SNARK_JABS_EN else SNARK_JABS_PL
val SNARK_GAP_JABS: List<String> get() = if (Lang.isEnglish) SNARK_GAP_JABS_EN else SNARK_GAP_JABS_PL
val SNARK_CLOSERS: List<String> get() = if (Lang.isEnglish) SNARK_CLOSERS_EN else SNARK_CLOSERS_PL
val SNARK_JABS_MILD: List<String> get() = if (Lang.isEnglish) SNARK_JABS_MILD_EN else SNARK_JABS_MILD_PL
val SNARK_JABS_SAVAGE: List<String> get() = if (Lang.isEnglish) SNARK_JABS_SAVAGE_EN else SNARK_JABS_SAVAGE_PL
val SNARK_TITLES_SAVAGE: List<String> get() = if (Lang.isEnglish) SNARK_TITLES_SAVAGE_EN else SNARK_TITLES_SAVAGE_PL
val SNARK_MASCOT_LINES: Map<Level, List<String>> get() = if (Lang.isEnglish) SNARK_MASCOT_LINES_EN else SNARK_MASCOT_LINES_PL
