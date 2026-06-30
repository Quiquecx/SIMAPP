package com.quiquecx.simaapp.view.dashboard

import android.content.Context
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
import com.quiquecx.simaapp.domain.useCase.GenerateReportUseCase
import com.quiquecx.simaapp.domain.useCase.ShareReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ActivityDetailsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val generateReportUseCase: GenerateReportUseCase,
    private val shareReportUseCase: ShareReportUseCase,
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

    private val _filteredLogs = MutableStateFlow<List<ProductivityEntity>>(emptyList())
    val filteredLogs: StateFlow<List<ProductivityEntity>> = _filteredLogs.asStateFlow()

    private val _workerSessions = MutableStateFlow<List<WorkerSessionLog>>(emptyList())
    val workerSessions: StateFlow<List<WorkerSessionLog>> = _workerSessions.asStateFlow()

    private val currentUser get() = auth.currentUser
    private val currentUserName get() = currentUser?.email ?: "Desconocido"

    private val activityId: String? = savedStateHandle.get<String>("activityId")?.let {
        URLDecoder.decode(it, "utf-8")
    }

    private var currentFilterStartDate: Date? = null
    private var currentFilterEndDate: Date? = null

    init {
        activityId?.let { id ->
            fetchActivityDetailsStream(id)
            fetchActivityHistory(id)
            fetchProductivityLogs(id)
            fetchWorkerSessions(id)
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

    fun fetchWorkerSessions(id: String) {
        firestore.collection("activities").document(id).collection("worker_sessions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                _workerSessions.value = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(WorkerSessionLog::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }
    }

    // --- GESTIÓN DE PERSONAL ---

    fun addPersonToActivity(name: String) {
        val id = activityId ?: return
        // Asegúrate de que WorkerDto soporte el nuevo parámetro opcional si lo deseas mapear directo, o manéjalo dinámicamente.
        val newWorker = WorkerDto(name = name, isTimerActive = false)
        firestore.collection("activities").document(id)
            .update("workers", FieldValue.arrayUnion(newWorker))
    }

    fun removePersonFromActivity(worker: WorkerEntity) {
        val id = activityId ?: return
        firestore.collection("activities").document(id)
            .update("workers", FieldValue.arrayRemove(WorkerDto.fromEntity(worker)))
    }

    fun toggleWorkerTimer(
        workerName: String,
        piecesOk: Int = 0,
        piecesNoOk: Int = 0,
        sessionDefects: List<DefectEntry> = emptyList()
    ) {
        val id = activityId ?: return
        val currentAct = _activity.value ?: return
        val worker = currentAct.workers.find { it.name == workerName } ?: return
        val now = Timestamp.now()
        val totalSessionPieces = piecesOk + piecesNoOk

        val updatedWorkers = currentAct.workers.map { w ->
            if (w.name == workerName) {
                if (!w.isTimerActive) {
                    // INICIAR TIEMPO
                    w.copy(isTimerActive = true, startTime = now)
                } else {
                    // PAUSAR TIEMPO
                    val startMs = w.startTime?.toDate()?.time ?: now.toDate().time
                    val diffHours = (now.toDate().time - startMs).toDouble() / 3600000.0

                    // 1. Guardar log individual de la sesión detallada
                    val sessionLog = mapOf(
                        "workerName" to workerName,
                        "timestamp" to now,
                        "durationHours" to diffHours,
                        "piecesChecked" to totalSessionPieces,
                        "piecesOk" to piecesOk,
                        "piecesNoOk" to piecesNoOk,
                        "defectos" to sessionDefects.filter { it.count > 0 },
                        "dia" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    )

                    viewModelScope.launch {
                        // Guardar la sesión del trabajador
                        firestore.collection("activities").document(id)
                            .collection("worker_sessions").add(sessionLog)

                        // 🌟 ¡NUEVO! CREAR EL LOG DE PRODUCTIVIDAD ASOCIADO PARA REPORTE Y CALIDAD
                        val productivityLog = mapOf(
                            "timestamp" to now,
                            "dia" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            "turno" to "Único", // Puedes cambiarlo dinámicamente si manejas turnos
                            "cantidadOk" to piecesOk,
                            "defectos" to sessionDefects.filter { it.count > 0 },
                            "registradoPor" to "Sistema - Operador",
                            "workerName" to workerName // Aquí amarra con el ProductivityLogItem que creamos
                        )

                        // Al añadirlo aquí, se reflejará de inmediato en la pestaña Reportes
                        firestore.collection("activities").document(id)
                            .collection("productivity_logs").add(productivityLog)

                        addHistoryEntry(
                            field = "Producción Operador ($workerName)",
                            oldValue = "Pausa tiempo",
                            newValue = "$totalSessionPieces pzs total ($piecesOk OK, $piecesNoOk No OK) en ${String.format("%.2f", diffHours)} hrs"
                        )
                    }

                    w.copy(
                        isTimerActive = false,
                        startTime = null,
                        accumulatedHours = w.accumulatedHours + diffHours
                    )
                }
            } else w
        }.map { WorkerDto.fromEntity(it) }

        // Si el temporizador estaba activo, significa que estamos pausando e impactando la Pantalla de Calidad Global
        if (worker.isTimerActive) {
            val totalOkGlobal = currentAct.cantidadOk + piecesOk
            val listaDefectosGlobalActualizada = currentAct.defectos.toMutableList()

            // Fusionar los defectos reportados por este operador con el acumulado de la actividad
            sessionDefects.forEach { capture ->
                if (capture.count > 0) {
                    val index = listaDefectosGlobalActualizada.indexOfFirst { it.name.equals(capture.name, ignoreCase = true) }
                    if (index != -1) {
                        val existing = listaDefectosGlobalActualizada[index]
                        listaDefectosGlobalActualizada[index] = existing.copy(count = existing.count + capture.count)
                    } else {
                        listaDefectosGlobalActualizada.add(capture)
                    }
                }
            }

            val totalNoOkGlobal = listaDefectosGlobalActualizada.sumOf { it.count }
            val progress = calculateProgress(totalOkGlobal, totalNoOkGlobal, currentAct.cantidadTotal)
            val nuevoEstado = if (totalOkGlobal + totalNoOkGlobal >= currentAct.cantidadTotal && currentAct.cantidadTotal > 0) "Finalizado" else "En curso"

            val updates = mapOf(
                "workers" to updatedWorkers,
                "cantidadOk" to totalOkGlobal,
                "defectos" to listaDefectosGlobalActualizada,
                "cantidadNoOk" to totalNoOkGlobal,
                "progreso" to progress,
                "estado" to nuevoEstado
            )
            updateFirestoreFields(updates)
        } else {
            firestore.collection("activities").document(id).update("workers", updatedWorkers)
        }
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

        val totalProcesado = totalOkAcumulado + totalNoOkAcumulado
        val nuevoEstado = if (totalProcesado >= current.cantidadTotal && current.cantidadTotal > 0) "Finalizado" else "En curso"

        val log = mapOf(
            "timestamp" to Timestamp.now(),
            "dia" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            "turno" to turno,
            "cantidadOk" to newOkCount,
            "defectos" to captureDefects.filter { it.count > 0 },
            "registradoPor" to currentUserName
        )

        viewModelScope.launch {
            firestore.collection("activities").document(id).collection("productivity_logs").add(log)
            addHistoryEntry("Producción ($turno)", "${current.cantidadOk} OK", "$totalOkAcumulado OK")

            if (current.estado != nuevoEstado) {
                addHistoryEntry("Estado", current.estado, nuevoEstado)
            }

            updateFirestoreFields(mapOf(
                "cantidadOk" to totalOkAcumulado,
                "defectos" to listaDefectosActualizada,
                "cantidadNoOk" to totalNoOkAcumulado,
                "progreso" to progress,
                "estado" to nuevoEstado
            ))
        }
    }

    fun adjustQualityTotals(newOkTotal: Int, defects: List<DefectEntry>) {
        val id = activityId ?: return
        val current = _activity.value ?: return
        val totalNoOk = defects.sumOf { it.count }
        val progress = calculateProgress(newOkTotal, totalNoOk, current.cantidadTotal)

        val nuevoEstado = if (progress >= 100) "Finalizado" else "En curso"

        if (current.cantidadOk != newOkTotal) {
            addHistoryEntry("Ajuste Manual OK", "${current.cantidadOk}", "$newOkTotal")
        }
        if (current.cantidadNoOk != totalNoOk) {
            addHistoryEntry("Ajuste Manual No OK", "${current.cantidadNoOk}", "$totalNoOk")
        }

        if (current.estado != nuevoEstado) {
            addHistoryEntry("Estado", current.estado, nuevoEstado)
        }

        updateFirestoreFields(mapOf(
            "cantidadOk" to newOkTotal,
            "defectos" to defects,
            "cantidadNoOk" to totalNoOk,
            "progreso" to progress,
            "estado" to nuevoEstado
        ))
    }

    fun deleteProductivityLog(log: ProductivityEntity) {
        val id = activityId ?: return
        val current = _activity.value ?: return

        viewModelScope.launch {
            try {
                val nuevaCantidadOk = (current.cantidadOk - log.cantidadOk).coerceAtLeast(0)
                val nuevoProgreso = calculateProgress(nuevaCantidadOk, current.cantidadNoOk, current.cantidadTotal)
                val nuevoEstado = if (nuevoProgreso >= 100) "Finalizado" else "En curso"

                firestore.collection("activities").document(id)
                    .collection("productivity_logs").document(log.id).delete()

                updateFirestoreFields(mapOf(
                    "cantidadOk" to nuevaCantidadOk,
                    "progreso" to nuevoProgreso,
                    "estado" to nuevoEstado
                ))

                if (current.estado != nuevoEstado) {
                    addHistoryEntry("Estado", current.estado, "$nuevoEstado (Log eliminado)")
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar log: ${e.message}"
            }
        }
    }

    // --- GENERAL ---

    fun updateGeneralDetails(cantidadTotal: Int, estHoras: String, estCosto: String, nota: String, cpm: String) {
        val current = _activity.value ?: return

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

    // --- REPORTES Y FILTRADO ---

    fun filterProductivityByRange(daysBack: Int?) {
        val id = activityId ?: return
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        if (daysBack != null) {
            calendar.add(Calendar.DAY_OF_YEAR, -daysBack)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
        } else {
            calendar.set(2020, 0, 1)
        }
        val startDate = calendar.time

        executeProductivityQuery(id, startDate, endDate)
    }

    fun filterProductivityByCustomRange(startMillis: Long, endMillis: Long) {
        val id = activityId ?: return

        val startCal = Calendar.getInstance().apply {
            timeInMillis = startMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = Calendar.getInstance().apply {
            timeInMillis = endMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        executeProductivityQuery(id, startCal.time, endCal.time)
    }

    private fun executeProductivityQuery(id: String, startDate: Date, endDate: Date) {
        currentFilterStartDate = startDate
        currentFilterEndDate = endDate

        firestore.collection("activities").document(id)
            .collection("productivity_logs")
            .whereGreaterThanOrEqualTo("timestamp", Timestamp(startDate))
            .whereLessThanOrEqualTo("timestamp", Timestamp(endDate))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Error en reporte: ${e.message}"
                    return@addSnapshotListener
                }
                _filteredLogs.value = snapshot?.documents?.mapNotNull { doc ->
                    val entity = doc.toObject(ProductivityEntity::class.java)
                    entity?.copy(id = doc.id)
                } ?: emptyList()
            }

        firestore.collection("activities").document(id)
            .collection("worker_sessions")
            .whereGreaterThanOrEqualTo("timestamp", Timestamp(startDate))
            .whereLessThanOrEqualTo("timestamp", Timestamp(endDate))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                _workerSessions.value = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(WorkerSessionLog::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }
    }

    fun generateReportWithCurrentFilters(context: Context) {
        viewModelScope.launch {
            val activity = _activity.value
            if (activity == null) {
                _error.value = "No hay actividad seleccionada"
                return@launch
            }

            android.util.Log.d("REPORT_DEBUG", "Fechas guardadas - start: $currentFilterStartDate, end: $currentFilterEndDate")

            val startTimestamp = currentFilterStartDate?.let { Timestamp(it) }
            val endTimestamp = currentFilterEndDate?.let { Timestamp(it) }

            val config = ReportConfig(
                activityId = activity.id,
                startDate = startTimestamp,
                endDate = endTimestamp,
                includeGeneralInfo = true,
                includeWorkers = true,
                includeDefects = true,
                includeProductivity = true,
                includeHistory = true,
                format = ReportFormat.PDF
            )

            try {
                // ✅ OBTENER WORKER_SESSIONS DE LA ACTIVIDAD (ya están en _workerSessions)
                val workerSessions = _workerSessions.value
                android.util.Log.d("REPORT_DEBUG", "Sesiones encontradas: ${workerSessions.size}")

                // ✅ PASAR LAS SESIONES AL GENERADOR
                val activitySessionsMap = mapOf(activity.id to workerSessions)

                val file = generateReportUseCase(
                    activities = listOf(activity),
                    config = config,
                    activitySessions = activitySessionsMap
                )
                shareReportUseCase(file, config.format, context)
            } catch (e: Exception) {
                _error.value = "Error al generar reporte: ${e.message}"
                android.util.Log.e("REPORT_DEBUG", "Error: ${e.message}", e)
            }
        }
    }
}