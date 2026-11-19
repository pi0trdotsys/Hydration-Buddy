package com.kropi.hydration.data

/** Ported 1:1 from the TypeScript mockup at src/data/hydration-content.ts. */

enum class Level { LOW, MID, HIGH, DONE }
enum class Daypart { MORNING, DAY, EVENING }

data class Bottle(val ml: Int, val label: String)

val BOTTLES: List<Bottle> get() = listOf(
    Bottle(100, tr("Łyk", "Sip")),
    Bottle(250, tr("Szklanka", "Glass")),
    Bottle(330, tr("Puszka", "Can")),
    Bottle(500, tr("Butelka", "Bottle")),
    Bottle(750, tr("Bidon", "Flask")),
)

private val FACTS_PL: List<String> = listOf(
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

private val SELF_CARE_PL: Map<Level, List<String>> = mapOf(
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

private val DAYPART_NOTES_PL: Map<Daypart, List<String>> = mapOf(
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

private val MASCOT_LINES_PL: Map<Level, List<String>> = mapOf(
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

private val FACTS_EN: List<String> = listOf(
    "The brain is about 73% water — dehydration of just 2% measurably lowers concentration.",
    "Afternoon tiredness is very often 1–2% dehydration, not a lack of caffeine.",
    "Thirst only shows up once you've lost about 1–2% of body weight in water — it's already a late signal.",
    "Your kidneys filter around 180 litres of fluid a day to produce 1.5–2 litres of urine.",
    "Coffee doesn't dehydrate you the way the myth says — up to about 4 cups a day your fluid balance stays positive.",
    "About 20–30% of daily hydration comes from food: cucumber is 96% water, watermelon 92%.",
    "A glass of water right after waking replaces what you lost through breathing and sweat overnight (up to 500 ml).",
    "Pale yellow urine is a good sign. Dark amber means it's time for water.",
    "During an hour of intense training you can lose 0.5–2 litres of sweat.",
    "Water is the main component of synovial fluid — hydration genuinely affects how your joints feel.",
    "Dehydration thickens the blood, so the heart has to work harder to deliver oxygen.",
    "Headaches are one of the first and most common signs of a fluid shortage.",
    "Dehydrated skin loses elasticity — test: pinch the back of your hand, it should snap back at once.",
    "Electrolytes (sodium, potassium, magnesium) decide whether water actually reaches your cells.",
    "For effort longer than 60–90 minutes it's worth adding a pinch of salt and a little carbohydrate to your water.",
    "Your needs rise by roughly 500–750 ml for every hour of physical activity.",
    "On a plane humidity drops below 20% — add a glass of water for every hour of flight.",
    "We drink less in winter, even though dry, heated air increases water loss through breathing.",
    "Water helps regulate body temperature — sweat is the most effective cooling system you have.",
    "Fat metabolism (lipolysis) needs water; dehydration measurably slows it down.",
    "Even mild dehydration worsens mood and makes training feel harder.",
    "Drinking water before a meal increases fullness and helps you eat more slowly.",
    "Sparkling water hydrates just as well as still — what matters is how much you drink.",
    "Too much water at once (over ~1 l/h) can dilute sodium in the blood — drink regularly, not in bulk.",
    "Herbal tea counts toward your daily fluid balance.",
    "Alcohol suppresses vasopressin, so you lose more fluid than you take in.",
    "Children and older people feel thirst less — for them a drinking routine matters more than body signals.",
    "About 60% of an adult body is water; in a newborn it's up to 75%.",
    "Water carries nutrients to every single cell.",
    "Regular hydration lowers the risk of kidney stones by tens of percent.",
    "A dry mouth is already a clear alarm signal, not a subtle hint.",
    "Keeping a bottle in sight raises daily water intake more than any app.",
    "Cool water (15–20°C) absorbs slightly faster and is drunk more willingly during exercise.",
    "Sleep gets shorter and shallower when dehydrated — but a litre before bed means waking up at night.",
    "Ideally: the last larger portion of water 1.5–2 hours before sleep.",
    "Fluids are absorbed mainly in the small intestine, not the stomach.",
    "Lemon water doesn't “cleanse the body”, but it often just makes you drink more.",
    "Rule of thumb: about 30 ml of water per kilogram of body weight a day, plus exercise and heat.",
    "Dehydration makes saliva stickier — hence the stale breath after a long break from drinking.",
    "Losing 4% of the body's water can cut physical performance by well over ten percent.",
    "Muscle is about 76% water — good hydration lowers the risk of cramps.",
    "Your body doesn't store water in reserve — so rhythm matters, not a one-off record.",
)

private val SELF_CARE_EN: Map<Level, List<String>> = mapOf(
    Level.LOW to listOf(
        "Take a break, stretch and have a glass — you don't have to catch up on everything at once.",
        "Don't judge yourself for a slow start. One glass now changes the whole chart of the day.",
        "Put the bottle where you can see it. Your body will ask before you can forget.",
        "Before you open another browser tab — open the bottle.",
        "Heavy head? Start with water, coffee comes after.",
        "A small goal for the next 30 minutes: 250 ml. That's enough.",
        "Rest counts as hydration too. Sit down, breathe, sip slowly.",
    ),
    Level.MID to listOf(
        "Good pace. Keep the hourly rhythm and the evening will be calmer.",
        "You're halfway — the best moment not to let go.",
        "Get up, walk to the kitchen, pour. Movement plus water is the cheapest reset.",
        "Your body already feels the difference, even if your head hasn't noticed yet.",
        "You don't have to be perfect. You have to be regular.",
        "Consistency beats intensity. One more glass and we go on.",
        "Check your shoulders — they're probably tense. Loosen up and have a drink.",
    ),
    Level.HIGH to listOf(
        "Close to the goal. The last sips are the easiest — just do them.",
        "Great job. Your kidneys are thanking you today.",
        "Not much left. Finish calmly, no rush.",
        "This is what a day looks like when you took care of yourself without grand gestures.",
        "Almost at the finish. Take a deep breath and finish it off.",
        "A well-looked-after body sleeps better. You're on the right track.",
    ),
    Level.DONE to listOf(
        "Goal reached. You have nothing more to prove — drink only when thirsty now.",
        "Done. That's exactly the small thing that makes a big difference.",
        "100%. Remember this feeling — it'll be easier to repeat tomorrow.",
        "Fully hydrated. Now take care of your sleep too.",
        "Goal delivered. The rest of the day is a bonus.",
    ),
)

private val DAYPART_NOTES_EN: Map<Daypart, List<String>> = mapOf(
    Daypart.MORNING to listOf(
        "Morning is the cheapest win of the day — a glass of water before the first coffee.",
        "After the night you're naturally dehydrated. Start with 300–500 ml.",
        "Water before breakfast kick-starts your metabolism more gently than caffeine.",
    ),
    Daypart.DAY to listOf(
        "Midday: set an hourly rhythm instead of one big gulp.",
        "Energy dip after lunch? Water first, conclusions later.",
        "Every meeting is a chance to top up your bottle.",
    ),
    Daypart.EVENING to listOf(
        "Slow down in the evening — smaller portions so you don't wake up at night.",
        "The last larger portion is best 1.5–2 h before sleep.",
        "Wrap up the day calmly. Hydration is a marathon, not a sprint.",
    ),
)

private val MASCOT_LINES_EN: Map<Level, List<String>> = mapOf(
    Level.LOW to listOf(
        "I'm a bit... concentrated. Too much.",
        "Hello? Drop here. It's dry in here.",
        "One sip and I'm back in shape!",
        "I have a plan: water. A simple plan.",
    ),
    Level.MID to listOf(
        "Oh, it's getting wet! I like it.",
        "Halfway there, glub glub.",
        "We're flowing nicely.",
        "I feel a wave of optimism.",
    ),
    Level.HIGH to listOf(
        "Just a moment more and I'm swimming!",
        "Almost a full bubble!",
        "Don't stop, it's beautiful.",
        "I can see the finish line, it's blue.",
    ),
    Level.DONE to listOf(
        "SPLASH! Goal reached!",
        "I'm fully myself. That means water.",
        "A gurgle of victory!",
        "Today you're an ocean.",
    ),
)

val FACTS: List<String> get() = if (Lang.isEnglish) FACTS_EN else FACTS_PL
val SELF_CARE: Map<Level, List<String>> get() = if (Lang.isEnglish) SELF_CARE_EN else SELF_CARE_PL
val DAYPART_NOTES: Map<Daypart, List<String>> get() = if (Lang.isEnglish) DAYPART_NOTES_EN else DAYPART_NOTES_PL
val MASCOT_LINES: Map<Level, List<String>> get() = if (Lang.isEnglish) MASCOT_LINES_EN else MASCOT_LINES_PL

data class WeekDay(val day: String, val pct: Float)


data class HistoryDay(
    val day: String,
    val date: String,
    val ml: Int,
    val goal: Int,
    val pct: Float,
    val reached: Boolean,
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
