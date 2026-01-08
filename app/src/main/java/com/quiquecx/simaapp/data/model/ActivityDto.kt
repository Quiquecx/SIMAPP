package com.quiquecx.simaapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.quiquecx.simaapp.domain.entity.*
import java.util.Date

@IgnoreExtraProperties
data class ActivityDto(
    @DocumentId val id: String? = null,
    val projectId: String? = null,
    val tipo: String? = null,
    val proveedorId: String? = null,
    val materialId: String? = null,
    val responsable: String? = null,
    val fechaInicio: Timestamp? = null,
    val cpmId: String? = null,

    @get:PropertyName("timerActive")
    @set:PropertyName("timerActive")
    var timerActive: Boolean = false,

    val timerStartTime: Timestamp? = null,
    val timerHistory: List<TimerEntry>? = null,

    // --- NUEVO: SOPORTE PARA TRABAJADORES ---
    val workers: List<WorkerDto>? = null,

    val defectos: List<DefectEntry>? = null,
    val defectoNota: String? = null,
    val cantidadTotal: Int? = 0,
    val cantidadOk: Int? = 0,
    val cantidadNoOk: Int? = 0,
    val estado: String? = null,
    val progreso: Int? = null,
    val horasAcumuladas: Double? = 0.0,
    val estimadoHoras: String? = null,
    val estimadoCosto: String? = null
) {
    fun toEntity(): ActivityEntity = ActivityEntity(
        id = id ?: "",
        projectId = projectId ?: "",
        tipo = tipo ?: "",
        proveedorId = proveedorId ?: "",
        materialId = materialId ?: "",
        responsable = responsable ?: "Equipo Sima",
        fechaInicio = fechaInicio?.toDate() ?: Date(),
        cpmId = cpmId ?: "",
        timerActive = timerActive,
        timerStartTime = timerStartTime?.toDate(),
        timerHistory = timerHistory ?: emptyList(),
        // Mapeo de trabajadores de DTO a Entidad
        workers = workers?.map { it.toEntity() } ?: emptyList(),
        defectos = defectos ?: emptyList(),
        defectoNota = defectoNota ?: "",
        cantidadTotal = cantidadTotal ?: 0,
        cantidadOk = cantidadOk ?: 0,
        cantidadNoOk = cantidadNoOk ?: 0,
        estado = estado ?: "Pendiente",
        progreso = progreso ?: 0,
        horasAcumuladas = horasAcumuladas ?: 0.0,
        estimadoHoras = estimadoHoras ?: "0",
        estimadoCosto = estimadoCosto ?: "0"
    )

    companion object {
        fun fromEntity(e: ActivityEntity): ActivityDto = ActivityDto(
            id = e.id.ifEmpty { null },
            projectId = e.projectId,
            tipo = e.tipo,
            proveedorId = e.proveedorId,
            materialId = e.materialId,
            responsable = e.responsable,
            fechaInicio = Timestamp(e.fechaInicio),
            cpmId = e.cpmId,
            timerActive = e.timerActive,
            timerStartTime = e.timerStartTime?.let { Timestamp(it) },
            timerHistory = e.timerHistory,
            // Mapeo de trabajadores de Entidad a DTO
            workers = e.workers.map { WorkerDto.fromEntity(it) },
            defectos = e.defectos,
            defectoNota = e.defectoNota,
            cantidadTotal = e.cantidadTotal,
            cantidadOk = e.cantidadOk,
            cantidadNoOk = e.cantidadNoOk,
            estado = e.estado,
            progreso = e.progreso,
            horasAcumuladas = e.horasAcumuladas,
            estimadoHoras = e.estimadoHoras,
            estimadoCosto = e.estimadoCosto
        )
    }
}

// --- DTO AUXILIAR PARA TRABAJADORES ---
@IgnoreExtraProperties
data class WorkerDto(
    val name: String? = null,
    @get:PropertyName("timerActive")
    @set:PropertyName("timerActive")
    var isTimerActive: Boolean = false,
    val startTime: Timestamp? = null,
    val accumulatedHours: Double? = 0.0
) {
    fun toEntity() = WorkerEntity(
        name = name ?: "",
        isTimerActive = isTimerActive,
        startTime = startTime,
        accumulatedHours = accumulatedHours ?: 0.0
    )

    companion object {
        fun fromEntity(w: WorkerEntity) = WorkerDto(
            name = w.name,
            isTimerActive = w.isTimerActive,
            startTime = w.startTime,
            accumulatedHours = w.accumulatedHours
        )
    }
}