package com.quiquecx.simaapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import com.google.firebase.firestore.toObject
import com.quiquecx.simaapp.data.model.ActivityDto
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DashboardRepository {

    // Nombre de la colección en Firestore (debe coincidir)
    private val activitiesCollection = firestore.collection("activities")

    // 1. READ STREAM (Lectura en tiempo real)
    override fun getActivitiesStream(projectId: String): Flow<List<ActivityEntity>> {

        return activitiesCollection
            .whereEqualTo("projectId", projectId)
            .orderBy("fechaInicio", Query.Direction.DESCENDING)
            .dataObjects<ActivityDto>() // Mapea el stream a ActivityDto
            .map { dtos ->
                dtos.mapNotNull { it.toEntity() } // Mapea de DTO a Entity, ignorando nulos
            }
    }

    // 2. READ (Detalles de una actividad específica)
    override suspend fun getActivityDetails(activityId: String): ActivityEntity? {
        return try {
            val snapshot = activitiesCollection.document(activityId).get().await()
            snapshot.toObject<ActivityDto>()?.toEntity()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 3. CREATE / UPDATE (Firestore lo maneja con set)
    override suspend fun saveActivity(activity: ActivityEntity): Result<Unit> {
        return try {
            val dto = ActivityDto.fromEntity(activity)
            // Si activity.id está vacío, Firestore genera un nuevo documento ID
            activitiesCollection.document(activity.id.ifEmpty { firestore.collection("activities").document().id })
                .set(dto)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // 4. DELETE
    override suspend fun deleteActivity(activityId: String): Result<Unit> {
        return try {
            activitiesCollection.document(activityId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}