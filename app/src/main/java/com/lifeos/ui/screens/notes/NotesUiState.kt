package com.lifeos.ui.screens.notes

import com.lifeos.data.db.entity.NoteEntity

data class AddNoteState(
    val id: Long = 0L,
    val title: String = "",
    val category: String = "",
    val bodyMarkdown: String = "",
    val tagsInput: String = "",   // comma-separated
    val pinned: Boolean = false,
) {
    val isEditing: Boolean get() = id != 0L
    val isValid: Boolean get() = title.isNotBlank()
}

data class NotesUiState(
    val isLoading: Boolean = true,
    val notes: List<NoteEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,   // null = All
    val searchQuery: String = "",
    val filteredNotes: List<NoteEntity> = emptyList(),
)
