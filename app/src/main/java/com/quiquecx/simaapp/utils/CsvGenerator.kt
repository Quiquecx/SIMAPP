package com.quiquecx.simaapp.utils

import android.content.Context
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.ReportConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class CsvGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun generate(activities: List<ActivityEntity>, config: ReportConfig): File {
        val file = File(context.cacheDir, "report_${System.currentTimeMillis()}.csv")
        csvWriter().open(file) {
            // Encabezados
            val headers = mutableListOf<String>()
            if (config.includeGeneralInfo) headers.addAll(listOf("ID", "Tipo", "Proveedor", "Material", "CPM", "Responsable", "Estado"))
            if (config.includeWorkers) headers.add("Personal (horas)")
            if (config.includeDefects) headers.add("Defectos")
            if (config.includeProductivity) headers.addAll(listOf("HorasReales", "HorasEstimadas", "Productividad%"))
            if (config.includeCosts) headers.add("CostoEstimado")
            writeRow(headers)

            // Datos
            activities.forEach { activity ->
                val row = mutableListOf<String>()
                if (config.includeGeneralInfo) {
                    row.add(activity.id)
                    row.add(activity.tipo)
                    row.add(activity.proveedorId)
                    row.add(activity.materialId)
                    row.add(activity.cpmId)
                    row.add(activity.responsable)
                    row.add(activity.estado)
                }
                if (config.includeWorkers) {
                    val workersStr = activity.workers.joinToString(";") { "${it.name}(${String.format("%.2f", it.accumulatedHours)}h)" }
                    row.add(workersStr)
                }
                if (config.includeDefects) {
                    val defectsStr = activity.defectos.joinToString(";") { "${it.name}:${it.count}" }
                    row.add(defectsStr)
                }
                if (config.includeProductivity) {
                    val real = activity.horasAcumuladas
                    val estimado = activity.estimadoHoras.toDoubleOrNull() ?: 0.0
                    val prod = if (estimado > 0) ((real / estimado) * 100).toInt() else 0
                    row.add(real.toString())
                    row.add(estimado.toString())
                    row.add(prod.toString())
                }
                if (config.includeCosts) {
                    row.add(activity.estimadoCosto)
                }
                writeRow(row)
            }
        }
        return file
    }
}