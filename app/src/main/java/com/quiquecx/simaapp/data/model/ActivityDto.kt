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

    // --- NUEVOS CAMPOS ---
    val cpmId: String? = null,
    val people: List<String>? = null,
    val ultimaSesionInicio: Date? = null,
    val timerHistory: List<TimerEntry>? = null,
    val defectos: List<DefectEntry>? = null, // La lista nueva
    val defectoNota: String? = null,

    // --- CAMPOS EXISTENTES ---
    val cantidadTotal: Int? = null,
    val cantidadOk: Int? = null,
    val cantidadNoOk: Int? = null,
    val horasAcumuladas: Int? = null,
    val estimadoHoras: String? = null,
    val estimadoCosto: String? = null,
    val estado: String? = null,
    val progreso: Int? = null
) {
    // Mapeo a Entity (Data -> Domain)
    fun toEntity(): ActivityEntity? {
        return if (id != null && tipo != null && proveedorId != null && materialId != null && fechaInicio != null) {
            ActivityEntity(
                id = id,
                tipo = tipo,
                proveedorId = proveedorId,
                materialId = materialId,
                responsable = responsable ?: "Equipo Sima",
                fechaInicio = fechaInicio,
                // Mapeo de nuevos campos con valores por defecto
                cpmId = cpmId ?: "",
                people = people ?: emptyList(),
                ultimaSesionInicio = ultimaSesionInicio,
                timerHistory = timerHistory ?: emptyList(),
                defectos = defectos ?: emptyList(),
                defectoNota = defectoNota ?: "",
                // Cantidades y progreso
                cantidadTotal = cantidadTotal ?: 0,
                cantidadOk = cantidadOk ?: 0,
                cantidadNoOk = cantidadNoOk ?: 0,
                horasAcumuladas = horasAcumuladas ?: 0,
                estimadoHoras = estimadoHoras ?: "0",
                estimadoCosto = estimadoCosto ?: "0",
                estado = estado ?: "En curso",
                progreso = progreso ?: 0
            )
        } else null
    }

    // Mapeo desde Entity (Domain -> Data)
    companion object {
        fun fromEntity(entity: ActivityEntity): ActivityDto {
            return ActivityDto(
                id = entity.id.ifEmpty { null },
                tipo = entity.tipo,
                proveedorId = entity.proveedorId,
                materialId = entity.materialId,
                responsable = entity.responsable,
                fechaInicio = entity.fechaInicio,
                // Nuevos campos
                cpmId = entity.cpmId,
                people = entity.people,
                ultimaSesionInicio = entity.ultimaSesionInicio,
                timerHistory = entity.timerHistory,
                defectos = entity.defectos,
                defectoNota = entity.defectoNota,
                // Existentes
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