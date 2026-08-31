package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Step count for a given day.
 * In the prod flavor, source is always "manual".
 * "health_connect" source is reserved for the healthVariant flavor only —
 * NEVER ship healthVariant to production.
 */
@Entity(tableName = "step_readings", indices = [Index("date")])
data class StepReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val steps: Int,
    val distanceMeters: Double? = null,
    val calories: Int? = null,
    val syncedAt: String? = null,
    val source: String = "manual",
)
