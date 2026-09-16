package com.lifeos.ui.screens.today

import com.lifeos.data.db.entity.DailyGoalEntity
import com.lifeos.data.db.entity.HobbyEntity
import com.lifeos.data.db.entity.JournalEntryEntity
import com.lifeos.data.db.entity.ProteinLogEntity
import com.lifeos.data.db.entity.StepReadingEntity
import com.lifeos.data.db.entity.WaterLogEntity
import java.time.LocalDate

data class TodayUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.now(),

    // ── Goals / hobbies ──────────────────────────────────────────────────────
    val goals: List<DailyGoalEntity> = emptyList(),
    val completions: Map<Long, Boolean> = emptyMap(),
    val hobbies: List<HobbyEntity> = emptyList(),
    val hobbyLogs: Map<Long, Int> = emptyMap(),   // hobbyId -> minutes

    // ── Nutrition / activity totals ──────────────────────────────────────────
    val proteinGrams: Int = 0,
    val waterMl: Int = 0,
    val stepsTotal: Int = 0,

    // ── Individual log entries (for correction / deletion) ───────────────────
    val proteinLogs: List<ProteinLogEntity> = emptyList(),
    val waterLogs: List<WaterLogEntity> = emptyList(),
    val stepLogs: List<StepReadingEntity> = emptyList(),

    // ── Journal ──────────────────────────────────────────────────────────────
    val journalEntry: JournalEntryEntity? = null,

    // ── Placement ────────────────────────────────────────────────────────────
    val dueRevisionCount: Int = 0,

    // ── Settings ─────────────────────────────────────────────────────────────
    val displayName: String = "",
    val proteinGoal: Int = 150,
    val waterGoal: Int = 2500,
    val stepGoal: Int = 8000,

    // ── Streaks ──────────────────────────────────────────────────────────────
    val currentStreak: Int = 0,
    val gymStreak: Int = 0,
)
