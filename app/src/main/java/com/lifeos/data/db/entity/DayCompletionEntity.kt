package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Aggregated per-day completion snapshot.
 * Powers the Today checklist summary and the Analytics heatmap.
 */
@Entity(
    tableName = "day_completions",
    indices = [Index("date", unique = true)],
)
data class DayCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val gym: Boolean? = null,
    val protein: Boolean? = null,
    val walking: Boolean? = null,
    val coding: Boolean? = null,
    val hobbiesCompleted: Int = 0,
    val hobbiesScheduled: Int = 0,
    val goalsCompleted: Int = 0,
    val goalsScheduled: Int = 0,
)
