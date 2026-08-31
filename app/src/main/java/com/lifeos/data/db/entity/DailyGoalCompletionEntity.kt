package com.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_goal_completions",
    indices = [
        Index("goalId"),
        Index("date"),
        Index(value = ["goalId", "date"]),
    ],
)
data class DailyGoalCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val date: String,              // YYYY-MM-DD
    @ColumnInfo(defaultValue = "0")
    val completed: Boolean = false,
    val completedAt: String? = null,  // ISO timestamp
)
