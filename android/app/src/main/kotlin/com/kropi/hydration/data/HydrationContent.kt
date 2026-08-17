package com.kropi.hydration.data

/** Ported 1:1 from the TypeScript mockup at src/data/hydration-content.ts. */

enum class Level { LOW, MID, HIGH, DONE }
enum class Daypart { MORNING, DAY, EVENING }

data class Bottle(val ml: Int, val label: String)

val BOTTLES = listOf(
    Bottle(100, "Łyk"),
    Bottle(250, "Szklanka"),
    Bottle(330, "Puszka"),
    Bottle(500, "Butelka"),
    Bottle(750, "Bidon"),
)

val FACTS: List<String> = listOf(
    "Mózg w ok. 73% składa się z wody — odwodnienie na poziomie 2% mierzalnie obniża koncentrację.",
    "Uczucie zmęczenia po południu to bardzo często odwodnienie rzędu 1–2%, a nie brak kofeiny.",
    "Pragnienie pojawia się dopiero, gdy stracisz ok. 1–2% masy ciała w wodzie — to już spóźniony sygnał.",
    "Nerki filtrują dziennie ok. 180 litrów płynu, żeby wyprodukować 1,5–2 litry moczu.",
    "Kawa nie odwadnia tak, jak głosi mit — do ok. 4 filiżanek dziennie bilans płynów pozostaje dodatni.",
    "Ok. 20–30% dziennego nawodnienia pochodzi z jedzenia: ogórek ma 96% wody, arbuz 92%.",
    "Szklanka wody zaraz po przebudzeniu uzupełnia to, co straciłeś przez oddech i pot w nocy (nawet 500 ml).",
    "Jasnożółty kolor moczu to dobry znak. Ciemny bursztyn oznacza, że czas na wodę.",
    "Podczas godziny intensywnego treningu można stracić 0,5–2 litry potu.",
    "Woda jest głównym składnikiem mazi stawowej — nawodnienie realnie wpływa na komfort stawów.",
    "Odwodnienie zagęszcza krew, więc serce musi pracować ciężej, by dostarczyć tlen.",
    "Bóle głowy są jednym z pierwszych i najczęstszych objawów niedoboru płynów.",
    "Skóra odwodniona traci elastyczność — test: uszczypnij grzbiet dłoni, powinna wrócić natychmiast.",
    "Elektrolity (sód, potas, magnez) decydują o tym, czy woda faktycznie trafi do komórek.",
    "Przy wysiłku powyżej 60–90 minut warto dodać do wody szczyptę soli i odrobinę węglowodanów.",
    "Zapotrzebowanie rośnie o ok. 500–750 ml na każdą godzinę aktywności fizycznej.",
    "W samolocie wilgotność spada poniżej 20% — na każdą godzinę lotu dolicz sobie szklankę wody.",
    "Zimą pijemy mniej, choć suche, ogrzewane powietrze zwiększa utratę wody przez oddech.",
    "Woda pomaga regulować temperaturę ciała — pot to najskuteczniejszy chłodziarka, jaką masz.",
    "Metabolizm tłuszczu (lipoliza) wymaga wody; odwodnienie realnie go spowalnia.",
    "Nawet lekkie odwodnienie pogarsza nastrój i zwiększa odczuwanie wysiłku podczas treningu.",
    "Picie wody przed posiłkiem zwiększa uczucie sytości i pomaga jeść wolniej.",
    "Woda gazowana nawadnia tak samo dobrze jak niegazowana — liczy się to, ile jej wypijesz.",
    "Zbyt duża ilość wody naraz (ponad ~1 l/h) może rozcieńczyć sód we krwi — pij regularnie, nie hurtowo.",
    "Herbata ziołowa liczy się do dziennego bilansu płynów.",
    "Alkohol hamuje wazopresynę, przez co tracisz więcej płynów niż wypijasz.",
    "Dzieci i osoby starsze odczuwają pragnienie słabiej — u nich rutyna picia jest ważniejsza niż sygnały ciała.",
    "Ok. 60% dorosłego ciała to woda; u noworodka nawet 75%.",
    "Woda bierze udział w transporcie składników odżywczych do każdej pojedynczej komórki.",
    "Regularne nawodnienie zmniejsza ryzyko kamieni nerkowych nawet o kilkadziesiąt procent.",
    "Suchość w ustach to już wyraźny sygnał alarmowy, nie subtelna wskazówka.",
    "Trzymanie butelki w zasięgu wzroku zwiększa dzienne spożycie wody bardziej niż jakakolwiek aplikacja.",
    "Chłodna woda (15–20°C) wchłania się nieco szybciej i jest chętniej pita podczas wysiłku.",
    "Sen skraca się i płycieje przy odwodnieniu — ale litr przed snem to z kolei nocne wybudzenia.",
    "Optymalnie: ostatnia większa porcja wody 1,5–2 godziny przed snem.",
    "Płyny wchłaniają się głównie w jelicie cienkim, nie w żołądku.",
    "Woda z cytryną nie „oczyszcza organizmu”, ale często sprawia, że pijesz jej po prostu więcej.",
    "Zasada kciuka: ok. 30 ml wody na kilogram masy ciała dziennie, plus wysiłek i upał.",
    "Odwodnienie zwiększa lepkość śliny — stąd nieświeży oddech po długiej przerwie w piciu.",
    "Utrata 4% wody z organizmu potrafi obniżyć wydolność fizyczną o kilkanaście procent.",
    "Mięśnie to ok. 76% wody — dobre nawodnienie zmniejsza ryzyko skurczów.",
    "Twoje ciało nie magazynuje wody na zapas — dlatego liczy się rytm, nie jednorazowy rekord.",
)

