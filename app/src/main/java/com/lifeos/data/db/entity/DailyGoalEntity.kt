package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val icon: String,          // Material icon name
    val color: String,         // hex colour string
    /** JSON-encoded List<String> of weekday keys: "mon"…"sun" */
    val weekdays: String = "[]",
    val reminderTime: String? = null,  // "HH:mm"
    val archived: Boolean = false,
    val createdAt: String,
)
