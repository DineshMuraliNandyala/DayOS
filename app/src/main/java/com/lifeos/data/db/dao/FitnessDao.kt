package com.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifeos.data.db.entity.ExerciseEntity
import com.lifeos.data.db.entity.ExerciseSetLogEntity
import com.lifeos.data.db.entity.ProteinLogEntity
import com.lifeos.data.db.entity.StepReadingEntity
import com.lifeos.data.db.entity.WaterLogEntity
import com.lifeos.data.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FitnessDao {

    // ─ Protein ─────────────────────────────────────────────────────────────

    @Insert
    suspend fun insertProteinLog(log: ProteinLogEntity)

    @Query("SELECT * FROM protein_logs WHERE date = :date ORDER BY loggedAt ASC")
    fun observeProteinLogsForDate(date: String): Flow<List<ProteinLogEntity>>

    @Query("SELECT COALESCE(SUM(grams), 0) FROM protein_logs WHERE date = :date")
    fun observeProteinTotalForDate(date: String): Flow<Int>

    @Query("DELETE FROM protein_logs WHERE id = :id")
    suspend fun deleteProteinLog(id: Long)

    // ─ Water ───────────────────────────────────────────────────────────────

    @Insert
    suspend fun insertWaterLog(log: WaterLogEntity)

    @Query("SELECT COALESCE(SUM(ml), 0) FROM water_logs WHERE date = :date")
    fun observeWaterTotalForDate(date: String): Flow<Int>

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteWaterLog(id: Long)

    // ─ Steps ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStepReading(reading: StepReadingEntity)

    @Query("SELECT * FROM step_readings WHERE date = :date LIMIT 1")
    fun observeStepsForDate(date: String): Flow<StepReadingEntity?>

    // ─ Exercises ───────────────────────────────────────────────────────────

    @Query("SELECT * FROM exercises WHERE weekday = :weekday AND archived = 0 ORDER BY \"order\" ASC")
    fun observeExercisesForWeekday(weekday: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY weekday, \"order\" ASC")
    fun observeAllActiveExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getExercise(id: Long): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: ExerciseEntity): Long

    @Query("UPDATE exercises SET archived = 1 WHERE id = :id")
    suspend fun archiveExercise(id: Long)

    // ─ Set logs ────────────────────────────────────────────────────────────

    @Insert
    suspend fun insertSetLog(setLog: ExerciseSetLogEntity): Long

    @Query("SELECT * FROM exercise_set_logs WHERE date = :date ORDER BY exerciseId, setNumber ASC")
    fun observeSetLogsForDate(date: String): Flow<List<ExerciseSetLogEntity>>

    @Query("SELECT * FROM exercise_set_logs WHERE exerciseId = :exerciseId ORDER BY date DESC, setNumber ASC LIMIT 30")
    suspend fun getRecentSetLogsForExercise(exerciseId: Long): List<ExerciseSetLogEntity>

    @Query("DELETE FROM exercise_set_logs WHERE id = :id")
    suspend fun deleteSetLog(id: Long)

    // ─ Workout sessions ────────────────────────────────────────────────────

    @Insert
    suspend fun insertWorkoutSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateWorkoutSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutSession(session: WorkoutSessionEntity): Long

    @Query("SELECT * FROM workout_sessions WHERE date = :date LIMIT 1")
    fun observeSessionForDate(date: String): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE date = :date LIMIT 1")
    suspend fun getSessionForDate(date: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC LIMIT 10")
    fun observeRecentSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteWorkoutSession(id: Long)
}
