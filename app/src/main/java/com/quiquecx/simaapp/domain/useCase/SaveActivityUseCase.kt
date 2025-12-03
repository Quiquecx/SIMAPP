package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.repository.DashboardRepository
import javax.inject.Inject

class SaveActivityUseCase @Inject constructor(
    private val repository: DashboardRepository
) {
    // Retorna Result<Unit> para manejar éxito o error de la operación de guardado.
    suspend operator fun invoke(activity: ActivityEntity): Result<Unit> {
        return repository.saveActivity(activity)
    }
}