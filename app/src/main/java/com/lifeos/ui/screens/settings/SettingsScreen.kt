package com.lifeos.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.LifeOSApp
import com.lifeos.data.db.entity.SettingsEntity
import com.lifeos.ui.theme.LocalLifeOSColors
import com.lifeos.ui.theme.SemanticDanger
import kotlinx.coroutines.flow.collectLatest

// ─────────────────────────────────────────────────────────────────────────────
// Entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val db = (context.applicationContext as LifeOSApp).database
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(context, db))

    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // ── File picker for restore ────────────────────────────────────────────
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { vm.onBackupFileSelected(it) } }

    // ── One-shot events ────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is SettingsEvent.ShareFile -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, event.chooserTitle))
                }
                is SettingsEvent.ShareText -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, event.text)
                    }
                    context.startActivity(Intent.createChooser(intent, event.chooserTitle))
                }
                is SettingsEvent.ShowSuccess ->
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                is SettingsEvent.ShowError ->
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Long)
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────
    var showBackupDialog by remember { mutableStateOf(false) }
    var showWipeDialog by remember { mutableStateOf(false) }

    if (showBackupDialog) {
        PassphraseDialog(
            title = "Encrypt Backup",
            body = "Enter a passphrase to encrypt your backup. " +
                "You will need this passphrase to restore — it is never stored.",
            confirmLabel = "Create Backup",
            needsConfirmation = true,
            inProgress = uiState.backupInProgress,
            onConfirm = { passphrase ->
                showBackupDialog = false
                vm.createBackup(passphrase)
            },
            onDismiss = { showBackupDialog = false },
        )
    }

    // Passphrase dialog for restore — appears after user picks a file
    if (uiState.pendingRestoreUri != null) {
        PassphraseDialog(
            title = "Restore Backup",
            body = "Enter the passphrase used when this backup was created.",
            confirmLabel = "Restore",
            needsConfirmation = false,
            inProgress = uiState.restoreInProgress,
            onConfirm = { passphrase -> vm.restoreBackup(passphrase) },
            onDismiss = { vm.clearPendingRestore() },
        )
    }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = { Text("Wipe All Data?") },
            text = {
                Text(
                    "This permanently deletes all your goals, habits, workouts, " +
                    "journal entries, and problems. This cannot be undone.",
                )
            },
            confirmButton = {
                Button(
                    onClick = { showWipeDialog = false; vm.wipeAllData() },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticDanger),
                ) { Text("Wipe Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) { Text("Cancel") }
            },
        )
    }

    // ── Layout ─────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        if (settings == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val s = settings!! // guaranteed non-null here
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // ── Profile ──────────────────────────────────────────────
                item { SettingSectionHeader("Profile") }
                item {
                    SettingTextField(
                        label = "Display name",
                        value = s.displayName ?: "",
                        onValueChange = { vm.updateDisplayName(it) },
                        placeholder = "Your name (shown in greeting)",
                    )
                }

                // ── Appearance ───────────────────────────────────────────
                item { SettingSectionHeader("Appearance") }
                item {
                    ThemePicker(
                        selected = s.theme,
                        onSelect = { vm.updateTheme(it) },
                    )
                }
                item {
                    AccentPicker(
                        selected = s.accentDomain,
                        onSelect = { vm.updateAccent(it) },
                    )
                }

                // ── Daily goals ──────────────────────────────────────────
                item { SettingSectionHeader("Daily Goals") }
                item {
                    SettingNumberField(
                        label = "Protein goal",
                        value = s.proteinGoalGrams,
                        unit = "g",
                        onValueChange = { vm.updateProteinGoal(it) },
                    )
                }
                item {
                    SettingNumberField(
                        label = "Water goal",
                        value = s.waterGoalMl,
                        unit = "ml",
                        onValueChange = { vm.updateWaterGoal(it) },
                    )
                }
                item {
                    SettingNumberField(
                        label = "Step goal",
                        value = s.stepGoal,
                        unit = "steps",
                        onValueChange = { vm.updateStepGoal(it) },
                    )
                }
                item {
                    SettingNumberField(
                        label = "Weekly coding goal",
                        value = s.weeklyCodingGoal,
                        unit = "problems",
                        onValueChange = { vm.updateCodingGoal(it) },
                    )
                }

                // ── Notifications ─────────────────────────────────────────
                item { SettingSectionHeader("Notifications") }
                item {
                    SettingSwitch(
                        label = "Enable reminders",
                        checked = s.notificationsEnabled,
                        onCheckedChange = { vm.updateNotificationsEnabled(it) },
                    )
                }
                if (s.notificationsEnabled) {
                    item {
                        SettingTextField(
                            label = "Morning reminder",
                            value = s.reminderTimeMorning ?: "",
                            onValueChange = { vm.updateReminderMorning(it) },
                            placeholder = "HH:mm (e.g. 07:30)",
                            keyboardType = KeyboardType.Number,
                        )
                    }
                    item {
                        SettingTextField(
                            label = "Evening reminder",
                            value = s.reminderTimeEvening ?: "",
                            onValueChange = { vm.updateReminderEvening(it) },
                            placeholder = "HH:mm (e.g. 21:30)",
                            keyboardType = KeyboardType.Number,
                        )
                    }
                    item {
                        SettingTextField(
                            label = "Gym reminder",
                            value = s.reminderTimeGym ?: "",
                            onValueChange = { vm.updateReminderGym(it) },
                            placeholder = "HH:mm (e.g. 18:00)",
                            keyboardType = KeyboardType.Number,
                        )
                    }
                }

                // ── Data ──────────────────────────────────────────────────
                item { SettingSectionHeader("Data") }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        color = LocalLifeOSColors.current.surface1,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DataInfoBanner()
                            Divider()

                            // Backup
                            DataActionRow(
                                icon = Icons.Outlined.Archive,
                                title = "Encrypted Backup",
                                subtitle = "AES-256 + PBKDF2 — save to any location",
                                inProgress = uiState.backupInProgress,
                                onClick = { showBackupDialog = true },
                            )

                            // Restore
                            DataActionRow(
                                icon = Icons.Outlined.Restore,
                                title = "Restore from Backup",
                                subtitle = "Replaces all current data — app will restart",
                                inProgress = uiState.restoreInProgress,
                                onClick = { restoreLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                            )

                            // Markdown export
                            DataActionRow(
                                icon = Icons.Outlined.Share,
                                title = "Export Journal as Markdown",
                                subtitle = "Human-readable, one-way — share to any app",
                                inProgress = uiState.exportInProgress,
                                onClick = { vm.exportMarkdown() },
                            )
                        }
                    }
                }

                // ── Danger zone ───────────────────────────────────────────
                item { Spacer(Modifier.height(24.dp)) }
                item {
                    OutlinedButton(
                        onClick = { showWipeDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticDanger),
                        border = BorderStroke(1.dp, SemanticDanger),
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("Wipe All Data")
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable setting components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SettingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = LocalLifeOSColors.current.textFaint) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}

