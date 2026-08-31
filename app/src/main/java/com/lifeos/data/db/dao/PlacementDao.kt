package com.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifeos.data.db.entity.MonthlyRevisionListEntity
import com.lifeos.data.db.entity.ProblemEntity
import com.lifeos.data.db.entity.SpacedRevisionEntity
import com.lifeos.data.db.entity.WeeklyRevisionListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlacementDao {

    // ─ Problems ─────────────────────────────────────────────────────────────

    @Query("SELECT * FROM problems ORDER BY solvedDate DESC")
    fun observeAllProblems(): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE id = :id LIMIT 1")
    suspend fun getProblem(id: Long): ProblemEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProblem(problem: ProblemEntity): Long

    @Update
    suspend fun updateProblem(problem: ProblemEntity)

    @Query("DELETE FROM problems WHERE id = :id")
    suspend fun deleteProblem(id: Long)

    // ─ Spaced revisions ───────────────────────────────────────────────────

    @Query("SELECT * FROM spaced_revisions WHERE dueDate <= :today ORDER BY dueDate ASC")
    fun observeDueRevisions(today: String): Flow<List<SpacedRevisionEntity>>

    @Query("SELECT * FROM spaced_revisions WHERE problemId = :problemId LIMIT 1")
    suspend fun getRevisionForProblem(problemId: Long): SpacedRevisionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRevision(revision: SpacedRevisionEntity): Long

    @Update
    suspend fun updateRevision(revision: SpacedRevisionEntity)

    /** Convenience — insert first, update if already exists (by primary key). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRevision(revision: SpacedRevisionEntity): Long

    @Query("DELETE FROM spaced_revisions WHERE problemId = :problemId")
    suspend fun deleteRevisionByProblemId(problemId: Long)

    // ─ Weekly revision lists ───────────────────────────────────────────────

    @Query("SELECT * FROM weekly_revision_lists ORDER BY weekStart DESC")
    fun observeWeeklyLists(): Flow<List<WeeklyRevisionListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeeklyList(list: WeeklyRevisionListEntity)

    // ─ Monthly revision lists ──────────────────────────────────────────────

    @Query("SELECT * FROM monthly_revision_lists ORDER BY month DESC")
    fun observeMonthlyLists(): Flow<List<MonthlyRevisionListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonthlyList(list: MonthlyRevisionListEntity)
}
