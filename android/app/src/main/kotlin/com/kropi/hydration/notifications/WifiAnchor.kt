package com.kropi.hydration.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.kropi.hydration.R
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.formatMl
import com.kropi.hydration.data.Habits
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * „Po powrocie do domu": gdy telefon połączy się z zapisaną domową siecią Wi-Fi, Kropi przypomina o szklance
 * wody. Android nie pozwala czytać nazwy sieci bez zgody na lokalizację, dlatego jest ona potrzebna przy
 * ustawianiu i przy rozpoznawaniu sieci; w tle niektóre telefony wymagają „Zawsze" — wtedy przypomnienie
 * po prostu nie przychodzi (aplikacja niczego nie zgaduje).
 */
object WifiAnchor {
    private const val REQUEST_CODE = 70

    /** Przypomnienie z tego powodu najwyżej raz na tyle minut. */
    const val COOLDOWN_MINUTES = 180

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Nazwa sieci bez cudzysłowów albo null, gdy nie ma Wi-Fi, brak zgody albo Android ukrywa nazwę. */
    @Suppress("DEPRECATION")
    fun currentSsid(context: Context): String? {
        if (!hasLocationPermission(context)) return null
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val raw = runCatching { wifi.connectionInfo?.ssid }.getOrNull() ?: return null
        return normalize(raw)
    }

    fun normalize(raw: String?): String? {
        val ssid = raw?.trim()?.removeSurrounding("\"")?.trim().orEmpty()
        return ssid.takeIf { it.isNotEmpty() && it != "<unknown ssid>" && it != "0x" }
    }

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE,
        Intent(context, WifiAnchorReceiver::class.java),
        // System dokłada do intencji dane sieci, więc musi być zmienialna; adres jest jawny (nasz odbiornik).
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    /** Włącza albo wyłącza nasłuch zmian sieci Wi-Fi zależnie od tego, czy jest zapisana sieć domowa. */
    fun sync(context: Context, ssid: String) {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        runCatching {
            if (ssid.isBlank()) {
                cm.unregisterNetworkCallback(pending(context))
            } else {
                cm.registerNetworkCallback(
                    NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
                    pending(context),
                )
            }
        }
    }
}

private const val TAG = "KropiWifi"

/** Co zrobiło ostatnie sprawdzenie sieci — do logu i do testów. */
enum class WifiOutcome { NO_HOME, OTHER_NETWORK, HELD, COOLDOWN, RECENT_SIP, SENT }

/**
 * Logika „po powrocie do domu", oddzielona od odbiornika, żeby dało się ją sprawdzić bez prawdziwej zmiany
 * sieci. [ssidNow] to nazwa aktualnej sieci (domyślnie odczyt z systemu).
 */
suspend fun handleWifiChange(
    context: Context,
    repo: HydrationRepository = HydrationRepository(context),
    ssidNow: () -> String? = { WifiAnchor.currentSsid(context) },
): WifiOutcome {
    val state = repo.current()
    val home = WifiAnchor.normalize(state.settings.homeWifiSsid)
    if (home == null) {
        WifiAnchor.sync(context, "")
        return WifiOutcome.NO_HOME
    }
    if (ssidNow() != home) return WifiOutcome.OTHER_NETWORK
    if (!Nudges.mayNudge(context, repo, state)) return WifiOutcome.HELD
    val epochMinute = System.currentTimeMillis() / 60_000L
    val last = repo.lastNudge("wifi")?.toLongOrNull() // epoch-minuta ostatniego przypomnienia
    if (last != null && epochMinute - last in 0 until WifiAnchor.COOLDOWN_MINUTES) return WifiOutcome.COOLDOWN
    val since = repo.minutesSinceLastIntake()
    if (since != null && since < Habits.RECENT_SIP_MINUTES) return WifiOutcome.RECENT_SIP
    repo.markNudge("wifi", epochMinute.toString())
    val ml = state.settings.reminderGlassMl
    Nudges.send(
        context, state,
        context.getString(R.string.n_anchor_home_title),
        context.getString(R.string.n_anchor_home_text, formatMl(ml), formatMl(state.total), formatMl(state.goal)),
        ml,
    )
    return WifiOutcome.SENT
}

class WifiAnchorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val outcome = handleWifiChange(context)
                Log.d(TAG, "Zmiana sieci: $outcome")
            } finally {
                pending.finish()
            }
        }
    }
}
