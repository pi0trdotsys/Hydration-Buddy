# Wydawanie Kropi

Dwa przepływy w `.github/workflows`:

- **CI** (`ci.yml`) — przy każdym pushu do `main` i w każdym PR: testy jednostkowe (razem z testami
  interfejsu na Robolectric), lint i build debug. Po wejściu do `main` odpala się też job z testami na
  emulatorze (API 34) — baza, kopie zapasowe, cofanie, akcje widgetu.
- **Wydanie** (`release.yml`) — po wypchnięciu tagu `vX.Y.Z` buduje podpisany APK i dołącza go do
  wydania (tworzy je, jeśli jeszcze nie istnieje). Można też odpalić ręcznie z podanym tagiem.

## Jednorazowa konfiguracja sekretów

Kropi **musi** być podpisane tym samym kluczem co Szpila — inaczej Szpila nie dostanie danych
(uprawnienie na poziomie podpisu). Workflow wydania odmówi publikacji, jeśli odcisk klucza się
nie zgadza.

W repozytorium: *Settings → Secrets and variables → Actions → New repository secret*:

| Sekret | Wartość |
| --- | --- |
| `KEYSTORE_BASE64` | plik `.jks` zakodowany w base64: `base64 -i szpila.jks \| pbcopy` |
| `KEYSTORE_PASSWORD` | hasło do keystore (`storePassword` z `android/keystore.properties`) |
| `KEY_ALIAS` | alias klucza (`keyAlias`) |
| `KEY_PASSWORD` | hasło klucza (`keyPassword`) |

Opcjonalnie zmienna (nie sekret) `EXPECTED_CERT_SHA256` z odciskiem SHA-256 certyfikatu; domyślnie jest
wpisany odcisk klucza Szpili (`94148ac3…`).

Klucz nigdy nie trafia do repozytorium — lokalnie leży w `android/keystore.properties` (poza gitem).

## Wydanie krok po kroku

1. Podbij `versionCode` i `versionName` w `android/app/build.gradle.kts`, zrób commit i wypchnij `main`.
2. `git tag v1.10.1 && git push origin v1.10.1`
3. Poczekaj na zielony bieg „Wydanie" — APK pojawi się w wydaniu jako `kropi-hydration-v1.10.1.apk`.
   Opis wydania można potem dopisać ręcznie albo wskazać własny przy `gh release create`.

## Lokalnie

```bash
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug          # to samo co CI
./gradlew connectedDebugAndroidTest                          # testy na urządzeniu/emulatorze
```
