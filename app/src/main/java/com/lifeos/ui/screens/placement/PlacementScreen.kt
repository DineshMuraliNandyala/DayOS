package com.lifeos.ui.screens.placement

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.LifeOSApp
import com.lifeos.data.db.entity.ProblemEntity
import com.lifeos.data.db.entity.SpacedRevisionEntity
import com.lifeos.domain.usecase.SpacedRepetitionUseCase
import com.lifeos.ui.theme.LocalLifeOSColors
import com.lifeos.ui.theme.SemanticSuccess
import com.lifeos.ui.theme.SemanticWarning
import com.lifeos.ui.theme.SemanticDanger
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Entry point
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacementScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as LifeOSApp).database
    val vm: PlacementViewModel = viewModel(factory = PlacementViewModel.Factory(db))

    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { 2 })

    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var editingProblem by remember { mutableStateOf<ProblemEntity?>(null) }

    // ── Add/Edit sheet ─────────────────────────────────────────────────────
    if (showAddSheet) {
        AddProblemSheet(
            initial = AddProblemState(),
            onSave = { state -> vm.addProblem(state) },
            onDismiss = { showAddSheet = false },
        )
    }
    editingProblem?.let { problem ->
        AddProblemSheet(
            initial = vm.problemToEditState(problem),
            onSave = { state -> vm.updateProblem(state) },
            onDelete = { id -> vm.deleteProblem(id) },
            onDismiss = { editingProblem = null },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Tabs ───────────────────────────────────────────────────────
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Problems (${uiState.totalSolved})") },
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Review")
                            val due = uiState.dueRevisions.size
                            if (due > 0) {
                                Badge { Text("$due") }
                            }
                        }
                    },
                )
            }

            // ── Pager content ──────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> ProblemsPage(
                        uiState = uiState,
                        onSearchChange = vm::setSearchQuery,
                        onDifficultyFilter = vm::setDifficultyFilter,
                        onPlatformFilter = vm::setPlatformFilter,
                        onOpenUrl = { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        onEdit = { editingProblem = it },
                        buildUrl = vm::buildUrl,
                    )
                    1 -> ReviewPage(
                        dueRevisions = uiState.dueRevisions,
                        onReview = { revision, outcome -> vm.review(revision, outcome) },
                        onOpenUrl = { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        buildUrl = vm::buildUrl,
                    )
                }
            }
        }

        // ── FAB (Problems tab only) ────────────────────────────────────────
        AnimatedVisibility(
            visible = pagerState.currentPage == 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 88.dp), // above bottom nav
        ) {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add problem")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Problems tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProblemsPage(
    uiState: PlacementUiState,
    onSearchChange: (String) -> Unit,
    onDifficultyFilter: (String?) -> Unit,
    onPlatformFilter: (String?) -> Unit,
    onOpenUrl: (String) -> Unit,
    onEdit: (ProblemEntity) -> Unit,
    buildUrl: (ProblemEntity) -> String?,
) {
    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Stats banner
        item(key = "stats") {
            StatsRow(
                total = uiState.totalSolved,
                easy = uiState.easySolved,
                medium = uiState.mediumSolved,
                hard = uiState.hardSolved,
            )
        }

        // Search bar
        item(key = "search") {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search by title or number…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
        }

        // Difficulty filters
        item(key = "difficulty_filters") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = uiState.selectedDifficulty == null,
                        onClick = { onDifficultyFilter(null) },
                        label = { Text("All") },
                    )
                }
                items(listOf("Easy", "Medium", "Hard")) { d ->
                    FilterChip(
                        selected = uiState.selectedDifficulty == d,
                        onClick = { onDifficultyFilter(if (uiState.selectedDifficulty == d) null else d) },
                        label = { Text(d) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = difficultyColor(d).copy(alpha = 0.18f),
                            selectedLabelColor = difficultyColor(d),
                        ),
                    )
                }
            }
        }

        if (uiState.filteredProblems.isEmpty()) {
            item(key = "empty") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (uiState.allProblems.isEmpty()) "No problems added yet.\nTap + to add your first solve."
                        else "No problems match your filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalLifeOSColors.current.textFaint,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            items(uiState.filteredProblems, key = { it.id }) { problem ->
                ProblemRow(
                    problem = problem,
                    url = buildUrl(problem),
                    onOpenUrl = onOpenUrl,
                    onEdit = onEdit,
                )
            }
        }

        item(key = "bottom") { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun StatsRow(total: Int, easy: Int, medium: Int, hard: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = LocalLifeOSColors.current.surface1,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatChip(label = "Total", count = total, color = MaterialTheme.colorScheme.primary)
            StatChip(label = "Easy", count = easy, color = SemanticSuccess)
            StatChip(label = "Medium", count = medium, color = SemanticWarning)
            StatChip(label = "Hard", count = hard, color = SemanticDanger)
        }
    }
}

@Composable
private fun StatChip(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalLifeOSColors.current.textFaint,
        )
    }
}

