<div align="center">

<img src="docs/kropi.svg" width="104" alt="Kropi" />

# Kropi

**Water, with a plan.**
Not "remember to drink". Something you can act on: 250 ml at 3:10 pm, four more by 10 pm — and the day is done.

[Polski](README.md) · **English**

<br />

[![Download APK](https://img.shields.io/github/v/release/pi0trdotsys/Hydration-Buddy?label=Download%20APK&style=for-the-badge&color=00DFE8&labelColor=020F19)](https://github.com/pi0trdotsys/Hydration-Buddy/releases/latest)
&nbsp;
![Android](https://img.shields.io/badge/Android-8%2B-00DFE8?style=for-the-badge&labelColor=020F19)
&nbsp;
![Offline](https://img.shields.io/badge/100%25-offline-00DFE8?style=for-the-badge&labelColor=020F19)

<br />

<img src="docs/screenshots/app-today.png" width="240" alt="Home: progress ring, pace pills and the day's chart" />
&nbsp;&nbsp;
<img src="docs/screenshots/app-plan.png" width="240" alt="Plan for the rest of the day with actual times" />
&nbsp;&nbsp;
<img src="docs/screenshots/app-settings.png" width="240" alt="Notification tone and snark level" />

</div>

<br />

> **Heads up:** the app itself speaks Polish only for now. The screenshots and
> quotes below are translated. English is next on the list — the groundwork is
> there, the strings just need a second language.

<br />

## ◉ A plan, not a nag

Every water app can type "drink something". Kropi works out **how much and
when**, so the day actually closes:

> **💧 Really, is it that hard?** · 10% • goal 2,500 ml
> Drink 250 ml at 3:40 pm — plus eight more by 10 pm, and 2,500 ml is done.

The plan takes your goal, your drinking window and your glass size, splits what
is missing into even portions and hands you the times. When the day is running
out, it makes the portions bigger instead of promising a goal you cannot hit.
Expand the notification and you get the whole schedule, where you stand against
the pace for this hour, and how long it has been since your last sip.

The action button logs **exactly the portion the plan calls for** — one tap and
you are back to whatever you were doing.

## ◉ A widget that actually tells you something

<div align="center">
<img src="docs/screenshots/widget-bar.png" width="430" alt="4×1 widget: progress, next portion and two log buttons" />
</div>

Not just a percentage, and not anonymous bars. The large tile draws a **stepped
hydration line**: every step is one sip, labelled with its volume, on an axis
with hours and a "now" marker. A dashed **plan line** runs alongside it, so you
can see at a glance whether you are above or below it.

Two status pills sit on top: `BELOW PLAN / 507 ml` (orange when you are behind)
and `NEXT / 250 ml at 12:30`. The footer carries your streak, the last logged
sip and how many portions are left today.

Four log buttons live on the tile, so **water gets logged without opening the
app**. Tapped the wrong one? Undo sits right next to them. A strip below the buttons lets you **pick a drink** (tea, coffee, juice…) for the next pour, and "**＋ wpis**" opens the full editor — past sips included. Three layouts —
a 4×1 bar, a 2×1 tile and a full 2×2 — each showing as much as it can fit.

The widget is **yours to tune**: long-press it and pick "Configure" to decide what each of the four buttons logs (say, "coffee 200" in one tap) and to choose a style — Kropi's own palette (light or dark per system) or **Material You** colours from your wallpaper (plus an always-dark "Kropi dark" variant). Drinks also remember a "typical portion". Tapping the chart opens today's entries for fixing, and after an undo a **Restore** option sits there for a few seconds. When you're behind plan the widget says plainly "Drink 250 ml now", and it resets itself at midnight. Two smaller widgets are available too: a **1×1 "pour a glass"** and a **weekly** one with seven bars and your streak.

<div align="center">
<img src="docs/screenshots/widget-large.png" width="330" alt="Large 2×2 widget with the day's chart" />
</div>

## ◉ Kropi bites. Or doesn't — your call

The mascot has two personalities. **Supportive** talks to you like a friend
after a workout. **Snarky** does not sugar-coat anything:

> Four hours without water. Your kidneys have just filed for unpaid leave. Even
> instant noodles get more water than you. Get up and drink. I won't repeat
> myself.

Snark comes in three levels: 🙂 **mild** (a jab, no harsh words), 😏 **normal**
and 🔥 **savage** — that last one is uncensored, so you turn it on deliberately.
The line is picked from how far you are from your goal and how long you have
gone dry. The numbers and the plan stay the same; only the tone changes — the
mascot inside the app included.

In the evening, once your drinking window closes, a **daily wrap-up** arrives:
the balance, how many sips, your streak, and a comment in whichever tone you
picked.

## ◉ Quiet when it should be

A reminder won't interrupt a meeting: Kropi peeks at the calendar on your phone
(locally only, with your permission) and waits until it ends. It does the same
while "Do not disturb" is on. The reminder isn't lost — it speaks up afterwards.

## ◉ It learns your hours

Kropi remembers when you **actually** drink and moves the portions there,
instead of spreading them evenly across the day. Same goal, two different
people:

```text
early riser   08:00  08:30  09:00  12:00  13:00  17:30
night owl     09:20  16:30  18:30  19:00  19:30  20:15
```

The first portion never lands later than an even split would put it — so "I
only drink in the evening" doesn't get baked in for good. Not your thing? One
switch in Settings and the even rhythm is back.

## ◉ Water without opening the app

- **Widget** — four buttons on your home screen, volumes are yours to set.
- **Quick Settings tile** — one tap from the shade, with the day's balance in
  the subtitle.
- **Launcher shortcuts** — long-press the icon and pick "Glass" or "500 ml".
- **Notification** — one button logs the planned portion, the other snoozes the
  reminder for 20 minutes.

## ◉ A goal calculated for you

The goal covers **drinks**, not all water including food: it follows EFSA and
NASEM recommendations (about 28 ml per kg from drinks), plus an allowance for
activity and heat. It is **realistic to actually do** — capped at about 300 ml
per hour of your active window — and Settings shows what it's made of. The plan
never asks for more than 500 ml at once (kidneys clear about 0.8–1 l an hour),
and the **Regularity** card shows in how many two-hour windows you really drank,
your longest gap, and warns when you pour in too much at once.

Give it your weight and your activity level — Kropi works out the goal (28 ml
per kilogram from drinks, plus a bump for heat and effort). Prefer your own number? Manual mode with a slider. On top of that,
**active drinking hours**, say 8 am to 10 pm: reminders and pace only count
inside that window, outside it Kropi leaves you alone.

Want the goal to work itself out? Connect Kropi to **Health Connect**: your
weight and the week's average steps set the goal, and what you drink goes back
to Health Connect as hydration (alcohol is skipped).

## ◉ A goal that adapts

The automatic goal changes during the day with what's going on — and Settings
shows every component:

- **Weather from Homebrew Weather.** If you run my weather app, Kropi reads the
  forecast **high of the day** from it (numbers only, no location) and adds water
  smoothly: nothing below 24 °C, the full allowance from 32 °C. Without it, or if
  the forecast is older than 36 hours, your manual temperature choice stays.
- **Workouts from Health Connect.** Sessions add about 600 ml an hour (the middle
  of the ACSM range), and a reminder with a concrete portion follows the workout.
- **The "how are you today?" question** — once a day from noon: what colour is
  your urine (Armstrong scale: pale = well hydrated). Dark after hitting the goal
  raises your goal by 3%, and very pale three times in a row lowers it — at most
  ±10%, from the next day. Dark after missing the goal changes nothing: the
  amount is to blame, not the goal.
- **Alcohol and caffeine.** Alcohol doesn't count and **raises today's goal** by
  half its volume (up to 600 ml), plus a warning card. Coffee and tea count
  normally up to about 400 mg of caffeine a day (EFSA's limit), and the excess at
  half value.

## ◉ A streak with a shield

A streak that ends after the first bad day is discouraging. So you start with one
**shield** and earn another for every 7 days in a row (two at most). A missed day
spends a shield instead of resetting the streak. When the streak is at risk, an
evening reminder tells you how much is missing.

## ◉ Reminders from your habits

- **Learned from your habits.** Kropi works out from recent weeks when you usually
  don't drink (meetings, driving, deep work) and reminds you **just before such a
  gap**, if you haven't just had a drink.
- **A glass of water after coffee.** Coffee or tea starts a 20-minute countdown;
  if no water went in, a short reminder arrives.
- **When you get home.** When the phone joins your home Wi-Fi. Android only lets
  an app learn the network name with the location permission, so it's needed (and
  used for that alone); some phones require "Always" for it in the background.

All of them respect Do Not Disturb, meetings, your active window and snoozing.

## ◉ NFC tags

A sticker on a cup or bottle that logs exactly what's written on it when you
**tap your phone** — say "coffee 200 ml". In Settings you pick the drink and the
amount, hold the phone to the tag, done (you need at least an NTAG213). Reading
needs no permissions, and two taps of the same tag within 6 seconds count as one.
The content is a plain address, `kropi://add?ml=250&drink=COFFEE`, so any tag app
can write one too.

## ◉ History that doesn't lie

Every closed day goes into history: how much you drank, what the goal was,
whether you made it. The week, a **month calendar** (every day tinted by how
close you got, arrows take you back through older months), the **last 30
days**, your best day and your **streak** — all computed from real entries, not
from sample data. Tap a day in the calendar to see its sips, fix them or **add
a forgotten one**; a **Trends** card compares this week with the last, shows when you usually
have your first glass, which hours you drink the most in and which weekdays go
best. Everything (days, or single entries with drink and note) exports to **CSV**
without any storage permissions.

## ◉ Not just water

Tap an entry on the timeline or **"＋ Dodaj wpis"** to log a sip after the
fact (today or yesterday, any time), change the amount or add a note. Every
entry has a **drink type**: tea, coffee, juice and milk count toward the goal
partially (coffee 80%, say), soda 70%, alcohol not at all. Prefer 1:1 for
everything? Turn off "Net hydration" in Settings. History shows **what you
drink** over the last 30 days.

## ◉ Backups and updates

Same as Szpila: a **daily copy** (last 14) and on-demand copies go to
`Download/Kropi/Kopie`, and Settings lists them with restore. Make a copy before
uninstalling, and after reinstalling pick it with **Restore from file…** (a fresh
install can't see the previous one's files — that's how Android works). The same
dialog also loads a **CSV from the history export**, if you were on an older
version without backups.

Updates come from this repository: Kropi checks the latest release, **notifies
you when there is a newer one**, downloads the APK and verifies the checksum,
package, version and signature before installing. You only confirm in the system
dialog.

## ◉ Works with Szpila

Running [Szpila](https://github.com/pi0trdotsys/Glow-Habit-Widget)? Water logged
in Kropi **lands there by itself** — no more typing the same thing twice. Every
entry refreshes Szpila's habit and widgets right away, even while it is closed.
The data travels locally, through a provider guarded by a signature-level
permission: only Szpila can read it, and nothing leaves your phone. The one
condition: both apps must be signed with the **same key** (see below).

<br />

---

<br />

## ◉ Polish and English

The interface, notifications, widgets and the mascot's lines come in two languages.
Pick one in Android's settings (**Apps → Kropi → Language**) or leave it to follow
the phone. Screen readers are supported: buttons have TalkBack labels and touch
targets are at least 48 dp.

## ▸ Install

1. Grab the **`kropi-hydration-….apk`** file from
   [Releases](https://github.com/pi0trdotsys/Hydration-Buddy/releases/latest).
2. Open it on your phone and allow installs from that source.
3. Launch Kropi, accept notifications and head into Settings.

Android 8.0 or newer. Updates install over the top — your data stays put.

## ▸ The first two minutes that matter

On first launch Kropi walks you through the same in four screens (goal, hours,
tone, notifications, widget) — below is a cheat sheet of where to change it all later.

|     | Where                                                        | Why                                                |
| --- | ------------------------------------------------------------ | -------------------------------------------------- |
| 🔔  | Allow notifications                                          | Reminders with a plan, plus the daily wrap-up      |
| ⚖️  | Settings → weight, temperature, activity                     | A goal calculated for you                          |
| 🕗  | Settings → **active drinking hours**                         | Reminders only when you want them                  |
| 😈  | Settings → **notification tone**                             | Supportive or snarky, in three levels              |
| ➕  | Long-press home screen → **Widgets** → Kropi                 | Logging in a single tap                            |
| ⚡  | Quick Settings → edit tiles → **Kropi**                      | Water straight from the shade                      |
| 🔋  | System settings → Battery → Kropi → **Unrestricted**         | Reminders on time (Xiaomi, POCO, Samsung)          |

## ▸ Your data is yours

Everything stays on the phone. **No account, no cloud, no ads, no tracking** —
the internet is used only to ask GitHub whether a newer version exists (no data
about you). Export your history to CSV whenever you like, and back everything up
with one tap. The only thing that ever leaves Kropi is the water handed locally
to Szpila, if you happen to run it. The location permission is optional and
used only to recognise your home Wi-Fi; the temperature comes from Homebrew
Weather locally, without the internet.

<br />

<div align="center">

**Kropi** · water, with a plan

<img src="docs/kropi.svg" width="64" alt="Kropi" />

</div>

<br />

<details>
<summary><b>For developers</b></summary>

<br />

The repository holds two things rendering the same design:

- **`android/`** — the real app: Kotlin, Jetpack Compose, a Glance widget,
  state in DataStore, reminders on WorkManager. The network is used only to
  check for and download updates from GitHub.
- **`src/`** — the web mockup (TanStack Start + React) the design system came
  from: palette, progress ring, mascot and tile layout.

Where the interesting parts live:

| File | What it does |
| --- | --- |
| [`data/HydrationPlan.kt`](android/app/src/main/kotlin/com/kropi/hydration/data/HydrationPlan.kt) | Spreads portions over the day, including the adaptive mode (quantiles of the hourly profile) |
| [`data/HydrationRepository.kt`](android/app/src/main/kotlin/com/kropi/hydration/data/HydrationRepository.kt) | Today's state, 400 days of history, streak, hourly profile |
| [`data/SnarkContent.kt`](android/app/src/main/kotlin/com/kropi/hydration/data/SnarkContent.kt) | Kropi's second personality: titles, jabs and closers across three snark levels |
| [`widget/HydrationWidget.kt`](android/app/src/main/kotlin/com/kropi/hydration/widget/HydrationWidget.kt) | The Glance widget in `SizeMode.Exact`; state collected as a `Flow` **inside** `provideContent` |
| [`widget/WidgetGraphics.kt`](android/app/src/main/kotlin/com/kropi/hydration/widget/WidgetGraphics.kt) | `IntakeChart` — the day's chart drawn by the same code onto a bitmap (widget) and a `Canvas` (app) |
| [`backup/BackupStore.kt`](android/app/src/main/kotlin/com/kropi/hydration/backup/BackupStore.kt) | Copies in `Download/Kropi/Kopie` via MediaStore, daily copy rotated to 14; format in `BackupFormat.kt` |
| [`update/UpdateManager.kt`](android/app/src/main/kotlin/com/kropi/hydration/update/UpdateManager.kt) | Updates from GitHub releases: download, SHA-256, package/version/signature check, system installer |
| [`export/HydrationExportProvider.kt`](android/app/src/main/kotlin/com/kropi/hydration/export/HydrationExportProvider.kt) | Read-only provider for Szpila, guarded by a signature-level permission |

```bash
cd android
./gradlew :app:assembleRelease
./gradlew :app:testReleaseUnitTest
adb install -r app/build/outputs/apk/release/app-release.apk
```

JDK 21 and Android SDK 36 required. **Signing:** `android/keystore.properties`
(git-ignored; `storeFile`, `storePassword`, `keyAlias`, `keyPassword`) — the same
key as Szpila's, because the `READ_HYDRATION` permission is `signature`-level and
Szpila gets no data without a matching signature. Without that file the build
falls back to the debug key (it installs, but won't talk to Szpila). One key for
all releases also means updates install without uninstalling first.

Web mockup: `bun install && bun run dev`.

</details>
