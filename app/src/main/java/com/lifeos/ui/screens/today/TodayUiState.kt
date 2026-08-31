package com.lifeos.ui.screens.today

import com.lifeos.data.db.entity.DailyGoalEntity
import com.lifeos.data.db.entity.HobbyEntity
import com.lifeos.data.db.entity.JournalEntryEntity
import java.time.LocalDate

/**
 * Immutable snapshot of everything the Today screen needs to render.
 * Produced by [TodayViewModel.uiState] as a [StateFlow].
 */
data class TodayUiState(
    val today: LocalDate = LocalDate.now(),
    val displayName: String = "",
    val dueRevisionCount: Int = 0,

    // Goals for today's weekday (already filtered)
    val goals: List<DailyGoalEntity> = emptyList(),
    // goalId → completed flag
    val completions: Map<Long, Boolean> = emptyMap(),

    // Hobbies for today's weekday (already filtered)
    val hobbies: List<HobbyEntity> = emptyList(),
    // hobbyId → logged minutes (0 = not logged)
    val hobbyLogs: Map<Long, Int> = emptyMap(),

    val proteinGrams: Int = 0,
    val proteinGoal: Int = 150,
    val waterMl: Int = 0,
    val waterGoal: Int = 2500,

    val journalEntry: JournalEntryEntity? = null,
    val isLoading: Boolean = true,
)
