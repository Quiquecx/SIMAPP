package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.ReportConfig
import com.quiquecx.simaapp.domain.entity.ReportFormat
import com.quiquecx.simaapp.domain.entity.WorkerSessionLog
import com.quiquecx.simaapp.utils.CsvGenerator
import com.quiquecx.simaapp.utils.HtmlGenerator
import java.io.File
import javax.inject.Inject

class GenerateReportUseCase @Inject constructor(
    private val htmlGenerator: HtmlGenerator,
    private val csvGenerator: CsvGenerator,
    private val enrichActivitiesUseCase: EnrichActivitiesForReportUseCase
) {

    suspend operator fun invoke(
        activities: List<ActivityEntity>,
        config: ReportConfig,
        activitySessions: Map<String, List<WorkerSessionLog>> = emptyMap()
    ): File {
        val startDate = config.startDate?.toDate()
        val endDate = config.endDate?.toDate()
        val enrichedActivities = enrichActivitiesUseCase(activities, startDate, endDate)

        return when (config.format) {
            ReportFormat.PDF -> {
                htmlGenerator.generate(
                    activities = enrichedActivities,
                    config = config,
                    activitySessions = activitySessions
                )
            }
            ReportFormat.EXCEL, ReportFormat.CSV -> {
                csvGenerator.generate(enrichedActivities, config)
            }
        }
    }
}