@Composable
private fun ProblemRow(
    problem: ProblemEntity,
    url: String?,
    onOpenUrl: (String) -> Unit,
    onEdit: (ProblemEntity) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = LocalLifeOSColors.current.surface1,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Number badge
            problem.number?.let { num ->
                Text(
                    text = "#$num",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalLifeOSColors.current.textFaint,
                    modifier = Modifier.width(42.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = problem.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DifficultyBadge(problem.difficulty)
                    PlatformBadge(problem.platform)
                }
            }

            // Open URL icon
            if (url != null) {
                IconButton(
                    onClick = { onOpenUrl(url) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Outlined.OpenInNew,
                        contentDescription = "Open problem",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Edit
            IconButton(
                onClick = { onEdit(problem) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit problem",
                    modifier = Modifier.size(18.dp),
                    tint = LocalLifeOSColors.current.textFaint,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Review tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReviewPage(
    dueRevisions: List<RevisionWithProblem>,
    onReview: (SpacedRevisionEntity, SpacedRepetitionUseCase.ReviewOutcome) -> Unit,
    onOpenUrl: (String) -> Unit,
    buildUrl: (ProblemEntity) -> String?,
) {
    if (dueRevisions.isEmpty()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("🎉", style = MaterialTheme.typography.displayMedium)
                Text(
                    "All caught up!",
                    style = MaterialTheme.typography.titleMedium,
                    color = SemanticSuccess,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "No problems due for review today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalLifeOSColors.current.textFaint,
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "review_header") {
            Text(
                text = "${dueRevisions.size} problem${if (dueRevisions.size == 1) "" else "s"} due today",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        items(dueRevisions, key = { it.revision.id }) { rwp ->
            ReviewCard(
                rwp = rwp,
                onReview = onReview,
                url = buildUrl(rwp.problem),
                onOpenUrl = onOpenUrl,
            )
        }

        item(key = "bottom") { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ReviewCard(
    rwp: RevisionWithProblem,
    onReview: (SpacedRevisionEntity, SpacedRepetitionUseCase.ReviewOutcome) -> Unit,
    url: String?,
    onOpenUrl: (String) -> Unit,
) {
    val (revision, problem) = rwp
    var reviewed by rememberSaveable(revision.id) { mutableStateOf(false) }

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
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    problem.number?.let {
                        Text("#$it", style = MaterialTheme.typography.labelSmall, color = LocalLifeOSColors.current.textFaint)
                    }
                    Text(
                        text = problem.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DifficultyBadge(problem.difficulty)
                        PlatformBadge(problem.platform)
                        Text(
                            text = "Stage ${revision.stage} · ${SpacedRepetitionUseCase.stageName(revision.stage)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalLifeOSColors.current.textFaint,
                        )
                    }
                }
                if (url != null) {
                    IconButton(onClick = { onOpenUrl(url) }) {
                        Icon(
                            Icons.Outlined.OpenInNew,
                            contentDescription = "Open problem",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Notes hint (if available)
            problem.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LocalLifeOSColors.current.surface2,
                ) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalLifeOSColors.current.textFaint,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            // Review buttons
            if (reviewed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("✓ Reviewed", color = SemanticSuccess, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Forgot
                    Button(
                        onClick = { onReview(revision, SpacedRepetitionUseCase.ReviewOutcome.FORGOT); reviewed = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SemanticDanger.copy(alpha = 0.16f), contentColor = SemanticDanger),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) { Text("😰 Forgot", style = MaterialTheme.typography.labelMedium) }

                    // Hard
                    Button(
                        onClick = { onReview(revision, SpacedRepetitionUseCase.ReviewOutcome.HARD); reviewed = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SemanticWarning.copy(alpha = 0.16f), contentColor = SemanticWarning),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) { Text("😐 Hard", style = MaterialTheme.typography.labelMedium) }

                    // Easy
                    Button(
                        onClick = { onReview(revision, SpacedRepetitionUseCase.ReviewOutcome.EASY); reviewed = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SemanticSuccess.copy(alpha = 0.16f), contentColor = SemanticSuccess),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) { Text("😊 Easy", style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared badge composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun difficultyColor(difficulty: String): Color = when (difficulty) {
    "Easy" -> SemanticSuccess
    "Medium" -> SemanticWarning
    "Hard" -> SemanticDanger
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun DifficultyBadge(difficulty: String) {
    val color = difficultyColor(difficulty)
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = difficulty,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private val PLATFORM_SHORT = mapOf(
    "LEETCODE" to "LC",
    "CODEFORCES" to "CF",
    "HACKERRANK" to "HR",
    "GFG" to "GFG",
    "CUSTOM" to "EXT",
)

@Composable
private fun PlatformBadge(platform: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = LocalLifeOSColors.current.surface2,
    ) {
        Text(
            text = PLATFORM_SHORT[platform] ?: platform.take(3),
            style = MaterialTheme.typography.labelSmall,
            color = LocalLifeOSColors.current.textFaint,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
