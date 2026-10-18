package com.fittrack.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fittrack.ai.data.local.dao.*
import com.fittrack.ai.data.local.entities.*

@Database(
    entities = [
        MealEntity::class,
        MealItemEntity::class,
        NutritionGoalEntity::class,
        WaterLogEntity::class,
        WeightLogEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FitTrackDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun goalDao(): GoalDao
    abstract fun waterDao(): WaterDao
    abstract fun weightDao(): WeightDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: FitTrackDatabase? = null

        fun getDatabase(context: Context): FitTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitTrackDatabase::class.java,
                    "fittrack_local.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
