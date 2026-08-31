package com.lifeos.data.db.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room type converters for complex fields stored as JSON strings.
 *
 * Usage: annotate @TypeConverters(Converters::class) on the @Database.
 */
class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    // ─ List<String> (weekdays, topics, tags, problemIds, photoIds, etc.) ──

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        json.decodeFromString(value)

    // ─ List<Long> (problemIds, completedProblemIds) ──────────────────────

    @TypeConverter
    fun fromLongList(value: List<Long>): String = json.encodeToString(value)

    @TypeConverter
    fun toLongList(value: String): List<Long> =
        json.decodeFromString(value)

    // ─ RevisionHistory JSON blob ────────────────────────────────────
    // Stored as a raw JSON string; parsed by SpacedRevisionEntity callers.
    // No separate converter needed — it's already a String column.
}
