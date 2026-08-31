package com.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [Index("category")],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val bodyMarkdown: String = "",
    val tags: String = "[]",
    @ColumnInfo(defaultValue = "0")
    val pinned: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
)
