package com.lifeos.ui.screens.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeos.data.db.LifeOSDatabase
import com.lifeos.data.db.entity.ExerciseEntity
import com.lifeos.data.db.entity.ExerciseSetLogEntity
import com.lifeos.data.db.entity.StepReadingEntity
import com.lifeos.data.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FitnessViewModel(private val db: LifeOSDatabase) : ViewModel() {

    private val dao = db.fitnessDao()
    private val settingsDao = db.settingsDao()

    val today: LocalDate = LocalDate.now()
    val todayStr: String = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    /** "mon" … "sun" — three-letter lowercase weekday key. */
    val weekdayKey: String = today.dayOfWeek.name.take(3).lowercase()

    // ── Source flows ──────────────────────────────────────────────────────────

    private val exercisesFlow = dao.observeExercisesForWeekday(weekdayKey)
    private val setLogsFlow = dao.observeSetLogsForDate(todayStr)
    private val sessionFlow = dao.observeSessionForDate(todayStr)
    private val stepsFlow = dao.observeStepsForDate(todayStr)
    private val recentSessionsFlow = dao.observeRecentSessions()
    private val settingsFlow = settingsDao.observe()

    // ── UI state ──────────────────────────────────────────────────────────────

    val uiState: StateFlow<FitnessUiState> = combine(
        exercisesFlow,
        setLogsFlow,
        sessionFlow,
        stepsFlow,
        recentSessionsFlow,
    ) { exercises, setLogs, session, stepReading, recentSessions ->

        // Map each exercise to its logged sets today
        val setsByExercise = setLogs.groupBy { it.exerciseId }
        val exercisesWithSets = exercises.map { exercise ->
            ExerciseWithSets(
                exercise = exercise,
                sets = setsByExercise[exercise.id] ?: emptyList(),
            )
        }

        FitnessUiState(
            isLoading = false,
            today = today,
            todayWeekday = weekdayKey,
            exercisesWithSets = exercisesWithSets,
            session = session,
            sessionActive = session != null && session.completedAt == null,
            sessionStartedAt = session?.startedAt,
            stepsTaken = stepReading?.steps ?: 0,
            recentSessions = recentSessions,
        )
    }
        .combine(settingsFlow) { s, settings ->
            s.copy(stepGoal = settings?.stepGoal ?: 8000)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FitnessUiState(),
        )

    // ── Workout session lifecycle ──────────────────────────────────────────────

    fun startWorkout() {
        viewModelScope.launch {
            val existing = dao.getSessionForDate(todayStr)
            if (existing == null) {
                dao.insertWorkoutSession(
                    WorkoutSessionEntity(
                        date = todayStr,
                        weekday = weekdayKey,
                        startedAt = Instant.now().toString(),
                    ),
                )
            }
            // If it exists but was completed, leave it (user can review history)
        }
    }

    fun finishWorkout() {
        viewModelScope.launch {
            val session = dao.getSessionForDate(todayStr) ?: return@launch
            // Compute total volume from today's cached UI state
            val setLogs = uiState.value.exercisesWithSets.flatMap { it.sets }
            val totalVolume = setLogs.sumOf { it.weightKg * it.reps }
            val prCount = setLogs.count { it.isPr }
            val started = Instant.parse(session.startedAt)
            val durationMinutes = ((Instant.now().toEpochMilli() - started.toEpochMilli()) / 60_000).toInt()

            dao.updateWorkoutSession(
                session.copy(
                    completedAt = Instant.now().toString(),
                    durationMinutes = durationMinutes,
                    totalVolumeKg = totalVolume,
                    newPrCount = prCount,
                ),
            )
        }
    }

    // ── Set logging ───────────────────────────────────────────────────────────

    /**
     * Logs a set for [exerciseId]. Automatically detects PRs:
     * if [weightKg] × [reps] (1RM via Epley formula) exceeds [ExerciseEntity.bestPrKg],
     * marks the set as a PR and updates both [currentPrKg] and [bestPrKg].
     */
    fun logSet(exerciseId: Long, weightKg: Double, reps: Int) {
        viewModelScope.launch {
            val exercise = dao.getExercise(exerciseId) ?: return@launch
            val todaySets = uiState.value.exercisesWithSets
                .find { it.exercise.id == exerciseId }?.sets ?: emptyList()
            val setNumber = todaySets.size + 1

            // Epley 1RM estimate: weight × (1 + reps/30)
            val estimated1rm = weightKg * (1.0 + reps / 30.0)
            val isPr = estimated1rm > (exercise.bestPrKg ?: 0.0)

            dao.insertSetLog(
                ExerciseSetLogEntity(
                    exerciseId = exerciseId,
                    date = todayStr,
                    setNumber = setNumber,
                    weightKg = weightKg,
                    reps = reps,
                    isPr = isPr,
                ),
            )

            // Update exercise PR if beaten
            if (isPr) {
                dao.updateExercise(
                    exercise.copy(
                        currentPrKg = weightKg,
                        bestPrKg = estimated1rm,
                    ),
                )
            }
        }
    }

    fun deleteSet(setLogId: Long) {
        viewModelScope.launch { dao.deleteSetLog(setLogId) }
    }

    // ── Steps (manual entry only in prod flavor) ───────────────────────────────

    fun logSteps(steps: Int) {
        viewModelScope.launch {
            dao.upsertStepReading(
                StepReadingEntity(
                    date = todayStr,
                    steps = steps,
                    source = "manual",
                ),
            )
        }
    }

    // ── Exercise CRUD ─────────────────────────────────────────────────────────

    fun addExercise(state: AddExerciseState) {
        viewModelScope.launch {
            dao.insertExercise(
                ExerciseEntity(
                    name = state.name.trim(),
                    muscleGroup = state.muscleGroup.trim(),
                    weekday = state.weekday,
                    targetSets = state.targetSets.toIntOrNull() ?: 3,
                    targetReps = state.targetReps.ifBlank { "8-12" },
                    notes = state.notes.ifBlank { null },
                    order = 0,
                    createdAt = Instant.now().toString(),
                ),
            )
        }
    }

    fun updateExercise(state: AddExerciseState) {
        viewModelScope.launch {
            val existing = dao.getExercise(state.id) ?: return@launch
            dao.updateExercise(
                existing.copy(
                    name = state.name.trim(),
                    muscleGroup = state.muscleGroup.trim(),
                    weekday = state.weekday,
                    targetSets = state.targetSets.toIntOrNull() ?: existing.targetSets,
                    targetReps = state.targetReps.ifBlank { existing.targetReps },
                    notes = state.notes.ifBlank { null },
                ),
            )
        }
    }

    fun archiveExercise(id: Long) {
        viewModelScope.launch { dao.archiveExercise(id) }
    }

    fun exerciseToEditState(exercise: ExerciseEntity): AddExerciseState =
        AddExerciseState(
            id = exercise.id,
            name = exercise.name,
            muscleGroup = exercise.muscleGroup,
            weekday = exercise.weekday,
            targetSets = exercise.targetSets.toString(),
            targetReps = exercise.targetReps,
            notes = exercise.notes ?: "",
        )

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val db: LifeOSDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FitnessViewModel(db) as T
    }
}
