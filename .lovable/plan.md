# Makiety aplikacji "Nawodnienie"

Cel: klikalne makiety (statyczne dane, stan lokalny w pamięci — bez backendu) w stylu material + minimalistic + tech-fancy: ciemne tło, szkło/warstwy, jeden akcent (cyjan/aqua), zaokrąglenia, subtelne animacje.

## Ekrany

```text
/            → Ekran główny (dashboard nawodnienia)
/widget      → Podgląd widgetu w 3 rozmiarach (S / M / L)
/insights    → Pełna lista ciekawostek + komentarzy self-care
```

## 1. Widget (skalowalny)
Jeden komponent `HydrationWidget` z propem `size: "sm" | "md" | "lg"`, renderowany identycznie na ekranie głównym i na stronie podglądu.

- **sm (1x1)**: pierścień postępu + % + maskotka mini
- **md (2x1)**: pierścień + ml/cel + rząd klikalnych butelek + jednolinijkowy komentarz
- **lg (2x2)**: to co md + wykres godzinowy dnia, dłuższy komentarz self-care i ciekawostka

Elementy wspólne:
- **Klikalne butelki**: 100 / 250 / 330 / 500 / 750 ml + kafelek „custom”. Tap = dolanie wody, animacja fali i haptyczny „puls”. Long-press/ikona cofnięcia = undo ostatniego łyka.
- **Progress dnia**: pierścień z animowanym wypełnieniem + falująca ciecz w tle karty, licznik `1 450 / 2 500 ml`, pozostało X ml, seria dni (streak).
- **Komentarz**: rotujący, dobierany do pory dnia i % celu.

## 2. Ekran główny
- Nagłówek: data, streak, cel dnia (edytowalny w makiecie)
- Duży widget `lg`
- Karta „Twój komentarz na dziś”: self-care (np. „Zrób przerwę, rozprostuj się i wypij szklankę — nie musisz nadrabiać wszystkiego naraz”) + ciekawostka („Uczucie zmęczenia po południu to często odwodnienie na poziomie 1–2%”)
- Oś czasu dzisiejszych łyków (godzina + ilość + ikona butelki)
- Pasek tygodnia: 7 mini-słupków z realizacją celu
- Sekcja maskotki

## 3. Maskotka („smaczek”)
Kropla-stworek o imieniu **Kropi** — SVG/CSS, kilka stanów zależnych od postępu:
- 0–25%: przygaszona, „susza”
- 26–60%: uśmiech, lekkie kołysanie
- 61–99%: energiczna, mruga
- 100%: świętowanie + konfetti z bąbelków

Reaguje też na kliknięcie (odbicie + losowy jednolinijkowy tekst).

## 4. Zestaw treści (obszerny)
Plik `src/data/hydration-content.ts`:
- ~40 ciekawostek o wodzie/nawodnieniu (fizjologia, sport, sen, skóra, kawa, elektrolity, mity)
- ~30 komentarzy self-care (pora dnia: rano / dzień / wieczór; oraz stan: niski / średni / wysoki / cel osiągnięty)
- ~15 krótkich kwestii maskotki
Dobór deterministyczny wg dnia + stanu, żeby makieta nie migotała przy re-renderze.

## Szczegóły techniczne
- TanStack Start, 3 route'y w `src/routes/`, każdy z własnym `head()` (unikalny title/description/OG).
- Tokeny kolorów i gradienty w `src/styles.css` (oklch), zero hardkodowanych kolorów w komponentach.
- Komponenty: `HydrationWidget`, `ProgressRing`, `BottlePicker`, `MascotDrop`, `IntakeTimeline`, `WeekBar`, `InsightCard`.
- Stan makiety: React `useState` w hooku `useHydrationMock` (brak bazy danych — dane resetują się po odświeżeniu).
- Font tech-owy (np. Space Grotesk + DM Sans) ładowany przez `<link>` w `__root.tsx`.
- Animacje: CSS/Tailwind (fala, pulsy, konfetti).

Jeśli później zechcesz, żeby dane były zapisywane między sesjami, dołożymy Lovable Cloud w kolejnym kroku.
