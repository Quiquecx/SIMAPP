package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.ReportConfig
import com.quiquecx.simaapp.domain.repository.DashboardRepository
import com.quiquecx.simaapp.domain.repository.ReportRepository
import javax.inject.Inject

class GetFilteredActivitiesForReportUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val reportRepository: ReportRepository
) {

    suspend operator fun invoke(config: ReportConfig): List<ActivityEntity> {
        // Obtener actividades del repositorio con filtros aplicados
        return reportRepository.getFilteredActivities(config)
    }
}
