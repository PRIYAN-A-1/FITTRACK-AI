package com.fittrack.ai.data.local.dao

import androidx.room.*
import com.fittrack.ai.data.local.entities.NutritionGoalEntity
import com.fittrack.ai.data.local.entities.WaterLogEntity
import com.fittrack.ai.data.local.entities.WeightLogEntity
import com.fittrack.ai.data.local.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM nutrition_goals WHERE isActive = 1 LIMIT 1")
    fun getActiveGoal(): Flow<NutritionGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: NutritionGoalEntity)
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_logs WHERE loggedDate = :date ORDER BY createdAt ASC")
    fun getWaterLogsForDate(date: String): Flow<List<WaterLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLogEntity)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_logs ORDER BY loggedDate DESC LIMIT :limit")
    fun getWeightHistory(limit: Int = 30): Flow<List<WeightLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: WeightLogEntity)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getAllPendingOperations(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun queueOperation(op: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE operationId = :operationId")
    suspend fun removeOperation(operationId: String)
}
