package com.quiquecx.simaapp.view.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.HistoryEntry
import com.quiquecx.simaapp.domain.entity.DefectEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

private const val TAG = "ActivityDetailsVM"

@HiltViewModel
class ActivityDetailsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _activity = MutableStateFlow<ActivityEntity?>(null)
    val activity: StateFlow<ActivityEntity?> = _activity.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val currentUser get() = auth.currentUser
    private val currentUserId get() = currentUser?.uid ?: "unknown"
    private val currentUserName get() = currentUser?.email ?: "Desconocido"

    private val activityId: String? = savedStateHandle.get<String>("activityId")?.let {
        URLDecoder.decode(it, "utf-8")
    }

    init {
        activityId?.let {
            fetchActivityDetailsStream(it)
            fetchActivityHistory(it)
        }
    }

    private fun fetchActivityDetailsStream(id: String) {
        firestore.collection("activities").document(id)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = e.message
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val activityObj = snapshot.toObject(ActivityEntity::class.java)
                    _activity.value = activityObj?.copy(id = snapshot.id)
                    _error.value = null
                }
                _isLoading.value = false
            }
    }

    private fun fetchActivityHistory(activityId: String) {
        firestore.collection("activities").document(activityId).collection("history")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                _history.value = snapshot?.documents?.mapNotNull { it.toObject(HistoryEntry::class.java) } ?: emptyList()
            }
    }

    // --- LÓGICA DE CONTROL DE CALIDAD (OK MANUAL + PROGRESO) ---
    fun updateQualityData(okCount: Int, newDefects: List<DefectEntry>) {
        val id = activityId ?: return
        val current = _activity.value ?: return

        val newTotalNoOk = newDefects.sumOf { it.count }

        // El progreso se basa en: (Piezas OK / Cantidad Total definida en detalles) * 100
        val progress = if (current.cantidadTotal > 0) {
            ((okCount.toFloat() / current.cantidadTotal.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else 0

        val updates = mapOf(
            "cantidadOk" to okCount,
            "defectos" to newDefects,
            "cantidadNoOk" to newTotalNoOk,
            "progreso" to progress
        )

        updateFirestoreFields(updates)
        logActivityChange(id, "Calidad", "Actualización", "OK: $okCount, No OK: $newTotalNoOk, Progreso: $progress%")
    }

    fun addNewDefectType(name: String) {
        val id = activityId ?: return
        val current = _activity.value ?: return
        if (current.defectos.any { it.name.equals(name, ignoreCase = true) }) return

        val newDefect = DefectEntry(name = name, count = 0)
        viewModelScope.launch {
            try {
                firestore.collection("activities").document(id)
                    .update("defectos", FieldValue.arrayUnion(newDefect))
                logActivityChange(id, "Estructura", "Nuevo defecto", name)
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    // --- LÓGICA DE PERSONAL (PEOPLE) ---
    fun addPersonToActivity(name: String) {
        val id = activityId ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                firestore.collection("activities").document(id)
                    .update("people", FieldValue.arrayUnion(name))
                logActivityChange(id, "Personal", "Agregado", name)
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun removePersonFromActivity(name: String) {
        val id = activityId ?: return
        viewModelScope.launch {
            try {
                firestore.collection("activities").document(id)
                    .update("people", FieldValue.arrayRemove(name))
                logActivityChange(id, "Personal", "Removido", name)
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    // --- LÓGICA DE ACTUALIZACIÓN GENERAL (INFO EDITABLE) ---
    fun updateGeneralDetails(cantidadTotal: Int, estHoras: String, estCosto: String, nota: String, cpm: String) {
        val updates = mapOf(
            "cantidadTotal" to cantidadTotal,
            "estimadoHoras" to estHoras,
            "estimadoCosto" to estCosto,
            "defectoNota" to nota,
            "cpmId" to cpm
        )
        updateFirestoreFields(updates)
        logActivityChange(activityId ?: "", "Detalles", "Edición manual", "Actualización general")
    }

    // --- ESTADO AUTOMÁTICO Y TIEMPO ---
    fun startTimerAndSetInProgress() {
        val current = _activity.value ?: return
        if (current.estado != "En curso" && current.estado != "Finalizado") {
            updateFirestoreFields(mapOf("estado" to "En curso"))
            logActivityChange(current.id, "Estado", current.estado, "En curso")
        }
    }

    fun finalizeActivity() {
        val id = activityId ?: return
        val updates = mapOf(
            "estado" to "Finalizado",
            "progreso" to 100 // Al finalizar forzamos el 100%
        )
        updateFirestoreFields(updates)
        logActivityChange(id, "Estado", "Cambio", "Finalizado")
    }

    fun saveTimerSession(minutes: Int) {
        val current = _activity.value ?: return
        val newHours = current.horasAcumuladas + (minutes.toFloat() / 60f)
        updateFirestoreFields(mapOf("horasAcumuladas" to newHours))
        logActivityChange(current.id, "Cronómetro", "Sesión", "+$minutes min")
    }

    private fun updateFirestoreFields(updates: Map<String, Any>) {
        val id = activityId ?: return
        viewModelScope.launch {
            try { firestore.collection("activities").document(id).update(updates) }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    private fun logActivityChange(id: String, field: String, old: Any?, new: Any?) {
        if (id.isBlank()) return
        val logEntry = HistoryEntry(
            timestamp = Timestamp.now(),
            userId = currentUserId,
            userName = currentUserName,
            field = field,
            oldValue = old?.toString() ?: "N/A",
            newValue = new?.toString() ?: "N/A"
        )
        firestore.collection("activities").document(id).collection("history").add(logEntry)
    }

    fun deleteActivity(onBack: () -> Unit) {
        activityId?.let { id ->
            firestore.collection("activities").document(id).delete()
                .addOnSuccessListener { onBack() }
        }
    }
}