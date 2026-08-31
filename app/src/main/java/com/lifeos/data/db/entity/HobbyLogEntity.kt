package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hobby_logs",
    indices = [Index("hobbyId"), Index("date")],
)
data class HobbyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hobbyId: Long,
    val date: String,
    val minutes: Int,
    val note: String? = null,
)
