package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.ReportConfig
import com.quiquecx.simaapp.domain.repository.ReportRepository
import java.io.File
import javax.inject.Inject

class GenerateReportUseCase @Inject constructor(
    private val repository: ReportRepository
) {
    suspend operator fun invoke(activities: List<ActivityEntity>, config: ReportConfig): File =
        repository.generateReport(activities, config)
}