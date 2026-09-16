package com.lifeos.ui.screens.fitness

import com.lifeos.data.db.entity.ExerciseEntity
import com.lifeos.data.db.entity.ExerciseSetLogEntity
import com.lifeos.data.db.entity.WorkoutSessionEntity
import java.time.LocalDate

/** Pairs an exercise with its logged sets for a specific date */
data class ExerciseWithSets(
    val exercise: ExerciseEntity,
    val sets: List<ExerciseSetLogEntity> = emptyList(),
)

data class FitnessUiState(
    val isLoading: Boolean = true,

    // ── Log tab ───────────────────────────────────────────────────────────────
    val selectedLogDate: LocalDate = LocalDate.now(),
    val activeSession: WorkoutSessionEntity? = null,
    val exercisesWithSets: List<ExerciseWithSets> = emptyList(),

    // ── Programme tab ─────────────────────────────────────────────────────────
    /** All non-archived exercises grouped by weekday key ("mon"…"sun") */
    val programmeByDay: Map<String, List<ExerciseEntity>> = emptyMap(),

    // ── Week view (Log tab header) ────────────────────────────────────────────
    val weekSessions: List<WorkoutSessionEntity> = emptyList(),

    // ── Draft set being entered ───────────────────────────────────────────────
    val draftWeightKg: String = "",
    val draftReps: String = "",

    // ── PRs ───────────────────────────────────────────────────────────────────
    /** Map exerciseId -> latest 1RM PR (Epley) */
    val latestPrs: Map<Long, Double> = emptyMap(),
)
