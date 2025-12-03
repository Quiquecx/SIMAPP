package com.quiquecx.simaapp.view.dashboard

import com.quiquecx.simaapp.domain.entity.ActivityEntity

// Esta clase contiene los KPIs y la lista detallada
data class DashboardUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val projectId: String = "", // El ID del proyecto actual (e.g., "ID_INCOMING")
    val allActivities: List<ActivityEntity> = emptyList(),

    // --- KPIs Resumen ---
    val totalPiecesInvolved: Int = 0,
    val totalActivities: Int = 0,
    val ongoingActivities: Int = 0,

    // Podemos agregar resúmenes por tipo si la maqueta lo requiere
    val sorteoCount: Int = 0,
    val retrabajoCount: Int = 0,

    // Ejemplo de KPI de progreso (basado en CantidadOk / CantidadTotal)
    val overallProgressPercent: Float = 0.0f
)