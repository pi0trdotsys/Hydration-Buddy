# Nawodnienie — dokumentacja makiet i specyfikacja wdrożenia logiki

Dokument opisuje aktualne makiety (UI zbudowane na danych mockowych) oraz to, co trzeba
zaimplementować, aby zamienić je w działającą aplikację z realnym backendem
(Lovable Cloud) i realną logiką biznesową.

Status: **UI gotowe / logika mockowa**. Wszystkie dane pochodzą z
`src/hooks/use-hydration-mock.ts` i `src/data/hydration-content.ts` i nie są nigdzie zapisywane.

---

## 1. Mapa ekranów

| Ścieżka | Plik | Zawartość | Stan |
|---|---|---|---|
| `/` | `src/routes/index.tsx` | Dashboard mobilny: widget 2×2, karty self-care + ciekawostka, timeline dnia, pasek tygodnia, suwak celu | makieta |
| `/widget` | `src/routes/widget.tsx` | Galeria widgetu w 3 rozmiarach (`sm`, `md`, `lg`) | makieta |
| `/history` | `src/routes/history.tsx` | Statystyki tygodniowe: kafelki KPI, wykres słupkowy, lista dni, najlepszy dzień | makieta |
| `/insights` | `src/routes/insights.tsx` | Przegląd bazy treści (ciekawostki, self-care) | makieta |

Nawigacja: `src/routes/__root.tsx`.

## 2. Komponenty

| Komponent | Plik | Props | Rola |
|---|---|---|---|
| `HydrationWidget` | `hydration-widget.tsx` | `state: HydrationState`, `size?: "sm" \| "md" \| "lg"` | skalowalny widget: pierścień, maskotka, butelki, komentarz |
| `ProgressRing` | `progress-ring.tsx` | `progress: 0..1`, `size`, `stroke`, `children` | SVG pierścień postępu |
| `MascotDrop` | `mascot-drop.tsx` | `level`, `size` | maskotka „Kropi”, mimika zależna od poziomu |
| `BottlePicker` | `bottle-picker.tsx` | `onAdd(ml)`, `compact?` | klikalne pojemności (100/250/330/500/750 ml) |
| `IntakeTimeline` / `WeekBar` | `intake-timeline.tsx` | `intakes` / `week` | oś dnia i pasek tygodnia |
| `HistoryWeekChart` | `history-week-chart.tsx` | `history` | wykres słupkowy 7 dni |
| `InsightCard` | `insight-card.tsx` | `title`, `text`, `icon` | karta self-care / ciekawostki |

Komponenty są **czyste prezentacyjnie** — przyjmują dane i callbacki, nie sięgają do źródeł danych.
Podmiana warstwy danych nie wymaga ich modyfikacji.

## 3. Warstwa treści (zostaje bez zmian)

`src/data/hydration-content.ts`:

- `BOTTLES` — lista pojemności (ml, etykieta, ikona).
- `FACTS` — ~42 ciekawostki o nawodnieniu.
- `SELF_CARE: Record<Level, string[]>` — komunikaty zależne od poziomu.
- `MASCOT_LINES: Record<Level, string[]>`, `DAYPART_NOTES: Record<Daypart, string[]>`.
- `levelFor(progress) -> "low" | "mid" | "high" | "done"`.
- `daypartFor(hour) -> "morning" | "day" | "evening"`.
- `pick(list, seed)` — deterministyczny wybór (ten sam seed = ten sam tekst, brak migotania przy re-renderze/SSR).

Zasada: dobór treści musi pozostać deterministyczny względem seeda (nie `Math.random()`),
inaczej SSR i klient wygenerują różny tekst.

## 4. Kontrakt stanu (do zachowania)

Docelowy hook `useHydration()` musi zwracać ten sam kształt co dzisiejszy `useHydrationMock()`
(`HydrationState`), aby UI nie wymagał zmian:

```ts
{
  goal: number;                 // dzienny cel w ml
  setGoal: (ml: number) => void;
  intakes: { id: number|string; ml: number; hour: number; minute: number }[];
  total: number;                // suma ml dzisiaj
  progress: number;             // 0..1, przycięte do 1
  remaining: number;            // max(goal - total, 0)
  level: Level;                 // levelFor(total / goal)
  daypart: Daypart;             // daypartFor(aktualna godzina)
  lastAdded: number | null;     // do animacji potwierdzenia
  add: (ml: number) => void;
  undo: () => void;
  streak: number;               // liczba kolejnych dni z osiągniętym celem
  week: { day: string; pct: number }[];
  history: HistoryDay[];        // 7 dni: day, date, ml, goal, pct, reached
  selfCare: string; selfCareAlt: string; fact: string; dayNote: string; mascotLine: string;
}
```

Różnice względem makiety, które trzeba usunąć przy wdrożeniu:
- `daypart` liczony jest ze sztywnej godziny `15` → użyć realnego czasu (odświeżanego co minutę).
- nowe wpisy mają zmyśloną godzinę → użyć `new Date()`.
- `streak: 5` jest zahardkodowany → liczyć z historii.
- `WEEK` / `HISTORY` to stałe tablice → pochodzą z bazy.

## 5. Model danych (Lovable Cloud / Postgres)

