package com.quiquecx.simaapp.domain.repository

import com.quiquecx.simaapp.domain.entity.ProjectEntity

interface ProjectsRepository {
    // Obtiene proyectos filtrados por el ID de la empresa
    suspend fun getProjectsByCompany(companyId: String): List<ProjectEntity>
}