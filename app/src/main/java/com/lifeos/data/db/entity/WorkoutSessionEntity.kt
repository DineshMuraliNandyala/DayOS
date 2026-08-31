package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    indices = [Index("date"), Index("weekday")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val weekday: String,
    val startedAt: String,
    val completedAt: String? = null,
    val durationMinutes: Int? = null,
    val totalVolumeKg: Double? = null,
    val newPrCount: Int = 0,
)
