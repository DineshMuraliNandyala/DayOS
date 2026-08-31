package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Spaced-repetition schedule for one problem.
 * stage 0-4 indexes into intervals [1, 3, 7, 14, 30] days.
 * history: JSON array of {date: String, result: "easy"|"hard"|"forgot"}.
 */
@Entity(
    tableName = "spaced_revisions",
    indices = [Index("problemId"), Index("dueDate"), Index("stage")],
)
data class SpacedRevisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val problemId: Long,
    val stage: Int = 0,
    val dueDate: String,
    val lastReviewedAt: String? = null,
    val history: String = "[]",
)
