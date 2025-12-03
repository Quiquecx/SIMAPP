package com.quiquecx.simaapp.domain.entity

import java.util.Date

data class ActivityEntity(
    // Identificación - Usar 'val' es el estándar para data class
    val id: String = "",

    // Información General
    val tipo: String = "",
    val proveedorId: String = "",
    val materialId: String = "",
    val responsable: String = "Equipo Sima",
    val fechaInicio: Date = Date(),

    // Cantidades
    val cantidadTotal: Int = 0,
    val cantidadOk: Int = 0,
    val cantidadNoOk: Int = 0,

    // Estado y Progreso
    val estado: String = "En curso",
    val progreso: Int = 0,
    val defecto: String = "Ninguno",

    // Tiempos y Costos
    val horasAcumuladas: Int = 0,
    val estimadoHoras: String = "0", // String
    val estimadoCosto: String = "0" // String
)