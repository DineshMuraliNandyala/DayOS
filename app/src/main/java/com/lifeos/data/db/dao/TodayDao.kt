package com.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifeos.data.db.entity.DailyGoalCompletionEntity
import com.lifeos.data.db.entity.DailyGoalEntity
import com.lifeos.data.db.entity.HobbyEntity
import com.lifeos.data.db.entity.HobbyLogEntity
import com.lifeos.data.db.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodayDao {

    // ─ Daily goals ───────────────────────────────────────────────────────────

    /** All non-archived goals. Filter by weekday in the ViewModel. */
    @Query("SELECT * FROM daily_goals WHERE archived = 0")
    fun observeActiveGoals(): Flow<List<DailyGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGoal(goal: DailyGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: DailyGoalEntity)

    // ─ Goal completions ───────────────────────────────────────────────────

    @Query("SELECT * FROM daily_goal_completions WHERE date = :date")
    fun observeCompletionsForDate(date: String): Flow<List<DailyGoalCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(completion: DailyGoalCompletionEntity)

    @Query("SELECT * FROM daily_goal_completions WHERE goalId = :goalId AND date = :date LIMIT 1")
    suspend fun getCompletion(goalId: Long, date: String): DailyGoalCompletionEntity?

    /** All completed dates — used by StreakUseCase. */
    @Query("SELECT DISTINCT date FROM daily_goal_completions WHERE completed = 1")
    suspend fun allCompletedDates(): List<String>

    // ─ Hobbies ─────────────────────────────────────────────────────────────

    @Query("SELECT * FROM hobbies WHERE archived = 0")
    fun observeActiveHobbies(): Flow<List<HobbyEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHobby(hobby: HobbyEntity): Long

    @Update
    suspend fun updateHobby(hobby: HobbyEntity)

    // ─ Hobby logs ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM hobby_logs WHERE date = :date")
    fun observeHobbyLogsForDate(date: String): Flow<List<HobbyLogEntity>>

    @Query("SELECT DISTINCT date FROM hobby_logs WHERE minutes > 0")
    suspend fun allLoggedDates(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHobbyLog(log: HobbyLogEntity)

    @Query("DELETE FROM hobby_logs WHERE hobbyId = :hobbyId AND date = :date")
    suspend fun deleteHobbyLogsForDate(hobbyId: Long, date: String)

    // ─ Journal ─────────────────────────────────────────────────────────────

    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    fun observeJournalEntry(date: String): Flow<JournalEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJournalEntry(entry: JournalEntryEntity)

    // ─ Protein (shown on Today tab) ─────────────────────────────────────

    @Query("SELECT COALESCE(SUM(grams), 0) FROM protein_logs WHERE date = :date")
    fun observeProteinTotalForDate(date: String): Flow<Int>

    @Insert
    suspend fun insertProteinLog(log: com.lifeos.data.db.entity.ProteinLogEntity)

    // ─ Spaced revision count (for revision banner on Today) ───────────────

    @Query("SELECT COUNT(*) FROM spaced_revisions WHERE dueDate <= :today")
    fun observeDueRevisionCount(today: String): Flow<Int>
}
