package com.quiquecx.simaapp.utils

import android.content.Context
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.ReportConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class HtmlGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun generate(activities: List<ActivityEntity>, config: ReportConfig): File {
        val file = File(context.cacheDir, "report_${System.currentTimeMillis()}.html")
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val html = buildString {
            append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Reporte SIMAPP</title>")
            append("<style>")
            append("body { font-family: sans-serif; margin: 20px; }")
            append("h1 { color: #6200EE; }")
            append(".activity { border: 1px solid #ccc; margin-bottom: 20px; padding: 15px; border-radius: 8px; }")
            append(".field { margin: 5px 0; }")
            append("table { border-collapse: collapse; width: 100%; }")
            append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
            append("th { background-color: #f2f2f2; }")
            append("</style></head><body>")
            append("<h1>Reporte SIMAPP</h1>")
            append("<p>Generado: ${dateFormat.format(Date())}</p>")

            activities.forEach { activity ->
                append("<div class='activity'>")
                if (config.includeGeneralInfo) {
                    append("<div class='field'><strong>ID:</strong> ${activity.id}</div>")
                    append("<div class='field'><strong>Tipo:</strong> ${activity.tipo}</div>")
                    append("<div class='field'><strong>Proveedor ID:</strong> ${activity.proveedorId}</div>")
                    append("<div class='field'><strong>Material ID:</strong> ${activity.materialId}</div>")
                    append("<div class='field'><strong>CPM ID:</strong> ${activity.cpmId}</div>")
                    append("<div class='field'><strong>Responsable:</strong> ${activity.responsable}</div>")
                    append("<div class='field'><strong>Estado:</strong> ${activity.estado}</div>")
                }
                if (config.includeWorkers && activity.workers.isNotEmpty()) {
                    append("<div class='field'><strong>Personal asignado:</strong><ul>")
                    activity.workers.forEach { worker ->
                        append("<li>${worker.name} (${String.format("%.2f", worker.accumulatedHours)} hrs)</li>")
                    }
                    append("</ul></div>")
                }
                if (config.includeDefects && activity.defectos.isNotEmpty()) {
                    append("<div class='field'><strong>Defectos:</strong><ul>")
                    activity.defectos.forEach { defect ->
                        append("<li>${defect.name}: ${defect.count}</li>")
                    }
                    if (activity.defectoNota.isNotBlank()) {
                        append("<li>Nota: ${activity.defectoNota}</li>")
                    }
                    append("</ul></div>")
                }
                if (config.includeProductivity) {
                    val prod = calculateProductivity(activity)
                    append("<div class='field'><strong>Productividad:</strong> $prod% (Horas reales: ${String.format("%.2f", activity.horasAcumuladas)} / Estimadas: ${activity.estimadoHoras})</div>")
                }
                if (config.includeCosts) {
                    append("<div class='field'><strong>Costo estimado:</strong> ${activity.estimadoCosto}</div>")
                }
                append("</div>")
            }
            append("</body></html>")
        }
        file.writeText(html)
        return file
    }

    private fun calculateProductivity(activity: ActivityEntity): Int {
        val estimado = activity.estimadoHoras.toDoubleOrNull() ?: 0.0
        val real = activity.horasAcumuladas
        return if (estimado > 0) ((real / estimado) * 100).toInt().coerceIn(0, 100) else 0
    }
}