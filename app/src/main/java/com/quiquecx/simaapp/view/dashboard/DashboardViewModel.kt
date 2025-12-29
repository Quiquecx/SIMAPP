package com.quiquecx.simaapp.view.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class IncomingDashboardViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _rawActivities = MutableStateFlow<List<ActivityEntity>>(emptyList())

    // Referencia para limpiar el listener si se cambia de proyecto
    private var snapshotListener: ListenerRegistration? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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

    private val _kpis = MutableStateFlow(KpiData())
    val kpis: StateFlow<KpiData> = _kpis.asStateFlow()

    /**
     * Función llamada desde el NavigationWrapper.
     * Configura el ID del proyecto y arranca la escucha filtrada.
     */
    fun initProject(projectId: String) {
        // Evitamos duplicar listeners si ya está escuchando el mismo proyecto
        snapshotListener?.remove()
        fetchActivitiesStream(projectId)
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun fetchActivitiesStream(projectId: String) {
        // 🚨 FILTRO CLAVE: Solo documentos donde projectId coincida con la selección (Incoming o P16)
        snapshotListener = firestore.collection("activities")
            .whereEqualTo("projectId", projectId)
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

    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove() // Limpieza de memoria al cerrar la pantalla
    }
}

data class KpiData(
    val totalActivities: Int = 0,
    val completedActivities: Int = 0,
    val inProgressActivities: Int = 0,
    val avgProgress: Int = 0
)