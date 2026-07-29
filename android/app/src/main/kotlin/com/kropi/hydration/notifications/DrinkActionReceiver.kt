package com.kropi.hydration.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import androidx.glance.appwidget.updateAll
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.widget.HydrationWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles taps on the notification's "Wypiłem" / "Za 20 min" actions without opening the app. */
class DrinkActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_DRUNK = "com.kropi.hydration.action.MARK_DRUNK"
        const val ACTION_SNOOZE = "com.kropi.hydration.action.SNOOZE"
        const val EXTRA_ML = "extra_ml"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = HydrationRepository(context)
                when (intent.action) {
                    ACTION_MARK_DRUNK -> {
                        val ml = intent.getIntExtra(EXTRA_ML, 250)
                        repo.addWater(ml)
                        HydrationWidget().updateAll(context)
                    }
                    ACTION_SNOOZE -> {
                        repo.snoozeFor(20)
                    }
                }
                context.getSystemService<NotificationManager>()
                    ?.cancel(NotificationHelper.REMINDER_NOTIFICATION_ID)
            } finally {
                pending.finish()
            }
        }
    }
}
