package com.lifeos.ui.screens.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.lifeos.ui.theme.LocalLifeOSColors
import com.lifeos.ui.theme.SemanticDanger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteSheet(
    initial: AddNoteState = AddNoteState(),
    onSave: (AddNoteState) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by rememberSaveable { mutableStateOf(initial.title) }
    var category by rememberSaveable { mutableStateOf(initial.category) }
    var bodyMarkdown by rememberSaveable { mutableStateOf(initial.bodyMarkdown) }
    var tagsInput by rememberSaveable { mutableStateOf(initial.tagsInput) }
    var pinned by rememberSaveable { mutableStateOf(initial.pinned) }

    val canSave = title.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LocalLifeOSColors.current.surface1,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (initial.isEditing) "Edit Note" else "New Note",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = title.isEmpty(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("e.g. System Design") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags") },
                    placeholder = { Text("tag1, tag2") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            OutlinedTextField(
                value = bodyMarkdown,
                onValueChange = { bodyMarkdown = it },
                label = { Text("Content (Markdown)") },
                placeholder = { Text("# Heading\n\n- bullet\n\n```code```") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                maxLines = 20,
            )

            // Pinned toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Pin to top", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = pinned, onCheckedChange = { pinned = it })
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (initial.isEditing && onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(initial.id); onDismiss() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticDanger),
                        border = BorderStroke(1.dp, SemanticDanger),
                    ) { Text("Delete") }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(AddNoteState(
                            id = initial.id, title = title, category = category,
                            bodyMarkdown = bodyMarkdown, tagsInput = tagsInput, pinned = pinned,
                        ))
                        onDismiss()
                    },
                    enabled = canSave,
                ) { Text(if (initial.isEditing) "Save" else "Add Note") }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
