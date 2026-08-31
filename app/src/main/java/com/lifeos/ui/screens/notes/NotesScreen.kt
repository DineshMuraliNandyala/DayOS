package com.lifeos.ui.screens.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.LifeOSApp
import com.lifeos.data.db.entity.NoteEntity
import com.lifeos.ui.theme.LocalLifeOSColors
import com.lifeos.ui.theme.SemanticWarning

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NotesScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as LifeOSApp).database
    val vm: NotesViewModel = viewModel(factory = NotesViewModel.Factory(db))
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }

    if (showAddSheet) {
        AddNoteSheet(
            onSave = vm::addNote,
            onDismiss = { showAddSheet = false },
        )
    }
    editingNote?.let { note ->
        AddNoteSheet(
            initial = vm.noteToEditState(note),
            onSave = vm::updateNote,
            onDelete = vm::deleteNote,
            onDismiss = { editingNote = null },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Search bar
                item(key = "search") {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = vm::setSearchQuery,
                        placeholder = { Text("Search notes…") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                // Category filter chips
                if (uiState.categories.isNotEmpty()) {
                    item(key = "cats") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedCategory == null,
                                    onClick = { vm.setCategory(null) },
                                    label = { Text("All") },
                                )
                            }
                            items(uiState.categories) { cat ->
                                FilterChip(
                                    selected = uiState.selectedCategory == cat,
                                    onClick = {
                                        vm.setCategory(
                                            if (uiState.selectedCategory == cat) null else cat
                                        )
                                    },
                                    label = { Text(cat) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                )
                            }
                        }
                    }
                }

                if (uiState.filteredNotes.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (uiState.notes.isEmpty())
                                    "No notes yet.\nTap + to create your first note."
                                else "No notes match your search.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LocalLifeOSColors.current.textFaint,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    items(uiState.filteredNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onOpen = { editingNote = note },
                            onPin = { vm.togglePin(note.id) },
                        )
                    }
                }

                item(key = "bottom") { Spacer(Modifier.height(88.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 88.dp),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "New note")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Note card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NoteCard(
    note: NoteEntity,
    onOpen: () -> Unit,
    onPin: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.pinned)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else LocalLifeOSColors.current.surface1,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (note.category.isNotBlank()) {
                        CategoryBadge(note.category)
                    }
                }
                IconButton(
                    onClick = onPin,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Outlined.PushPin,
                        contentDescription = if (note.pinned) "Unpin" else "Pin",
                        modifier = Modifier.size(16.dp),
                        tint = if (note.pinned)
                            SemanticWarning
                        else LocalLifeOSColors.current.textFaint,
                    )
                }
            }

            // Body preview — strip markdown markers for cleaner preview
            if (note.bodyMarkdown.isNotBlank()) {
                val preview = note.bodyMarkdown
                    .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
                    .replace(Regex("\\*{1,2}([^*]+)\\*{1,2}"), "\$1")
                    .replace(Regex("`[^`]+`"), "…")
                    .trim()
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalLifeOSColors.current.textFaint,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Date
            Text(
                text = note.updatedAt.take(10),
                style = MaterialTheme.typography.labelSmall,
                color = LocalLifeOSColors.current.textFaint,
            )
        }
    }
}

@Composable
private fun CategoryBadge(category: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
