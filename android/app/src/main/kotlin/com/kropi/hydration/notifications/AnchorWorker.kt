package com.kropi.hydration.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kropi.hydration.R
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.formatMl
import java.util.concurrent.TimeUnit

/**
 * „Po kawie szklanka wody": kawa albo herbata uruchamia odliczanie [DELAY_MINUTES] minut, a jeśli do tego
 * czasu nie wpadła woda, przychodzi krótkie przypomnienie. Powiązanie nowego nawyku ze starym
 * (kawa → woda) działa lepiej niż przypomnienia o dowolnej porze.
 */
class AnchorWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val coffeeMinute = inputData.getInt(KEY_MINUTE, -1)
        if (coffeeMinute < 0) return Result.success()
        val repo = HydrationRepository(applicationContext)
        val state = repo.current()
        if (!state.settings.anchorAfterCoffee) return Result.success()
        if (!Nudges.mayNudge(applicationContext, repo, state)) return Result.success()
        // Woda wypita po kawie wystarcza — nie przypominamy o tym, co już zrobione.
        val waterSince = state.intakes.filter { it.drink == Drink.WATER && it.minuteOfDay >= coffeeMinute }.sumOf { it.ml }
        if (waterSince >= MIN_WATER_ML) return Result.success()
        val ml = state.settings.reminderGlassMl
        Nudges.send(
            applicationContext, state,
            applicationContext.getString(R.string.n_anchor_coffee_title),
            applicationContext.getString(R.string.n_anchor_coffee_text, formatMl(ml)),
            ml,
        )
        return Result.success()
    }

    companion object {
        const val DELAY_MINUTES = 20L
        private const val MIN_WATER_ML = 150
        private const val KEY_MINUTE = "coffee_minute"
        private const val WORK_NAME = "anchor_after_coffee"

        fun scheduleAfterCoffee(context: Context, coffeeMinuteOfDay: Int) {
            val request = OneTimeWorkRequestBuilder<AnchorWorker>()
                .setInitialDelay(DELAY_MINUTES, TimeUnit.MINUTES)
                .setInputData(Data.Builder().putInt(KEY_MINUTE, coffeeMinuteOfDay).build())
                .build()
            // REPLACE: druga kawa przesuwa przypomnienie, zamiast dawać dwa.
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
