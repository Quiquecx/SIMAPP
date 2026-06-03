package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.ReportConfig
import java.io.File
import com.quiquecx.simaapp.utils.HtmlGenerator
import com.quiquecx.simaapp.utils.CsvGenerator
import javax.inject.Inject


class GenerateReportUseCase @Inject constructor(
    private val htmlGenerator: HtmlGenerator,
    private val csvGenerator: CsvGenerator,
    private val enrichActivitiesUseCase: EnrichActivitiesForReportUseCase
) {

    suspend operator fun invoke(
        activities: List<ActivityEntity>,
        config: ReportConfig
    ): File {

        val enrichedActivities = enrichActivitiesUseCase(activities)


        return when (config.format) {
            com.quiquecx.simaapp.domain.entity.ReportFormat.PDF ->
                htmlGenerator.generate(enrichedActivities, config)
            com.quiquecx.simaapp.domain.entity.ReportFormat.EXCEL,
            com.quiquecx.simaapp.domain.entity.ReportFormat.CSV ->
                csvGenerator.generate(enrichedActivities, config)
        }
    }
}
