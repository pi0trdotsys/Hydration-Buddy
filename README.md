# Hydration Buddy — Kropi

[![Pobierz APK](https://img.shields.io/github/v/release/pi0trdotsys/hydration-buddy?label=Pobierz%20APK&style=for-the-badge&color=00DFE8&logoColor=white)](https://github.com/pi0trdotsys/hydration-buddy/releases/latest)

Najnowsza wersja natywnej aplikacji na Androida: [Releases → v1.3.0](https://github.com/pi0trdotsys/hydration-buddy/releases/tag/v1.3.0) (plik `kropi-hydration-v1.3.0.apk`).

![Widget w trzech rozmiarach](docs/widget-showcase.svg)

Jeden design, jeden komponent — renderowany na dwóch platformach:

- **Web** (`src/`) — makiety TanStack Start/React: dashboard, podgląd widgetu w 3 rozmiarach (`/widget`) i pełna lista ciekawostek (`/insights`).
- **Android** (`android/`) — natywna aplikacja z takimi samymi zakładkami jak makiety webowe (Główny / Widget / Historia / Treści), plus własna zakładka Ustawień, zbudowana w Kotlinie/Compose + skalowalny widget na ekran główny w Jetpack Glance, z prawdziwą interakcją (dolewanie wody, licznik dnia, maskotka), zapisem stanu w DataStore i inteligentnymi powiadomieniami przypominającymi o piciu wody.

![Architektura](docs/architecture.svg)

## Development

Prefer working locally? You need Node.js and npm — [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating).

```sh
git clone <this-repository-url>
cd <repository-name>
npm i
npm run dev
```

## Natywna aplikacja na Androida

Kod w [`android/`](android/) to osobny projekt Gradle/Kotlin (Jetpack Compose + Glance + DataStore), niezależny od aplikacji webowej powyżej, ale renderujący ten sam design — łącznie z tymi samymi zakładkami, jakie zaplanowano dla makiet w `src/routes/`:

| Zakładka | Web (makieta) | Android (`ui/`) |
| --- | --- | --- |
| 🏠 Główny | `src/routes/index.tsx` | `HomeScreen.kt` — nagłówek z realną datą, suwak celu, duży widget, karty self-care/ciekawostka, oś czasu łyków, pasek tygodnia, maskotka |
| 🔲 Widget | `src/routes/widget.tsx` | `WidgetScreen.kt` — podgląd 3 rozmiarów + przycisk przypinający widget na ekran główny |
| 📅 Historia | `src/routes/history.tsx` | `HistoryScreen.kt` — statystyki tygodnia, wykres słupkowy, szczegóły dni, najlepszy dzień |
| 💡 Treści | `src/routes/insights.tsx` | `InsightsScreen.kt` — cała baza treści (self-care, pory dnia, ciekawostki, kwestie maskotki) |
| ⚙️ Ustawienia | *(bez odpowiednika — makieta nie miała ustawień)* | `SettingsScreen.kt` — kalkulator celu, aktywne godziny, przypomnienia |

Wspólne elementy widgetu (`WidgetPreview.kt`: pierścień, maskotka, butelki rysowane na `Canvas`) są dzielone między zakładkę Główny i Widget, tak jak w web makiecie jeden komponent `HydrationWidget` renderuje się na `/` i `/widget`.

- `widget/HydrationWidget.kt` — prawdziwy `GlanceAppWidget` na ekranie głównym telefonu, z `SizeMode.Responsive` (sm/md/lg), pierścieniem, maskotką i wykresem dnia renderowanymi na bitmapie (Glance nie ma dostępu do dowolnego Canvasu). Stan zbierany jest jako `Flow` **wewnątrz** `provideContent`, więc każdy zapis (także kilka łyków pod rząd) odświeża kafelek natychmiast.
- `widget/WidgetGraphics.kt` — `IntakeChart`: wykres „ile i o której” (schodek na każdy łyk z podpisaną objętością, przerywana linia planu dnia, oś godzin, znacznik „teraz”), rysowany tym samym kodem na bitmapie dla widgetu i na `Canvas` w aplikacji.
- `data/HydrationPlan.kt` — kalkulacja planu: ile porcji, po ile i o której wypaść, żeby domknąć dzienny cel przed końcem okna picia. Zasila powiadomienia, kartę „Plan na resztę dnia” i podpis pod wykresem.
- `data/HydrationRepository.kt` — stan (cel, łyki, streak, historia) trzymany w Jetpack DataStore, z rolowaniem dnia o północy — odpowiednik `useHydrationMock`, ale z realnym zapisem między sesjami.
- `data/HydrationContent.kt` — 1:1 port `src/data/hydration-content.ts` i `src/hooks/use-hydration-mock.ts` (ciekawostki, self-care, kwestie maskotki, dane historii/tygodnia).
- `ui/MainActivity.kt` — dolna nawigacja (`NavigationBar`) spinająca powyższe 5 ekranów, każdy czytający ten sam `HydrationRepository`.

### Widget nie odświeżył się od razu po dotknięciu?

Do wersji 1.2.0 widget potrafił „zaciąć się” po drugim dolaniu wody pod rząd: kompozycja czytała stan jednorazowo, **przed** `provideContent`, więc kolejne `update()` trafiały na tę samą, zamrożoną wartość i kafelek pokazywał starą liczbę. Od 1.3.0 stan jest zbierany jako `Flow` wewnątrz kompozycji, a akcje odświeżają wszystkie instancje widgetu (`updateAll`) — każdy kolejny łyk widać od razu.

Jeśli mimo to odświeżenie się spóźnia: na telefonach z MIUI/HyperOS (Xiaomi, Redmi, POCO) i podobnie agresywnym zarządzaniem baterią system potrafi usypiać (`Freezer`) proces aplikacji w tle. Dotknięcie butelki na widgecie **zapisuje łyk od razu** (to działa niezależnie od tego usypiania), ale samo przerysowanie kafelka czeka, aż system obudzi proces. W zakładce **Ustawienia** jest karta „Szybsze powiadomienia i widget” z przełącznikiem wyłączającym optymalizację baterii dla Kropi oraz (na MIUI) skrótem do ustawień autostartu.

### Przypomnienia i cel dzienny

Kropi sam pilnuje, żebyś nie zapomniał/a o wodzie:

- **Fancy powiadomienia z konkretnym planem** — jeśli nie zanotujesz łyka wody przez zbyt długi czas (interwał wyliczony z Twojego celu i aktywnych godzin picia), Kropi wysyła powiadomienie z własnym, zsyntezowanym dźwiękiem („plusk" — `res/raw/water_notification.wav`) i wyliczeniem pod Ciebie: *„Wypij 250 ml o 15:10 — i jeszcze 4× do 22:00, a cel 2 500 ml będzie zrobiony"*. Po rozwinięciu widać cały rozkład godzin, bilans względem planu na tę porę, czas od ostatniego łyka i zdanie self-care; pasek postępu pokazuje dzisiejsze nawodnienie. Przycisk **„💧 Wypiłem/-am X ml"** dolewa dokładnie tyle, ile przewiduje najbliższa porcja planu, i odświeża widget bez otwierania aplikacji (plus „Za 20 min" do odłożenia przypomnienia).
- **Cel automatyczny** — na podstawie wagi, temperatury otoczenia i poziomu aktywności (`GoalCalculator.kt`: ~33 ml/kg + bonus za aktywność/upał) albo cel ręczny — do wyboru w ustawieniach w aplikacji.
- **Aktywne godziny picia** (np. 8–22) — przypomnienia i wyliczenie tempa działają tylko w tym oknie.
- **„Do teraz: X ml • nast. 15:10" na widgecie** — ile powinieneś/aś już wypić o tej porze dnia (na pomarańczowo, gdy jesteś w tyle) i kiedy wypada najbliższa porcja. Widget odświeża się automatycznie co ok. 15 minut (`ReminderWorker`, WorkManager) niezależnie od tego, czy dotkniesz go ręcznie.
- **Wykres dnia zamiast anonimowych słupków** — duży widget (i jego podgląd w aplikacji) pokazuje schodkową linię nawodnienia: każdy skok to jeden łyk, podpisany objętością, na osi z godzinami i znacznikiem „teraz". Przerywana linia obok to plan dnia — od razu widać, czy jesteś nad nią, czy pod. Pod wykresem podsumowanie: *„5 × 250 ml o 15:10, 16:25 … 21:50 → cel 2 500 ml"*.

Uruchomienie lokalnie (wymaga Android SDK):

```sh
cd android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
