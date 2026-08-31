package com.lifeos.ui.screens.today

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.LifeOSApp
import com.lifeos.data.db.entity.DailyGoalEntity
import com.lifeos.data.db.entity.HobbyEntity
import com.lifeos.data.db.entity.JournalEntryEntity
import com.lifeos.ui.theme.LocalLifeOSColors
import com.lifeos.ui.theme.SemanticSuccess
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// Today screen — entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TodayScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as LifeOSApp).database
    val vm: TodayViewModel = viewModel(factory = TodayViewModel.Factory(db))

    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Greeting ──────────────────────────────────────────────────────────
        item(key = "header") {
            GreetingHeader(
                displayName = uiState.displayName,
                today = uiState.today,
                streak = streak,
            )
        }

        // ── Revision banner ───────────────────────────────────────────────────
        if (uiState.dueRevisionCount > 0) {
            item(key = "revision_banner") {
                RevisionBanner(count = uiState.dueRevisionCount)
            }
        }

        // ── Protein ───────────────────────────────────────────────────────────
        item(key = "protein") {
            NutritionCard(
                label = "Protein",
                current = uiState.proteinGrams,
                goal = uiState.proteinGoal,
                unit = "g",
                quickAmounts = listOf(20, 30, 50),
                accentColor = MaterialTheme.colorScheme.tertiary,
                onAdd = vm::addProtein,
            )
        }

        // ── Water ─────────────────────────────────────────────────────────────
        item(key = "water") {
            NutritionCard(
                label = "Water",
                current = uiState.waterMl,
                goal = uiState.waterGoal,
                unit = "ml",
                quickAmounts = listOf(250, 500, 1000),
                accentColor = MaterialTheme.colorScheme.primary,
                onAdd = vm::addWater,
            )
        }

        // ── Goals ─────────────────────────────────────────────────────────────
        item(key = "goals_header") { SectionHeader("Goals") }

        if (uiState.goals.isEmpty()) {
            item(key = "goals_empty") {
                EmptyPlaceholder("No goals scheduled for today")
            }
        } else {
            items(uiState.goals, key = { "goal_${it.id}" }) { goal ->
                val completed = uiState.completions[goal.id] ?: false
                GoalRow(
                    goal = goal,
                    completed = completed,
                    onToggle = { vm.toggleGoal(goal.id, completed) },
                )
            }
        }

        // ── Hobbies ───────────────────────────────────────────────────────────
        if (uiState.hobbies.isNotEmpty()) {
            item(key = "hobbies_header") { SectionHeader("Hobbies") }
            items(uiState.hobbies, key = { "hobby_${it.id}" }) { hobby ->
                val logged = (uiState.hobbyLogs[hobby.id] ?: 0) > 0
                HobbyRow(
                    hobby = hobby,
                    logged = logged,
                    onToggle = { vm.toggleHobby(hobby.id, logged, hobby.goalMinutes) },
                )
            }
        }

        // ── Reflection ────────────────────────────────────────────────────────
        item(key = "reflection") {
            ReflectionCard(
                journalEntry = uiState.journalEntry,
                onSave = { reflection, topic, mood ->
                    vm.saveJournalEntry(reflection, topic, mood)
                },
            )
        }

        item(key = "bottom_spacer") { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GreetingHeader(
    displayName: String,
    today: LocalDate,
    streak: Int,
) {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val name = displayName.ifBlank { null }
    val dateStr = today.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (name != null) "$greeting, $name" else greeting,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (streak > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = LocalLifeOSColors.current.warningDim,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalFireDepartment,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = SemanticSuccess,
                        )
                        Text(
                            text = "$streak day streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = SemanticSuccess,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RevisionBanner(count: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = LocalLifeOSColors.current.accentDim,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "$count problem${if (count == 1) "" else "s"} due for review",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun NutritionCard(
    label: String,
    current: Int,
    goal: Int,
    unit: String,
    quickAmounts: List<Int>,
    accentColor: Color,
    onAdd: (Int) -> Unit,
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false; customInput = "" },
            title = { Text("Custom $label") },
            text = {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it.filter(Char::isDigit) },
                    label = { Text("Amount ($unit)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = customInput.toIntOrNull()
                    if (amount != null && amount > 0) {
                        onAdd(amount)
                        showCustomDialog = false
                        customInput = ""
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false; customInput = "" }) {
                    Text("Cancel")
                }
            },
        )
    }

    val progress = if (goal > 0) (current.toFloat() / goal).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = LocalLifeOSColors.current.surface1,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Label + value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "$current / $goal $unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            // Quick-add chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                quickAmounts.forEach { amount ->
                    FilterChip(
                        selected = false,
                        onClick = { onAdd(amount) },
                        label = { Text("+$amount$unit", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = LocalLifeOSColors.current.surface2,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { showCustomDialog = true },
                    label = { Text("Custom", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = LocalLifeOSColors.current.surface2,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun EmptyPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalLifeOSColors.current.textFaint,
        )
    }
}

@Composable
private fun GoalRow(
    goal: DailyGoalEntity,
    completed: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(
            checked = completed,
            onCheckedChange = { onToggle() },
        )
        // Color dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    runCatching { Color(android.graphics.Color.parseColor(goal.color)) }
                        .getOrElse { MaterialTheme.colorScheme.primary },
                ),
        )
        Text(
            text = goal.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (completed) LocalLifeOSColors.current.textFaint
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HobbyRow(
    hobby: HobbyEntity,
    logged: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = logged, onCheckedChange = { onToggle() })
        Text(
            text = hobby.name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (logged) LocalLifeOSColors.current.textFaint
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${hobby.goalMinutes} min",
            style = MaterialTheme.typography.bodySmall,
            color = LocalLifeOSColors.current.textFaint,
        )
    }
}

