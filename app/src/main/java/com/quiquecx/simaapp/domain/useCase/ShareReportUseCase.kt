package com.quiquecx.simaapp.domain.useCase

import android.content.Context
import com.quiquecx.simaapp.domain.entity.ReportFormat
import com.quiquecx.simaapp.domain.repository.ReportRepository
import java.io.File
import javax.inject.Inject

class ShareReportUseCase @Inject constructor(
    private val repository: ReportRepository
) {
    suspend operator fun invoke(file: File, format: ReportFormat, context: Context) =
        repository.shareReport(file, format, context)
}