```sql
-- profil ustawień użytkownika
create table public.hydration_settings (
  user_id uuid primary key references auth.users(id) on delete cascade,
  daily_goal_ml integer not null default 2500 check (daily_goal_ml between 500 and 6000),
  timezone text not null default 'Europe/Warsaw',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- pojedyncze wypicia
create table public.hydration_intakes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  ml integer not null check (ml between 10 and 2000),
  drank_at timestamptz not null default now(),
  local_day date not null,          -- dzień wg strefy użytkownika, klucz agregacji
  created_at timestamptz not null default now()
);
create index on public.hydration_intakes (user_id, local_day);
```

- GRANTy dla `authenticated` + `service_role`, RLS włączone, polityki `user_id = auth.uid()`
  dla select/insert/update/delete.
- Cele historyczne: dzień dziedziczy `daily_goal_ml` z ustawień; jeśli cel ma być
  „zamrożony” per dzień, dołożyć tabelę `hydration_daily_goals(user_id, local_day, goal_ml)`
  zapisywaną przy pierwszym wpisie danego dnia.

## 6. Server functions (TanStack Start, `src/lib/hydration.functions.ts`)

| Funkcja | Wejście | Wyjście | Uwagi |
|---|---|---|---|
| `getToday` | – | `{ goal, intakes[], total }` | filtr `local_day = dzisiaj(tz)` |
| `addIntake` | `{ ml }` (Zod, 10–2000) | nowy wpis | `local_day` liczony serwerowo ze strefy z ustawień |
| `undoLastIntake` | – | `{ ok }` | usuwa najnowszy wpis z dzisiaj |
| `setGoal` | `{ ml }` (500–6000) | ustawienia | upsert |
| `getHistory` | `{ days?: 7 }` | `HistoryDay[]` + `streak` | agregacja `sum(ml) group by local_day`, dni bez wpisów uzupełnić zerami |

Wszystkie z `.middleware([requireSupabaseAuth])`. Wywoływać przez `useServerFn` +
React Query (loadery tylko pod `_authenticated/`).

## 7. Logika do zaimplementowania

1. **Agregacja dnia** — `total = Σ ml` dla `local_day`; `progress = min(total/goal, 1)`;
   `remaining = max(goal - total, 0)`. Przekroczenie celu pokazujemy jako `>100%` w tekście,
   ale pierścień pozostaje na 100%.
2. **Granica doby** — zawsze wg strefy użytkownika, nie UTC; wyliczanie `local_day` na serwerze.
   UI musi przeładować dane po zmianie doby (interwał lub `visibilitychange`).
3. **Poziom i treści** — `levelFor(progress)`; seed = `intakes.length + round(total/100)` (stabilny
   w obrębie jednego stanu, zmienia się po każdym wypiciu → nowa ciekawostka jako nagroda).
4. **Streak** — liczba kolejnych dni wstecz (od wczoraj, plus dziś jeśli już osiągnięty),
   dla których `sum(ml) >= goal`.
5. **Optymistyczny update** — `add()` natychmiast aktualizuje cache React Query i ustawia
   `lastAdded` (animacja pulsu), po błędzie rollback + toast (sonner).
6. **Undo** — okno 5 s po dodaniu; potem przycisk znika.
7. **Tydzień** — `history` zawsze 7 pozycji Pn–Nd bieżącego tygodnia, brakujące dni = 0 ml.
8. **Cel** — suwak 1000–4000 ml, krok 100; debounce 400 ms przed zapisem.
9. **Offline/gość** — wersja bez logowania: te same funkcje zapisujące do `localStorage`
   za tym samym interfejsem `HydrationState`; migracja do konta przy pierwszym logowaniu.

## 8. Widget (skalowalność)

- `sm` (2×2): pierścień + maskotka, bez butelek — tap otwiera aplikację.
- `md` (4×2): pierścień, licznik, 3 najczęstsze pojemności, jednolinijkowy komentarz.
- `lg`: pełny zestaw butelek, komentarz self-care + ciekawostka, streak.
- Widget musi działać jako komponent w aplikacji oraz jako niezależny render — nie może
  zakładać obecności nawigacji ani routera.

## 9. Kryteria akceptacji

- Dodanie wody utrzymuje się po odświeżeniu strony i na innym urządzeniu tego samego konta.
- O północy (strefa użytkownika) licznik startuje od zera, a poprzedni dzień trafia do historii.
- Historia pokazuje 7 dni, w tym dni bez wpisów jako 0 ml.
- Streak zgadza się z danymi w historii.
- Dwóch różnych użytkowników nigdy nie widzi swoich danych (RLS zweryfikowane testem).
- Brak `Math.random()` w doborze treści; ten sam stan = ten sam tekst po SSR i hydracji.

## 10. Kolejność prac

1. Włączyć Lovable Cloud + migracja (tabele, GRANT, RLS).
2. `hydration.functions.ts` + walidacja Zod.
3. `useHydration()` zwracający `HydrationState` (React Query, optymistyczne mutacje).
4. Podmiana `useHydrationMock` na `useHydration` w `/`, `/widget`, `/history` (bez zmian w komponentach).
5. Auth + tryb gościa (localStorage) i migracja danych.
6. Testy kryteriów z sekcji 9.
