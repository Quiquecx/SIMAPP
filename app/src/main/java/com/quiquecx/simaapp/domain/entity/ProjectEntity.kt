package com.quiquecx.simaapp.domain.entity

data class ProjectEntity(
    val id: String,
    val name: String,
    val description: String,
    val status: String,
    val responsible: String,
    val imageType: String
)