package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.entity.ReportConfig
import com.quiquecx.simaapp.domain.repository.ReportRepository
import javax.inject.Inject

class GetFilteredActivitiesForReportUseCase @Inject constructor(
    private val repository: ReportRepository
) {
    suspend operator fun invoke(config: ReportConfig) = repository.getFilteredActivities(config)
}