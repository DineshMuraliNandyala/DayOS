package com.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_set_logs",
    indices = [Index("exerciseId"), Index("date")],
)
data class ExerciseSetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val date: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    @ColumnInfo(defaultValue = "0")
    val isPr: Boolean = false,
)
