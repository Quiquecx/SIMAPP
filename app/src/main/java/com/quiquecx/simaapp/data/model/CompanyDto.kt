package com.quiquecx.simaapp.data.model

import com.quiquecx.simaapp.domain.entity.CompanyEntity

// Mapeo de la colección "Empresas"
data class CompanyDto(
    val id: String = "",
    val nombre: String = "",
    val responsable: String = ""
)

fun CompanyDto.toDomain(): CompanyEntity {
    return CompanyEntity(
        id = id,
        name = nombre,
        responsible = responsable
    )
}