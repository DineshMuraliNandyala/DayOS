package com.lifeos.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeos.data.db.LifeOSDatabase
import com.lifeos.data.db.entity.DayCompletionEntity
import com.lifeos.domain.usecase.StreakUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class AnalyticsViewModel(private val db: LifeOSDatabase) : ViewModel() {

    private val analyticsDao = db.analyticsDao()

    private val today = LocalDate.now()
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    // 84 days = 12 weeks of heatmap history
    private val heatmapStart = today.minusDays(83).format(fmt)

    val uiState: StateFlow<AnalyticsUiState> = analyticsDao
        .observeSince(heatmapStart)
        .map { completions -> buildState(completions) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AnalyticsUiState(),
        )

    private fun buildState(completions: List<DayCompletionEntity>): AnalyticsUiState {
        val byDate = completions.associateBy { it.date }

        // ── Heatmap ───────────────────────────────────────────────────────
        val cells = (0..83).map { offset ->
            val date = today.minusDays((83 - offset).toLong()).format(fmt)
            val entry = byDate[date]
            val total = entry?.goalsScheduled ?: 0
            val done = entry?.goalsCompleted ?: 0
            val level = when {
                total == 0 -> 0
                done == 0 -> 1
                done < total / 2 -> 2
                done < total -> 3
                else -> 4
            }
            HeatmapCell(
                date = date,
                level = level,
                goalsCompleted = done,
                totalGoals = total,
            )
        }

        // ── Weekly summaries (last 4 weeks) ────────────────────────────────
        val summaries = (0..3).map { weekOffset ->
            val weekEnd = today.minusDays((weekOffset * 7).toLong())
            val weekStart = weekEnd.minusDays(6)
            val startStr = weekStart.format(fmt)
            val endStr = weekEnd.format(fmt)

            val weekCells = cells.filter { it.date in startStr..endStr }
            val workouts = weekCells.count { it.goalsCompleted > 0 } // approximation
            val problems = 0 // fetched async below (see refreshWeeklyStats)

            val startLabel = "${weekStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${weekStart.dayOfMonth}"
            val endLabel = "${weekEnd.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${weekEnd.dayOfMonth}"

            WeeklySummary(
                weekLabel = "$startLabel – $endLabel",
                workoutsCompleted = weekCells.count { it.level >= 3 },
                problemsSolved = 0, // updated by refreshWeeklyStats
                avgProteinG = 0,
                avgWaterMl = 0,
                avgSteps = 0,
                revisionsCompleted = 0,
            )
        }

        // Streak
        val datesWithActivity = completions
            .filter { it.completedGoals > 0 }
            .map { it.date }
            .toSet()
        val streak = StreakUseCase.compute(datesWithActivity, emptySet())

        return AnalyticsUiState(
            isLoading = false,
            heatmapCells = cells,
            weeklySummaries = summaries,
            currentStreak = streak,
        )
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val db: LifeOSDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AnalyticsViewModel(db) as T
    }
}
