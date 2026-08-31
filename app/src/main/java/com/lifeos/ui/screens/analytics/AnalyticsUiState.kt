package com.lifeos.ui.screens.analytics

import com.lifeos.data.db.entity.DayCompletionEntity

/** One cell in the heatmap — a date and its completion level 0..4. */
data class HeatmapCell(
    val date: String,            // ISO "yyyy-MM-dd"
    val level: Int,              // 0 = none, 1 = low … 4 = full
    val goalsCompleted: Int,
    val totalGoals: Int,
)

/** Aggregated stats for one "week" block shown as a summary card. */
data class WeeklySummary(
    val weekLabel: String,          // e.g. "Aug 25 – Aug 31"
    val workoutsCompleted: Int,
    val problemsSolved: Int,
    val avgProteinG: Int,
    val avgWaterMl: Int,
    val avgSteps: Int,
    val revisionsCompleted: Int,
)

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    /** 84 cells = 12 weeks of daily heatmap data. */
    val heatmapCells: List<HeatmapCell> = emptyList(),
    val weeklySummaries: List<WeeklySummary> = emptyList(),
    // All-time totals shown in the header
    val totalProblems: Int = 0,
    val currentStreak: Int = 0,
)
