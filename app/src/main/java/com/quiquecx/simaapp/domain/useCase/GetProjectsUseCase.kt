package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.entity.ProjectEntity
import com.quiquecx.simaapp.domain.repository.ProjectsRepository
import javax.inject.Inject

class GetProjectsUseCase @Inject constructor(
    private val repository: ProjectsRepository
) {
    suspend operator fun invoke(companyId: String): List<ProjectEntity> {
        return repository.getProjectsByCompany(companyId)
    }
}