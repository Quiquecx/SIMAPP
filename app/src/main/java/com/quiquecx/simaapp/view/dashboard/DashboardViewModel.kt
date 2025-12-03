// archivo: IncomingDashboardViewModel.kt

package com.quiquecx.simaapp.view.dashboard

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class IncomingDashboardViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // 1. Estado para la lista completa de actividades
    private val _activities = MutableStateFlow<List<ActivityEntity>>(emptyList())
    val activities: StateFlow<List<ActivityEntity>> = _activities.asStateFlow()

    // 2. Estado para los Key Performance Indicators (KPIs)
    private val _kpis = MutableStateFlow(KpiData())
    val kpis: StateFlow<KpiData> = _kpis.asStateFlow()

    init {
        // Iniciar la escucha en tiempo real para todas las actividades
        fetchActivitiesStream()
    }

    // Método para la LECTURA (R) en tiempo real de la colección completa
    // Método para la LECTURA (R) en tiempo real de la colección completa
    private fun fetchActivitiesStream() {
        // Escucha en tiempo real toda la colección "activities"
        firestore.collection("activities")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Error al escuchar actividades: $e")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // 🚨 CORRECCIÓN CLAVE: Usamos mapNotNull con try-catch para evitar crashes.
                    val activityList = snapshot.documents.mapNotNull { document ->
                        try {
                            val activity = document.toObject(ActivityEntity::class.java)

                            // Si el mapeo es exitoso, aseguramos que el ID se asigna
                            activity?.copy(id = document.id)
                        } catch (ex: Exception) {
                            // Capturamos el error de mapeo y lo reportamos en el log,
                            // pero permitimos que la app siga funcionando.
                            println("❌ Documento fallido encontrado. ID: ${document.id}. Error: ${ex.localizedMessage}")
                            null // mapNotNull ignora este elemento
                        }
                    }

                    _activities.value = activityList
                    // Recalcular los KPIs cada vez que la lista se actualiza
                    calculateKpis(activityList)
                }
            }
    }

    // Función para calcular los KPIs
    private fun calculateKpis(activities: List<ActivityEntity>) {
        val totalActivities = activities.size
        val completedActivities = activities.count { it.estado == "Finalizado" }
        val inProgressActivities = totalActivities - completedActivities

        // Calcular el progreso promedio general
        val totalProgress = activities.sumOf { it.progreso }
        val avgProgress = if (totalActivities > 0) totalProgress / totalActivities else 0

        _kpis.value = KpiData(
            totalActivities = totalActivities,
            completedActivities = completedActivities,
            inProgressActivities = inProgressActivities,
            avgProgress = avgProgress
        )
    }
}

// Data class para modelar los KPIs
data class KpiData(
    val totalActivities: Int = 0,
    val completedActivities: Int = 0,
    val inProgressActivities: Int = 0,
    val avgProgress: Int = 0 // Progreso promedio de todas las actividades
)