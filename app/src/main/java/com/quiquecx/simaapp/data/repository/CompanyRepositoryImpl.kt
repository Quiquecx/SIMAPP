package com.quiquecx.simaapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.quiquecx.simaapp.data.model.CompanyDto
import com.quiquecx.simaapp.data.model.toDomain
import com.quiquecx.simaapp.domain.entity.CompanyEntity
import com.quiquecx.simaapp.domain.repository.CompanyRepository

import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CompanyRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CompanyRepository {

    override suspend fun getAllCompanies(): List<CompanyEntity> {
        return try {
            // Consulta a la colección "Empresas"
            val snapshot = firestore.collection("Empresas")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                // Aseguramos que el ID del documento se use como ID de la DTO
                doc.toObject(CompanyDto::class.java)?.copy(id = doc.id)?.toDomain()
            }
        } catch (e: Exception) {
            throw Exception("Error al cargar la lista de empresas: ${e.message}")
        }
    }
}