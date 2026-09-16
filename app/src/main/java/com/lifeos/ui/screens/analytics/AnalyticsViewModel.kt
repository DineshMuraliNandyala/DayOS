package com.lifeos.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeos.data.db.LifeOSDatabase
import com.lifeos.data.db.entity.DayCompletionEntity
import com.lifeos.domain.usecase.StreakUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(private val db: LifeOSDatabase) : ViewModel() {

    private val analyticsDao = db.analyticsDao()
    private val today = LocalDate.now()
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val heatmapStart = today.minusDays(83).format(fmt)

    val uiState: StateFlow<AnalyticsUiState> = analyticsDao
        .observeSince(heatmapStart)
        .flatMapLatest { completions -> flow { emit(buildState(completions)) } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AnalyticsUiState(),
        )

    private suspend fun buildState(completions: List<DayCompletionEntity>): AnalyticsUiState =
        coroutineScope {
            val byDate = completions.associateBy { it.date }

            // ── Heatmap ───────────────────────────────────────────────────
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
                HeatmapCell(date = date, level = level, goalsCompleted = done, totalGoals = total)
            }

            // ── Weekly summaries with real data (parallel async queries) ──
            val summaries = (0..3).map { weekOffset ->
                val weekEnd = today.minusDays((weekOffset * 7).toLong())
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val weekStart = weekEnd.minusDays(6)
                val startStr = weekStart.format(fmt)
                val endStr = weekEnd.format(fmt)

                val workoutsDeferred = async { analyticsDao.completedWorkoutCount(startStr) }
                val problemsDeferred = async { analyticsDao.problemsSolvedSince(startStr) }
                val proteinDeferred = async { analyticsDao.avgDailyProtein(startStr) }
                val stepsDeferred = async { analyticsDao.avgDailySteps(startStr) }
                val waterDeferred = async { analyticsDao.avgDailyWaterMl(startStr) }
                val revisionsDeferred = async { analyticsDao.revisionsCompletedSince(startStr) }

                val startLabel = "${weekStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${weekStart.dayOfMonth}"
                val endLabel = "${weekEnd.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${weekEnd.dayOfMonth}"

                WeeklySummary(
                    weekLabel = "$startLabel – $endLabel",
                    workoutsCompleted = workoutsDeferred.await(),
                    problemsSolved = problemsDeferred.await(),
                    avgProteinG = (proteinDeferred.await() ?: 0.0).toInt(),
                    avgWaterMl = (waterDeferred.await() ?: 0.0).toInt(),
                    avgSteps = (stepsDeferred.await() ?: 0.0).toInt(),
                    revisionsCompleted = revisionsDeferred.await(),
                )
            }

            // ── Streak ────────────────────────────────────────────────────
            val datesWithActivity = completions
                .filter { it.goalsCompleted > 0 }
                .map { it.date }
                .toSet()
            val streak = StreakUseCase.compute(datesWithActivity, emptySet())

            AnalyticsUiState(
                isLoading = false,
                heatmapCells = cells,
                weeklySummaries = summaries,
                currentStreak = streak,
            )
        }

    class Factory(private val db: LifeOSDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AnalyticsViewModel(db) as T
    }
}
