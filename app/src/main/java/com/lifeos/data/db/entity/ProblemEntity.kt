package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A coding problem in the placement tracker.
 *
 * platform enum (stored as String): LEETCODE | CODEFORCES | HACKERRANK | GFG | CUSTOM
 *
 * URL construction logic:
 *   LEETCODE   -> https://leetcode.com/problems/{leetcodeSlug}/
 *   CODEFORCES -> https://codeforces.com/problemset/problem/{platformUrl}  (e.g. "1234/A")
 *   HACKERRANK -> https://hackerrank.com/challenges/{platformUrl}
 *   GFG        -> https://geeksforgeeks.org/problems/{platformUrl}
 *   CUSTOM     -> platformUrl as-is (full URL)
 */
@Entity(
    tableName = "problems",
    indices = [Index("difficulty"), Index("solvedDate"), Index("platform")],
)
data class ProblemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: Int? = null,
    val title: String,
    val difficulty: String,
    val topics: String = "[]",
    val notes: String? = null,
    val approach: String? = null,
    val mistakes: String? = null,
    val solvedDate: String,
    val platform: String = "LEETCODE",
    val platformUrl: String? = null,
    val leetcodeSlug: String? = null,
    val source: String = "manual",
)
