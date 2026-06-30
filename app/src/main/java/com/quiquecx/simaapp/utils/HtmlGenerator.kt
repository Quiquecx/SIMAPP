package com.quiquecx.simaapp.utils

import android.content.Context
import android.util.Base64
import com.quiquecx.simaapp.R
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.ReportConfig
import com.quiquecx.simaapp.domain.entity.WorkerSessionLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class HtmlGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun generate(
        activities: List<ActivityEntity>,
        config: ReportConfig,
        activitySessions: Map<String, List<WorkerSessionLog>> = emptyMap()
    ): File {
        val file = File(context.cacheDir, "report_${System.currentTimeMillis()}.html")
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val logoBase64 = getLogoBase64()

        val html = buildString {
            // ==================== HEAD ====================
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
                            background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
                            padding: 30px;
                            color: #333;
                            min-height: 100vh;
                        }
                        .container {
                            max-width: 1200px;
                            margin: 0 auto;
                            background: #ffffff;
                            padding: 35px;
                            border-radius: 16px;
                            box-shadow: 0 10px 40px rgba(0,0,0,0.12);
                        }
                        .header {
                            display: flex;
                            align-items: center;
                            justify-content: space-between;
                            border-bottom: 4px solid #EC221F;
                            margin-bottom: 30px;
                            padding-bottom: 20px;
                            flex-wrap: wrap;
                            gap: 15px;
                        }
                        .header-left {
                            display: flex;
                            align-items: center;
                            gap: 15px;
                        }
                        .header-logo {
                            height: 60px;
                            width: auto;
                            object-fit: contain;
                        }
                        .header-title h1 {
                            color: #EC221F;
                            font-size: 28px;
                            font-weight: 700;
                            margin-bottom: 2px;
                        }
                        .header-title p {
                            color: #666;
                            font-size: 13px;
                        }
                        .header-right {
                            text-align: right;
                            background: #f8f9fa;
                            padding: 10px 18px;
                            border-radius: 8px;
                            border-left: 4px solid #EC221F;
                        }
                        .header-right .label {
                            font-size: 11px;
                            text-transform: uppercase;
                            color: #999;
                            letter-spacing: 0.5px;
                        }
                        .header-right .value {
                            font-size: 16px;
                            font-weight: 600;
                            color: #333;
                        }
                        .activity-block {
                            border: 2px solid #E8ECF1;
                            margin-bottom: 35px;
                            padding: 25px;
                            border-radius: 12px;
                            background: #FAFBFD;
                            transition: all 0.2s;
                        }
                        .activity-block:hover {
                            border-color: #1976D2;
                            box-shadow: 0 4px 12px rgba(25,118,210,0.08);
                        }
                        .activity-title {
                            font-size: 22px;
                            font-weight: 700;
                            color: #0D47A1;
                            margin-bottom: 18px;
                            border-bottom: 2px solid #E8ECF1;
                            padding-bottom: 12px;
                            display: flex;
                            align-items: center;
                            gap: 10px;
                        }
                        .activity-title .badge {
                            font-size: 12px;
                            font-weight: 600;
                            padding: 3px 12px;
                            border-radius: 20px;
                            background: #E3F2FD;
                            color: #1565C0;
                        }
                        .section {
                            margin-bottom: 25px;
                        }
                        .section-title {
                            font-size: 17px;
                            font-weight: 700;
                            color: #1565C0;
                            margin-bottom: 12px;
                            padding: 10px 14px;
                            background: linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 100%);
                            border-radius: 8px;
                            border-left: 5px solid #1565C0;
                            display: flex;
                            align-items: center;
                            gap: 8px;
                        }
                        table {
                            width: 100%;
                            border-collapse: collapse;
                            margin: 8px 0;
                            border-radius: 8px;
                            overflow: hidden;
                            box-shadow: 0 1px 4px rgba(0,0,0,0.04);
                        }
                        th {
                            background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%);
                            color: white;
                            padding: 14px 16px;
                            text-align: left;
                            font-weight: 600;
                            font-size: 14px;
                            letter-spacing: 0.3px;
                        }
                        td {
                            padding: 12px 16px;
                            border-bottom: 1px solid #EEF2F7;
                            vertical-align: top;
                            font-size: 14px;
                        }
                        tr:hover td {
                            background-color: #F8FAFF;
                        }
                        tr:last-child td {
                            border-bottom: none;
                        }
                        .field-row {
                            display: flex;
                            padding: 6px 0;
                            border-bottom: 1px solid #F0F0F0;
                        }
                        .field-row:last-child {
                            border-bottom: none;
                        }
                        .field-label {
                            font-weight: 600;
                            width: 200px;
                            min-width: 180px;
                            color: #444;
                        }
                        .field-value {
                            flex: 1;
                            color: #666;
                        }
                        .status-completed {
                            color: #2E7D32;
                            font-weight: 700;
                            background-color: #E8F5E9;
                            padding: 4px 14px;
                            border-radius: 20px;
                            display: inline-block;
                            font-size: 13px;
                        }
                        .status-inprogress {
                            color: #0D47A1;
                            font-weight: 700;
                            background-color: #E3F2FD;
                            padding: 4px 14px;
                            border-radius: 20px;
                            display: inline-block;
                            font-size: 13px;
                        }
                        .status-pending {
                            color: #E65100;
                            font-weight: 700;
                            background-color: #FFF3E0;
                            padding: 4px 14px;
                            border-radius: 20px;
                            display: inline-block;
                            font-size: 13px;
                        }
                        .metrics {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
                            gap: 15px;
                            margin: 15px 0;
                        }
                        .metric-card {
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            padding: 18px 20px;
                            border-radius: 12px;
                            text-align: center;
                            box-shadow: 0 4px 12px rgba(102,126,234,0.3);
                            transition: transform 0.2s;
                        }
                        .metric-card:nth-child(2) {
                            background: linear-gradient(135deg, #43A047 0%, #2E7D32 100%);
                        }
                        .metric-card:nth-child(3) {
                            background: linear-gradient(135deg, #E53935 0%, #B71C1C 100%);
                        }
                        .metric-card:nth-child(4) {
                            background: linear-gradient(135deg, #FB8C00 0%, #E65100 100%);
                        }
                        .metric-label {
                            font-size: 11px;
                            opacity: 0.9;
                            text-transform: uppercase;
                            letter-spacing: 0.5px;
                            margin-bottom: 4px;
                        }
                        .metric-value {
                            font-size: 28px;
                            font-weight: 700;
                        }
                        .day-block {
                            background: #f8faff;
                            padding: 14px 16px;
                            margin: 10px 0;
                            border-radius: 10px;
                            border: 1px solid #E8ECF5;
                            box-shadow: 0 2px 6px rgba(0,0,0,0.03);
                        }
                        .day-title {
                            font-weight: 700;
                            color: #0D47A1;
                            font-size: 15px;
                            margin-bottom: 8px;
                            display: flex;
                            align-items: center;
                            gap: 10px;
                        }
                        .day-title .day-stats {
                            font-weight: 400;
                            font-size: 13px;
                            color: #555;
                        }
                        .session-item {
                            background: #ffffff;
                            padding: 8px 14px;
                            margin: 5px 0;
                            border-radius: 6px;
                            font-size: 13px;
                            border-left: 3px solid #1976D2;
                            box-shadow: 0 1px 3px rgba(0,0,0,0.04);
                            display: flex;
                            flex-wrap: wrap;
                            align-items: center;
                            gap: 8px;
                        }
                        .session-item .time {
                            font-weight: 600;
                            color: #0D47A1;
                            min-width: 60px;
                        }
                        .session-item .pieces {
                            font-weight: 500;
                        }
                        .session-item .defects {
                            font-size: 12px;
                            color: #C62828;
                            background: #FFEBEE;
                            padding: 2px 10px;
                            border-radius: 12px;
                            margin-left: auto;
                        }
                        .defect-item {
                            background: #FFEBEE;
                            padding: 10px 14px;
                            margin: 4px 0;
                            border-radius: 6px;
                            border-left: 4px solid #E53935;
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                        }
                        .defect-item .defect-name {
                            font-weight: 500;
                            color: #B71C1C;
                        }
                        .defect-item .defect-count {
                            font-weight: 700;
                            color: #C62828;
                            background: white;
                            padding: 2px 12px;
                            border-radius: 12px;
                        }
                        .badge-ok {
                            color: #2E7D32;
                            font-weight: 700;
                        }
                        .badge-nok {
                            color: #C62828;
                            font-weight: 700;
                        }
                        .badge-total {
                            color: #0D47A1;
                            font-weight: 700;
                        }
                        .note-box {
                            margin-top: 10px;
                            padding: 12px 16px;
                            background: #FFF8E1;
                            border-radius: 8px;
                            border-left: 4px solid #FFB300;
                            font-size: 13px;
                            color: #5D4037;
                        }
                        .footer {
                            margin-top: 40px;
                            padding-top: 20px;
                            border-top: 2px solid #E8ECF1;
                            text-align: center;
                            color: #999;
                            font-size: 12px;
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            flex-wrap: wrap;
                            gap: 10px;
                        }
                        .footer p { margin: 2px 0; }
                        @media print {
                            body { background: white; padding: 0; }
                            .container { box-shadow: none; padding: 20px; }
                            .activity-block:hover { border-color: #E8ECF1; box-shadow: none; }
                            .metric-card:hover { transform: none; }
                        }
                        @media (max-width: 768px) {
                            .header { flex-direction: column; align-items: stretch; text-align: center; }
                            .header-left { justify-content: center; flex-wrap: wrap; }
                            .header-right { text-align: center; }
                            .field-row { flex-direction: column; padding: 8px 0; }
                            .field-label { width: 100%; }
                            .metrics { grid-template-columns: 1fr 1fr; }
                            .container { padding: 15px; }
                            th, td { padding: 8px 10px; font-size: 13px; }
                        }
                        @media (max-width: 480px) {
                            .metrics { grid-template-columns: 1fr; }
                            .session-item { flex-direction: column; align-items: flex-start; }
                            .session-item .defects { margin-left: 0; }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
            """.trimIndent()
            )

            // ==================== HEADER ====================
            append("""
                <div class="header">
                    <div class="header-left">
                        <img src="$logoBase64" alt="SIMA Logo" class="header-logo">
                        <div class="header-title">
                            <h1>📊 Reporte Detallado SIMAPP</h1>
                            <p>Generado: ${dateFormat.format(Date())}</p>
                        </div>
                    </div>
                    <div class="header-right">
                        <div class="label">Total de Actividades</div>
                        <div class="value">${activities.size}</div>
            """.trimIndent()
            )

            if (config.startDate != null || config.endDate != null) {
                val periodText = when {
                    config.startDate != null && config.endDate != null ->
                        "${dateOnlyFormat.format(config.startDate.toDate())} - ${dateOnlyFormat.format(config.endDate.toDate())}"
                    config.startDate != null -> "Desde: ${dateOnlyFormat.format(config.startDate.toDate())}"
                    config.endDate != null -> "Hasta: ${dateOnlyFormat.format(config.endDate.toDate())}"
                    else -> ""
                }
                append("""
                        <div class="label" style="margin-top:4px;">Período</div>
                        <div class="value" style="font-size:13px;">$periodText</div>
                """.trimIndent()
                )
            }

            append("</div></div>")

            // ==================== ACTIVIDADES ====================
            activities.forEach { activity ->
                val sessionsForActivity = activitySessions[activity.id] ?: emptyList()
                val sessionsByWorker = sessionsForActivity.groupBy { it.workerName }

                append("""
                    <div class="activity-block">
                        <div class="activity-title">
                            ${if (config.includeGeneralInfo) "🔹 ${activity.cpmId} - ${activity.tipo}" else "Actividad"}
                            <span class="badge">${activity.estado}</span>
                        </div>
                """.trimIndent()
                )

                // INFORMACIÓN GENERAL
                if (config.includeGeneralInfo) {
                    append("""
                        <div class="section">
                            <div class="section-title">📋 Información General</div>
                            <div class="field-row"><div class="field-label">ID Actividad</div><div class="field-value">${activity.id}</div></div>
                            <div class="field-row"><div class="field-label">CPM ID</div><div class="field-value">${activity.cpmId}</div></div>
                            <div class="field-row"><div class="field-label">Tipo</div><div class="field-value">${activity.tipo}</div></div>
                            <div class="field-row"><div class="field-label">Proveedor ID</div><div class="field-value">${activity.proveedorId}</div></div>
                            <div class="field-row"><div class="field-label">Material ID</div><div class="field-value">${activity.materialId}</div></div>
                            <div class="field-row"><div class="field-label">Responsable</div><div class="field-value">${activity.responsable}</div></div>
                            <div class="field-row"><div class="field-label">Fecha Inicio</div><div class="field-value">${dateFormat.format(activity.fechaInicio)}</div></div>
                            <div class="field-row"><div class="field-label">Estado</div><div class="field-value"><span class="status-${getStatusClass(activity.estado)}">${activity.estado}</span></div></div>
                        </div>
                    """.trimIndent()
                    )
                }

                // PERSONAL Y TIEMPOS
                if (config.includeWorkers && activity.workers.isNotEmpty()) {
                    append("""
                        <div class="section">
                            <div class="section-title">👥 Personal y Horas Trabajadas</div>
                            <table>
                                <thead><tr><th style="width:20%;">Trabajador</th><th style="width:15%;">Horas Totales</th><th>Detalle de Producción</th></tr></thead>
                                <tbody>
                    """.trimIndent()
                    )

                    activity.workers.forEach { worker ->
                        val workerSessions = sessionsByWorker[worker.name] ?: emptyList()
                        val totalPiezas = workerSessions.sumOf { it.piecesChecked }
                        val totalOk = workerSessions.sumOf { it.piecesOk }
                        val totalNoOk = workerSessions.sumOf { it.piecesNoOk }

                        append("""
                            <tr><td><strong>${worker.name}</strong></td>
                            <td>${String.format("%.2f", worker.accumulatedHours)} hrs</td>
                            <td>
                        """.trimIndent()
                        )

                        if (workerSessions.isNotEmpty()) {
                            val sessionsByDay = workerSessions.groupBy { it.dia }

                            sessionsByDay.forEach { (dia, sesionesDelDia) ->
                                val fechaFormateada = try {
                                    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    parser.parse(dia)?.let { formatter.format(it) } ?: dia
                                } catch (e: Exception) { dia }

                                val totalPiezasDia = sesionesDelDia.sumOf { it.piecesChecked }
                                val totalOkDia = sesionesDelDia.sumOf { it.piecesOk }
                                val totalNoOkDia = sesionesDelDia.sumOf { it.piecesNoOk }
                                val totalHorasDia = sesionesDelDia.sumOf { it.durationHours }

                                append("""
                                    <div class="day-block">
                                        <div class="day-title">📅 $fechaFormateada <span class="day-stats">(${String.format("%.2f", totalHorasDia)} hrs | <span class="badge-total">$totalPiezasDia pzs</span> | <span class="badge-ok">$totalOkDia OK</span> | <span class="badge-nok">$totalNoOkDia No OK</span>)</span></div>
                                """.trimIndent()
                                )

                                sesionesDelDia.forEach { session ->
                                    val horaSesion = SimpleDateFormat("HH:mm", Locale.getDefault()).format(session.timestamp.toDate())
                                    append("""
                                        <div class="session-item">
                                            <span class="time">🕐 $horaSesion</span>
                                            <span class="pieces">${session.piecesChecked} pzs (<span class="badge-ok">${session.piecesOk} OK</span>, <span class="badge-nok">${session.piecesNoOk} No OK</span>)</span>
                                    """.trimIndent()
                                    )

                                    if (session.defectos.isNotEmpty()) {
                                        val defectosStr = session.defectos.filter { it.count > 0 }
                                            .joinToString(", ") { "${it.name}: ${it.count}" }
                                        append("""
                                            <span class="defects">⚠️ $defectosStr</span>
                                        """.trimIndent()
                                        )
                                    }
                                    append("</div>")
                                }
                                append("</div>")
                            }
                        } else {
                            append("<em style='color:#999;'>Sin registros de producción en el periodo</em>")
                        }
                        append("</td></tr>")
                    }

                    append("""
                                </tbody>
                            </table>
                        </div>
                    """.trimIndent()
                    )
                }

                // DEFECTOS
                if (config.includeDefects && activity.defectos.isNotEmpty()) {
                    append("""
                        <div class="section">
                            <div class="section-title">⚠️ Defectos Registrados</div>
                    """.trimIndent()
                    )

                    activity.defectos.forEach { defect ->
                        append("""
                            <div class="defect-item"><span class="defect-name">${defect.name}</span><span class="defect-count">${defect.count} unidades</span></div>
                        """.trimIndent()
                        )
                    }

                    if (activity.defectoNota.isNotBlank()) {
                        append("""
                            <div class="note-box">📝 <strong>Nota:</strong> ${activity.defectoNota}</div>
                        """.trimIndent()
                        )
                    }
                    append("</div>")
                }

                // PRODUCTIVIDAD
                if (config.includeProductivity) {
                    val productivityPercent = calculateProductivity(activity)
                    append("""
                        <div class="section">
                            <div class="section-title">📈 Productividad</div>
                            <div class="metrics">
                                <div class="metric-card"><div class="metric-label">Total Producido</div><div class="metric-value">${activity.cantidadTotal}</div></div>
                                <div class="metric-card"><div class="metric-label">Producción OK</div><div class="metric-value">${activity.cantidadOk}</div></div>
                                <div class="metric-card"><div class="metric-label">Defectos</div><div class="metric-value">${activity.cantidadNoOk}</div></div>
                                <div class="metric-card"><div class="metric-label">% Calidad</div><div class="metric-value">$productivityPercent%</div></div>
                            </div>
                            <table>
                                <tr><td style="width:40%;"><strong>Progreso General</strong></td><td>${activity.progreso}%</td></tr>
                                <tr><td><strong>Horas Acumuladas (Reales)</strong></td><td>${String.format("%.2f", activity.horasAcumuladas)} hrs</td></tr>
                                <tr><td><strong>Horas Estimadas</strong></td><td>${activity.estimadoHoras} hrs</td></tr>
                            </table>
                        </div>
                    """.trimIndent()
                    )
                }

                // COSTOS
                if (config.includeCosts) {
                    append("""
                        <div class="section">
                            <div class="section-title">💰 Análisis de Costos</div>
                            <table>
                                <tr><td style="width:40%;"><strong>Costo Estimado</strong></td><td>${activity.estimadoCosto}</td></tr>
                                <tr><td><strong>Costo por Hora</strong></td><td>$${String.format("%.2f", activity.costPerHour)}</td></tr>
                                <tr><td><strong>Costo Total de Mano de Obra</strong></td><td>$${String.format("%.2f", activity.totalLaborCost)}</td></tr>
                                <tr><td><strong>Varianza de Costo</strong></td><td>${String.format("%.2f", activity.costVariance)}%</td></tr>
                            </table>
                        </div>
                    """.trimIndent()
                    )
                }

                append("</div>")
            }

            // ==================== FOOTER ====================
            append("""
                <div class="footer">
                    <div><p>📋 Reporte generado automáticamente por <strong>SIMAPP</strong></p><p>Sistema de Inspección y Medición de Actividades</p></div>
                    <div><p style="font-size:11px; color:#bbb;">${dateFormat.format(Date())}</p></div>
                </div>
                    </div>
                </body>
                </html>
            """.trimIndent()
            )
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

    private fun getLogoBase64(): String {
        return try {
            val drawable = context.resources.getDrawable(R.drawable.simalogo, null)
            val bitmap = android.graphics.Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            val bytes = stream.toByteArray()
            "data:image/png;base64,${Base64.encodeToString(bytes, Base64.DEFAULT)}"
        } catch (e: Exception) {
            "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='50'%3E%3Crect width='100' height='50' fill='%23EC221F'/%3E%3Ctext x='10' y='30' fill='white' font-size='16' font-family='Arial'%3ESIMA%3C/text%3E%3C/svg%3E"
        }
    }
}