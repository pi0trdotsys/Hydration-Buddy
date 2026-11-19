package com.kropi.hydration.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import com.kropi.hydration.data.HealthMapping
import com.kropi.hydration.data.Intake
import com.kropi.hydration.data.WorkoutSession
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Cienka warstwa nad klientem Health Connect — każde wywołanie jest „best effort" i nie rzuca. */
object HealthConnect {

    val PERMISSIONS: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    enum class Availability { AVAILABLE, NEEDS_INSTALL_OR_UPDATE, UNSUPPORTED }

    fun availability(context: Context): Availability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.NEEDS_INSTALL_OR_UPDATE
        else -> Availability.UNSUPPORTED
    }

    private fun client(context: Context): HealthConnectClient? =
        if (availability(context) == Availability.AVAILABLE) HealthConnectClient.getOrCreate(context) else null

    suspend fun grantedPermissions(context: Context): Set<String> =
        runCatching { client(context)?.permissionController?.getGrantedPermissions() }.getOrNull().orEmpty()

    suspend fun canWriteHydration(context: Context): Boolean =
        HealthPermission.getWritePermission(HydrationRecord::class) in grantedPermissions(context)

    suspend fun canReadProfile(context: Context): Boolean = grantedPermissions(context).containsAll(
        setOf(HealthPermission.getReadPermission(WeightRecord::class), HealthPermission.getReadPermission(StepsRecord::class)),
    )

    suspend fun canReadWorkouts(context: Context): Boolean =
        HealthPermission.getReadPermission(ExerciseSessionRecord::class) in grantedPermissions(context)

    /** Treningi, które zaczęły się dziś (od północy czasu lokalnego), od najstarszego. */
    suspend fun todaysWorkouts(context: Context): List<WorkoutSession> = runCatching {
        val c = client(context) ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant()
        val result = c.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, Instant.now().plusSeconds(60)),
                ascendingOrder = true,
                pageSize = 50,
            ),
        )
        result.records.map { WorkoutSession(it.metadata.id, it.startTime.toEpochMilli(), it.endTime.toEpochMilli(), it.title) }
    }.getOrDefault(emptyList())

    /** Ostatnia waga z ostatnich 30 dni (tyle HC pozwala czytać wstecz) albo null. */
    suspend fun latestWeightKg(context: Context): Float? = runCatching {
        val c = client(context) ?: return null
        val now = Instant.now()
        val result = c.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(now.minus(Duration.ofDays(30)), now),
                ascendingOrder = false,
                pageSize = 1,
            ),
        )
        HealthMapping.sanitizeWeight(result.records.firstOrNull()?.weight?.inKilograms)
    }.getOrNull()

    /** Średnia dzienna liczba kroków z ostatnich [days] pełnych dni (bez dzisiaj) albo null, gdy brak danych. */
    suspend fun averageDailySteps(context: Context, days: Int = 7): Int? = runCatching {
        val c = client(context) ?: return null
        val zone = ZoneId.systemDefault()
        val end = LocalDate.now().atStartOfDay(zone).toInstant()
        val start = end.minus(Duration.ofDays(days.toLong()))
        val total = c.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(start, end)))[StepsRecord.COUNT_TOTAL]
        total?.takeIf { it > 0 }?.let { (it / days).toInt() }
    }.getOrNull()

    /**
     * Zapisuje wpisy jako nawodnienie. Klucz [clientRecordId] sprawia, że ponowny zapis tego samego
     * wpisu nadpisuje stary, a nie dubluje; wersja = czas zapisu, żeby poprawka wygrywała ze starą wartością.
     */
    suspend fun upsertHydration(context: Context, intakes: List<Intake>): Boolean = runCatching {
        val c = client(context) ?: return false
        val zone = ZoneId.systemDefault()
        val version = System.currentTimeMillis()
        val records = intakes.mapNotNull { intake ->
            val ml = HealthMapping.hydrationMl(intake.drink, intake.ml)
            if (ml <= 0) return@mapNotNull null
            val start = LocalDate.ofEpochDay(intake.epochDay).atTime(intake.hour, intake.minute).atZone(zone)
            HydrationRecord(
                startTime = start.toInstant(),
                startZoneOffset = start.offset,
                endTime = start.toInstant().plusSeconds(60),
                endZoneOffset = start.offset,
                volume = Volume.milliliters(ml.toDouble()),
                metadata = Metadata.manualEntry(clientRecordId = clientId(intake.id), clientRecordVersion = version),
            )
        }
        if (records.isNotEmpty()) c.insertRecords(records)
        true
    }.getOrDefault(false)

    suspend fun deleteHydration(context: Context, intakeIds: Collection<Long>): Boolean = runCatching {
        val c = client(context) ?: return false
        if (intakeIds.isNotEmpty()) {
            c.deleteRecords(HydrationRecord::class, recordIdsList = emptyList(), clientRecordIdsList = intakeIds.map { clientId(it) })
        }
        true
    }.getOrDefault(false)

    private fun clientId(intakeId: Long) = "kropi-$intakeId"
}
