package com.lifeos.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeos.data.backup.BackupManager
import com.lifeos.data.db.LifeOSDatabase
import com.lifeos.data.db.entity.SettingsEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// State + Events
// ─────────────────────────────────────────────────────────────────────────────

data class SettingsUiState(
    val settings: SettingsEntity = SettingsEntity(createdAt = "", updatedAt = ""),
    val isLoading: Boolean = true,
    val backupInProgress: Boolean = false,
    val restoreInProgress: Boolean = false,
    val exportInProgress: Boolean = false,
    /** URI of file awaiting restore — set after user picks file, cleared after passphrase dialog */
    val pendingRestoreUri: Uri? = null,
)

/**
 * One-shot events emitted by [SettingsViewModel] for the screen to handle.
 * Each event is consumed at most once (SharedFlow, 0 replay).
 */
sealed interface SettingsEvent {
    /** Ask the OS share sheet to send the backup file at [uri]. */
    data class ShareFile(val uri: Uri, val mimeType: String, val chooserTitle: String) : SettingsEvent
    /** Ask the OS share sheet to send markdown text. */
    data class ShareText(val text: String, val chooserTitle: String) : SettingsEvent
    /** Show a transient success message. */
    data class ShowSuccess(val message: String) : SettingsEvent
    /** Show a transient error message. */
    data class ShowError(val message: String) : SettingsEvent
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class SettingsViewModel(
    private val context: Context,
    private val db: LifeOSDatabase,
) : ViewModel() {

    private val settingsDao = db.settingsDao()
    private val backupManager = BackupManager(context, db)

    // ── Settings state (live from DB) ────────────────────────────────────────

    val settings: StateFlow<SettingsEntity?> = settingsDao.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── UI state ─────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // ── One-shot events ───────────────────────────────────────────────────────

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    // ── Settings mutations ────────────────────────────────────────────────────

    fun updateDisplayName(name: String) = save { it.copy(displayName = name.ifBlank { null }) }
    fun updateTheme(theme: String) = save { it.copy(theme = theme) }
    fun updateAccent(accent: String) = save { it.copy(accentDomain = accent) }
    fun updateProteinGoal(grams: Int) = save { it.copy(proteinGoalGrams = grams) }
    fun updateWaterGoal(ml: Int) = save { it.copy(waterGoalMl = ml) }
    fun updateStepGoal(steps: Int) = save { it.copy(stepGoal = steps) }
    fun updateCodingGoal(problems: Int) = save { it.copy(weeklyCodingGoal = problems) }
    fun updateNotificationsEnabled(enabled: Boolean) = save { it.copy(notificationsEnabled = enabled) }
    fun updateReminderMorning(time: String?) = save { it.copy(reminderTimeMorning = time?.ifBlank { null }) }
    fun updateReminderEvening(time: String?) = save { it.copy(reminderTimeEvening = time?.ifBlank { null }) }
    fun updateReminderGym(time: String?) = save { it.copy(reminderTimeGym = time?.ifBlank { null }) }

    private fun save(transform: (SettingsEntity) -> SettingsEntity) {
        viewModelScope.launch {
            val current = settings.value ?: return@launch
            settingsDao.upsert(transform(current).copy(updatedAt = java.time.Instant.now().toString()))
        }
    }

    // ── Backup ────────────────────────────────────────────────────────────────

    /**
     * Creates an encrypted .lbk backup and emits a [SettingsEvent.ShareFile]
     * so the screen can open the OS share sheet.
     *
     * PBKDF2 derivation takes ~1-2 s — a loading indicator is shown via
     * [SettingsUiState.backupInProgress] during this time.
     */
    fun createBackup(passphrase: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(backupInProgress = true) }
            runCatching { backupManager.createBackup(passphrase) }
                .onSuccess { uri ->
                    _events.emit(
                        SettingsEvent.ShareFile(
                            uri = uri,
                            mimeType = "application/octet-stream",
                            chooserTitle = "Save LifeOS Backup",
                        ),
                    )
                    _events.emit(SettingsEvent.ShowSuccess("Backup created"))
                }
                .onFailure { e ->
                    _events.emit(SettingsEvent.ShowError("Backup failed: ${e.message}"))
                }
            _uiState.update { it.copy(backupInProgress = false) }
        }
    }

    /** Called by the file picker result — stores the URI until the passphrase dialog confirms. */
    fun onBackupFileSelected(uri: Uri) {
        _uiState.update { it.copy(pendingRestoreUri = uri) }
    }

    fun clearPendingRestore() {
        _uiState.update { it.copy(pendingRestoreUri = null) }
    }

    /**
     * Restores the database from the pending backup URI using [passphrase].
     * On success the process is killed and the OS restarts the app.
     * On failure emits [SettingsEvent.ShowError] — the current DB is untouched.
     */
    fun restoreBackup(passphrase: String) {
        val uri = _uiState.value.pendingRestoreUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(restoreInProgress = true, pendingRestoreUri = null) }
            runCatching {
                backupManager.restoreBackup(uri, passphrase) // noreturn on success
            }.onFailure { e ->
                _events.emit(
                    SettingsEvent.ShowError(
                        when {
                            e.message?.contains("AEADBadTagException") == true ||
                            e is javax.crypto.AEADBadTagException ->
                                "Wrong passphrase or corrupted backup"
                            e.message?.contains("magic") == true ->
                                "Not a valid LifeOS backup file"
                            else -> "Restore failed: ${e.message}"
                        },
                    ),
                )
                _uiState.update { it.copy(restoreInProgress = false) }
            }
        }
    }

    // ── Markdown export ───────────────────────────────────────────────────────

    fun exportMarkdown() {
        viewModelScope.launch {
            _uiState.update { it.copy(exportInProgress = true) }
            runCatching { backupManager.exportMarkdown() }
                .onSuccess { markdown ->
                    _events.emit(
                        SettingsEvent.ShareText(
                            text = markdown,
                            chooserTitle = "Export Journal",
                        ),
                    )
                }
                .onFailure { e ->
                    _events.emit(SettingsEvent.ShowError("Export failed: ${e.message}"))
                }
            _uiState.update { it.copy(exportInProgress = false) }
        }
    }

    // ── Wipe ──────────────────────────────────────────────────────────────────

    /** Deletes all app data and restarts. Irreversible — show a confirmation dialog first. */
    fun wipeAllData() {
        viewModelScope.launch {
            db.close()
            context.getDatabasePath("lifeos.db").delete()
            context.getDatabasePath("lifeos.db-wal").delete()
            context.getDatabasePath("lifeos.db-shm").delete()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(
        private val context: Context,
        private val db: LifeOSDatabase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(context, db) as T
    }
}
