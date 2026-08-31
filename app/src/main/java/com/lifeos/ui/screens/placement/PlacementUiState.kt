package com.lifeos.ui.screens.placement

import com.lifeos.data.db.entity.ProblemEntity
import com.lifeos.data.db.entity.SpacedRevisionEntity
import java.time.LocalDate

/** A problem paired with its current revision schedule. */
data class RevisionWithProblem(
    val revision: SpacedRevisionEntity,
    val problem: ProblemEntity,
)

/** Transient form state for Add / Edit problem bottom sheet. */
data class AddProblemState(
    val id: Long = 0L,
    val title: String = "",
    val number: String = "",
    val platform: String = "LEETCODE",
    /** LeetCode slug OR Codeforces "1234/A" style, etc. */
    val platformUrl: String = "",
    val difficulty: String = "Medium",
    /** Comma-separated topics, e.g. "Array, BFS, DP" */
    val topicsInput: String = "",
    val solvedDate: String = LocalDate.now().toString(),
    val notes: String = "",
    val approach: String = "",
    val mistakes: String = "",
) {
    val isEditing: Boolean get() = id != 0L

    /** Validation: only title is mandatory. */
    val isValid: Boolean get() = title.isNotBlank()
}

data class PlacementUiState(
    val isLoading: Boolean = true,
    /** All problems, unfiltered (used for stats). */
    val allProblems: List<ProblemEntity> = emptyList(),
    /** Filtered/searched subset shown in the list. */
    val filteredProblems: List<ProblemEntity> = emptyList(),
    val searchQuery: String = "",
    /** null = All difficulties. */
    val selectedDifficulty: String? = null,
    /** null = All platforms. */
    val selectedPlatform: String? = null,
    /** Revisions due today or earlier, each paired with its problem. */
    val dueRevisions: List<RevisionWithProblem> = emptyList(),
    // ── Stats ──────────────────────────────────────────────────────────────
    val totalSolved: Int = 0,
    val easySolved: Int = 0,
    val mediumSolved: Int = 0,
    val hardSolved: Int = 0,
)
