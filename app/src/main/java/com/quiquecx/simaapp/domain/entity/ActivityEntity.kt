package com.quiquecx.simaapp.domain.entity

import com.google.firebase.Timestamp
import java.util.Date

data class ActivityEntity(
    val id: String = "",
    val projectId: String = "",
    val tipo: String = "",
    val proveedorId: String = "",
    val materialId: String = "",
    val responsable: String = "Equipo Sima",
    val fechaInicio: Date = Date(),
    val cpmId: String = "",

    // Cronómetro General de la Actividad
    val timerActive: Boolean = false,
    val timerStartTime: Date? = null,
    val horasAcumuladas: Double = 0.0,

    // --- CAMPO CLAVE PARA LA PESTAÑA PERSONAL ---
    val workers: List<WorkerEntity> = emptyList(),

    // Calidad y Producción
    val defectos: List<DefectEntry> = emptyList(),
    val defectoNota: String = "",
    val cantidadTotal: Int = 0,
    val cantidadOk: Int = 0,
    val cantidadNoOk: Int = 0,

    // Estado y Planificación
    val estado: String = "Pendiente", // Pendiente, En curso, Finalizado
    val progreso: Int = 0,
    val estimadoHoras: String = "0",
    val estimadoCosto: String = "0",

    // Historial técnico (opcional si lo usas)
    val timerHistory: List<TimerEntry> = emptyList()
)

// --- ENTIDADES RELACIONADAS ---

data class WorkerEntity(
    val name: String = "",
    val isTimerActive: Boolean = false,
    val startTime: Timestamp? = null, // Usamos Timestamp para mejor compatibilidad con Firebase
    val accumulatedHours: Double = 0.0
)

data class TimerEntry(
    val startTime: Date = Date(),
    val endTime: Date? = null,
    val durationMinutes: Double = 0.0,
    val user: String = ""
)

data class DefectEntry(
    val name: String = "",
    val count: Int = 0
)

data class ProductivityEntity(
    val id: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val dia: String = "",
    val turno: String = "",
    val cantidadOk: Int = 0,
    val defectos: List<DefectEntry> = emptyList(),
    val registradoPor: String = ""
)