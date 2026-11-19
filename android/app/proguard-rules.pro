# Czytelne ślady stosu z wydania (razem z mapping.txt z builda).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Akcje widgetu Glance są zapisywane w PendingIntentach jako NAZWA klasy i odtwarzane refleksją
# (ActionCallbackBroadcastReceiver) — zmiana nazwy przez R8 rozwaliłaby każde dotknięcie widgetu.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }

# Zadania WorkManagera tworzone przez nazwę klasy (przypomnienia, północ, cofanie, sesje Glance).
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }

# Nazwy napojów, tonów i stylów trafiają do DataStore i kopii zapasowych jako tekst — ich stałe muszą przetrwać.
-keepclassmembers enum com.kropi.hydration.** { *; }
