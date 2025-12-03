package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDashboardActivitiesStreamUseCase @Inject constructor(
    private val repository: DashboardRepository
) {
    // Usamos el ID del proyecto que ya está seleccionado (asumimos que es "incoming" o lo obtenemos)
    operator fun invoke(projectId: String): Flow<List<ActivityEntity>> {
        return repository.getActivitiesStream(projectId)
    }
}