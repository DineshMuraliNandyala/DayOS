package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "protein_logs", indices = [Index("date")])
data class ProteinLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val grams: Int,
    val loggedAt: String,
)
