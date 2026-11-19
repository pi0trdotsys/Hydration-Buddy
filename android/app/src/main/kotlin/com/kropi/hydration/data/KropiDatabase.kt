package com.kropi.hydration.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Jeden łyk. [minuteOfDay] to godzina*60+minuta, [ml] — ile wlano do szklanki
 * (nie ile się liczy do celu: to wynika z [drink] i ustawień).
 */
@Entity(tableName = "intake", indices = [Index("epochDay")])
data class IntakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val minuteOfDay: Int,
    val ml: Int,
    val drink: String = Drink.WATER.name,
    val note: String? = null,
)

/** Cel obowiązujący w danym dniu; wiersz istnieje dla każdego zamkniętego dnia z historii. */
@Entity(tableName = "day_goal")
data class DayGoalEntity(
    @PrimaryKey val epochDay: Long,
    @ColumnInfo(name = "goal") val goal: Int,
)

@Dao
interface IntakeDao {
    @Query("SELECT * FROM intake ORDER BY epochDay, minuteOfDay, id")
    fun observeIntakes(): Flow<List<IntakeEntity>>

    @Query("SELECT * FROM day_goal ORDER BY epochDay")
    fun observeDayGoals(): Flow<List<DayGoalEntity>>

    @Query("SELECT * FROM intake WHERE epochDay = :day ORDER BY minuteOfDay, id")
    suspend fun intakesOfDay(day: Long): List<IntakeEntity>

    @Query("SELECT * FROM intake ORDER BY epochDay, minuteOfDay, id")
    suspend fun allIntakes(): List<IntakeEntity>

    @Query("SELECT * FROM day_goal ORDER BY epochDay")
    suspend fun allDayGoals(): List<DayGoalEntity>

    @Query("SELECT * FROM intake WHERE id = :id")
    suspend fun intakeById(id: Long): IntakeEntity?

    @Query("SELECT MAX(minuteOfDay) FROM intake WHERE epochDay = :day")
    suspend fun lastMinuteOfDay(day: Long): Int?

    @Query("SELECT id FROM intake WHERE epochDay = :day ORDER BY id DESC LIMIT 1")
    suspend fun newestIdOfDay(day: Long): Long?

    @Query("SELECT COUNT(*) FROM intake")
    suspend fun intakeCount(): Int

    @Query("SELECT COALESCE(SUM(ml), 0) FROM intake")
    suspend fun totalMl(): Long

    @Query("SELECT COUNT(*) FROM day_goal")
    suspend fun dayGoalCount(): Int

    @Insert
    suspend fun insert(intake: IntakeEntity): Long

    @Insert
    suspend fun insertAll(intakes: List<IntakeEntity>)

    @Update
    suspend fun update(intake: IntakeEntity)

    @Query("DELETE FROM intake WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDayGoalIfAbsent(goal: DayGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayGoals(goals: List<DayGoalEntity>)

    @Query("DELETE FROM intake")
    suspend fun clearIntakes()

    @Query("DELETE FROM day_goal")
    suspend fun clearDayGoals()

    @Query("DELETE FROM intake WHERE epochDay < :epochDay")
    suspend fun pruneIntakes(epochDay: Long)

    @Query("DELETE FROM day_goal WHERE epochDay < :epochDay")
    suspend fun pruneDayGoals(epochDay: Long)

    @Transaction
    suspend fun replaceAll(intakes: List<IntakeEntity>, goals: List<DayGoalEntity>) {
        clearIntakes()
        clearDayGoals()
        insertAll(intakes)
        insertDayGoals(goals)
    }
}

@Database(entities = [IntakeEntity::class, DayGoalEntity::class], version = 1, exportSchema = true)
abstract class KropiDatabase : RoomDatabase() {
    abstract fun dao(): IntakeDao

    companion object {
        @Volatile
        private var instance: KropiDatabase? = null

        fun get(context: Context): KropiDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, KropiDatabase::class.java, "kropi.db")
                .build()
                .also { instance = it }
        }
    }
}