val SELF_CARE: Map<Level, List<String>> = mapOf(
    Level.LOW to listOf(
        "Zrób przerwę, rozprostuj się i wypij szklankę — nie musisz nadrabiać wszystkiego naraz.",
        "Nie oceniaj się za wolny start. Jedna szklanka teraz zmienia cały wykres dnia.",
        "Postaw butelkę tam, gdzie ją widzisz. Twoje ciało poprosi, zanim zdążysz zapomnieć.",
        "Zanim otworzysz kolejną kartę w przeglądarce — otwórz butelkę.",
        "Głowa ciężka? Zacznij od wody, dopiero potem od kawy.",
        "Mały cel na najbliższe 30 minut: 250 ml. Tyle wystarczy.",
        "Odpoczynek to też nawodnienie. Usiądź, oddychaj, pij powoli.",
    ),
    Level.MID to listOf(
        "Dobre tempo. Trzymaj rytm co godzinę, a wieczór będzie spokojniejszy.",
        "Jesteś w połowie drogi — to najlepszy moment, żeby nie odpuścić.",
        "Wstań, przejdź się do kuchni, nalej. Ruch plus woda to najtańszy reset.",
        "Twoje ciało już czuje różnicę, nawet jeśli głowa jeszcze nie zauważyła.",
        "Nie musisz być idealny. Musisz być regularny.",
        "Konsekwencja bije intensywność. Kolejna szklanka i lecimy dalej.",
        "Sprawdź ramiona — pewnie są napięte. Rozluźnij i napij się.",
    ),
    Level.HIGH to listOf(
        "Blisko celu. Ostatnie łyki są najłatwiejsze — po prostu je zrób.",
        "Świetna robota. Twoje nerki dziś ci dziękują.",
        "Zostało niewiele. Dopij spokojnie, bez pośpiechu.",
        "Tak wygląda dzień, w którym zadbałeś o siebie bez wielkich gestów.",
        "Prawie meta. Zrób głęboki wdech i dokończ.",
        "Dobrze zaopiekowany organizm lepiej śpi. Jesteś na dobrej drodze.",
    ),
    Level.DONE to listOf(
        "Cel osiągnięty. Nie musisz nic więcej udowadniać — pij już tylko z pragnienia.",
        "Zrobione. To jest właśnie ta mała rzecz, która robi dużą różnicę.",
        "100%. Zapamiętaj to uczucie — jutro będzie łatwiej je powtórzyć.",
        "Pełne nawodnienie. Teraz zadbaj też o sen.",
        "Cel dowieziony. Reszta dnia jest bonusem.",
    ),
)