private val MOODS = listOf(
    "great" to "😊",
    "good" to "🙂",
    "okay" to "😐",
    "low" to "😔",
    "rough" to "😰",
)

@Composable
private fun ReflectionCard(
    journalEntry: JournalEntryEntity?,
    onSave: (reflection: String, topic: String?, mood: String?) -> Unit,
) {
    // Key on journalEntry.id so state resets when the entry ID changes
    // (e.g. after the DB row is first created, id goes from null → Long).
    val entryId = journalEntry?.id

    var reflectionText by rememberSaveable(entryId) {
        mutableStateOf(journalEntry?.reflectionMarkdown ?: "")
    }
    var topicText by rememberSaveable(entryId) {
        mutableStateOf(journalEntry?.systemDesignTopic ?: "")
    }
    var selectedMood by rememberSaveable(entryId) {
        mutableStateOf(journalEntry?.mood)
    }
    var showSaved by remember { mutableStateOf(false) }

    // Debounced auto-save: 1.5 s after the last change
    LaunchedEffect(reflectionText, topicText, selectedMood) {
        delay(1_500L)
        onSave(reflectionText, topicText.ifBlank { null }, selectedMood)
        showSaved = true
        delay(1_800L)
        showSaved = false
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LocalLifeOSColors.current.surface1),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Daily Reflection",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                AnimatedVisibility(visible = showSaved, enter = fadeIn(), exit = fadeOut()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = SemanticSuccess,
                        )
                        Text(
                            text = "Saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = SemanticSuccess,
                        )
                    }
                }
            }

            // System Design Topic
            OutlinedTextField(
                value = topicText,
                onValueChange = { topicText = it },
                label = { Text("System Design Topic") },
                placeholder = { Text("e.g. Design a URL shortener") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Reflection body
            OutlinedTextField(
                value = reflectionText,
                onValueChange = { reflectionText = it },
                label = { Text("Reflection") },
                placeholder = { Text("What went well? What will you improve?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                maxLines = 8,
            )

            // Mood selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Mood",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MOODS.forEach { (moodKey, emoji) ->
                        val isSelected = selectedMood == moodKey
                        Surface(
                            modifier = Modifier.clickable {
                                selectedMood = if (isSelected) null else moodKey
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) LocalLifeOSColors.current.accentDim
                                    else LocalLifeOSColors.current.surface2,
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(emoji, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = moodKey,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else LocalLifeOSColors.current.textFaint,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
