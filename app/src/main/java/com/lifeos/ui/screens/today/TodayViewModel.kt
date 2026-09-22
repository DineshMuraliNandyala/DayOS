package com.lifeos.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeos.data.db.LifeOSDatabase
import com.lifeos.data.db.entity.DailyGoalCompletionEntity
import com.lifeos.data.db.entity.HobbyLogEntity
import com.lifeos.data.db.entity.JournalEntryEntity
import com.lifeos.data.db.entity.ProteinLogEntity
import com.lifeos.data.db.entity.StepReadingEntity
import com.lifeos.data.db.entity.WaterLogEntity
import com.lifeos.data.db.entity.WorkoutSessionEntity
import com.lifeos.domain.usecase.StreakUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TodayViewModel(private val db: LifeOSDatabase) : ViewModel() {

    private val todayDao = db.todayDao()
    private val fitnessDao = db.fitnessDao()
    private val settingsDao = db.settingsDao()

    val today: LocalDate = LocalDate.now()
    val todayStr: String = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    private val weekdayKey: String = today.dayOfWeek.name.take(3).lowercase()

    // ── Per-weekday filtered flows ──────────────────────────────────────────

    private val goalsFlow = todayDao.observeActiveGoals().map { list ->
        list.filter { weekdayKey in parseWeekdays(it.weekdays) }
    }
    private val completionsFlow = todayDao.observeCompletionsForDate(todayStr).map { list ->
        list.associate { it.goalId to it.completed }
    }
    private val hobbiesFlow = todayDao.observeActiveHobbies().map { list ->
        list.filter { weekdayKey in parseWeekdays(it.weekdays) }
    }
    private val hobbyLogsFlow = todayDao.observeHobbyLogsForDate(todayStr).map { list ->
        list.associate { it.hobbyId to it.minutes }
    }

    // ── Combined UI state ──────────────────────────────────────────────────

    val uiState = combine(goalsFlow, completionsFlow, hobbiesFlow, hobbyLogsFlow) {
            goals, completions, hobbies, hobbyLogs ->
        TodayUiState(
            today = today,
            goals = goals,
            completions = completions,
            hobbies = hobbies,
            hobbyLogs = hobbyLogs,
            isLoading = false,
        )
    }
        // Protein total + log entries
        .combine(todayDao.observeProteinTotalForDate(todayStr)) { s, protein ->
            s.copy(proteinGrams = protein)
        }
        .combine(todayDao.observeProteinLogsForDate(todayStr)) { s, logs ->
            s.copy(proteinLogs = logs)
        }
        // Water total + log entries
        .combine(fitnessDao.observeWaterTotalForDate(todayStr)) { s, water ->
            s.copy(waterMl = water)
        }
        .combine(fitnessDao.observeWaterLogsForDate(todayStr)) { s, logs ->
            s.copy(waterLogs = logs)
        }
        // Steps total + log entries
        .combine(fitnessDao.observeStepsTotalForDate(todayStr)) { s, steps ->
            s.copy(stepsTotal = steps)
        }
        .combine(fitnessDao.observeStepLogsForDate(todayStr)) { s, logs ->
            s.copy(stepLogs = logs)
        }
        // Journal
        .combine(todayDao.observeJournalEntry(todayStr)) { s, journal ->
            s.copy(journalEntry = journal)
        }
        // Due revisions
        .combine(todayDao.observeDueRevisionCount(todayStr)) { s, count ->
            s.copy(dueRevisionCount = count)
        }
        // Settings
        .combine(settingsDao.observe()) { s, settings ->
            s.copy(
                displayName = settings?.displayName ?: "",
                proteinGoal = settings?.proteinGoalGrams ?: 150,
                waterGoal = settings?.waterGoalMl ?: 2500,
                stepGoal = settings?.stepGoal ?: 8000,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(),
        )

    init {
        viewModelScope.launch {
            val completedDates = todayDao.allCompletedDates().toSet()
            val hobbyDates = todayDao.allLoggedDates().toSet()
            val streak = StreakUseCase.compute(completedDates, hobbyDates)
            // Streak is wired inline via combine; gym streak computed separately below
        }
    }

    // ── Goal actions ──────────────────────────────────────────────────────

    fun toggleGoal(goalId: Long, currentlyDone: Boolean) {
        viewModelScope.launch {
            val completion = DailyGoalCompletionEntity(
                goalId = goalId,
                date = todayStr,
                completed = !currentlyDone,
                completedAt = if (!currentlyDone) Instant.now().toString() else null,
            )
            todayDao.upsertCompletion(completion)
        }
    }

    fun logHobbyMinutes(hobbyId: Long, minutes: Int) {
        viewModelScope.launch {
            if (minutes <= 0) {
                todayDao.deleteHobbyLogsForDate(hobbyId, todayStr)
            } else {
                todayDao.upsertHobbyLog(
                    HobbyLogEntity(
                        hobbyId = hobbyId,
                        date = todayStr,
                        minutes = minutes,
                    ),
                )
            }
        }
    }

    // ── Protein actions ───────────────────────────────────────────────────

    fun addProtein(grams: Int) {
        viewModelScope.launch {
            todayDao.insertProteinLog(
                ProteinLogEntity(
                    date = todayStr,
                    grams = grams,
                    loggedAt = Instant.now().toString(),
                ),
            )
        }
    }

    fun deleteProteinLog(id: Long) {
        viewModelScope.launch { todayDao.deleteProteinLog(id) }
    }

    // ── Water actions ─────────────────────────────────────────────────────

    fun addWater(ml: Int) {
        viewModelScope.launch {
            fitnessDao.insertWaterLog(
                WaterLogEntity(
                    date = todayStr,
                    ml = ml,
                    loggedAt = Instant.now().toString(),
                ),
            )
        }
    }

    fun deleteWaterLog(id: Long) {
        viewModelScope.launch { fitnessDao.deleteWaterLog(id) }
    }

    // ── Steps actions ─────────────────────────────────────────────────────

    fun addSteps(steps: Int) {
        viewModelScope.launch {
            val now = Instant.now().toString()
            fitnessDao.insertStepLog(
                StepReadingEntity(
                    date = todayStr,
                    steps = steps,
                    source = "manual",
                    syncedAt = now,
                ),
            )
        }
    }

    fun deleteStepLog(id: Long) {
        viewModelScope.launch { fitnessDao.deleteStepLog(id) }
    }

    // ── Journal ───────────────────────────────────────────────────────────

    fun saveReflection(text: String, mood: String?) {
        viewModelScope.launch {
            todayDao.upsertJournalEntry(
                JournalEntryEntity(
                    date = todayStr,
                    reflectionMarkdown = text,
                    mood = mood,
                ),
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun parseWeekdays(json: String): Set<String> =
        json.trim().removePrefix("[").removeSuffix("]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotEmpty() }
            .toSet()

    // ── Factory ───────────────────────────────────────────────────────────

    class Factory(private val db: LifeOSDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TodayViewModel(db) as T
    }
}
