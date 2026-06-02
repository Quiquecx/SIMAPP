package com.quiquecx.simaapp.domain.entity

import com.google.firebase.Timestamp

enum class ReportFormat { PDF, EXCEL, CSV }

data class ReportConfig(
    val companyId: String,
    val projectId: String? = null,
    val activityId: String? = null,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val minProductivity: Int = 0, // 0-100
    val includeGeneralInfo: Boolean = true,
    val includeWorkers: Boolean = true,
    val includeDefects: Boolean = true,
    val includeProductivity: Boolean = true,
    val includeCosts: Boolean = false,
    val format: ReportFormat = ReportFormat.PDF
)