package com.kropi.hydration.notifications

import android.content.Context
import com.kropi.hydration.widget.updateAllWidgets
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kropi.hydration.data.HydrationRepository
import com.kropi.hydration.data.MidnightSchedule
import java.util.concurrent.TimeUnit

/**
 * Tuż po północy zamyka wczorajszy dzień (cel ląduje w historii, łyki zostają w bazie)
 * i odświeża widgety, żeby od 00:00 pokazywały pusty dzień, a nie wczorajszą sumę.
 * Bez tego rozliczenie czekało na pierwszy odczyt: kolejne cykliczne sprawdzenie
 * (do 15 minut) albo otwarcie aplikacji.
 */
class MidnightRollWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = HydrationRepository(applicationContext)
        repo.current()
        // Nowy dzień = świeża prognoza upału i czysty licznik treningu.
        repo.refreshOutdoor(force = true)
        repo.syncWorkouts()
        updateAllWidgets(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "hydration_midnight_roll"

        /**
         * Praca okresowa co 24 h z opóźnieniem do najbliższej północy. Zmiana czasu
         * przesuwa ją o godzinę dwa razy w roku — wtedy i tak zadziała 15-minutowy tik.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MidnightRollWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(MidnightSchedule.delayMs(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
