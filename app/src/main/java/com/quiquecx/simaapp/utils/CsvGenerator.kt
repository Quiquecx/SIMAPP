package com.quiquecx.simaapp.utils

import android.content.Context
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.ReportConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class CsvGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun generate(activities: List<ActivityEntity>, config: ReportConfig): File {
        val file = File(context.cacheDir, "report_${System.currentTimeMillis()}.csv")
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val csvContent = buildString {
            // Metadatos (comentarios o líneas separadas)
            append("Fecha de generación;${dateFormat.format(Date())}\n")
            if (config.startDate != null || config.endDate != null) {
                val period = when {
                    config.startDate != null && config.endDate != null ->
                        "Período;${dateOnlyFormat.format(config.startDate.toDate())} - ${dateOnlyFormat.format(config.endDate.toDate())}"
                    config.startDate != null -> "Desde;${dateOnlyFormat.format(config.startDate.toDate())}"
                    config.endDate != null -> "Hasta;${dateOnlyFormat.format(config.endDate.toDate())}"
                    else -> ""
                }
                if (period.isNotEmpty()) append("$period\n")
            }
            append("\n") // línea en blanco

            // Encabezados (usando punto y coma)
            val headers = mutableListOf<String>()
            if (config.includeGeneralInfo) {
                headers.addAll(listOf("ID", "Tipo", "Proveedor ID", "Material ID", "CPM ID", "Responsable", "Estado"))
            }
            if (config.includeWorkers) {
                headers.addAll(listOf("Personal (nombres)", "Horas totales", "Detalle horas por trabajador (nombre:horas)"))
            }
            if (config.includeDefects) {
                headers.add("Defectos (nombre:cantidad)")
            }
            if (config.includeProductivity) {
                headers.addAll(listOf("Productividad (%)", "Horas estimadas", "Horas reales"))
            }
            if (config.includeCosts) {
                headers.add("Costo estimado")
            }
            append(headers.joinToString(";"))
            append("\n")

            // Datos
            activities.forEach { activity ->
                val row = mutableListOf<String>()

                if (config.includeGeneralInfo) {
                    row.add(escapeCsv(activity.id))
                    row.add(escapeCsv(activity.tipo))
                    row.add(escapeCsv(activity.proveedorId))
                    row.add(escapeCsv(activity.materialId))
                    row.add(escapeCsv(activity.cpmId))
                    row.add(escapeCsv(activity.responsable))
                    row.add(escapeCsv(activity.estado))
                }

                if (config.includeWorkers) {
                    val workerNames = activity.workers.joinToString(";") { it.name }
                    val totalHours = activity.horasAcumuladas
                    val detailHours = activity.workers.joinToString(";") { "${it.name}:${String.format("%.2f", it.accumulatedHours)}" }
                    row.add(escapeCsv(workerNames))
                    row.add(String.format("%.2f", totalHours))
                    row.add(escapeCsv(detailHours))
                }

                if (config.includeDefects) {
                    val defects = activity.defectos.joinToString(";") { "${it.name}:${it.count}" }
                    row.add(escapeCsv(defects))
                }

                if (config.includeProductivity) {
                    val productivity = calculateProductivity(activity)
                    row.add(productivity.toString())
                    row.add(escapeCsv(activity.estimadoHoras))
                    row.add(String.format("%.2f", activity.horasAcumuladas))
                }

                if (config.includeCosts) {
                    row.add(escapeCsv(activity.estimadoCosto))
                }

                append(row.joinToString(";"))
                append("\n")
            }
        }

        file.writeText(csvContent)
        return file
    }

    private fun calculateProductivity(activity: ActivityEntity): Int {
        val estimado = activity.estimadoHoras.toDoubleOrNull() ?: 0.0
        val real = activity.horasAcumuladas
        return if (estimado > 0) ((real / estimado) * 100).toInt().coerceIn(0, 100) else 0
    }

    private fun escapeCsv(value: String): String {
        // Si contiene punto y coma, comillas dobles o saltos de línea, se escapa entre comillas dobles
        val needsQuotes = value.contains(";") || value.contains("\"") || value.contains("\n")
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}