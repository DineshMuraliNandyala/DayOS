package com.lifeos.ui.screens.fitness

import com.lifeos.data.db.entity.ExerciseEntity
import com.lifeos.data.db.entity.ExerciseSetLogEntity
import com.lifeos.data.db.entity.WorkoutSessionEntity
import java.time.LocalDate

/** One exercise card: definition + all sets logged today. */
data class ExerciseWithSets(
    val exercise: ExerciseEntity,
    val sets: List<ExerciseSetLogEntity> = emptyList(),
)

/** Form state for the Add/Edit exercise bottom sheet. */
data class AddExerciseState(
    val id: Long = 0L,
    val name: String = "",
    val muscleGroup: String = "",
    val weekday: String = "mon",
    val targetSets: String = "3",
    val targetReps: String = "8-12",
    val notes: String = "",
) {
    val isEditing: Boolean get() = id != 0L
    val isValid: Boolean get() = name.isNotBlank() && weekday.isNotBlank()
}

/** Form state for logging a single set inline on an exercise card. */
data class LogSetState(
    val weightKg: String = "",
    val reps: String = "",
) {
    val isValid: Boolean
        get() = weightKg.toDoubleOrNull() != null && reps.toIntOrNull()?.let { it > 0 } == true
}

data class FitnessUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val todayWeekday: String = "",            // "mon" … "sun"
    val exercisesWithSets: List<ExerciseWithSets> = emptyList(),
    val session: WorkoutSessionEntity? = null,
    val sessionActive: Boolean = false,
    val sessionStartedAt: String? = null,
    val stepsTaken: Int = 0,
    val stepGoal: Int = 8000,
    val recentSessions: List<WorkoutSessionEntity> = emptyList(),
)
