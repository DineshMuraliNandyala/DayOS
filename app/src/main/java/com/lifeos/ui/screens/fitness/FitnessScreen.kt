package com.lifeos.ui.screens.fitness

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.LifeOSApp
import com.lifeos.data.db.entity.ExerciseEntity
import com.lifeos.data.db.entity.ExerciseSetLogEntity
import com.lifeos.data.db.entity.WorkoutSessionEntity
import com.lifeos.ui.theme.LocalLifeOSColors
import com.lifeos.ui.theme.SemanticSuccess
import com.lifeos.ui.theme.SemanticWarning
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FitnessScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as LifeOSApp).database
    val vm: FitnessViewModel = viewModel(factory = FitnessViewModel.Factory(db))
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddExercise by rememberSaveable { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<ExerciseEntity?>(null) }

    if (showAddExercise || editingExercise != null) {
        AddExerciseSheet(
            initial = editingExercise,
            onSave = { vm.saveExercise(it); showAddExercise = false; editingExercise = null },
            onArchive = { vm.archiveExercise(it); editingExercise = null },
            onDismiss = { showAddExercise = false; editingExercise = null },
        )
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Programme") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Log") })
            }
            when (selectedTab) {
                0 -> ProgrammeTab(uiState = uiState, onEditExercise = { editingExercise = it })
                1 -> LogTab(uiState = uiState, vm = vm)
            }
        }
        if (selectedTab == 0) {
            FloatingActionButton(
                onClick = { showAddExercise = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 88.dp),
                containerColor = MaterialTheme.colorScheme.primary,
            ) { Icon(Icons.Outlined.Add, "Add exercise") }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Programme tab — 7-day plan grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProgrammeTab(
    uiState: FitnessUiState,
    onEditExercise: (ExerciseEntity) -> Unit,
) {
    val weekdays = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedDay by rememberSaveable { mutableStateOf(LocalDate.now().dayOfWeek.name.take(3).lowercase()) }

    Column(Modifier.fillMaxSize()) {
        // Day selector chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            weekdays.forEachIndexed { i, day ->
                item {
                    FilterChip(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        label = { Text(dayLabels[i]) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        }

        val exercises = uiState.programmeByDay[selectedDay] ?: emptyList()
        if (exercises.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.FitnessCenter, null, modifier = Modifier.size(48.dp), tint = LocalLifeOSColors.current.textFaint)
                    Text("No exercises for ${dayLabels[weekdays.indexOf(selectedDay)]}", color = LocalLifeOSColors.current.textFaint)
                    Text("Tap + to add exercises to this day", style = MaterialTheme.typography.bodySmall, color = LocalLifeOSColors.current.textFaint)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(exercises, key = { it.id }) { exercise ->
                    ProgrammeExerciseCard(exercise = exercise, onEdit = { onEditExercise(exercise) })
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }
}

@Composable
private fun ProgrammeExerciseCard(exercise: ExerciseEntity, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LocalLifeOSColors.current.surface1),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(exercise.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${exercise.muscleGroup} · ${exercise.targetSets}×${exercise.targetReps}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalLifeOSColors.current.textFaint,
                )
            }
            if (exercise.bestPrKg != null) {
                Surface(shape = RoundedCornerShape(8.dp), color = SemanticWarning.copy(alpha = 0.15f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.EmojiEvents, null, modifier = Modifier.size(14.dp), tint = SemanticWarning)
                        Text("PR ${exercise.bestPrKg.toInt()}kg", style = MaterialTheme.typography.labelSmall, color = SemanticWarning)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Log tab — date picker + session + set logging
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogTab(uiState: FitnessUiState, vm: FitnessViewModel) {
    val today = LocalDate.now()
    // Build Mon-Sun of the current week
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Date chip strip ──────────────────────────────────────────────────
        item(key = "date_chips") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(weekDays) { date ->
                    val isSelected = date == uiState.selectedLogDate
                    val hasSession = uiState.weekSessions.any { it.date == date.format(DateTimeFormatter.ISO_LOCAL_DATE) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = isSelected,
                            onClick = { vm.setLogDate(date) },
                            label = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
                                    Text(date.dayOfMonth.toString(), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                        if (hasSession) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SemanticSuccess),
                            )
                        }
                    }
                }
            }
        }

        // ── Session banner ───────────────────────────────────────────────────
        item(key = "session_banner") {
            val session = uiState.activeSession
            val dateLabel = uiState.selectedLogDate.format(DateTimeFormatter.ofPattern("EEE MMM d"))
            if (session == null) {
                FilledTonalButton(onClick = { vm.startSession() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.FitnessCenter, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start workout for $dateLabel")
                }
            } else if (session.completedAt == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Session in progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(dateLabel, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { vm.finishWorkout() }) { Text("Finish") }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = SemanticSuccess.copy(alpha = 0.12f),
                ) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Workout done ✓", fontWeight = FontWeight.SemiBold)
                            Text(
                                "${session.durationMinutes ?: "?"} min · ${session.totalVolumeKg?.toInt() ?: 0}kg volume" +
                                    if ((session.newPrCount) > 0) " · 🏆 ${session.newPrCount} PR" else "",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        IconButton(onClick = { vm.deleteSession(session.id) }) {
                            Icon(Icons.Outlined.Delete, "Delete session", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // ── Exercise cards with inline set logging ───────────────────────────
        if (uiState.exercisesWithSets.isEmpty()) {
            item(key = "empty_log") {
                Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No exercises scheduled for ${uiState.selectedLogDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}.\nAdd them in the Programme tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalLifeOSColors.current.textFaint,
                    )
                }
            }
        } else {
            items(uiState.exercisesWithSets, key = { it.exercise.id }) { item ->
                ExerciseLogCard(exerciseWithSets = item, onLogSet = vm::logSet, onDeleteSet = vm::deleteSet)
            }
        }

        item(key = "bottom") { Spacer(Modifier.height(88.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseLogCard(
    exerciseWithSets: ExerciseWithSets,
    onLogSet: (Long, Double, Int) -> Unit,
    onDeleteSet: (Long) -> Unit,
) {
    val exercise = exerciseWithSets.exercise
    var weightInput by rememberSaveable(exercise.id) { mutableStateOf("") }
    var repsInput by rememberSaveable(exercise.id) { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LocalLifeOSColors.current.surface1),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(exercise.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${exercise.targetSets}×${exercise.targetReps} · ${exercise.muscleGroup}",
                        style = MaterialTheme.typography.bodySmall, color = LocalLifeOSColors.current.textFaint)
                }
                if (exercise.bestPrKg != null) {
                    Surface(shape = RoundedCornerShape(6.dp), color = SemanticWarning.copy(alpha = 0.15f)) {
                        Text("PR ${exercise.bestPrKg.toInt()}kg",
                            style = MaterialTheme.typography.labelSmall, color = SemanticWarning,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }

            // Logged sets with swipe-to-dismiss
            exerciseWithSets.sets.forEachIndexed { i, set ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) { onDeleteSet(set.id); true } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(end = 16.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.onErrorContainer) }
                    },
                    enableDismissFromStartToEnd = false,
                ) {
                    Surface(
                        color = LocalLifeOSColors.current.surface2,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Set ${i + 1}", style = MaterialTheme.typography.bodySmall, color = LocalLifeOSColors.current.textFaint)
                            Text("${set.weightKg}kg × ${set.reps}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            val oneRm = if (set.reps == 1) set.weightKg else set.weightKg * (1 + set.reps / 30.0)
                            Text("~${oneRm.toInt()}kg 1RM", style = MaterialTheme.typography.labelSmall, color = LocalLifeOSColors.current.textFaint)
                        }
                    }
                }
            }

            // Input row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("kg") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it.filter { c -> c.isDigit() } },
                    label = { Text("reps") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        val w = weightInput.toDoubleOrNull() ?: return@Button
                        val r = repsInput.toIntOrNull() ?: return@Button
                        onLogSet(exercise.id, w, r)
                        weightInput = ""; repsInput = ""
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) { Text("Log") }
            }
        }
    }
}
