package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weekly_revision_lists",
    indices = [Index("weekStart", unique = true)],
)
data class WeeklyRevisionListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekStart: String,
    val problemIds: String = "[]",
    val completedProblemIds: String = "[]",
    val createdAt: String,
)
