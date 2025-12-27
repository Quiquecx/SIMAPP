package com.quiquecx.simaapp.domain.entity

import java.util.Date

data class ActivityEntity(
    val id: String = "",
    val tipo: String = "",
    val proveedorId: String = "",
    val materialId: String = "",
    val responsable: String = "Equipo Sima",
    val fechaInicio: Date = Date(),

    val cpmId: String = "",
    val people: List<String> = emptyList(),

    // --- CAMBIOS PARA CRONÓMETRO PRO ---
    val timerActive: Boolean = false,    // Indica si el cronómetro está activo
    val timerStartTime: Date? = null,       // La hora exacta en que se le dio "Iniciar"
    val timerHistory: List<TimerEntry> = emptyList(),

    val defectos: List<DefectEntry> = emptyList(),
    val defectoNota: String = "",

    val cantidadTotal: Int = 0,
    val cantidadOk: Int = 0,
    val cantidadNoOk: Int = 0,

    val estado: String = "Pendiente",
    val progreso: Int = 0,

    // --- CAMBIO A DOUBLE PARA DECIMALES ---
    val horasAcumuladas: Double = 0.0,
    val estimadoHoras: String = "0",
    val estimadoCosto: String = "0"
)

data class TimerEntry(
    val startTime: Date,
    val endTime: Date?,
    val durationMinutes: Double, // También en Double para precisión
    val user: String
)

data class DefectEntry(
    val name: String = "",
    val count: Int = 0
)