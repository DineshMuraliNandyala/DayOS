package com.lifeos.ui.screens.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeos.data.db.LifeOSDatabase
import com.lifeos.data.db.entity.ExerciseSetLogEntity
import com.lifeos.data.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalCoroutinesApi::class)
class FitnessViewModel(private val db: LifeOSDatabase) : ViewModel() {

    private val dao = db.fitnessDao()
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    // ── Selected log date ─────────────────────────────────────────────────────
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    // ── Week range for sessions ───────────────────────────────────────────────
    private val weekStartFlow = _selectedDate.map { date ->
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(fmt)
    }
    private val weekEndFlow = _selectedDate.map { date ->
        date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).format(fmt)
    }

    // ── Programme (all exercises grouped by weekday) ──────────────────────────
    private val programmeFlow = dao.observeAllActiveExercises().map { list ->
        val ordered = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
        ordered.associateWith { day -> list.filter { it.weekday == day } }
    }

    // ── Exercises for selected day ────────────────────────────────────────────
    private val exercisesForDayFlow = _selectedDate.flatMapLatest { date ->
        val weekday = date.dayOfWeek.name.take(3).lowercase()
        dao.observeExercisesForWeekday(weekday)
    }

    // ── Set logs for selected day ─────────────────────────────────────────────
    private val setLogsForDayFlow = _selectedDate.flatMapLatest { date ->
        dao.observeSetLogsForDate(date.format(fmt))
    }

    // ── Active session for selected day ───────────────────────────────────────
    private val sessionForDayFlow = _selectedDate.flatMapLatest { date ->
        dao.observeSessionForDate(date.format(fmt))
    }

    // ── Week sessions ─────────────────────────────────────────────────────────
    private val weekSessionsFlow = combine(weekStartFlow, weekEndFlow) { start, end ->
        Pair(start, end)
    }.flatMapLatest { (start, end) ->
        dao.observeSessionsForWeek(start, end)
    }

    // ── Combined UI state ─────────────────────────────────────────────────────
    val uiState: StateFlow<FitnessUiState> = combine(
        _selectedDate,
        exercisesForDayFlow,
        setLogsForDayFlow,
        sessionForDayFlow,
        programmeFlow,
    ) { date, exercises, setLogs, session, programme ->
        val setsByExercise = setLogs.groupBy { it.exerciseId }
        val exercisesWithSets = exercises.map { ex ->
            ExerciseWithSets(exercise = ex, sets = setsByExercise[ex.id] ?: emptyList())
        }
        FitnessUiState(
            isLoading = false,
            selectedLogDate = date,
            activeSession = session,
            exercisesWithSets = exercisesWithSets,
            programmeByDay = programme,
        )
    }
        .combine(weekSessionsFlow) { state, weekSessions ->
            state.copy(weekSessions = weekSessions)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FitnessUiState())

    // ── Date navigation ───────────────────────────────────────────────────────

    fun setLogDate(date: LocalDate) { _selectedDate.value = date }

    // ── Session lifecycle ─────────────────────────────────────────────────────

    fun startSession() {
        viewModelScope.launch {
            val date = _selectedDate.value
            val dateStr = date.format(fmt)
            val existing = dao.getSessionForDate(dateStr)
            if (existing == null) {
                val weekday = date.dayOfWeek.name.take(3).lowercase()
                dao.upsertWorkoutSession(
                    WorkoutSessionEntity(
                        date = dateStr,
                        weekday = weekday,
                        startedAt = Instant.now().toString(),
                    ),
                )
            }
        }
    }

    fun finishWorkout() {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(fmt)
            val session = dao.getSessionForDate(dateStr) ?: return@launch
            val allSets = dao.observeSetLogsForDate(dateStr).stateIn(
                viewModelScope, SharingStarted.Eagerly, emptyList()
            ).value
            val totalVolume = allSets.sumOf { it.weightKg * it.reps }
            val now = Instant.now().toString()

            // Persist PRs for each exercise
            val setsByExercise = allSets.groupBy { it.exerciseId }
            var prCount = 0
            setsByExercise.forEach { (exerciseId, sets) ->
                val bestOneRm = sets.maxOfOrNull { epley1RM(it.weightKg, it.reps) } ?: return@forEach
                val exercise = dao.getExercise(exerciseId) ?: return@forEach
                if (exercise.bestPrKg == null || bestOneRm > exercise.bestPrKg) {
                    dao.upsertExercise(exercise.copy(bestPrKg = bestOneRm, currentPrKg = bestOneRm))
                    prCount++
                }
            }

            dao.upsertWorkoutSession(
                session.copy(
                    completedAt = now,
                    durationMinutes = ((Instant.now().toEpochMilli() -
                        Instant.parse(session.startedAt).toEpochMilli()) / 60_000).toInt(),
                    totalVolumeKg = totalVolume,
                    newPrCount = prCount,
                ),
            )
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch { dao.deleteWorkoutSession(id) }
    }

    // ── Set logging ───────────────────────────────────────────────────────────

    fun logSet(exerciseId: Long, weightKg: Double, reps: Int) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(fmt)
            // Auto-start session if needed
            if (dao.getSessionForDate(dateStr) == null) startSession()
            val setCount = dao.observeSetLogsForDate(dateStr).stateIn(
                viewModelScope, SharingStarted.Eagerly, emptyList()
            ).value.count { it.exerciseId == exerciseId }
            dao.insertSetLog(
                ExerciseSetLogEntity(
                    exerciseId = exerciseId,
                    date = dateStr,
                    setNumber = setCount + 1,
                    weightKg = weightKg,
                    reps = reps,
                    loggedAt = Instant.now().toString(),
                ),
            )
        }
    }

    fun deleteSet(id: Long) {
        viewModelScope.launch { dao.deleteSetLog(id) }
    }

    // ── Exercise management ───────────────────────────────────────────────────

    fun saveExercise(exercise: com.lifeos.data.db.entity.ExerciseEntity) {
        viewModelScope.launch { dao.upsertExercise(exercise) }
    }

    fun archiveExercise(id: Long) {
        viewModelScope.launch { dao.archiveExercise(id) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun epley1RM(weight: Double, reps: Int): Double =
        if (reps == 1) weight else weight * (1.0 + reps / 30.0)

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val db: LifeOSDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FitnessViewModel(db) as T
    }
}
