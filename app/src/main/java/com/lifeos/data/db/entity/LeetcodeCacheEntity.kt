package com.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caches the LeetCode problem number → slug mapping fetched from
 * https://leetcode.com/api/problems/all/ so the user only needs to
 * type a problem number; the slug (and title) are resolved automatically.
 */
@Entity(tableName = "leetcode_slug_cache")
data class LeetcodeCacheEntity(
    @PrimaryKey val number: Int,
    val slug: String,
    val title: String,
    val difficulty: Int,   // 1=Easy 2=Medium 3=Hard (from API)
    val fetchedAt: String,
)
