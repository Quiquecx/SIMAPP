package com.quiquecx.simaapp.domain.entity

import java.util.Date

data class ActivityEntity(
    // Información General
    val id: String = "",
    val tipo: String = "",
    val proveedorId: String = "",
    val materialId: String = "",
    val responsable: String = "Equipo Sima",
    val fechaInicio: Date = Date(),

    //Cambios
    val cpmId: String = "",
    val people: List<String> = emptyList(),
    val ultimaSesionInicio: Date? = null,
    val timerHistory: List<TimerEntry> = emptyList(),

    // --- CAMBIO PARA DEFECTOS MÚLTIPLES ---
    val defectos: List<DefectEntry> = emptyList(), // Ahora soporta 1, 2 o más defectos
    val defectoNota: String = "",

    // Cantidades
    val cantidadTotal: Int = 0,
    val cantidadOk: Int = 0,
    val cantidadNoOk: Int = 0,

    // Estado y Progreso
    val estado: String = "En curso",
    val progreso: Int = 0,

    // Tiempos y Costos
    val horasAcumuladas: Int = 0,
    val estimadoHoras: String = "0", // String
    val estimadoCosto: String = "0" // String
)
    data class TimerEntry(
        val startTime: Date,
        val endTime: Date?,
        val durationMinutes: Int,
        val user: String
    )

    data class DefectEntry(
        val name: String = "",
        val count: Int = 0
    )