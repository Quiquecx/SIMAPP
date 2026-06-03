package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.DailyHours
import com.quiquecx.simaapp.domain.entity.DailyWorkerHours
import com.quiquecx.simaapp.domain.repository.DashboardRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.quiquecx.simaapp.domain.entity.TimerEntry
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

/**
 * UseCase que enriquece actividades con datos reales de productivity_logs e history
 * Calcula horas reales por trabajador, historial completo, y produktividad exacta
 */
class EnrichActivitiesForReportUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val firestore: FirebaseFirestore
) {

    suspend operator fun invoke(activities: List<ActivityEntity>): List<ActivityEntity> {
        return activities.map { activity ->
            enrichActivity(activity)
        }
    }

    private suspend fun enrichActivity(activity: ActivityEntity): ActivityEntity {
        try {
            val productivityLogs = getProductivityLogs(activity.id)
            val historyEntries = getHistoryEntries(activity.id)

            // Calcular desglose diario (solo para mostrar, sin reemplazar horas reales)
            val workerDailyBreakdown = calculateWorkerDailyHours(activity, productivityLogs)

            // Enriquecer workers con dailyHours, PERO conservar accumulatedHours original
            val enrichedWorkers = activity.workers.map { worker ->
                val workerLogs = workerDailyBreakdown.filter { it.workerName == worker.name }
                val dailyHours = workerLogs.map { log ->
                    DailyHours(
                        date = log.date,
                        hoursWorked = log.hoursWorked,      // Este es estimado, pero solo visual
                        tasksCompleted = log.tasksCompleted,
                        defectsFound = 0
                    )
                }
                // ✅ Mantener accumulatedHours original (real)
                worker.copy(
                    dailyHours = dailyHours,
                    accumulatedHours = worker.accumulatedHours  // ← conservar valor real
                )
            }

            // ✅ Horas acumuladas totales reales (suma de los accumulatedHours originales)
            val realTotalHours = enrichedWorkers.sumOf { it.accumulatedHours }

            return activity.copy(
                horasAcumuladas = realTotalHours,  // ← usar el valor real
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

    private suspend fun getProductivityLogs(activityId: String): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("activities").document(activityId)
                .collection("productivity_logs")
                .get()
                .await()
            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getHistoryEntries(activityId: String): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("activities").document(activityId)
                .collection("history")
                .get()
                .await()
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

        // Procesar cada log de productividad
        productivityLogs.forEach { log ->
            val registradoPor = (log["registradoPor"] as? String) ?: "Desconocido"
            val timestamp = log["timestamp"]
            val cantidadOk = (log["cantidadOk"] as? Number)?.toInt() ?: 0

            val date = when (timestamp) {
                is com.google.firebase.Timestamp -> timestamp.toDate()
                is Date -> timestamp
                else -> Date()
            }

            // Buscar si ya existe una entrada para este trabajador en este día
            val dayKey = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            val existing = result.find {
                it.workerName == registradoPor &&
                        java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.date) == dayKey
            }

            if (existing != null) {
                // Actualizar entrada existente
                val index = result.indexOf(existing)
                result[index] = existing.copy(
                    tasksCompleted = existing.tasksCompleted + cantidadOk,
                    hoursWorked = existing.hoursWorked + estimateHours(cantidadOk)
                )
            } else {
                // Crear nueva entrada
                result.add(DailyWorkerHours(
                    date = date,
                    workerName = registradoPor,
                    hoursWorked = estimateHours(cantidadOk),
                    tasksCompleted = cantidadOk
                ))
            }
        }

        return result
    }

    private fun estimateHours(tasksCompleted: Int): Double {
        // Estimación: si se completaron X tareas, asumir Y horas
        // Ajusta según tu lógica de negocio (ej: 10 tareas = 1 hora)
        return (tasksCompleted / 10.0).coerceAtLeast(0.0)
    }
}
