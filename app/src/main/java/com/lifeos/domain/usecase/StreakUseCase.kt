package com.lifeos.domain.usecase

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Computes the current daily habit streak.
 *
 * Rules (ported from use-streak.ts in the web prototype):
 *  - Walk backward from today.
 *  - If TODAY has activity, count it and then walk back from yesterday.
 *  - If today has no activity yet (still in progress), skip it without
 *    breaking the streak, then walk back from yesterday.
 *  - Stop at the first past day with no activity.
 *  - A day "counts" if it appears in [completedDates] OR [loggedDates].
 */
object StreakUseCase {

    private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun compute(completedDates: Set<String>, loggedDates: Set<String>): Int {
        val today = LocalDate.now()
        val todayStr = today.format(FORMATTER)
        val hasTodayActivity = todayStr in completedDates || todayStr in loggedDates

        var streak = 0
        // Start cursor: today if it has activity (counts it), else yesterday
        var cursor = if (hasTodayActivity) {
            streak = 1
            today.minusDays(1)
        } else {
            today.minusDays(1)
        }

        // Walk backward until we hit a day with no activity
        while (true) {
            val dateStr = cursor.format(FORMATTER)
            if (dateStr !in completedDates && dateStr !in loggedDates) break
            streak++
            cursor = cursor.minusDays(1)
        }

        return streak
    }
}