@Composable
private fun SettingNumberField(
    label: String,
    value: Int,
    unit: String,
    onValueChange: (Int) -> Unit,
) {
    var text by rememberSaveable(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input.filter(Char::isDigit)
            text.toIntOrNull()?.let { onValueChange(it) }
        },
        label = { Text(label) },
        suffix = { Text(unit, color = LocalLifeOSColors.current.textFaint) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemePicker(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("dark" to "Dark", "amoled" to "AMOLED", "light" to "Light")
    SegmentedPicker(label = "Theme", options = options, selected = selected, onSelect = onSelect)
}

@Composable
private fun AccentPicker(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("focus" to "Focus", "energy" to "Energy", "calm" to "Calm")
    SegmentedPicker(label = "Accent", options = options, selected = selected, onSelect = onSelect)
}

@Composable
private fun SegmentedPicker(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (key, displayName) ->
                val isSelected = key == selected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(key) },
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else LocalLifeOSColors.current.surface2,
                ) {
                    Text(
                        text = displayName,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun DataInfoBanner() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "No cloud sync",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Your data stays on this device. " +
                "The encrypted backup file (.lbk) is AES-256 protected — " +
                "only accessible with your passphrase.",
            style = MaterialTheme.typography.bodySmall,
            color = LocalLifeOSColors.current.textFaint,
        )
    }
}

@Composable
private fun DataActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    inProgress: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !inProgress, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (inProgress) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalLifeOSColors.current.textFaint)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Passphrase dialog (shared by backup + restore flows)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PassphraseDialog(
    title: String,
    body: String,
    confirmLabel: String,
    needsConfirmation: Boolean,
    inProgress: Boolean,
    onConfirm: (passphrase: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val mismatch = needsConfirmation && confirm.isNotEmpty() && passphrase != confirm

    AlertDialog(
        onDismissRequest = { if (!inProgress) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(body, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (needsConfirmation) {
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("Confirm passphrase") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        isError = mismatch,
                        supportingText = if (mismatch) ({ Text("Passphrases don't match") }) else null,
                    )
                }
                if (inProgress) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Deriving encryption key…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(passphrase) },
                enabled = passphrase.length >= 6 && !mismatch && !inProgress,
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !inProgress) { Text("Cancel") }
        },
    )
}
