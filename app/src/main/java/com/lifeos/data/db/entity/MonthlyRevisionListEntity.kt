package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "monthly_revision_lists",
    indices = [Index("month", unique = true)],
)
data class MonthlyRevisionListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: String,
    val problemIds: String = "[]",
    val completedProblemIds: String = "[]",
    val createdAt: String,
)
