package com.kropi.hydration.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat

/**
 * Kiedy przypomnienie ma zaczekać: gdy system ma włączone „Nie przeszkadzać" albo trwa spotkanie
 * z kalendarza. Przypomnienie nie przepada — następny 15-minutowy tik sprawdza to samo i odezwie się
 * po spotkaniu.
 */
object ReminderGate {
    enum class Reason(private val labelPl: String, private val labelEn: String) {
        DND("Nie przeszkadzać", "Do not disturb"),
        MEETING("spotkanie w kalendarzu", "a calendar meeting");

        val label: String get() = com.kropi.hydration.data.tr(labelPl, labelEn)
    }

    fun holdReason(dndActive: Boolean, inMeeting: Boolean, respectDnd: Boolean, quietDuringMeetings: Boolean): Reason? = when {
        respectDnd && dndActive -> Reason.DND
        quietDuringMeetings && inMeeting -> Reason.MEETING
        else -> null
    }

    /** Wydarzenie wyciszające przypomnienia: wpis „zajęty", nie całodniowy i niewycofany przez użytkownika. */
    fun isBusyMeeting(allDay: Boolean, availability: Int, selfStatus: Int): Boolean =
        !allDay &&
            availability == CalendarContract.Events.AVAILABILITY_BUSY &&
            selfStatus != CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED

    fun isDndActive(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return when (manager.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL, NotificationManager.INTERRUPTION_FILTER_UNKNOWN -> false
            else -> true
        }
    }

    fun hasCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Czy w tej chwili trwa wydarzenie z widocznego kalendarza. Bez zgody albo przy błędzie — false. */
    fun isInMeeting(context: Context, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!hasCalendarPermission(context)) return false
        val projection = arrayOf(
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.AVAILABILITY,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
        )
        return runCatching {
            CalendarContract.Instances.query(context.contentResolver, projection, nowMs, nowMs + 1).use { cursor ->
                var busy = false
                while (cursor.moveToNext()) {
                    if (isBusyMeeting(cursor.getInt(0) == 1, cursor.getInt(1), cursor.getInt(2))) { busy = true; break }
                }
                busy
            }
        }.getOrDefault(false)
    }

    /** Aktualny powód wstrzymania albo null — to samo sprawdza worker i podgląd w Ustawieniach. */
    fun currentHold(context: Context, respectDnd: Boolean, quietDuringMeetings: Boolean): Reason? =
        holdReason(
            dndActive = respectDnd && isDndActive(context),
            inMeeting = quietDuringMeetings && isInMeeting(context),
            respectDnd = respectDnd,
            quietDuringMeetings = quietDuringMeetings,
        )
}
