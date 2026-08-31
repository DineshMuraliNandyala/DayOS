package com.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifeos.data.db.entity.JournalEntryEntity
import com.lifeos.data.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {

    // ─ Knowledge-base notes ────────────────────────────────────────────────

    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE category = :category ORDER BY pinned DESC, updatedAt DESC")
    fun observeNotesByCategory(category: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNote(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("SELECT DISTINCT category FROM notes ORDER BY category ASC")
    fun observeCategories(): Flow<List<String>>

    @Query("UPDATE notes SET pinned = NOT pinned, updatedAt = :now WHERE id = :id")
    suspend fun togglePinned(id: Long, now: String)

    // ─ Journal entries (past view, read-only from Notes tab) ───────────────

    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun observeAllJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun getJournalEntry(id: Long): JournalEntryEntity?
}
