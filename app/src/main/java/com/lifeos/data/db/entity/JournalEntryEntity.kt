package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_entries",
    indices = [Index("date", unique = true)],
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val reflectionMarkdown: String = "",
    val systemDesignTopic: String? = null,
    val mood: String? = null,
    val photoIds: String = "[]",
)
