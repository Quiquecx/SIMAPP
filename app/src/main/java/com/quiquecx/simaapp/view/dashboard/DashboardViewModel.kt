package com.quiquecx.simaapp.view.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class IncomingDashboardViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // 1. Lista "cruda" que viene directo de Firebase
    private val _rawActivities = MutableStateFlow<List<ActivityEntity>>(emptyList())

    // 2. Estado para el texto de búsqueda
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 3. Flow combinado: Filtra la lista cruda basándose en la búsqueda
    // Usamos stateIn para que sea un StateFlow y la UI pueda suscribirse
    val activities: StateFlow<List<ActivityEntity>> = combine(_rawActivities, _searchQuery) { activities, query ->
        if (query.isBlank()) {
            activities
        } else {
            activities.filter { activity ->
                activity.cpmId.contains(query, ignoreCase = true) ||
                        activity.proveedorId.contains(query, ignoreCase = true) ||
                        activity.tipo.contains(query, ignoreCase = true) ||
                        activity.materialId.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 4. KPIs
    private val _kpis = MutableStateFlow(KpiData())
    val kpis: StateFlow<KpiData> = _kpis.asStateFlow()

    init {
        fetchActivitiesStream()
    }

    // Función para actualizar el texto de búsqueda desde la UI
    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun fetchActivitiesStream() {
        firestore.collection("activities")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Error al escuchar actividades: $e")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val activityList = snapshot.documents.mapNotNull { document ->
                        try {
                            val activity = document.toObject(ActivityEntity::class.java)
                            activity?.copy(id = document.id)
                        } catch (ex: Exception) {
                            println("Documento fallido: ${document.id}. Error: ${ex.localizedMessage}")
                            null
                        }
                    }

                    _rawActivities.value = activityList
                    calculateKpis(activityList)
                }
            }
    }

    private fun calculateKpis(activities: List<ActivityEntity>) {
        val totalActivities = activities.size
        val completedActivities = activities.count { it.estado == "Finalizado" }
        val inProgressActivities = totalActivities - completedActivities
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

data class KpiData(
    val totalActivities: Int = 0,
    val completedActivities: Int = 0,
    val inProgressActivities: Int = 0,
    val avgProgress: Int = 0
)