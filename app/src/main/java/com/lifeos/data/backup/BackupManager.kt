package com.lifeos.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.lifeos.data.db.LifeOSDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Orchestrates encrypted backup creation and restoration.
 *
 * Backup pipeline:
 *   WAL checkpoint → read .db bytes → GZIP → AES-256-GCM encrypt → cache file → share URI
 *
 * Restore pipeline:
 *   read picked URI → AES-256-GCM decrypt → GUNZIP → validate SQLite header
 *   → close Room → overwrite .db file → kill process (clean restart)
 *
 * No external dependencies — uses [BackupCrypto] (javax.crypto) and stdlib GZIP.
 *
 * All functions must be called from [Dispatchers.IO]; they switch internally
 * when needed but assume an IO dispatcher context so callers are not blocked.
 */
class BackupManager(
    private val context: Context,
    private val db: LifeOSDatabase,
) {
    // SQLite file magic: first 16 bytes of every valid SQLite 3 database file
    private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    // ── Backup ─────────────────────────────────────────────────────────────────

    /**
     * Creates an encrypted backup and returns a [FileProvider] URI that can be
     * used with [android.content.Intent.ACTION_SEND] to share / save the file.
     *
     * @param passphrase User-supplied passphrase. Never stored.
     * @return content:// URI pointing to a transient file in [Context.getCacheDir].
     *
     * @throws Exception on I/O or crypto errors — callers should catch and surface.
     */
    suspend fun createBackup(passphrase: String): Uri = withContext(Dispatchers.IO) {
        // 1. Checkpoint WAL — flush all committed WAL pages into the main .db file.
        //    TRUNCATE mode also zeroes the WAL, making the main file self-contained.
        db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")

        // 2. Read the database file
        val dbFile = context.getDatabasePath("lifeos.db")
        val dbBytes = dbFile.readBytes()

        // 3. GZIP compress (typically 60-70 % reduction for SQLite)
        val compressed = gzip(dbBytes)

        // 4. Encrypt (CPU-bound — PBKDF2 takes ~1-2 s)
        val encrypted = BackupCrypto.encrypt(compressed, passphrase)

        // 5. Write to cacheDir — old backups are cleaned up here
        val cacheDir = File(context.cacheDir, "lifeos_backups").also { it.mkdirs() }
        // Rotate: keep only the 3 most-recent cached files
        cacheDir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(2)
            ?.forEach { it.delete() }

        val filename = "lifeos-backup-${LocalDate.now()}.lbk"
        val backupFile = File(cacheDir, filename)
        backupFile.writeBytes(encrypted)

        // 6. Return a content:// URI (FileProvider) — required for ACTION_SEND
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile,
        )
    }

    // ── Restore ────────────────────────────────────────────────────────────────

    /**
     * Restores the database from an encrypted backup file.
     *
     * IMPORTANT: this function does not return normally. On success it calls
     * [android.os.Process.killProcess] to terminate the app. The OS relaunches
     * it immediately, and Room re-initialises against the restored database.
     *
     * On failure it throws an exception — the caller should display the error
     * and leave the existing database intact.
     *
     * @param fileUri  [Uri] from [android.activity.result.contract.ActivityResultContracts.OpenDocument].
     * @param passphrase The passphrase used when the backup was created.
     *
     * @throws IllegalArgumentException on format or version errors.
     * @throws javax.crypto.AEADBadTagException on wrong passphrase or file tampering.
     * @throws IllegalStateException if the decrypted content is not a SQLite database.
     */
    suspend fun restoreBackup(fileUri: Uri, passphrase: String): Nothing =
        withContext(Dispatchers.IO) {
            // 1. Read backup bytes
            val encrypted = context.contentResolver
                .openInputStream(fileUri)!!
                .use { it.readBytes() }

            // 2. Decrypt — throws AEADBadTagException on wrong passphrase
            val compressed = BackupCrypto.decrypt(encrypted, passphrase)

            // 3. Decompress
            val dbBytes = gunzip(compressed)

            // 4. Validate SQLite magic header
            val magic = dbBytes.take(SQLITE_MAGIC.size).toByteArray()
            check(magic.contentEquals(SQLITE_MAGIC)) {
                "Decrypted content is not a valid SQLite database"
            }

            // 5. Close the Room singleton — must happen before overwriting the file
            db.close()

            // 6. Overwrite the database (and delete WAL files to avoid partial merge)
            val dbFile = context.getDatabasePath("lifeos.db")
            dbFile.writeBytes(dbBytes)
            context.getDatabasePath("lifeos.db-wal").delete()
            context.getDatabasePath("lifeos.db-shm").delete()

            // 7. Kill the process. The OS restarts the main activity automatically.
            //    This is the safest way to force Room to re-initialise the singleton.
            android.os.Process.killProcess(android.os.Process.myPid())

            // Unreachable — satisfies Nothing return type
            @Suppress("UNREACHABLE_CODE")
            error("Process.killProcess did not terminate — should never happen")
        }

    // ── Markdown journal export ────────────────────────────────────────────────

    /**
     * Generates a human-readable Markdown string of all journal entries,
     * suitable for sharing via [android.content.Intent.ACTION_SEND].
     *
     * One-way export only — this format is NOT importable. It exists for the
     * user to read their history in any Markdown viewer, archive it, or
     * transfer it to another app.
     */
    suspend fun exportMarkdown(): String = withContext(Dispatchers.IO) {
        val entries = db.notesDao().observeAllJournalEntries().first()
        buildString {
            appendLine("# LifeOS Journal Export")
            appendLine("Exported: ${LocalDate.now()}")
            appendLine("Entries: ${entries.size}")
            appendLine()
            appendLine("---")
            appendLine()
            entries.forEach { entry ->
                appendLine("## ${entry.date}")
                entry.mood?.let { appendLine("**Mood:** $it") }
                entry.systemDesignTopic
                    ?.ifBlank { null }
                    ?.let { appendLine("**System Design:** $it") }
                if (entry.reflectionMarkdown.isNotBlank()) {
                    appendLine()
                    appendLine(entry.reflectionMarkdown.trim())
                }
                appendLine()
                appendLine("---")
                appendLine()
            }
        }
    }

    // ── GZIP helpers ──────────────────────────────────────────────────────────

    private fun gzip(data: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { it.write(data) }
        return baos.toByteArray()
    }

    private fun gunzip(data: ByteArray): ByteArray =
        GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
}
