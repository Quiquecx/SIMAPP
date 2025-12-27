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
                    // Firestore mapeará automáticamente los números a Double y las fechas a Date/Timestamp
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

    // --- LÓGICA DE CRONÓMETRO PERSISTENTE (MODIFICADA) ---

    fun startTimerAndSetInProgress() {
        val id = activityId ?: return
        val current = _activity.value ?: return

        // Si ya está corriendo, no hacemos nada
        if (current.timerActive) return

        val updates = mutableMapOf<String, Any>(
            "timerActive" to true,
            "timerStartTime" to FieldValue.serverTimestamp() // Usamos la hora del servidor para evitar trampas con el reloj del celular
        )

        // Si el estado no era "En curso", lo cambiamos
        if (current.estado != "En curso") {
            updates["estado"] = "En curso"
        }

        updateFirestoreFields(updates)
        logActivityChange(id, "Cronómetro", "Estado", "Iniciado / En curso")
    }

    fun pauseTimerAndSave() {
        val id = activityId ?: return
        val current = _activity.value ?: return
        val startTime = current.timerStartTime ?: return

        // Calculamos la diferencia entre ahora y cuando inició
        val now = Date()
        val diffInMs = now.time - startTime.time

        // Convertimos milisegundos a horas decimales (ms / 1000 / 60 / 60)
        val sessionHours = diffInMs.toDouble() / 3600000.0
        val newTotalHours = current.horasAcumuladas + sessionHours

        val updates = mapOf(
            "isTimerRunning" to false,
            "timerStartTime" to FieldValue.delete(), // Limpiamos la hora de inicio
            "horasAcumuladas" to newTotalHours
        )

        updateFirestoreFields(updates)
        logActivityChange(id, "Cronómetro", "Pausado", "Sesión: ${String.format("%.4f", sessionHours)} hrs")
    }

    // --- CONTROL DE CALIDAD ---
    fun updateQualityData(okCount: Int, newDefects: List<DefectEntry>) {
        val id = activityId ?: return
        val current = _activity.value ?: return

        val newTotalNoOk = newDefects.sumOf { it.count }

        val progress = if (current.cantidadTotal > 0) {
            ((okCount.toDouble() / current.cantidadTotal.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else 0

        val updates = mapOf(
            "cantidadOk" to okCount,
            "defectos" to newDefects,
            "cantidadNoOk" to newTotalNoOk,
            "progreso" to progress
        )

        updateFirestoreFields(updates)
        logActivityChange(id, "Calidad", "Actualización", "OK: $okCount, Progreso: $progress%")
    }

    // --- PERSONAL ---
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

    // --- ACTUALIZACIÓN GENERAL ---
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

    fun finalizeActivity() {
        val id = activityId ?: return
        // Si el cronómetro está corriendo al finalizar, lo pausamos primero
        if (_activity.value?.timerActive == true) {
            pauseTimerAndSave()
        }

        val updates = mapOf(
            "estado" to "Finalizado",
            "progreso" to 100
        )
        updateFirestoreFields(updates)
        logActivityChange(id, "Estado", "Cambio", "Finalizado")
    }

    // --- FUNCIONES AUXILIARES ---
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
}