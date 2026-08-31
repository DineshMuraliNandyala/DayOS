package com.lifeos.ui.screens.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeos.data.db.LifeOSDatabase
import com.lifeos.data.db.entity.ProblemEntity
import com.lifeos.data.db.entity.SpacedRevisionEntity
import com.lifeos.domain.usecase.SpacedRepetitionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PlacementViewModel(private val db: LifeOSDatabase) : ViewModel() {

    private val dao = db.placementDao()
    private val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    private val tomorrowStr = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

    // ── Filter state ──────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    private val _selectedDifficulty = MutableStateFlow<String?>(null)
    private val _selectedPlatform = MutableStateFlow<String?>(null)

    // ── Source flows ──────────────────────────────────────────────────────────

    private val allProblemsFlow = dao.observeAllProblems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val dueRevisionsFlow = dao.observeDueRevisions(todayStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Main UI state (reactive combination) ──────────────────────────────────

    val uiState: StateFlow<PlacementUiState> = combine(
        allProblemsFlow,
        dueRevisionsFlow,
        _searchQuery,
        _selectedDifficulty,
        _selectedPlatform,
    ) { problems, revisions, query, difficulty, platform ->

        val filtered = problems.filter { p ->
            (query.isBlank() || p.title.contains(query, ignoreCase = true) ||
                p.number?.toString()?.contains(query) == true) &&
            (difficulty == null || p.difficulty == difficulty) &&
            (platform == null || p.platform == platform)
        }

        val problemsById = problems.associateBy { it.id }
        val dueWithProblems = revisions.mapNotNull { rev ->
            problemsById[rev.problemId]?.let { RevisionWithProblem(rev, it) }
        }

        PlacementUiState(
            isLoading = false,
            allProblems = problems,
            filteredProblems = filtered,
            searchQuery = query,
            selectedDifficulty = difficulty,
            selectedPlatform = platform,
            dueRevisions = dueWithProblems,
            totalSolved = problems.size,
            easySolved = problems.count { it.difficulty == "Easy" },
            mediumSolved = problems.count { it.difficulty == "Medium" },
            hardSolved = problems.count { it.difficulty == "Hard" },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PlacementUiState(),
    )

    // ── Filter actions ────────────────────────────────────────────────────────

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setDifficultyFilter(d: String?) { _selectedDifficulty.value = d }
    fun setPlatformFilter(p: String?) { _selectedPlatform.value = p }

    // ── Problem CRUD ──────────────────────────────────────────────────────────

    /**
     * Inserts a new problem + creates its stage-0 spaced revision.
     * First revision is due tomorrow (stage-0 interval = 1 day).
     */
    fun addProblem(state: AddProblemState) {
        viewModelScope.launch {
            val problem = ProblemEntity(
                title = state.title.trim(),
                number = state.number.toIntOrNull(),
                platform = state.platform,
                platformUrl = state.platformUrl.ifBlank { null },
                leetcodeSlug = if (state.platform == "LEETCODE") state.platformUrl.ifBlank { null } else null,
                difficulty = state.difficulty,
                topics = encodeTopics(parseTopics(state.topicsInput)),
                solvedDate = state.solvedDate.ifBlank { todayStr },
                notes = state.notes.ifBlank { null },
                approach = state.approach.ifBlank { null },
                mistakes = state.mistakes.ifBlank { null },
            )
            val id = dao.insertProblem(problem)
            if (id > 0) {
                dao.insertRevision(SpacedRevisionEntity(problemId = id, stage = 0, dueDate = tomorrowStr))
            }
        }
    }

    /** Updates an existing problem's fields. Does NOT touch the revision schedule. */
    fun updateProblem(state: AddProblemState) {
        viewModelScope.launch {
            val existing = dao.getProblem(state.id) ?: return@launch
            dao.updateProblem(
                existing.copy(
                    title = state.title.trim(),
                    number = state.number.toIntOrNull(),
                    platform = state.platform,
                    platformUrl = state.platformUrl.ifBlank { null },
                    leetcodeSlug = if (state.platform == "LEETCODE") state.platformUrl.ifBlank { null } else null,
                    difficulty = state.difficulty,
                    topics = encodeTopics(parseTopics(state.topicsInput)),
                    solvedDate = state.solvedDate.ifBlank { todayStr },
                    notes = state.notes.ifBlank { null },
                    approach = state.approach.ifBlank { null },
                    mistakes = state.mistakes.ifBlank { null },
                ),
            )
        }
    }

    fun deleteProblem(id: Long) {
        viewModelScope.launch {
            dao.deleteProblem(id)
            dao.deleteRevisionByProblemId(id)
        }
    }

    /** Converts a [ProblemEntity] into an [AddProblemState] ready for the edit sheet. */
    fun problemToEditState(problem: ProblemEntity): AddProblemState =
        AddProblemState(
            id = problem.id,
            title = problem.title,
            number = problem.number?.toString() ?: "",
            platform = problem.platform,
            platformUrl = problem.platformUrl ?: problem.leetcodeSlug ?: "",
            difficulty = problem.difficulty,
            topicsInput = decodeTopics(problem.topics).joinToString(", "),
            solvedDate = problem.solvedDate,
            notes = problem.notes ?: "",
            approach = problem.approach ?: "",
            mistakes = problem.mistakes ?: "",
        )

    // ── Spaced repetition ─────────────────────────────────────────────────────

    fun review(revision: SpacedRevisionEntity, outcome: SpacedRepetitionUseCase.ReviewOutcome) {
        viewModelScope.launch {
            dao.upsertRevision(SpacedRepetitionUseCase.review(revision, outcome))
        }
    }

    // ── URL builder ───────────────────────────────────────────────────────────

    fun buildUrl(problem: ProblemEntity): String? = when (problem.platform) {
        "LEETCODE" ->
            (problem.leetcodeSlug ?: problem.platformUrl)?.let { "https://leetcode.com/problems/$it/" }
        "CODEFORCES" ->
            problem.platformUrl?.let { "https://codeforces.com/problemset/problem/$it" }
        "HACKERRANK" ->
            problem.platformUrl?.let { "https://www.hackerrank.com/challenges/$it" }
        "GFG" ->
            problem.platformUrl?.let { "https://www.geeksforgeeks.org/problems/$it" }
        "CUSTOM" -> problem.platformUrl
        else -> null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** "Array, BFS, DP" → ["Array", "BFS", "DP"] */
    private fun parseTopics(input: String): List<String> =
        input.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    /** ["Array", "BFS"] → ["Array","BFS"] (minimal JSON, no library needed). */
    private fun encodeTopics(topics: List<String>): String =
        if (topics.isEmpty()) "[]"
        else "[" + topics.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" } + "]"

    /**
     * Parses a JSON string array without pulling in a JSON library.
     * Handles values produced by [encodeTopics] and by the Room Converters.
     */
    private fun decodeTopics(json: String): List<String> {
        val trimmed = json.trim()
        if (trimmed == "[]" || trimmed.isEmpty()) return emptyList()
        return trimmed
            .removePrefix("[").removeSuffix("]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotEmpty() }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val db: LifeOSDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlacementViewModel(db) as T
    }
}
