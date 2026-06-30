package com.quiquecx.simaapp.domain.useCase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.quiquecx.simaapp.domain.entity.*
import com.quiquecx.simaapp.domain.repository.DashboardRepository
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

/**
 * UseCase que enriquece actividades con datos reales de productivity_logs e history
 * Calcula horas reales por trabajador, historial completo, y productividad exacta
 */
class EnrichActivitiesForReportUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val firestore: FirebaseFirestore
) {

    suspend operator fun invoke(
        activities: List<ActivityEntity>,
        startDate: Date? = null,
        endDate: Date? = null
    ): List<ActivityEntity> {
        return activities.map { activity ->
            enrichActivity(activity, startDate, endDate)
        }
    }

    private suspend fun enrichActivity(
        activity: ActivityEntity,
        startDate: Date?,
        endDate: Date?
    ): ActivityEntity {
        try {
            // Obtener logs filtrando por rango de fechas si está definido
            val productivityLogs = getProductivityLogs(activity.id, startDate, endDate)
            val historyEntries = getHistoryEntries(activity.id, startDate, endDate)

            // Calcular desglose diario (solo para mostrar, sin reemplazar horas reales)
            val workerDailyBreakdown = calculateWorkerDailyHours(activity, productivityLogs)

            // Enriquecer workers con dailyHours, PERO conservar accumulatedHours original
            val enrichedWorkers = activity.workers.map { worker ->
                val workerLogs = workerDailyBreakdown.filter { it.workerName == worker.name }
                val dailyHours = workerLogs.map { log ->
                    DailyHours(
                        date = log.date,
                        hoursWorked = log.hoursWorked,      // Estimado visual, no afecta horas reales
                        tasksCompleted = log.tasksCompleted,
                        defectsFound = 0
                    )
                }
                // Mantener accumulatedHours original (real)
                worker.copy(
                    dailyHours = dailyHours,
                    accumulatedHours = worker.accumulatedHours
                )
            }

            // Horas acumuladas totales reales (suma de los accumulatedHours originales)
            val realTotalHours = enrichedWorkers.sumOf { it.accumulatedHours }

            return activity.copy(
                horasAcumuladas = realTotalHours,
                workers = enrichedWorkers,
                workerDailyBreakdown = workerDailyBreakdown,
                timerHistory = historyEntries.map { entry ->
                    TimerEntry(
                        startTime = entry["timestamp"] as? Date ?: Date(),
                        user = entry["userName"] as? String ?: "Desconocido",
                        durationMinutes = 0.0
                    )
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("EnrichActivities", "Error enriqueciendo actividad ${activity.id}", e)
            return activity
        }
    }

    private suspend fun getProductivityLogs(
        activityId: String,
        startDate: Date?,
        endDate: Date?
    ): List<Map<String, Any>> {
        return try {
            // 🔹 LOG 1: Ver las fechas que se usan para filtrar
            android.util.Log.d("ENRICH", "Filtrando logs desde startDate=$startDate hasta endDate=$endDate")

            var query = firestore.collection("activities").document(activityId)
                .collection("productivity_logs")
                .orderBy("timestamp", Query.Direction.ASCENDING)

            if (startDate != null) {
                query = query.whereGreaterThanOrEqualTo("timestamp", Timestamp(startDate))
            }
            if (endDate != null) {
                query = query.whereLessThanOrEqualTo("timestamp", Timestamp(endDate))
            }

            val snapshot = query.get().await()

            // 🔹 LOG 2: Cuántos documentos se obtuvieron
            android.util.Log.d("ENRICH", "Se encontraron ${snapshot.documents.size} productivity_logs en el rango")

            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            android.util.Log.e("ENRICH", "Error obteniendo productivity_logs", e)
            emptyList()
        }
    }

    private suspend fun getHistoryEntries(
        activityId: String,
        startDate: Date?,
        endDate: Date?
    ): List<Map<String, Any>> {
        return try {
            var query = firestore.collection("activities").document(activityId)
                .collection("history")
                .orderBy("timestamp", Query.Direction.ASCENDING)

            if (startDate != null) {
                query = query.whereGreaterThanOrEqualTo("timestamp", Timestamp(startDate))
            }
            if (endDate != null) {
                query = query.whereLessThanOrEqualTo("timestamp", Timestamp(endDate))
            }

            val snapshot = query.get().await()
            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun calculateWorkerDailyHours(
        activity: ActivityEntity,
        productivityLogs: List<Map<String, Any>>
    ): List<DailyWorkerHours> {
        val result = mutableListOf<DailyWorkerHours>()

        productivityLogs.forEach { log ->
            val registradoPor = (log["registradoPor"] as? String) ?: "Desconocido"
            val timestamp = log["timestamp"]
            val cantidadOk = (log["cantidadOk"] as? Number)?.toInt() ?: 0

            val date = when (timestamp) {
                is Timestamp -> timestamp.toDate()
                is Date -> timestamp
                else -> Date()
            }

            val dayKey = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            val existing = result.find {
                it.workerName == registradoPor &&
                        java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.date) == dayKey
            }

            if (existing != null) {
                val index = result.indexOf(existing)
                result[index] = existing.copy(
                    tasksCompleted = existing.tasksCompleted + cantidadOk,
                    hoursWorked = existing.hoursWorked + estimateHours(cantidadOk)
                )
            } else {
                result.add(
                    DailyWorkerHours(
                        date = date,
                        workerName = registradoPor,
                        hoursWorked = estimateHours(cantidadOk),
                        tasksCompleted = cantidadOk
                    )
                )
            }
        }
        return result
    }

    private fun estimateHours(tasksCompleted: Int): Double {
        // Estimación visual: 10 tareas = 1 hora (ajustable según negocio)
        return (tasksCompleted / 10.0).coerceAtLeast(0.0)
    }
}