val DAYPART_NOTES: Map<Daypart, List<String>> = mapOf(
    Daypart.MORNING to listOf(
        "Poranek to najtańsza wygrana dnia — szklanka wody przed pierwszą kawą.",
        "Po nocy jesteś naturalnie odwodniony. Zacznij od 300–500 ml.",
        "Woda przed śniadaniem rozkręca metabolizm łagodniej niż kofeina.",
    ),
    Daypart.DAY to listOf(
        "Środek dnia: ustaw sobie rytm co godzinę zamiast jednego dużego łyka.",
        "Spadek energii po lunchu? Najpierw woda, potem wnioski.",
        "Każde spotkanie to okazja, żeby dolać do butelki.",
    ),
    Daypart.EVENING to listOf(
        "Wieczorem zwolnij tempo — mniejsze porcje, żeby nie budzić się w nocy.",
        "Ostatnia większa porcja najlepiej 1,5–2 h przed snem.",
        "Podsumuj dzień spokojnie. Nawodnienie to maraton, nie sprint.",
    ),
)

val MASCOT_LINES: Map<Level, List<String>> = mapOf(
    Level.LOW to listOf(
        "Jestem trochę... skoncentrowana. Za bardzo.",
        "Halo? Tu kropla. Sucho tu.",
        "Jeden łyk i wracam do formy!",
        "Mam plan: woda. Prosty plan.",
    ),
    Level.MID to listOf(
        "O, robi się mokro! Lubię to.",
        "Połowa za nami, bulg bulg.",
        "Płynie nam nieźle.",
        "Czuję falę optymizmu.",
    ),
    Level.HIGH to listOf(
        "Jeszcze chwila i pływam!",
        "Prawie pełen bąbelek!",
        "Nie zatrzymuj się, jest pięknie.",
        "Widzę metę, jest niebieska.",
    ),
    Level.DONE to listOf(
        "PLUSK! Cel zaliczony!",
        "Jestem w pełni sobą. Czyli wodą.",
        "Bulgot zwycięstwa!",
        "Dziś jesteś oceanem.",
    ),
)

data class WeekDay(val day: String, val pct: Float)

/** Ported from WEEK in src/hooks/use-hydration-mock.ts (static mock days Mon–Sat; today is appended live). */
val WEEK = listOf(
    WeekDay("Pn", 0.82f),
    WeekDay("Wt", 1f),
    WeekDay("Śr", 0.64f),
    WeekDay("Cz", 0.95f),
    WeekDay("Pt", 1f),
    WeekDay("So", 0.48f),
)

data class HistoryDay(
    val day: String,
    val date: String,
    val ml: Int,
    val goal: Int,
    val pct: Float,
    val reached: Boolean,
)

/** Ported from HISTORY in src/hooks/use-hydration-mock.ts (today is appended live with real data). */
val HISTORY = listOf(
    HistoryDay("Pn", "11.08", 2050, 2500, 0.82f, false),
    HistoryDay("Wt", "12.08", 2500, 2500, 1f, true),
    HistoryDay("Śr", "13.08", 1600, 2500, 0.64f, false),
    HistoryDay("Cz", "14.08", 2375, 2500, 0.95f, false),
    HistoryDay("Pt", "15.08", 2500, 2500, 1f, true),
    HistoryDay("So", "16.08", 1200, 2500, 0.48f, false),
)

fun levelFor(progress: Double): Level = when {
    progress >= 1.0 -> Level.DONE
    progress >= 0.61 -> Level.HIGH
    progress >= 0.26 -> Level.MID
    else -> Level.LOW
}

fun daypartFor(hour: Int): Daypart = when {
    hour < 11 -> Daypart.MORNING
    hour < 18 -> Daypart.DAY
    else -> Daypart.EVENING
}

/** Deterministyczny wybór — stabilny przy re-renderze, tak jak w makiecie. */
fun <T> pick(list: List<T>, seed: Int): T = list[Math.floorMod(seed, list.size)]
