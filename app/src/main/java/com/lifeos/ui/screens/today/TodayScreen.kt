package com.lifeos.ui.screens.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.LifeOSApp
import com.lifeos.data.db.entity.ProteinLogEntity
import com.lifeos.data.db.entity.StepReadingEntity
import com.lifeos.data.db.entity.WaterLogEntity
import com.lifeos.ui.theme.LocalLifeOSColors
import com.lifeos.ui.theme.SemanticSuccess
import com.lifeos.ui.theme.SemanticWarning
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TodayScreen(onNavigateToPlacement: () -> Unit = {}) {
    val context = LocalContext.current
    val db = (context.applicationContext as LifeOSApp).database
    val vm: TodayViewModel = viewModel(factory = TodayViewModel.Factory(db))
    val s by vm.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Greeting ────────────────────────────────────────────────────────
        item(key = "greeting") {
            val dayName = s.today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val hour = java.time.LocalDateTime.now().hour
            val greeting = when (hour) {
                in 5..11 -> "Good morning"
                in 12..17 -> "Good afternoon"
                else -> "Good evening"
            }
            val name = s.displayName.ifBlank { null }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (name != null) "$greeting, $name" else greeting,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = dayName + " · " + s.today.format(DateTimeFormatter.ofPattern("MMM d")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalLifeOSColors.current.textFaint,
                )
            }
        }

        // ── Streak row ──────────────────────────────────────────────────────
        item(key = "streak") {
            StreakRow(streak = s.currentStreak, gymStreak = s.gymStreak)
        }

        // ── Due revisions banner ────────────────────────────────────────────
        if (s.dueRevisionCount > 0) {
            item(key = "revisions") {
                RevisionBanner(count = s.dueRevisionCount, onClick = onNavigateToPlacement)
            }
        }

        // ── Protein card ────────────────────────────────────────────────────
        item(key = "protein") {
            MetricCard(
                label = "Protein",
                emoji = "🥩",
                current = s.proteinGrams,
                goal = s.proteinGoal,
                unit = "g",
                quickAmounts = listOf(25, 50, 100),
                onAdd = vm::addProtein,
                logs = s.proteinLogs.map { LogEntry(it.id, "+${it.grams}g", it.loggedAt.take(16).takeLast(5)) },
                onDeleteLog = vm::deleteProteinLog,
            )
        }

        // ── Water card ──────────────────────────────────────────────────────
        item(key = "water") {
            MetricCard(
                label = "Water",
                emoji = "💧",
                current = s.waterMl,
                goal = s.waterGoal,
                unit = "ml",
                quickAmounts = listOf(250, 500, 1000),
                onAdd = vm::addWater,
                logs = s.waterLogs.map { LogEntry(it.id, "+${it.ml}ml", it.loggedAt.take(16).takeLast(5)) },
                onDeleteLog = vm::deleteWaterLog,
            )
        }

        // ── Steps card ──────────────────────────────────────────────────────
        item(key = "steps") {
            MetricCard(
                label = "Steps",
                emoji = "👟",
                current = s.stepsTotal,
                goal = s.stepGoal,
                unit = " steps",
                quickAmounts = listOf(1000, 2500, 5000),
                onAdd = vm::addSteps,
                logs = s.stepLogs.map { LogEntry(it.id, "+${it.steps}", it.syncedAt?.take(16)?.takeLast(5) ?: "") },
                onDeleteLog = vm::deleteStepLog,
            )
        }

        // ── Daily goals ─────────────────────────────────────────────────────
        if (s.goals.isNotEmpty()) {
            item(key = "goals_header") {
                SectionHeader("Today's Goals")
            }
            items(s.goals, key = { "goal_${it.id}" }) { goal ->
                val done = s.completions[goal.id] == true
                GoalRow(
                    label = goal.title,
                    done = done,
                    onToggle = { vm.toggleGoal(goal.id, done) },
                )
            }
        }

        // ── Hobbies ─────────────────────────────────────────────────────────
        if (s.hobbies.isNotEmpty()) {
            item(key = "hobbies_header") {
                SectionHeader("Hobbies")
            }
            items(s.hobbies, key = { "hobby_${it.id}" }) { hobby ->
                val mins = s.hobbyLogs[hobby.id] ?: 0
                HobbyRow(
                    label = hobby.name,
                    minutes = mins,
                    onLog = { vm.logHobbyMinutes(hobby.id, it) },
                )
            }
        }

        // ── Journal reflection ───────────────────────────────────────────────
        item(key = "journal") {
            JournalCard(
                entry = s.journalEntry,
                onSave = { text, mood -> vm.saveReflection(text, mood) },
            )
        }

        item(key = "bottom") { Spacer(Modifier.height(88.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Streak row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StreakRow(streak: Int, gymStreak: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SemanticWarning.copy(alpha = 0.15f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.LocalFireDepartment, null, tint = SemanticWarning, modifier = Modifier.size(18.dp))
                Text(
                    text = "$streak day streak",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = SemanticWarning,
                )
            }
        }
        if (gymStreak > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = "💪 $gymStreak gym",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Revision banner — promoted to a full card when reviews are due
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RevisionBanner(count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.MenuBook, null, modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        "$count problem${if (count > 1) "s" else ""} due for review",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("Tap to open Placement →", style = MaterialTheme.typography.bodySmall, color = LocalLifeOSColors.current.textFaint)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Metric card with collapsible log
// ─────────────────────────────────────────────────────────────────────────────

data class LogEntry(val id: Long, val label: String, val time: String)

@Composable
private fun MetricCard(
    label: String,
    emoji: String,
    current: Int,
    goal: Int,
    unit: String,
    quickAmounts: List<Int>,
    onAdd: (Int) -> Unit,
    logs: List<LogEntry>,
    onDeleteLog: (Long) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showCustom by rememberSaveable { mutableStateOf(false) }
    var customInput by rememberSaveable { mutableStateOf("") }

    val progress = if (goal > 0) (current.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val done = current >= goal

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LocalLifeOSColors.current.surface1),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji)
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$current$unit / $goal$unit",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (done) SemanticSuccess else LocalLifeOSColors.current.textFaint,
                    )
                    if (logs.isNotEmpty()) {
                        IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = "Toggle log",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (done) SemanticSuccess else MaterialTheme.colorScheme.primary,
            )

            // Quick-add chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickAmounts.forEach { amount ->
                    FilterChip(
                        selected = false,
                        onClick = { onAdd(amount) },
                        label = { Text("+$amount") },
                    )
                }
                FilterChip(
                    selected = showCustom,
                    onClick = { showCustom = !showCustom; customInput = "" },
                    label = { Text("Custom") },
                )
            }

            // Custom input
            AnimatedVisibility(showCustom) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Amount") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Button(onClick = {
                        customInput.toIntOrNull()?.let { onAdd(it); showCustom = false; customInput = "" }
                    }) { Text("Add") }
                }
            }

            // Expandable log
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                    Text("Today's log", style = MaterialTheme.typography.labelSmall, color = LocalLifeOSColors.current.textFaint)
                    logs.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(entry.label, style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.time, style = MaterialTheme.typography.labelSmall, color = LocalLifeOSColors.current.textFaint)
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = { onDeleteLog(entry.id) },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(Icons.Outlined.Close, "Delete entry", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Goals / Hobbies / Journal
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun GoalRow(label: String, done: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (done) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                contentDescription = null,
                tint = if (done) SemanticSuccess else LocalLifeOSColors.current.textFaint,
                modifier = Modifier.size(20.dp),
            )
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Checkbox(checked = done, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun HobbyRow(label: String, minutes: Int, onLog: (Int) -> Unit) {
    var showInput by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf(if (minutes > 0) minutes.toString() else "") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (showInput) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() } },
                    label = { Text("min") },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                )
                Button(
                    onClick = { onLog(input.toIntOrNull() ?: 0); showInput = false },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) { Text("Save") }
            }
        } else {
            FilledTonalIconButton(onClick = { showInput = true; input = if (minutes > 0) minutes.toString() else "" }) {
                Text(if (minutes > 0) "${minutes}m" else "+", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun JournalCard(
    entry: com.lifeos.data.db.entity.JournalEntryEntity?,
    onSave: (String, String?) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(entry?.reflectionMarkdown ?: "") }
    var selectedMood by rememberSaveable { mutableStateOf(entry?.mood) }
    val moods = listOf("great" to "😄", "good" to "🙂", "okay" to "😐", "low" to "😔", "rough" to "😞")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LocalLifeOSColors.current.surface1),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Daily Reflection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                moods.forEach { (key, emoji) ->
                    FilterChip(
                        selected = selectedMood == key,
                        onClick = { selectedMood = if (selectedMood == key) null else key },
                        label = { Text(emoji) },
                    )
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("How did today go?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
            )
            Button(
                onClick = { onSave(text, selectedMood) },
                modifier = Modifier.align(Alignment.End),
            ) { Text("Save") }
        }
    }
}
