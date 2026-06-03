// domain/entity/ReportConfig.kt
package com.quiquecx.simaapp.domain.entity

import com.google.firebase.Timestamp
import java.util.*

enum class TimeFilter {
    TODAY,           // Hoy
    LAST_7_DAYS,    // Últimos 7 días
    LAST_30_DAYS,   // Últimos 30 días
    ALL,            // Todo
    CUSTOM          // Rango personalizado
}

data class ReportConfig(
    val companyId: String = "",
    val projectId: String? = null,
    val activityId: String? = null,

    //Filtro de tiempo
    val timeFilter: TimeFilter = TimeFilter.ALL,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,

    val minProductivity: Int = 0,

    // Qué incluir en el reporte
    val includeGeneralInfo: Boolean = true,
    val includeWorkers: Boolean = true,
    val includeDefects: Boolean = true,
    val includeProductivity: Boolean = true,
    val includeCosts: Boolean = false,
    val includeHistory: Boolean = false,

    val format: ReportFormat = ReportFormat.PDF
)

enum class ReportFormat {
    PDF, EXCEL, CSV
}