package com.quiquecx.simaapp.domain.repository

import android.content.Context
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.ReportConfig
import com.quiquecx.simaapp.domain.entity.ReportFormat
import java.io.File

interface ReportRepository {
    suspend fun getFilteredActivities(config: ReportConfig): List<ActivityEntity>
    suspend fun generateReport(activities: List<ActivityEntity>, config: ReportConfig): File
    suspend fun shareReport(file: File, format: ReportFormat, context: Context)  // ← añadir context
}