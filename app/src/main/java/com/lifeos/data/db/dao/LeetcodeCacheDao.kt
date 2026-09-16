package com.lifeos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeos.data.db.entity.LeetcodeCacheEntity

@Dao
interface LeetcodeCacheDao {

    @Query("SELECT * FROM leetcode_slug_cache WHERE number = :number LIMIT 1")
    suspend fun getByNumber(number: Int): LeetcodeCacheEntity?

    @Query("SELECT * FROM leetcode_slug_cache WHERE title LIKE '%' || :query || '%' LIMIT 10")
    suspend fun searchByTitle(query: String): List<LeetcodeCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<LeetcodeCacheEntity>)

    @Query("SELECT COUNT(*) FROM leetcode_slug_cache")
    suspend fun count(): Int

    @Query("DELETE FROM leetcode_slug_cache")
    suspend fun clearAll()
}
