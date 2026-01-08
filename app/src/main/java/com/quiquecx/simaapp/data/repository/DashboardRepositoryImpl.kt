package com.quiquecx.simaapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
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

    private val activitiesCollection = firestore.collection("activities")

    override fun getActivitiesStream(projectId: String): Flow<List<ActivityEntity>> {
        return activitiesCollection
            .whereEqualTo("projectId", projectId)
            .orderBy("fechaInicio", Query.Direction.DESCENDING)
            .dataObjects<ActivityDto>()
            .map { dtos ->
                // Filtramos nulos por seguridad y convertimos a Entity
                dtos.filterNotNull().map { it.toEntity() }
            }
    }

    override suspend fun getActivityDetails(activityId: String): ActivityEntity? {
        return try {
            val snapshot = activitiesCollection.document(activityId).get().await()
            // Importante: al convertir a objeto, el @DocumentId del DTO se llena automáticamente
            snapshot.toObject(ActivityDto::class.java)?.toEntity()
        } catch (e: Exception) {
            android.util.Log.e("REPO_ERROR", "Error obteniendo detalles: ${e.message}")
            null
        }
    }

    override suspend fun saveActivity(activity: ActivityEntity): Result<Unit> {
        return try {
            // 1. Convertimos la entidad a DTO
            val dto = ActivityDto.fromEntity(activity)

            // 2. Determinamos la referencia del documento
            val docRef = if (activity.id.isBlank()) {
                activitiesCollection.document()
            } else {
                activitiesCollection.document(activity.id)
            }

            // 3. Guardamos.
            // NOTA: Firebase ignorará el campo marcado con @DocumentId en el DTO
            // al hacer el set(), lo cual es correcto para no duplicar el ID.
            docRef.set(dto).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("REPO_ERROR", "Error al guardar: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteActivity(activityId: String): Result<Unit> {
        return try {
            if (activityId.isNotBlank()) {
                activitiesCollection.document(activityId).delete().await()
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("ID vacío"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}