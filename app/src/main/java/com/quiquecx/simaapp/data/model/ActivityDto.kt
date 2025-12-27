package com.quiquecx.simaapp.data.model

import com.google.firebase.firestore.DocumentId
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.DefectEntry
import com.quiquecx.simaapp.domain.entity.TimerEntry
import java.util.Date

data class ActivityDto(
    @DocumentId
    val id: String? = null,
    val tipo: String? = null,
    val proveedorId: String? = null,
    val materialId: String? = null,
    val responsable: String? = null,
    val fechaInicio: Date? = null,

    val cpmId: String? = null,
    val people: List<String>? = null,

    // --- NUEVOS CAMPOS PERSISTENCIA ---
    val timerActive: Boolean? = null,
    val timerStartTime: Date? = null,
    val timerHistory: List<TimerEntry>? = null,

    val defectos: List<DefectEntry>? = null,
    val defectoNota: String? = null,

    val cantidadTotal: Int? = null,
    val cantidadOk: Int? = null,
    val cantidadNoOk: Int? = null,

    // --- CAMBIO A DOUBLE ---
    val horasAcumuladas: Double? = null,
    val estimadoHoras: String? = null,
    val estimadoCosto: String? = null,
    val estado: String? = null,
    val progreso: Int? = null
) {
    fun toEntity(): ActivityEntity? {
        return if (id != null && tipo != null && proveedorId != null && materialId != null && fechaInicio != null) {
            ActivityEntity(
                id = id,
                tipo = tipo,
                proveedorId = proveedorId,
                materialId = materialId,
                responsable = responsable ?: "Equipo Sima",
                fechaInicio = fechaInicio,
                cpmId = cpmId ?: "",
                people = people ?: emptyList(),

                // Mapeo nuevos campos
                timerActive = timerActive ?: false,
                timerStartTime = timerStartTime,
                timerHistory = timerHistory ?: emptyList(),

                defectos = defectos ?: emptyList(),
                defectoNota = defectoNota ?: "",
                cantidadTotal = cantidadTotal ?: 0,
                cantidadOk = cantidadOk ?: 0,
                cantidadNoOk = cantidadNoOk ?: 0,

                // Mapeo Double
                horasAcumuladas = horasAcumuladas ?: 0.0,
                estimadoHoras = estimadoHoras ?: "0",
                estimadoCosto = estimadoCosto ?: "0",
                estado = estado ?: "Pendiente",
                progreso = progreso ?: 0
            )
        } else null
    }

    companion object {
        fun fromEntity(entity: ActivityEntity): ActivityDto {
            return ActivityDto(
                id = entity.id.ifEmpty { null },
                tipo = entity.tipo,
                proveedorId = entity.proveedorId,
                materialId = entity.materialId,
                responsable = entity.responsable,
                fechaInicio = entity.fechaInicio,
                cpmId = entity.cpmId,
                people = entity.people,

                // Nuevos campos
                timerActive = entity.timerActive,
                timerStartTime = entity.timerStartTime,
                timerHistory = entity.timerHistory,

                defectos = entity.defectos,
                defectoNota = entity.defectoNota,
                cantidadTotal = entity.cantidadTotal,
                cantidadOk = entity.cantidadOk,
                cantidadNoOk = entity.cantidadNoOk,
                horasAcumuladas = entity.horasAcumuladas,
                estimadoHoras = entity.estimadoHoras,
                estimadoCosto = entity.estimadoCosto,
                estado = entity.estado,
                progreso = entity.progreso
            )
        }
    }
}