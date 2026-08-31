package com.lifeos.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeos.data.db.LifeOSDatabase
import com.lifeos.data.db.entity.NoteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class NotesViewModel(private val db: LifeOSDatabase) : ViewModel() {

    private val dao = db.notesDao()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)

    private val allNotesFlow = dao.observeAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val categoriesFlow = dao.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<NotesUiState> = combine(
        allNotesFlow,
        categoriesFlow,
        _searchQuery,
        _selectedCategory,
    ) { notes, categories, query, category ->

        val filtered = notes.filter { note ->
            (query.isBlank() ||
                note.title.contains(query, ignoreCase = true) ||
                note.bodyMarkdown.contains(query, ignoreCase = true) ||
                note.category.contains(query, ignoreCase = true)) &&
            (category == null || note.category == category)
        }

        NotesUiState(
            isLoading = false,
            notes = notes,
            categories = categories,
            selectedCategory = category,
            searchQuery = query,
            filteredNotes = filtered,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        NotesUiState(),
    )

    // ── Filter actions ────────────────────────────────────────────────────────

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setCategory(c: String?) { _selectedCategory.value = c }

    // ── Note CRUD ─────────────────────────────────────────────────────────────

    fun addNote(state: AddNoteState) {
        viewModelScope.launch {
            val now = Instant.now().toString()
            dao.insertNote(
                NoteEntity(
                    title = state.title.trim(),
                    category = state.category.trim().ifBlank { "General" },
                    bodyMarkdown = state.bodyMarkdown,
                    tags = encodeTags(parseTags(state.tagsInput)),
                    pinned = state.pinned,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    fun updateNote(state: AddNoteState) {
        viewModelScope.launch {
            val existing = dao.getNote(state.id) ?: return@launch
            val now = Instant.now().toString()
            dao.updateNote(
                existing.copy(
                    title = state.title.trim(),
                    category = state.category.trim().ifBlank { "General" },
                    bodyMarkdown = state.bodyMarkdown,
                    tags = encodeTags(parseTags(state.tagsInput)),
                    pinned = state.pinned,
                    updatedAt = now,
                ),
            )
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { dao.deleteNote(id) }
    }

    fun togglePin(id: Long) {
        viewModelScope.launch { dao.togglePinned(id, Instant.now().toString()) }
    }

    fun noteToEditState(note: NoteEntity): AddNoteState =
        AddNoteState(
            id = note.id,
            title = note.title,
            category = note.category,
            bodyMarkdown = note.bodyMarkdown,
            tagsInput = decodeTags(note.tags).joinToString(", "),
            pinned = note.pinned,
        )

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseTags(input: String): List<String> =
        input.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    private fun encodeTags(tags: List<String>): String =
        if (tags.isEmpty()) "[]"
        else "[" + tags.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" } + "]"

    private fun decodeTags(json: String): List<String> {
        val trimmed = json.trim()
        if (trimmed == "[]" || trimmed.isEmpty()) return emptyList()
        return trimmed.removePrefix("[").removeSuffix("]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotEmpty() }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val db: LifeOSDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NotesViewModel(db) as T
    }
}
