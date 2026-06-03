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
        val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val html = buildString {
            append("""
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Reporte SIMAPP - Detallado</title>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background-color: #f5f5f5;
                            padding: 20px;
                            color: #333;
                        }
                        .container {
                            max-width: 1200px;
                            margin: 0 auto;
                            background: white;
                            padding: 30px;
                            border-radius: 8px;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                        }
                        .header {
                            border-bottom: 3px solid #EC221F;
                            margin-bottom: 30px;
                            padding-bottom: 15px;
                        }
                        .header h1 {
                            color: #EC221F;
                            font-size: 32px;
                            margin-bottom: 5px;
                        }
                        .header p {
                            color: #666;
                            font-size: 14px;
                        }
                        .activity-block {
                            border: 2px solid #E0E0E0;
                            margin-bottom: 30px;
                            padding: 20px;
                            border-radius: 8px;
                            background: #fafafa;
                        }
                        .activity-title {
                            font-size: 20px;
                            font-weight: bold;
                            color: #1976D2;
                            margin-bottom: 15px;
                            border-bottom: 2px solid #E0E0E0;
                            padding-bottom: 10px;
                        }
                        .section {
                            margin-bottom: 20px;
                        }
                        .section-title {
                            font-size: 16px;
                            font-weight: bold;
                            color: #1565C0;
                            margin-bottom: 10px;
                            padding: 8px;
                            background-color: #E3F2FD;
                            border-left: 4px solid #1565C0;
                        }
                        table {
                            width: 100%;
                            border-collapse: collapse;
                            margin: 10px 0;
                        }
                        th {
                            background-color: #1976D2;
                            color: white;
                            padding: 12px;
                            text-align: left;
                            font-weight: 600;
                        }
                        td {
                            padding: 10px 12px;
                            border-bottom: 1px solid #E0E0E0;
                        }
                        tr:hover {
                            background-color: #f5f5f5;
                        }
                        .field-row {
                            display: flex;
                            margin: 8px 0;
                        }
                        .field-label {
                            font-weight: bold;
                            width: 200px;
                            color: #333;
                        }
                        .field-value {
                            flex: 1;
                            color: #666;
                        }
                        .status-completed {
                            color: #4CAF50;
                            font-weight: bold;
                            background-color: #E8F5E9;
                            padding: 4px 8px;
                            border-radius: 4px;
                        }
                        .status-inprogress {
                            color: #2196F3;
                            font-weight: bold;
                            background-color: #E3F2FD;
                            padding: 4px 8px;
                            border-radius: 4px;
                        }
                        .status-pending {
                            color: #FF9800;
                            font-weight: bold;
                            background-color: #FFF3E0;
                            padding: 4px 8px;
                            border-radius: 4px;
                        }
                        .metrics {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                            gap: 15px;
                            margin: 15px 0;
                        }
                        .metric-card {
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            padding: 15px;
                            border-radius: 8px;
                            text-align: center;
                        }
                        .metric-label {
                            font-size: 12px;
                            opacity: 0.9;
                            text-transform: uppercase;
                            margin-bottom: 5px;
                        }
                        .metric-value {
                            font-size: 24px;
                            font-weight: bold;
                        }
                        .worker-info {
                            background-color: #f0f0f0;
                            padding: 10px;
                            margin: 5px 0;
                            border-radius: 4px;
                            border-left: 4px solid #4CAF50;
                        }
                        .defect-item {
                            background-color: #FFEBEE;
                            padding: 8px;
                            margin: 4px 0;
                            border-radius: 4px;
                            border-left: 4px solid #F44336;
                        }
                        .history-item {
                            background-color: #E8EAF6;
                            padding: 8px;
                            margin: 4px 0;
                            border-radius: 4px;
                            border-left: 4px solid #3F51B5;
                            font-size: 13px;
                        }
                        .footer {
                            margin-top: 40px;
                            padding-top: 20px;
                            border-top: 1px solid #E0E0E0;
                            text-align: center;
                            color: #999;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
            """.trimIndent())

            // Header
            append("""
                <div class="header">
                    <h1>📊 Reporte Detallado SIMAPP</h1>
                    <p>Generado: ${dateFormat.format(Date())}</p>
                    <p>Total de actividades: ${activities.size}</p>
                </div>
            """.trimIndent())

            // Procesar cada actividad
            activities.forEach { activity ->
                append("""
                    <div class="activity-block">
                        <div class="activity-title">
                            ${if (config.includeGeneralInfo) "ID: ${activity.cpmId} - ${activity.tipo}" else "Actividad"}
                        </div>
                """.trimIndent())

                // SECCIÓN: INFORMACIÓN GENERAL
                if (config.includeGeneralInfo) {
                    append("""
                        <div class="section">
                            <div class="section-title">📋 Información General</div>
                            <div class="field-row">
                                <div class="field-label">ID Actividad:</div>
                                <div class="field-value">${activity.id}</div>
                            </div>
                            <div class="field-row">
                                <div class="field-label">CPM ID:</div>
                                <div class="field-value">${activity.cpmId}</div>
                            </div>
                            <div class="field-row">
                                <div class="field-label">Tipo:</div>
                                <div class="field-value">${activity.tipo}</div>
                            </div>
                            <div class="field-row">
                                <div class="field-label">Proveedor ID:</div>
                                <div class="field-value">${activity.proveedorId}</div>
                            </div>
                            <div class="field-row">
                                <div class="field-label">Material ID:</div>
                                <div class="field-value">${activity.materialId}</div>
                            </div>
                            <div class="field-row">
                                <div class="field-label">Responsable:</div>
                                <div class="field-value">${activity.responsable}</div>
                            </div>
                            <div class="field-row">
                                <div class="field-label">Fecha Inicio:</div>
                                <div class="field-value">${dateFormat.format(activity.fechaInicio)}</div>
                            </div>
                            <div class="field-row">
                                <div class="field-label">Estado:</div>
                                <div class="field-value">
                                    <span class="status-${getStatusClass(activity.estado)}">${activity.estado}</span>
                                </div>
                            </div>
                        </div>
                    """.trimIndent())
                }

                // SECCIÓN: PERSONAL Y TIEMPOS
                if (config.includeWorkers && activity.workers.isNotEmpty()) {
                    append("""
                        <div class="section">
                            <div class="section-title">👥 Personal y Horas Trabajadas</div>
                            <table>
                                <thead>
                                    <tr>
                                        <th>Trabajador</th>
                                        <th>Horas Acumuladas</th>
                                        <th>Detalles por Día</th>
                                    </tr>
                                </thead>
                                <tbody>
                    """.trimIndent())

                    activity.workers.forEach { worker ->
                        append("""
                            <tr>
                                <td><strong>${worker.name}</strong></td>
                                <td>${String.format("%.2f", worker.accumulatedHours)} hrs</td>
                                <td>
                        """.trimIndent())

                        // Mostrar desglose diario si está disponible
                        if (worker.dailyHours.isNotEmpty()) {
                            worker.dailyHours.forEach { daily ->
                                append("""
                                    <div class="worker-info">
                                        📅 ${dateOnlyFormat.format(daily.date)}: 
                                        ${String.format("%.2f", daily.hoursWorked)} hrs 
                                        (${daily.tasksCompleted} tareas)
                                    </div>
                                """.trimIndent())
                            }
                        } else {
                            append("<em>Sin desglose diario disponible</em>")
                        }

                        append("</td></tr>")
                    }

                    append("""
                                </tbody>
                            </table>
                        </div>
                    """.trimIndent())
                }

                // SECCIÓN: DEFECTOS
                if (config.includeDefects && activity.defectos.isNotEmpty()) {
                    append("""
                        <div class="section">
                            <div class="section-title">⚠️ Defectos Registrados</div>
                    """.trimIndent())

                    activity.defectos.forEach { defect ->
                        append("""
                            <div class="defect-item">
                                <strong>${defect.name}:</strong> ${defect.count} unidades
                            </div>
                        """.trimIndent())
                    }

                    if (activity.defectoNota.isNotBlank()) {
                        append("""
                            <div style="margin-top: 10px; padding: 10px; background-color: #fff3cd; border-radius: 4px;">
                                <strong>Nota:</strong> ${activity.defectoNota}
                            </div>
                        """.trimIndent())
                    }

                    append("</div>")
                }

                // SECCIÓN: PRODUCTIVIDAD
                if (config.includeProductivity) {
                    val productivityPercent = calculateProductivity(activity)
                    append("""
                        <div class="section">
                            <div class="section-title">📈 Productividad</div>
                            <div class="metrics">
                                <div class="metric-card">
                                    <div class="metric-label">Total Producido</div>
                                    <div class="metric-value">${activity.cantidadTotal}</div>
                                </div>
                                <div class="metric-card">
                                    <div class="metric-label">Producción OK</div>
                                    <div class="metric-value" style="color: #4CAF50;">${activity.cantidadOk}</div>
                                </div>
                                <div class="metric-card">
                                    <div class="metric-label">Defectos</div>
                                    <div class="metric-value" style="color: #F44336;">${activity.cantidadNoOk}</div>
                                </div>
                                <div class="metric-card">
                                    <div class="metric-label">% Calidad</div>
                                    <div class="metric-value">$productivityPercent%</div>
                                </div>
                            </div>
                            <table>
                                <tr>
                                    <td><strong>Progreso General:</strong></td>
                                    <td>${activity.progreso}%</td>
                                </tr>
                                <tr>
                                    <td><strong>Horas Acumuladas (Reales):</strong></td>
                                    <td>${String.format("%.2f", activity.horasAcumuladas)} hrs</td>
                                </tr>
                                <tr>
                                    <td><strong>Horas Estimadas:</strong></td>
                                    <td>${activity.estimadoHoras} hrs</td>
                                </tr>
                            </table>
                        </div>
                    """.trimIndent())
                }

                // SECCIÓN: COSTOS
                if (config.includeCosts) {
                    append("""
                        <div class="section">
                            <div class="section-title">💰 Análisis de Costos</div>
                            <table>
                                <tr>
                                    <td><strong>Costo Estimado:</strong></td>
                                    <td>${activity.estimadoCosto}</td>
                                </tr>
                                <tr>
                                    <td><strong>Costo por Hora:</strong></td>
                                    <td>$${String.format("%.2f", activity.costPerHour)}</td>
                                </tr>
                                <tr>
                                    <td><strong>Costo Total de Mano de Obra:</strong></td>
                                    <td>$${String.format("%.2f", activity.totalLaborCost)}</td>
                                </tr>
                                <tr>
                                    <td><strong>Varianza de Costo:</strong></td>
                                    <td>${String.format("%.2f", activity.costVariance)}%</td>
                                </tr>
                            </table>
                        </div>
                    """.trimIndent())
                }

                // SECCIÓN: HISTORIAL TÉCNICO
                if (config.includeHistory && activity.timerHistory.isNotEmpty()) {
                    append("""
                        <div class="section">
                            <div class="section-title">📜 Historial Técnico</div>
                    """.trimIndent())

                    activity.timerHistory.forEach { entry ->
                        val endTime = entry.endTime?.let { dateFormat.format(it) } ?: "En progreso"
                        append("""
                            <div class="history-item">
                                <strong>${entry.user}</strong> | 
                                ${dateFormat.format(entry.startTime)} → $endTime | 
                                ${String.format("%.2f", entry.durationMinutes)} min
                            </div>
                        """.trimIndent())
                    }

                    append("</div>")
                }

                append("</div>") // Cierre activity-block
            }

            // Footer
            append("""
                <div class="footer">
                    <p>Reporte generado automáticamente por SIMAPP - Sistema de Inspección y Medición de Actividades</p>
                    <p>Todos los datos mostrados son registros precisos del sistema</p>
                </div>
                    </div>
                </body>
                </html>
            """.trimIndent())
        }

        file.writeText(html)
        return file
    }

    private fun getStatusClass(estado: String): String {
        return when (estado.lowercase()) {
            "finalizado" -> "completed"
            "en curso" -> "inprogress"
            else -> "pending"
        }
    }

    private fun calculateProductivity(activity: ActivityEntity): Int {
        if (activity.cantidadTotal <= 0) return 0
        return ((activity.cantidadOk.toDouble() / activity.cantidadTotal.toDouble()) * 100).toInt()
    }
}
