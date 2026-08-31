package com.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeos.data.db.entity.DayCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {

    @Query("SELECT * FROM day_completions ORDER BY date DESC")
    fun observeAll(): Flow<List<DayCompletionEntity>>

    @Query("SELECT * FROM day_completions WHERE date >= :fromDate ORDER BY date ASC")
    fun observeSince(fromDate: String): Flow<List<DayCompletionEntity>>

    @Query("SELECT * FROM day_completions WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): DayCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DayCompletionEntity)

    @Query("SELECT AVG(daily_total) FROM (SELECT date, SUM(grams) AS daily_total FROM protein_logs WHERE date >= :fromDate GROUP BY date)")
    suspend fun avgDailyProtein(fromDate: String): Double?

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE date >= :fromDate AND completedAt IS NOT NULL")
    suspend fun completedWorkoutCount(fromDate: String): Int

    @Query("SELECT COUNT(*) FROM problems WHERE solvedDate >= :fromDate")
    suspend fun problemsSolvedSince(fromDate: String): Int

    @Query("SELECT AVG(daily_steps) FROM (SELECT date, SUM(steps) AS daily_steps FROM step_readings WHERE date >= :fromDate GROUP BY date)")
    suspend fun avgDailySteps(fromDate: String): Double?

    @Query("SELECT AVG(daily_water) FROM (SELECT date, SUM(ml) AS daily_water FROM water_logs WHERE date >= :fromDate GROUP BY date)")
    suspend fun avgDailyWaterMl(fromDate: String): Double?

    @Query("SELECT COUNT(*) FROM spaced_revisions WHERE lastReviewedAt >= :fromDate")
    suspend fun revisionsCompletedSince(fromDate: String): Int

    @Query("SELECT * FROM day_completions WHERE date >= :fromDate AND date <= :toDate ORDER BY date ASC")
    fun observeRange(fromDate: String, toDate: String): Flow<List<DayCompletionEntity>>
}
