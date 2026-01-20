package com.quiquecx.simaapp.view.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.quiquecx.simaapp.data.model.ActivityDto
import com.quiquecx.simaapp.data.model.WorkerDto
import com.quiquecx.simaapp.domain.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val _productivityLogs = MutableStateFlow<List<ProductivityEntity>>(emptyList())
    val productivityLogs: StateFlow<List<ProductivityEntity>> = _productivityLogs.asStateFlow()

    private val currentUser get() = auth.currentUser
    private val currentUserName get() = currentUser?.email ?: "Desconocido"

    private val activityId: String? = savedStateHandle.get<String>("activityId")?.let {
        URLDecoder.decode(it, "utf-8")
    }

    init {
        activityId?.let { id ->
            fetchActivityDetailsStream(id)
            fetchActivityHistory(id)
            fetchProductivityLogs(id)
            startClockTicker()
        }
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (true) {
                _currentTime.value = System.currentTimeMillis()
                delay(1000)
            }
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
                    try {
                        val dto = snapshot.toObject(ActivityDto::class.java)
                        _activity.value = dto?.toEntity()?.copy(id = snapshot.id)
                    } catch (mapError: Exception) {
                        _error.value = "Error de mapeo: ${mapError.message}"
                    }
                }
                _isLoading.value = false
            }
    }

    // --- GESTIÓN DE PERSONAL ---

    fun addPersonToActivity(name: String) {
        val id = activityId ?: return
        val newWorker = WorkerDto(name = name, isTimerActive = false)
        firestore.collection("activities").document(id)
            .update("workers", FieldValue.arrayUnion(newWorker))
    }

    fun removePersonFromActivity(worker: WorkerEntity) {
        val id = activityId ?: return
        firestore.collection("activities").document(id)
            .update("workers", FieldValue.arrayRemove(WorkerDto.fromEntity(worker)))
    }

    fun toggleWorkerTimer(workerName: String) {
        val id = activityId ?: return
        val currentAct = _activity.value ?: return
        val worker = currentAct.workers.find { it.name == workerName } ?: return
        val now = Timestamp.now()

        val updatedWorkers = currentAct.workers.map { w ->
            if (w.name == workerName) {
                if (!w.isTimerActive) {
                    w.copy(isTimerActive = true, startTime = now)
                } else {
                    val startMs = w.startTime?.toDate()?.time ?: now.toDate().time
                    val diffHours = (now.toDate().time - startMs).toDouble() / 3600000.0
                    w.copy(
                        isTimerActive = false,
                        startTime = null,
                        accumulatedHours = w.accumulatedHours + diffHours
                    )
                }
            } else w
        }.map { WorkerDto.fromEntity(it) }

        firestore.collection("activities").document(id).update("workers", updatedWorkers)
    }

    // --- CALIDAD Y PRODUCCIÓN ---

    fun addQualityCaptureWithShift(newOkCount: Int, captureDefects: List<DefectEntry>, turno: String) {
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
        val progress = calculateProgress(totalOkAcumulado, totalNoOkAcumulado, current.cantidadTotal)

        val log = mapOf(
            "timestamp" to Timestamp.now(),
            "dia" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            "turno" to turno,
            "cantidadOk" to newOkCount,
            "defectos" to captureDefects.filter { it.count > 0 },
            "registradoPor" to currentUserName
        )

        viewModelScope.launch {
            // Historial de turnos
            firestore.collection("activities").document(id).collection("productivity_logs").add(log)

            // BUG FIX: Registrar en Historial General
            addHistoryEntry("Producción ($turno)", "${current.cantidadOk} OK", "$totalOkAcumulado OK")

            updateFirestoreFields(mapOf(
                "cantidadOk" to totalOkAcumulado,
                "defectos" to listaDefectosActualizada,
                "cantidadNoOk" to totalNoOkAcumulado,
                "progreso" to progress
            ))
        }
    }

    fun adjustQualityTotals(newOkTotal: Int, defects: List<DefectEntry>) {
        val id = activityId ?: return
        val current = _activity.value ?: return
        val totalNoOk = defects.sumOf { it.count }
        val progress = calculateProgress(newOkTotal, totalNoOk, current.cantidadTotal)

        // BUG FIX: Registrar ajuste manual si hubo cambios
        if (current.cantidadOk != newOkTotal) {
            addHistoryEntry("Ajuste Manual OK", "${current.cantidadOk}", "$newOkTotal")
        }
        if (current.cantidadNoOk != totalNoOk) {
            addHistoryEntry("Ajuste Manual No OK", "${current.cantidadNoOk}", "$totalNoOk")
        }

        updateFirestoreFields(mapOf(
            "cantidadOk" to newOkTotal,
            "defectos" to defects,
            "cantidadNoOk" to totalNoOk,
            "progreso" to progress
        ))
    }

    fun deleteProductivityLog(log: ProductivityEntity) {
        val id = activityId ?: return
        viewModelScope.launch {
            try {
                firestore.collection("activities").document(id)
                    .collection("productivity_logs").document(log.id).delete()
            } catch (e: Exception) {
                _error.value = "Error al eliminar log: ${e.message}"
            }
        }
    }

    // --- GENERAL ---

    fun updateGeneralDetails(cantidadTotal: Int, estHoras: String, estCosto: String, nota: String, cpm: String) {
        val current = _activity.value ?: return

        // BUG FIX: Registrar cambios en la planificación
        if (current.cantidadTotal != cantidadTotal) {
            addHistoryEntry("Cambio Lote Total", "${current.cantidadTotal}", "$cantidadTotal")
        }
        if (current.cpmId != cpm) {
            addHistoryEntry("Cambio CPM ID", current.cpmId, cpm)
        }

        val progress = calculateProgress(current.cantidadOk, current.cantidadNoOk, cantidadTotal)

        updateFirestoreFields(mapOf(
            "cantidadTotal" to cantidadTotal,
            "estimadoHoras" to estHoras,
            "estimadoCosto" to estCosto,
            "defectoNota" to nota,
            "cpmId" to cpm,
            "progreso" to progress
        ))
    }

    fun addNewDefectType(name: String) {
        val id = activityId ?: return
        val current = _activity.value ?: return
        if (current.defectos.any { it.name.equals(name, ignoreCase = true) }) return
        val newDefect = DefectEntry(name = name, count = 0)
        firestore.collection("activities").document(id).update("defectos", FieldValue.arrayUnion(newDefect))
    }

    fun removeDefectType(defect: DefectEntry) {
        val id = activityId ?: return
        firestore.collection("activities").document(id).update("defectos", FieldValue.arrayRemove(defect))
    }

    // --- UTILS & HISTORY ---

    private fun addHistoryEntry(field: String, oldValue: String, newValue: String) {
        val id = activityId ?: return
        val entry = mapOf(
            "field" to field,
            "oldValue" to oldValue,
            "newValue" to newValue,
            "userName" to currentUserName,
            "timestamp" to Timestamp.now()
        )
        viewModelScope.launch {
            try {
                firestore.collection("activities").document(id)
                    .collection("history").add(entry)
            } catch (e: Exception) {
                _error.value = "Error al guardar historial: ${e.message}"
            }
        }
    }

    private fun calculateProgress(ok: Int, noOk: Int, total: Int): Int {
        if (total <= 0) return 0
        return (((ok + noOk).toDouble() / total.toDouble()) * 100).toInt().coerceIn(0, 100)
    }

    private fun updateFirestoreFields(updates: Map<String, Any>) {
        val id = activityId ?: return
        viewModelScope.launch {
            try {
                firestore.collection("activities").document(id).update(updates)
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun deleteActivity(onBack: () -> Unit) {
        activityId?.let { id ->
            firestore.collection("activities").document(id).delete().addOnSuccessListener { onBack() }
        }
    }

    private fun fetchActivityHistory(activityId: String) {
        firestore.collection("activities").document(activityId).collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                _history.value = snapshot?.documents?.mapNotNull { it.toObject(HistoryEntry::class.java) } ?: emptyList()
            }
    }

    private fun fetchProductivityLogs(activityId: String) {
        firestore.collection("activities").document(activityId).collection("productivity_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                _productivityLogs.value = snapshot?.documents?.mapNotNull { doc ->
                    val entity = doc.toObject(ProductivityEntity::class.java)
                    entity?.copy(id = doc.id)
                } ?: emptyList()
            }
    }
}