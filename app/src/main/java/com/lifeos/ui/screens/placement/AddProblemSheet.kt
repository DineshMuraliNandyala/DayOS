package com.lifeos.ui.screens.placement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lifeos.ui.theme.LocalLifeOSColors
import com.lifeos.ui.theme.SemanticDanger

private val PLATFORMS = listOf("LEETCODE", "CODEFORCES", "HACKERRANK", "GFG", "CUSTOM")
private val PLATFORM_LABELS = mapOf(
    "LEETCODE" to "LeetCode",
    "CODEFORCES" to "Codeforces",
    "HACKERRANK" to "HackerRank",
    "GFG" to "GFG",
    "CUSTOM" to "Custom",
)
private val DIFFICULTIES = listOf("Easy", "Medium", "Hard")
private val DIFFICULTY_COLORS = mapOf(
    "Easy" to 0xFF6DC99A,
    "Medium" to 0xFFF7C462,
    "Hard" to 0xFFF47070,
)

/** Label shown for the platform-specific URL field. */
private fun platformUrlLabel(platform: String) = when (platform) {
    "LEETCODE" -> "LeetCode slug  (e.g. two-sum)"
    "CODEFORCES" -> "Problem path  (e.g. 1234/A)"
    "HACKERRANK" -> "Challenge slug"
    "GFG" -> "Problem slug"
    else -> "URL (full link)"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProblemSheet(
    initial: AddProblemState = AddProblemState(),
    onSave: (AddProblemState) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Mirror all fields into local mutable state so the UI stays snappy
    var title by rememberSaveable { mutableStateOf(initial.title) }
    var number by rememberSaveable { mutableStateOf(initial.number) }
    var platform by rememberSaveable { mutableStateOf(initial.platform) }
    var platformUrl by rememberSaveable { mutableStateOf(initial.platformUrl) }
    var difficulty by rememberSaveable { mutableStateOf(initial.difficulty) }
    var topicsInput by rememberSaveable { mutableStateOf(initial.topicsInput) }
    var solvedDate by rememberSaveable { mutableStateOf(initial.solvedDate) }
    var notes by rememberSaveable { mutableStateOf(initial.notes) }
    var approach by rememberSaveable { mutableStateOf(initial.approach) }
    var mistakes by rememberSaveable { mutableStateOf(initial.mistakes) }

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
            // ── Header ─────────────────────────────────────────────────────
            Text(
                text = if (initial.isEditing) "Edit Problem" else "Add Problem",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            // ── Platform selector ──────────────────────────────────────────
            SheetLabel("Platform")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                items(PLATFORMS) { p ->
                    FilterChip(
                        selected = platform == p,
                        onClick = {
                            platform = p
                            platformUrl = "" // clear when switching platform
                        },
                        label = { Text(PLATFORM_LABELS[p] ?: p, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            // ── Title ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                placeholder = { Text("e.g. Two Sum") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                isError = title.isEmpty(),
            )

            // ── Number + Platform URL ──────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it.filter(Char::isDigit) },
                    label = { Text("#") },
                    placeholder = { Text("1") },
                    modifier = Modifier.weight(0.28f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = platformUrl,
                    onValueChange = { platformUrl = it },
                    label = { Text(platformUrlLabel(platform)) },
                    modifier = Modifier.weight(0.72f),
                    singleLine = true,
                )
            }

            // ── Difficulty ─────────────────────────────────────────────────
            SheetLabel("Difficulty")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DIFFICULTIES.forEach { d ->
                    val color = Color(DIFFICULTY_COLORS[d]!!)
                    FilterChip(
                        selected = difficulty == d,
                        onClick = { difficulty = d },
                        label = { Text(d, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f),
                            selectedLabelColor = color,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = difficulty == d,
                            selectedBorderColor = color,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                }
            }

            // ── Topics ─────────────────────────────────────────────────────
            OutlinedTextField(
                value = topicsInput,
                onValueChange = { topicsInput = it },
                label = { Text("Topics") },
                placeholder = { Text("Array, BFS, Dynamic Programming") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // ── Solved date ────────────────────────────────────────────────
            OutlinedTextField(
                value = solvedDate,
                onValueChange = { solvedDate = it },
                label = { Text("Solved date") },
                placeholder = { Text("YYYY-MM-DD") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // ── Optional sections ──────────────────────────────────────────
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                placeholder = { Text("Key observations…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                maxLines = 5,
            )
            OutlinedTextField(
                value = approach,
                onValueChange = { approach = it },
                label = { Text("Approach") },
                placeholder = { Text("Algorithm used, time/space complexity…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                maxLines = 5,
            )
            OutlinedTextField(
                value = mistakes,
                onValueChange = { mistakes = it },
                label = { Text("Mistakes / Edge cases") },
                placeholder = { Text("What tripped you up…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                maxLines = 5,
            )

            // ── Actions ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Delete (edit mode only)
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
                        onSave(
                            AddProblemState(
                                id = initial.id,
                                title = title,
                                number = number,
                                platform = platform,
                                platformUrl = platformUrl,
                                difficulty = difficulty,
                                topicsInput = topicsInput,
                                solvedDate = solvedDate,
                                notes = notes,
                                approach = approach,
                                mistakes = mistakes,
                            ),
                        )
                        onDismiss()
                    },
                    enabled = canSave,
                ) { Text(if (initial.isEditing) "Save" else "Add Problem") }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
