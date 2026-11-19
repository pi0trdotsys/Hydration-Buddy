package com.kropi.hydration.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.HydrationState
import java.time.LocalTime

/**
 * Krótkie przypomnienia poza planem godzinowym (przed typową przerwą, po kawie, po powrocie do domu,
 * po treningu, ratunek dla serii). Wszystkie przechodzą przez tę samą bramkę co zwykłe przypomnienia:
 * włączone przypomnienia, okno aktywności, odroczenie, „Nie przeszkadzać" i spotkania.
 */
object Nudges {

    /** Czy w tej chwili wolno cokolwiek wysłać. Cel zrealizowany też zatrzymuje szturchanie. */
    suspend fun mayNudge(context: Context, repo: HydrationRepository, state: HydrationState, nowMinute: Int = nowMinute()): Boolean {
        val s = state.settings
        if (!s.remindersEnabled) return false
        if (state.remaining <= 0) return false
        if (nowMinute < s.activeStartHour * 60 || nowMinute >= s.activeEndHour * 60) return false
        if (repo.isSnoozed()) return false
        if (ReminderGate.currentHold(context, s.respectDnd, s.quietDuringMeetings) != null) return false
        return hasPermission(context)
    }

    /** Wysyła przypomnienie; poprzednie takie samo zastępuje (jedno miejsce w szufladzie). */
    @SuppressLint("MissingPermission")
    fun send(context: Context, state: HydrationState, title: String, text: String, ml: Int) {
        if (!hasPermission(context)) return
        NotificationHelper.ensureChannel(context)
        NotificationManagerCompat.from(context).notify(
            NotificationHelper.NUDGE_NOTIFICATION_ID,
            NotificationHelper.buildNudgeNotification(context, title, text, ml, state),
        )
    }

    fun nowMinute(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
