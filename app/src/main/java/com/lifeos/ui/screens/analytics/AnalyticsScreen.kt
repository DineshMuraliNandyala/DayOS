package com.lifeos.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.LifeOSApp
import com.lifeos.ui.theme.LocalLifeOSColors
import com.lifeos.ui.theme.SemanticSuccess
import com.lifeos.ui.theme.SemanticWarning

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as LifeOSApp).database
    val vm: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.Factory(db))
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header stat row ─────────────────────────────────────────────────
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    label = "Streak",
                    value = "${uiState.currentStreak}d",
                    color = SemanticWarning,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Problems",
                    value = "${uiState.totalProblems}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Heatmap section ─────────────────────────────────────────────────
        item(key = "heatmap_title") {
            Text(
                "Activity  — last 12 weeks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        item(key = "heatmap") {
            HeatmapGrid(cells = uiState.heatmapCells)
        }

        // Heatmap legend
        item(key = "legend") {
            HeatmapLegend()
        }

        // ── Weekly summaries ────────────────────────────────────────────────
        if (uiState.weeklySummaries.isNotEmpty()) {
            item(key = "weekly_title") {
                Text(
                    "Weekly Snapshots",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(uiState.weeklySummaries, key = { it.weekLabel }) { summary ->
                WeeklySummaryCard(summary)
            }
        }

        item(key = "bottom") { Spacer(Modifier.height(88.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Heatmap
// ─────────────────────────────────────────────────────────────────────────────

/** Color for each activity level 0..4 (GitHub-style green progression). */
@Composable
private fun heatmapColor(level: Int): Color = when (level) {
    0 -> LocalLifeOSColors.current.surface2
    1 -> SemanticSuccess.copy(alpha = 0.20f)
    2 -> SemanticSuccess.copy(alpha = 0.45f)
    3 -> SemanticSuccess.copy(alpha = 0.70f)
    4 -> SemanticSuccess
    else -> LocalLifeOSColors.current.surface2
}

@Composable
private fun HeatmapGrid(cells: List<HeatmapCell>) {
    // 84 cells arranged in 12 columns (weeks) × 7 rows (days)
    // LazyVerticalGrid with fixed 12 columns, fixed height
    val cellSize = 18.dp
    val gap = 3.dp
    val gridHeight = (cellSize * 7) + (gap * 6) + gap * 2

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight),
        shape = RoundedCornerShape(12.dp),
        color = LocalLifeOSColors.current.surface1,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(12),
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            userScrollEnabled = false,
        ) {
            items(cells, key = { it.date }) { cell ->
                Box(
                    modifier = Modifier
                        .size(cellSize)
                        .clip(RoundedCornerShape(3.dp))
                        .background(heatmapColor(cell.level)),
                )
            }
        }
    }
}

@Composable
private fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Less",
            style = MaterialTheme.typography.labelSmall,
            color = LocalLifeOSColors.current.textFaint,
        )
        Spacer(Modifier.width(4.dp))
        (0..4).forEach { level ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .padding(horizontal = 1.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when (level) {
                            0 -> Color.Gray.copy(alpha = 0.2f)
                            1 -> SemanticSuccess.copy(alpha = 0.20f)
                            2 -> SemanticSuccess.copy(alpha = 0.45f)
                            3 -> SemanticSuccess.copy(alpha = 0.70f)
                            else -> SemanticSuccess
                        }
                    ),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "More",
            style = MaterialTheme.typography.labelSmall,
            color = LocalLifeOSColors.current.textFaint,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Weekly summary card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklySummaryCard(summary: WeeklySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LocalLifeOSColors.current.surface1),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                summary.weekLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryChip("💪 ${summary.workoutsCompleted} workouts", Modifier.weight(1f))
                SummaryChip("💻 ${summary.problemsSolved} problems", Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (summary.avgProteinG > 0)
                    SummaryChip("🥩 ${summary.avgProteinG}g protein avg", Modifier.weight(1f))
                if (summary.avgSteps > 0)
                    SummaryChip("👟 ${summary.avgSteps} steps avg", Modifier.weight(1f))
            }
            if (summary.revisionsCompleted > 0) {
                SummaryChip("🔄 ${summary.revisionsCompleted} revisions")
            }
        }
    }
}

@Composable
private fun SummaryChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = LocalLifeOSColors.current.surface2,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stat tile (header)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = LocalLifeOSColors.current.textFaint,
            )
        }
    }
}
