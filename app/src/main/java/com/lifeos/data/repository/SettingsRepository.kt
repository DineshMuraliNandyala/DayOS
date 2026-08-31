package com.lifeos.data.repository

import com.lifeos.data.db.dao.SettingsDao
import com.lifeos.data.db.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Thin repository over SettingsDao.
 * Ensures the singleton row (id = 1) is seeded on first access.
 */
class SettingsRepository(private val dao: SettingsDao) {

    val settingsFlow: Flow<SettingsEntity?> = dao.observe()

    /** Seed the singleton row if it does not exist yet. Call once at app start. */
    suspend fun ensureSeed() {
        if (dao.get() == null) {
            val now = Instant.now().toString()
            dao.upsert(
                SettingsEntity(
                    id = 1,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun update(settings: SettingsEntity) {
        dao.upsert(settings.copy(updatedAt = Instant.now().toString()))
    }
}
