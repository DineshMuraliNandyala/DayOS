package com.lifeos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Singleton settings row — always id = 1.
 * LeetCode / Google Fit fields from the web prototype are intentionally
 * omitted: no API integration in the Android app.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(defaultValue = "0")
    val onboardingComplete: Boolean = false,

    val displayName: String? = null,

    /** "dark" | "amoled" | "light" */
    @ColumnInfo(defaultValue = "dark")
    val theme: String = "dark",

    /** "focus" | "energy" | "calm" */
    @ColumnInfo(defaultValue = "focus")
    val accentDomain: String = "focus",

    // ─ Goals ────────────────────────────────────────────────────────────
    @ColumnInfo(defaultValue = "7")
    val weeklyCodingGoal: Int = 7,          // problems/week

    @ColumnInfo(defaultValue = "45")
    val dailyCodingGoalMinutes: Int = 45,

    @ColumnInfo(defaultValue = "150")
    val proteinGoalGrams: Int = 150,

    @ColumnInfo(defaultValue = "2500")
    val waterGoalMl: Int = 2500,

    @ColumnInfo(defaultValue = "8000")
    val stepGoal: Int = 8000,

    // ─ Notifications ───────────────────────────────────────────────────
    @ColumnInfo(defaultValue = "0")
    val notificationsEnabled: Boolean = false,

    // Flattened reminder times ("HH:mm" or null)
    val reminderTimeMorning: String? = "07:30",
    val reminderTimeEvening: String? = "21:30",
    val reminderTimeGym: String? = "18:00",
    val reminderTimeWalking: String? = null,
    val reminderTimeProtein: String? = null,

    val createdAt: String = "",
    val updatedAt: String = "",
)
