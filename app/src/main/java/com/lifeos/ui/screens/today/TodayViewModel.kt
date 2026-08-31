package com.lifeos.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeos.data.db.LifeOSDatabase
import com.lifeos.data.db.entity.DailyGoalCompletionEntity
import com.lifeos.data.db.entity.HobbyLogEntity
import com.lifeos.data.db.entity.JournalEntryEntity
import com.lifeos.data.db.entity.ProteinLogEntity
import com.lifeos.data.db.entity.WaterLogEntity
import com.lifeos.domain.usecase.StreakUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TodayViewModel(private val db: LifeOSDatabase) : ViewModel() {

    private val todayDao = db.todayDao()
    private val fitnessDao = db.fitnessDao()
    private val settingsDao = db.settingsDao()

    val today: LocalDate = LocalDate.now()
    val todayStr: String = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    /** "mon", "tue", … "sun" — used to filter goals/hobbies for the current day. */
    private val weekdayKey: String = today.dayOfWeek.name.take(3).lowercase()

    private val json = Json { ignoreUnknownKeys = true }

    // ── Streak ─────────────────────────────────────────────────────────────────

    private val _streak = MutableStateFlow(0)
    val streak = _streak.asStateFlow()

    // ── Per-weekday filtered flows ──────────────────────────────────────────────

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

    // ── UI state (all flows combined) ──────────────────────────────────────────

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
        .combine(todayDao.observeProteinTotalForDate(todayStr)) { s, protein ->
            s.copy(proteinGrams = protein)
        }
        .combine(fitnessDao.observeWaterTotalForDate(todayStr)) { s, water ->
            s.copy(waterMl = water)
        }
        .combine(todayDao.observeJournalEntry(todayStr)) { s, journal ->
            s.copy(journalEntry = journal)
        }
        .combine(todayDao.observeDueRevisionCount(todayStr)) { s, count ->
            s.copy(dueRevisionCount = count)
        }
        .combine(settingsDao.observe()) { s, settings ->
            s.copy(
                displayName = settings?.displayName ?: "",
                proteinGoal = settings?.proteinGoalGrams ?: 150,
                waterGoal = settings?.waterGoalMl ?: 2500,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(),
        )

    init {
        refreshStreak()
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    fun toggleGoal(goalId: Long, currentlyCompleted: Boolean) {
        viewModelScope.launch {
            val now = Instant.now().toString()
            val existing = todayDao.getCompletion(goalId, todayStr)
            if (existing != null) {
                todayDao.upsertCompletion(
                    existing.copy(
                        completed = !currentlyCompleted,
                        completedAt = if (!currentlyCompleted) now else null,
                    ),
                )
            } else {
                todayDao.upsertCompletion(
                    DailyGoalCompletionEntity(
                        goalId = goalId,
                        date = todayStr,
                        completed = true,
                        completedAt = now,
                    ),
                )
            }
            refreshStreak()
        }
    }

    fun toggleHobby(hobbyId: Long, currentlyLogged: Boolean, goalMinutes: Int) {
        viewModelScope.launch {
            if (currentlyLogged) {
                todayDao.deleteHobbyLogsForDate(hobbyId, todayStr)
            } else {
                todayDao.upsertHobbyLog(
                    HobbyLogEntity(hobbyId = hobbyId, date = todayStr, minutes = goalMinutes),
                )
            }
            refreshStreak()
        }
    }

    fun addProtein(grams: Int) {
        viewModelScope.launch {
            todayDao.insertProteinLog(
                ProteinLogEntity(date = todayStr, grams = grams, loggedAt = Instant.now().toString()),
            )
        }
    }

    fun addWater(ml: Int) {
        viewModelScope.launch {
            fitnessDao.insertWaterLog(
                WaterLogEntity(date = todayStr, ml = ml, loggedAt = Instant.now().toString()),
            )
        }
    }

    fun saveJournalEntry(reflection: String, systemDesignTopic: String?, mood: String?) {
        viewModelScope.launch {
            val existing = uiState.value.journalEntry
            todayDao.upsertJournalEntry(
                JournalEntryEntity(
                    id = existing?.id ?: 0,
                    date = todayStr,
                    reflectionMarkdown = reflection,
                    systemDesignTopic = systemDesignTopic?.ifBlank { null },
                    mood = mood,
                    photoIds = existing?.photoIds ?: "[]",
                ),
            )
        }
    }

    private fun refreshStreak() {
        viewModelScope.launch {
            val completed = todayDao.allCompletedDates().toSet()
            val logged = todayDao.allLoggedDates().toSet()
            _streak.value = StreakUseCase.compute(completed, logged)
        }
    }

    private fun parseWeekdays(jsonStr: String): List<String> = try {
        json.decodeFromString<List<String>>(jsonStr)
    } catch (_: Exception) {
        emptyList()
    }

    class Factory(private val db: LifeOSDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TodayViewModel(db) as T
    }
}
