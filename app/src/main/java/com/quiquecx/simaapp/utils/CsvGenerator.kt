package com.quiquecx.simaapp.utils

import android.content.Context
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.ReportConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class CsvGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun generate(activities: List<ActivityEntity>, config: ReportConfig): File {
        val file = File(context.cacheDir, "report_${System.currentTimeMillis()}.csv")

        val csvContent = buildString {
            // Header
            if (config.includeGeneralInfo) {
                append("ID,Tipo,Proveedor ID,Material ID,CPM ID,Responsable,Estado,")
            }
            if (config.includeWorkers) {
                append("Personal,Horas Acumuladas,")
            }
            if (config.includeDefects) {
                append("Defectos,")
            }
            if (config.includeProductivity) {
                append("Productividad (%),Horas Estimadas,Horas Reales,")
            }
            if (config.includeCosts) {
                append("Costo Estimado,")
            }
            appendLine()

            // Datos
            activities.forEach { activity ->
                if (config.includeGeneralInfo) {
                    append("\"${activity.id}\",")
                    append("\"${activity.tipo}\",")
                    append("\"${activity.proveedorId}\",")
                    append("\"${activity.materialId}\",")
                    append("\"${activity.cpmId}\",")
                    append("\"${activity.responsable}\",")
                    append("\"${activity.estado}\",")
                }
                if (config.includeWorkers) {
                    val workers = activity.workers.joinToString(";") { it.name }
                    append("\"$workers\",")
                    append("${activity.horasAcumuladas},")
                }
                if (config.includeDefects) {
                    val defects = activity.defectos.joinToString(";") { "${it.name}:${it.count}" }
                    append("\"$defects\",")
                }
                if (config.includeProductivity) {
                    val productivity = calculateProductivity(activity)
                    append("$productivity,")
                    append("\"${activity.estimadoHoras}\",")
                    append("${activity.horasAcumuladas},")
                }
                if (config.includeCosts) {
                    append("\"${activity.estimadoCosto}\",")
                }
                appendLine()
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
}
