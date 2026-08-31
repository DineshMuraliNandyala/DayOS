package com.lifeos.ui.screens.fitness

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.LifeOSApp
import com.lifeos.data.db.entity.ExerciseEntity
import com.lifeos.data.db.entity.ExerciseSetLogEntity
import com.lifeos.data.db.entity.WorkoutSessionEntity
import com.lifeos.ui.theme.LocalLifeOSColors
import com.lifeos.ui.theme.SemanticDanger
import com.lifeos.ui.theme.SemanticSuccess
import com.lifeos.ui.theme.SemanticWarning

// ─────────────────────────────────────────────────────────────────────────────
// Entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FitnessScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as LifeOSApp).database
    val vm: FitnessViewModel = viewModel(factory = FitnessViewModel.Factory(db))
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    var showAddExercise by rememberSaveable { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<ExerciseEntity?>(null) }
    var showStepsDialog by remember { mutableStateOf(false) }

    if (showAddExercise) {
        AddExerciseSheet(
            initial = AddExerciseState(weekday = uiState.todayWeekday),
            onSave = vm::addExercise,
            onDismiss = { showAddExercise = false },
        )
    }
    editingExercise?.let { ex ->
        AddExerciseSheet(
            initial = vm.exerciseToEditState(ex),
            onSave = vm::updateExercise,
            onArchive = vm::archiveExercise,
            onDismiss = { editingExercise = null },
        )
    }
    if (showStepsDialog) {
        StepsInputDialog(
            current = uiState.stepsTaken,
            onConfirm = { vm.logSteps(it); showStepsDialog = false },
            onDismiss = { showStepsDialog = false },
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ── Session banner ─────────────────────────────────────────
                item(key = "session") {
                    SessionBanner(
                        uiState = uiState,
                        onStart = vm::startWorkout,
                        onFinish = vm::finishWorkout,
                    )
                }

                // ── Steps card ─────────────────────────────────────────────
                item(key = "steps") {
                    StepsCard(
                        steps = uiState.stepsTaken,
                        goal = uiState.stepGoal,
                        onClick = { showStepsDialog = true },
                    )
                }

                // ── Exercise section header ────────────────────────────────
                item(key = "ex_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Today's Workout",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        val dayLabel = uiState.todayWeekday
                            .replaceFirstChar { it.uppercaseChar() }
                        Text(
                            dayLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalLifeOSColors.current.textFaint,
                        )
                    }
                }

                if (uiState.exercisesWithSets.isEmpty()) {
                    item(key = "no_exercises") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No exercises scheduled for today.\nTap + to add one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LocalLifeOSColors.current.textFaint,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    items(uiState.exercisesWithSets, key = { it.exercise.id }) { ews ->
                        ExerciseCard(
                            ews = ews,
                            sessionActive = uiState.sessionActive,
                            onLogSet = { weight, reps -> vm.logSet(ews.exercise.id, weight, reps) },
                            onDeleteSet = vm::deleteSet,
                            onEdit = { editingExercise = ews.exercise },
                        )
                    }
                }

                // ── Recent sessions ────────────────────────────────────────
                if (uiState.recentSessions.isNotEmpty()) {
                    item(key = "recent_header") {
                        Text(
                            "Recent Sessions",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(uiState.recentSessions.take(5), key = { "sess_${it.id}" }) { session ->
                        SessionHistoryRow(session)
                    }
                }

                item(key = "bottom") { Spacer(Modifier.height(88.dp)) }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddExercise = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 88.dp),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add exercise")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Session banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SessionBanner(
    uiState: FitnessUiState,
    onStart: () -> Unit,
    onFinish: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = when {
            uiState.session?.completedAt != null -> SemanticSuccess.copy(alpha = 0.12f)
            uiState.sessionActive -> MaterialTheme.colorScheme.primaryContainer
            else -> LocalLifeOSColors.current.surface1
        },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = when {
                    uiState.session?.completedAt != null -> Icons.Outlined.Check
                    uiState.sessionActive -> Icons.Outlined.LocalFireDepartment
                    else -> Icons.Outlined.FitnessCenter
                },
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = when {
                    uiState.session?.completedAt != null -> SemanticSuccess
                    uiState.sessionActive -> MaterialTheme.colorScheme.primary
                    else -> LocalLifeOSColors.current.textFaint
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        uiState.session?.completedAt != null -> "Workout complete"
                        uiState.sessionActive -> "Workout in progress"
                        else -> "Ready to train?"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                uiState.session?.let { s ->
                    val stats = buildList {
                        s.durationMinutes?.let { add("${it}min") }
                        s.totalVolumeKg?.let { add("${it.toInt()}kg volume") }
                        if (s.newPrCount > 0) add("${s.newPrCount} PR${if (s.newPrCount > 1) "s" else ""}!")
                    }
                    if (stats.isNotEmpty()) {
                        Text(
                            stats.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalLifeOSColors.current.textFaint,
                        )
                    }
                }
            }
            when {
                uiState.session?.completedAt != null -> { /* done — no button */ }
                uiState.sessionActive -> {
                    Button(
                        onClick = onFinish,
                        colors = ButtonDefaults.buttonColors(containerColor = SemanticSuccess),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) { Text("Finish", style = MaterialTheme.typography.labelMedium) }
                }
                else -> {
                    Button(
                        onClick = onStart,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) { Text("Start", style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Steps card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepsCard(steps: Int, goal: Int, onClick: () -> Unit) {
    val progress = if (goal > 0) (steps.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val reached = steps >= goal

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = LocalLifeOSColors.current.surface1,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Steps",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "$steps / $goal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (reached) SemanticSuccess else LocalLifeOSColors.current.textFaint,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (reached) SemanticSuccess else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                "Tap to update",
                style = MaterialTheme.typography.labelSmall,
                color = LocalLifeOSColors.current.textFaint,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Exercise card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExerciseCard(
    ews: ExerciseWithSets,
    sessionActive: Boolean,
    onLogSet: (Double, Int) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onEdit: () -> Unit,
) {
    val exercise = ews.exercise
    var weightInput by rememberSaveable(exercise.id) { mutableStateOf("") }
    var repsInput by rememberSaveable(exercise.id) { mutableStateOf("") }
    val canLog = weightInput.toDoubleOrNull() != null &&
        repsInput.toIntOrNull()?.let { it > 0 } == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LocalLifeOSColors.current.surface1),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val subtitle = buildList {
                        if (exercise.muscleGroup.isNotBlank()) add(exercise.muscleGroup)
                        add("${exercise.targetSets}×${exercise.targetReps}")
                        exercise.bestPrKg?.let { add("PR: ${String.format("%.1f", it)}kg 1RM") }
                    }.joinToString(" · ")
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalLifeOSColors.current.textFaint,
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp),
                        tint = LocalLifeOSColors.current.textFaint,
                    )
                }
            }

            // Logged sets
            if (ews.sets.isNotEmpty()) {
                Divider()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ews.sets.forEach { set ->
                        SetRow(set = set, onDelete = { onDeleteSet(set.id) })
                    }
                }
            }

            // Log new set (only during active session)
            if (sessionActive) {
                Divider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("kg") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        value = repsInput,
                        onValueChange = { repsInput = it.filter(Char::isDigit) },
                        label = { Text("reps") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Button(
                        onClick = {
                            val w = weightInput.toDoubleOrNull() ?: return@Button
                            val r = repsInput.toIntOrNull() ?: return@Button
                            onLogSet(w, r)
                            weightInput = ""
                            repsInput = ""
                        },
                        enabled = canLog,
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Log set", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SetRow(set: ExerciseSetLogEntity, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Set number badge
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(LocalLifeOSColors.current.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${set.setNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = LocalLifeOSColors.current.textFaint,
            )
        }

        Text(
            "${String.format("%.1f", set.weightKg)} kg × ${set.reps}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )

        // PR badge
        if (set.isPr) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = SemanticWarning.copy(alpha = 0.18f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = SemanticWarning,
                    )
                    Text("PR", style = MaterialTheme.typography.labelSmall, color = SemanticWarning)
                }
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete set",
                modifier = Modifier.size(16.dp),
                tint = SemanticDanger.copy(alpha = 0.7f),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Session history row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SessionHistoryRow(session: WorkoutSessionEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = LocalLifeOSColors.current.surface2,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                val parts = buildList {
                    session.durationMinutes?.let { add("${it}min") }
                    session.totalVolumeKg?.let { add("${it.toInt()}kg") }
                    if (session.newPrCount > 0) add("${session.newPrCount} PR")
                }
                if (parts.isNotEmpty()) {
                    Text(
                        parts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalLifeOSColors.current.textFaint,
                    )
                }
            }
            if (session.completedAt != null) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = SemanticSuccess,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Steps input dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepsInputDialog(
    current: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf(if (current > 0) current.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Steps") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter(Char::isDigit) },
                label = { Text("Steps taken today") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            Button(
                onClick = { input.toIntOrNull()?.let { onConfirm(it) } },
                enabled = input.toIntOrNull()?.let { it > 0 } == true,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
