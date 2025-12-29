package com.quiquecx.simaapp.view.dashboard

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
import java.util.Date
import javax.inject.Inject

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

    // --- LÓGICA DE CRONÓMETRO (RELOJ LOCAL) ---

    fun startTimerAndSetInProgress() {
        val id = activityId ?: return
        val current = _activity.value ?: return
        if (current.timerActive) return
        val updates = mutableMapOf<String, Any>("timerActive" to true, "timerStartTime" to Date())
        if (current.estado != "En curso") updates["estado"] = "En curso"
        updateFirestoreFields(updates)
        logActivityChange(id, "Cronómetro", "Estado", "Iniciado")
    }

    fun pauseTimerAndSave() {
        val id = activityId ?: return
        val current = _activity.value ?: return
        val startTime = current.timerStartTime ?: return
        val now = Date()
        val diffInMs = maxOf(0L, now.time - startTime.time)
        val sessionHours = diffInMs.toDouble() / 3600000.0
        val newTotalHours = current.horasAcumuladas + sessionHours
        val updates = mapOf(
            "timerActive" to false,
            "timerStartTime" to FieldValue.delete(),
            "horasAcumuladas" to newTotalHours
        )
        updateFirestoreFields(updates)
        logActivityChange(id, "Cronómetro", "Pausado", "Sesión: ${String.format("%.4f", sessionHours)} hrs")
    }

    // --- CONTROL DE CALIDAD ACUMULATIVO ---

    fun addQualityCapture(newOkCount: Int, captureDefects: List<DefectEntry>) {
        val id = activityId ?: return
        val current = _activity.value ?: return
        val totalOkAcumulado = current.cantidadOk + newOkCount
        val listaDefectosActualizada = current.defectos.toMutableList()

        captureDefects.forEach { capture ->
            if (capture.count > 0) {
                val index = listaDefectosActualizada.indexOfFirst { it.name.equals(capture.name, ignoreCase = true) }
                if (index != -1) {
                    val existing = listaDefectosActualizada[index]
                    listaDefectosActualizada[index] = existing.copy(count = existing.count + capture.count)
                } else {
                    listaDefectosActualizada.add(capture)
                }
            }
        }

        val totalNoOkAcumulado = listaDefectosActualizada.sumOf { it.count }
        val progress = if (current.cantidadTotal > 0) {
            ((totalOkAcumulado.toDouble() / current.cantidadTotal.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else 0

        updateFirestoreFields(mapOf(
            "cantidadOk" to totalOkAcumulado,
            "defectos" to listaDefectosActualizada,
            "cantidadNoOk" to totalNoOkAcumulado,
            "progreso" to progress
        ))
        logActivityChange(id, "Calidad", "Captura Lote", "Añadido OK: $newOkCount")
    }

    // --- CORRECCIÓN DE ERRORES (SOBREESCRIBIR) ---

    fun adjustQualityTotals(newOkTotal: Int, adjustedDefects: List<DefectEntry>) {
        val id = activityId ?: return
        val current = _activity.value ?: return
        val newTotalNoOk = adjustedDefects.sumOf { it.count }
        val progress = if (current.cantidadTotal > 0) {
            ((newOkTotal.toDouble() / current.cantidadTotal.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else 0

        updateFirestoreFields(mapOf(
            "cantidadOk" to newOkTotal,
            "defectos" to adjustedDefects,
            "cantidadNoOk" to newTotalNoOk,
            "progreso" to progress
        ))
        logActivityChange(id, "Calidad", "AJUSTE MANUAL", "Corrección de totales")
    }

    // --- OTRAS FUNCIONES ---

    fun addPersonToActivity(name: String) {
        val id = activityId ?: return
        viewModelScope.launch {
            try { firestore.collection("activities").document(id).update("people", FieldValue.arrayUnion(name))
                logActivityChange(id, "Personal", "Agregado", name)
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun removePersonFromActivity(name: String) {
        val id = activityId ?: return
        viewModelScope.launch {
            try { firestore.collection("activities").document(id).update("people", FieldValue.arrayRemove(name))
                logActivityChange(id, "Personal", "Removido", name)
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun updateGeneralDetails(cantidadTotal: Int, estHoras: String, estCosto: String, nota: String, cpm: String) {
        updateFirestoreFields(mapOf("cantidadTotal" to cantidadTotal, "estimadoHoras" to estHoras, "estimadoCosto" to estCosto, "defectoNota" to nota, "cpmId" to cpm))
        logActivityChange(activityId ?: "", "Detalles", "Edición", "Info general")
    }

    fun finalizeActivity() {
        val id = activityId ?: return
        if (_activity.value?.timerActive == true) pauseTimerAndSave()
        updateFirestoreFields(mapOf("estado" to "Finalizado", "timerActive" to false, "progreso" to 100))
        logActivityChange(id, "Estado", "Cambio", "Finalizado")
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
        val logEntry = HistoryEntry(Timestamp.now(), currentUserId, currentUserName, field, old?.toString() ?: "N/A", new?.toString() ?: "N/A")
        firestore.collection("activities").document(id).collection("history").add(logEntry)
    }

    fun deleteActivity(onBack: () -> Unit) {
        activityId?.let { id -> firestore.collection("activities").document(id).delete().addOnSuccessListener { onBack() } }
    }

    fun addNewDefectType(name: String) {
        val id = activityId ?: return
        val current = _activity.value ?: return
        if (current.defectos.any { it.name.equals(name, ignoreCase = true) }) return
        viewModelScope.launch {
            try { firestore.collection("activities").document(id).update("defectos", FieldValue.arrayUnion(DefectEntry(name, 0)))
                logActivityChange(id, "Estructura", "Nuevo defecto", name)
            } catch (e: Exception) { _error.value = e.message }
        }
    }
}