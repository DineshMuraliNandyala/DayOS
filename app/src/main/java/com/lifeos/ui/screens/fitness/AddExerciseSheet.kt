package com.lifeos.ui.screens.fitness

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

private val WEEKDAYS = listOf(
    "mon" to "Mon", "tue" to "Tue", "wed" to "Wed",
    "thu" to "Thu", "fri" to "Fri", "sat" to "Sat", "sun" to "Sun",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseSheet(
    initial: AddExerciseState = AddExerciseState(),
    onSave: (AddExerciseState) -> Unit,
    onArchive: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by rememberSaveable { mutableStateOf(initial.name) }
    var muscleGroup by rememberSaveable { mutableStateOf(initial.muscleGroup) }
    var weekday by rememberSaveable { mutableStateOf(initial.weekday) }
    var targetSets by rememberSaveable { mutableStateOf(initial.targetSets) }
    var targetReps by rememberSaveable { mutableStateOf(initial.targetReps) }
    var notes by rememberSaveable { mutableStateOf(initial.notes) }

    val canSave = name.isNotBlank()

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
                text = if (initial.isEditing) "Edit Exercise" else "Add Exercise",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            // Day picker
            Text(
                "Day",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(WEEKDAYS) { (key, label) ->
                    FilterChip(
                        selected = weekday == key,
                        onClick = { weekday = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Exercise name *") },
                placeholder = { Text("e.g. Bench Press") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = name.isEmpty(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )

            OutlinedTextField(
                value = muscleGroup,
                onValueChange = { muscleGroup = it },
                label = { Text("Muscle group") },
                placeholder = { Text("e.g. Chest, Triceps") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = targetSets,
                    onValueChange = { targetSets = it.filter(Char::isDigit) },
                    label = { Text("Sets") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = targetReps,
                    onValueChange = { targetReps = it },
                    label = { Text("Reps") },
                    placeholder = { Text("8-12") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                placeholder = { Text("Form cues, equipment…") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (initial.isEditing && onArchive != null) {
                    OutlinedButton(
                        onClick = { onArchive(initial.id); onDismiss() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticDanger),
                        border = BorderStroke(1.dp, SemanticDanger),
                    ) { Text("Archive") }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(AddExerciseState(
                            id = initial.id,
                            name = name, muscleGroup = muscleGroup,
                            weekday = weekday, targetSets = targetSets,
                            targetReps = targetReps, notes = notes,
                        ))
                        onDismiss()
                    },
                    enabled = canSave,
                ) { Text(if (initial.isEditing) "Save" else "Add") }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
