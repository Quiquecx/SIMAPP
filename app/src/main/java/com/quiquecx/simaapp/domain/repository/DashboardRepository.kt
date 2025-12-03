package com.quiquecx.simaapp.domain.repository

import com.quiquecx.simaapp.domain.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    // 1. READ (Stream en tiempo real, filtrado por proyecto)
    fun getActivitiesStream(projectId: String): Flow<List<ActivityEntity>>

    // 2. READ (Detalles de una actividad específica)
    suspend fun getActivityDetails(activityId: String): ActivityEntity?

    // 3. CREATE / UPDATE
    suspend fun saveActivity(activity: ActivityEntity): Result<Unit>

    // 4. DELETE
    suspend fun deleteActivity(activityId: String): Result<Unit>
}