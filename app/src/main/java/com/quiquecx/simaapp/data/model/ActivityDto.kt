package com.quiquecx.simaapp.data.model

import com.google.firebase.firestore.DocumentId
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import java.util.Date

data class ActivityDto(
    @DocumentId
    val id: String? = null,
    val tipo: String? = null,
    val proveedorId: String? = null,
    val materialId: String? = null,
    val responsable: String? = null,
    val defecto: String? = null,
    val cantidadTotal: Int? = null,
    val cantidadOk: Int? = null,
    val cantidadNoOk: Int? = null,
    val horasAcumuladas: Int? = null,
    // 🚨 CORRECCIÓN: Cambiar de Double? a String? para coincidir con ActivityEntity
    val estimadoHoras: String? = null,
    val estimadoCosto: String? = null,
    val fechaInicio: Date? = null,
    val estado: String? = null,
    val progreso: Int? = null
) {
    // Mapeo a Entity (Data -> Domain)
    fun toEntity(): ActivityEntity? {
        // Validación básica de campos obligatorios
        return if (id != null && tipo != null && proveedorId != null && materialId != null && fechaInicio != null && estado != null) {
            ActivityEntity(
                id = id,
                tipo = tipo,
                proveedorId = proveedorId,
                materialId = materialId,
                responsable = responsable ?: "",
                defecto = defecto ?: "",
                cantidadTotal = cantidadTotal ?: 0,
                cantidadOk = cantidadOk ?: 0,
                cantidadNoOk = cantidadNoOk ?: 0,
                horasAcumuladas = horasAcumuladas ?: 0,
                // ✅ Ahora el tipo coincide (String? ?: String)
                estimadoHoras = estimadoHoras ?: "0",
                estimadoCosto = estimadoCosto ?: "0", // 🚨 CORRECCIÓN: Usar "0" también aquí
                fechaInicio = fechaInicio,
                estado = estado,
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
                defecto = entity.defecto,
                cantidadTotal = entity.cantidadTotal,
                cantidadOk = entity.cantidadOk,
                cantidadNoOk = entity.cantidadNoOk,
                horasAcumuladas = entity.horasAcumuladas,
                estimadoHoras = entity.estimadoHoras,
                estimadoCosto = entity.estimadoCosto,
                fechaInicio = entity.fechaInicio,
                estado = entity.estado,
                progreso = entity.progreso
            )
        }
    }
}