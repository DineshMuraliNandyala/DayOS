package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    indices = [Index("weekday"), Index("muscleGroup"), Index("order")],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val weekday: String,
    val targetSets: Int = 3,
    val targetReps: String = "8-12",
    val currentPrKg: Double? = null,
    val bestPrKg: Double? = null,
    val notes: String? = null,
    val order: Int = 0,
    val archived: Boolean = false,
    val createdAt: String,
)
