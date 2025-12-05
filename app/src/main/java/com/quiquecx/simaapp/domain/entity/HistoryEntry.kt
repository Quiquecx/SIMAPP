package com.quiquecx.simaapp.domain.entity

import com.google.firebase.Timestamp
import java.io.Serializable
import java.text.DecimalFormat

/**
 * Modelo de datos para un registro de auditoría en la subcolección 'history'.
 */
data class HistoryEntry(
    val timestamp: Timestamp = Timestamp.now(),
    val userId: String = "",
    val userName: String = "Desconocido",
    val field: String = "",
    val oldValue: Any? = null,
    val newValue: Any? = null,
) : Serializable {

    // Constructor sin argumentos necesario para Firestore deserialization
    @Suppress("unused")
    constructor() : this(Timestamp.now())

    /**
     * Función de utilidad para formatear los valores para la vista.
     */
    fun formatValue(value: Any?): String {
        return when (value) {
            is String -> if (value.isBlank()) "[Vacío]" else value
            is Number -> {
                // Formato para números enteros y decimales con separador de miles
                val formatter = DecimalFormat("#,##0.##")
                formatter.format(value)
            }
            is Boolean -> if (value) "Sí" else "No"
            null -> "[Nulo]"
            else -> value.toString()
        }
    }
}