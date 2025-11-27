package com.quiquecx.simaapp.data.model

import com.quiquecx.simaapp.domain.entity.ProjectEntity

data class ProjectDto(
    val id: String = "",
    val empresaId: String = "",
    val nombre: String = "",
    val descripcion: String = "", // Ojo: en tu seeder pusiste "descripcion" o "description"? Revisa el nombre exacto en Firebase
    val estado: String = "",
    val responsable: String = "",
    val imageType: String = "default" // "incoming", "cadenas", "vcts"
)

fun ProjectDto.toDomain(): ProjectEntity {
    return ProjectEntity(
        id = id,
        name = nombre,
        description = descripcion,
        status = estado,
        responsible = responsable,
        imageType = imageType
    )
}