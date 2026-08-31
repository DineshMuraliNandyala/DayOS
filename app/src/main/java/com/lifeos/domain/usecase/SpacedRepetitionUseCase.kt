package com.lifeos.domain.usecase

import com.lifeos.data.db.entity.SpacedRevisionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Spaced-repetition review algorithm.
 *
 * Intervals per stage (SM-2 inspired, simplified):
 *   Stage 0 → 1 day
 *   Stage 1 → 3 days
 *   Stage 2 → 7 days
 *   Stage 3 → 14 days
 *   Stage 4 → 30 days  (max — stays here on repeated "Easy")
 *
 * Review outcomes:
 *   Easy   → stage + 1 (capped at 4), next due = today + intervals[newStage]
 *   Hard   → stage - 1 (floor at 0), next due = today + intervals[newStage]
 *   Forgot → stage = 0,               next due = today + 1
 *
 * Each review appends a JSON record to [SpacedRevisionEntity.history] so the
 * full review trail is preserved for analytics.
 */
object SpacedRepetitionUseCase {

    private val INTERVALS = intArrayOf(1, 3, 7, 14, 30)
    private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    enum class ReviewOutcome { EASY, HARD, FORGOT }

    /**
     * Computes the updated [SpacedRevisionEntity] after a review.
     * Does NOT write to the DB — caller is responsible for persisting.
     */
    fun review(revision: SpacedRevisionEntity, outcome: ReviewOutcome): SpacedRevisionEntity {
        val todayStr = LocalDate.now().format(DATE_FMT)
        val newStage = when (outcome) {
            ReviewOutcome.EASY   -> (revision.stage + 1).coerceAtMost(INTERVALS.lastIndex)
            ReviewOutcome.HARD   -> (revision.stage - 1).coerceAtLeast(0)
            ReviewOutcome.FORGOT -> 0
        }
        val daysUntilDue = INTERVALS[newStage].toLong()
        val dueDate = LocalDate.now().plusDays(daysUntilDue).format(DATE_FMT)

        val historyEntry = """{"date":"$todayStr","result":"${outcome.name.lowercase()}"}"""
        val newHistory = appendToJsonArray(revision.history, historyEntry)

        return revision.copy(
            stage = newStage,
            dueDate = dueDate,
            lastReviewedAt = Instant.now().toString(),
            history = newHistory,
        )
    }

    /** Human-readable label for the current stage. */
    fun stageName(stage: Int): String = when (stage) {
        0 -> "New"
        1 -> "Learning"
        2 -> "Reviewing"
        3 -> "Mature"
        4 -> "Long-term"
        else -> "Unknown"
    }

    /** Days until next review for a given stage. */
    fun intervalDays(stage: Int): Int = INTERVALS.getOrElse(stage) { INTERVALS.last() }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Appends a JSON object literal to a JSON array string without pulling in
     * a JSON library — keeps the dependency count zero.
     *
     * e.g. appendToJsonArray("[]", "{\"a\":1}") → "[{\"a\":1}]"
     *      appendToJsonArray("[{\"a\":1}]", "{\"b\":2}") → "[{\"a\":1},{\"b\":2}]"
     */
    private fun appendToJsonArray(json: String, entry: String): String {
        val trimmed = json.trim()
        return if (trimmed == "[]") "[$entry]"
        else "${trimmed.dropLast(1)},$entry]"
    }
}
