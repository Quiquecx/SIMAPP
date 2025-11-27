package com.quiquecx.simaapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.quiquecx.simaapp.data.model.ProjectDto
import com.quiquecx.simaapp.data.model.toDomain
import com.quiquecx.simaapp.domain.entity.ProjectEntity
import com.quiquecx.simaapp.domain.repository.ProjectsRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProjectsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProjectsRepository {

    override suspend fun getProjectsByCompany(companyId: String): List<ProjectEntity> {
        try {
            // Buscamos en la colección "Proyectos" donde "empresaId" coincida
            val snapshot = firestore.collection("Proyectos")
                .whereEqualTo("empresaId", companyId)
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                doc.toObject(ProjectDto::class.java)?.toDomain()
            }
        } catch (e: Exception) {
            throw Exception("Error al cargar proyectos: ${e.message}")
        }
    }
}