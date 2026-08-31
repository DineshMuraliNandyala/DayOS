package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hobbies")
data class HobbyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val color: String,
    /** JSON-encoded List<String> of weekday keys: "mon"…"sun" */
    val weekdays: String = "[]",
    val goalMinutes: Int = 30,
    val reminderTime: String? = null,
    val archived: Boolean = false,
    val createdAt: String,
)
