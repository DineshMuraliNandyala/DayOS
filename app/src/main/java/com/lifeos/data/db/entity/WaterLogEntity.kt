package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "water_logs", indices = [Index("date")])
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val ml: Int,
    val loggedAt: String,
)
