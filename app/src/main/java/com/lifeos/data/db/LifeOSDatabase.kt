package com.lifeos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lifeos.data.db.converter.Converters
import com.lifeos.data.db.dao.AnalyticsDao
import com.lifeos.data.db.dao.FitnessDao
import com.lifeos.data.db.dao.NotesDao
import com.lifeos.data.db.dao.PlacementDao
import com.lifeos.data.db.dao.SettingsDao
import com.lifeos.data.db.dao.TodayDao
import com.lifeos.data.db.entity.DailyGoalCompletionEntity
import com.lifeos.data.db.entity.DailyGoalEntity
import com.lifeos.data.db.entity.DayCompletionEntity
import com.lifeos.data.db.entity.ExerciseEntity
import com.lifeos.data.db.entity.ExerciseSetLogEntity
import com.lifeos.data.db.entity.HobbyEntity
import com.lifeos.data.db.entity.HobbyLogEntity
import com.lifeos.data.db.entity.JournalEntryEntity
import com.lifeos.data.db.entity.MonthlyRevisionListEntity
import com.lifeos.data.db.entity.NoteEntity
import com.lifeos.data.db.entity.ProblemEntity
import com.lifeos.data.db.entity.ProteinLogEntity
import com.lifeos.data.db.entity.SettingsEntity
import com.lifeos.data.db.entity.SpacedRevisionEntity
import com.lifeos.data.db.entity.StepReadingEntity
import com.lifeos.data.db.entity.WaterLogEntity
import com.lifeos.data.db.entity.WeeklyRevisionListEntity
import com.lifeos.data.db.entity.WorkoutSessionEntity

@Database(
    entities = [
        SettingsEntity::class,
        DailyGoalEntity::class,
        DailyGoalCompletionEntity::class,
        HobbyEntity::class,
        HobbyLogEntity::class,
        ExerciseEntity::class,
        ExerciseSetLogEntity::class,
        WorkoutSessionEntity::class,
        ProteinLogEntity::class,
        WaterLogEntity::class,
        StepReadingEntity::class,
        ProblemEntity::class,
        SpacedRevisionEntity::class,
        WeeklyRevisionListEntity::class,
        MonthlyRevisionListEntity::class,
        JournalEntryEntity::class,
        NoteEntity::class,
        DayCompletionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LifeOSDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao
    abstract fun todayDao(): TodayDao
    abstract fun placementDao(): PlacementDao
    abstract fun fitnessDao(): FitnessDao
    abstract fun notesDao(): NotesDao
    abstract fun analyticsDao(): AnalyticsDao

    companion object {
        @Volatile
        private var INSTANCE: LifeOSDatabase? = null

        fun getInstance(context: Context): LifeOSDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifeOSDatabase::class.java,
                    "lifeos.db",
                )
                // No fallbackToDestructiveMigration — all migrations must be explicit.
                .build()
                .also { INSTANCE = it }
            }
    }
}
