# Hydration Buddy — Kropi

[![Pobierz APK](https://img.shields.io/github/v/release/pi0trdotsys/hydration-buddy?label=Pobierz%20APK&style=for-the-badge&color=00DFE8&logoColor=white)](https://github.com/pi0trdotsys/hydration-buddy/releases/latest)

Najnowsza wersja natywnej aplikacji na Androida: [Releases → v1.1.0](https://github.com/pi0trdotsys/hydration-buddy/releases/tag/v1.1.0) (plik `kropi-hydration-v1.1.0.apk`).

![Widget w trzech rozmiarach](docs/widget-showcase.svg)

Jeden design, jeden komponent — renderowany na dwóch platformach:

- **Web** (`src/`) — makiety TanStack Start/React: dashboard, podgląd widgetu w 3 rozmiarach (`/widget`) i pełna lista ciekawostek (`/insights`).
- **Android** (`android/`) — natywny, skalowalny widget na ekran główny zbudowany w Kotlinie z Jetpack Glance, 1:1 odwzorowujący design z makiet, z prawdziwą interakcją (dolewanie wody, licznik dnia, maskotka), zapisem stanu w DataStore i inteligentnymi powiadomieniami przypominającymi o piciu wody.

![Architektura](docs/architecture.svg)

## Development

Prefer working locally? You need Node.js and npm — [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating).

```sh
git clone <this-repository-url>
cd <repository-name>
npm i
npm run dev
```

## Natywny widget na Androida

Kod w [`android/`](android/) to osobny projekt Gradle/Kotlin (Jetpack Glance + Compose + DataStore), niezależny od aplikacji webowej powyżej, ale renderujący ten sam design.

- `HydrationWidget.kt` — `GlanceAppWidget` z `SizeMode.Responsive` (sm/md/lg), z pierścieniem postępu i maskotką renderowanymi na bitmapie (Glance nie ma dostępu do dowolnego Canvasu) oraz klikalnymi butelkami zbudowanymi z natywnych komponentów Glance.
- `HydrationRepository.kt` — stan (cel, łyki, streak) trzymany w Jetpack DataStore, z rolowaniem dnia o północy — odpowiednik `useHydrationMock`, ale z realnym zapisem między sesjami.
- `HydrationContent.kt` — 1:1 port `src/data/hydration-content.ts` (ciekawostki, self-care, kwestie maskotki).
- `MainActivity.kt` — ekran hosta z podglądem wszystkich trzech rozmiarów (odpowiednik `/widget`) i przyciskiem, który przypina widget na ekran główny (`requestPinAppWidget`).

### Przypomnienia i cel dzienny

Kropi sam pilnuje, żebyś nie zapomniał/a o wodzie:

- **Fancy powiadomienia** — jeśli nie zanotujesz łyka wody przez zbyt długi czas (interwał wyliczony z Twojego celu i aktywnych godzin picia), Kropi wysyła powiadomienie z własnym, zsyntezowanym dźwiękiem („plusk" — `res/raw/water_notification.wav`), treścią dopasowaną do pory dnia i aktualnego nawodnienia, oraz przyciskiem **„💧 Wypiłem/-am X ml"**, który dolewa wodę i odświeża widget bez otwierania aplikacji (plus „Za 20 min" do odłożenia przypomnienia).
- **Cel automatyczny** — na podstawie wagi, temperatury otoczenia i poziomu aktywności (`GoalCalculator.kt`: ~33 ml/kg + bonus za aktywność/upał) albo cel ręczny — do wyboru w ustawieniach w aplikacji.
- **Aktywne godziny picia** (np. 8–22) — przypomnienia i wyliczenie tempa działają tylko w tym oknie.
- **„Do teraz: X ml" na widgecie** — pokazuje, ile powinieneś/aś już wypić o tej porze dnia, na czerwono/pomarańczowo gdy jesteś w tyle. Widget odświeża się automatycznie co ok. 15 minut (`ReminderWorker`, WorkManager) niezależnie od tego, czy dotkniesz go ręcznie.

Uruchomienie lokalnie (wymaga Android SDK):

```sh
cd android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
