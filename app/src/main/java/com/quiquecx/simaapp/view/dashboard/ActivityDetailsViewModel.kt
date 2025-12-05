package com.quiquecx.simaapp.view.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.HistoryEntry // Asegúrate de que esta clase exista
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

// Etiqueta para el logging profesional
private const val TAG = "ActivityDetailsVM"

/**
 * ViewModel responsable de la lógica de la pantalla de detalles y auditoría de la actividad.
 *
 * Incluye funcionalidad para:
 * 1. Escuchar la actividad y su historial en tiempo real.
 * 2. Registrar cambios en una subcolección de auditoría.
 * 3. Implementar la lógica de actualización de datos de la actividad.
 */
@HiltViewModel
class ActivityDetailsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth, // Inyección para auditoría
    @ApplicationContext private val context: Context, // Inyección para futuras utilidades (e.g., PDF)
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // -------------------------------------------------------------------------
    // ESTADOS DE UI Y DATOS DE ACTIVIDAD
    // -------------------------------------------------------------------------

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _activity = MutableStateFlow<ActivityEntity?>(null)
    val activity: StateFlow<ActivityEntity?> = _activity.asStateFlow()

    // -------------------------------------------------------------------------
    // ESTADOS DE AUDITORÍA (HISTORIAL DE CAMBIOS)
    // -------------------------------------------------------------------------

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    // -------------------------------------------------------------------------
    // VARIABLES DE AYUDA Y MANEJO DE ARGUMENTOS
    // -------------------------------------------------------------------------

    // Propiedades de ayuda para la auditoría (obtener el usuario actual)
    private val currentUser get() = auth.currentUser
    private val currentUserId get() = currentUser?.uid ?: "unknown"
    private val currentUserName get() = currentUser?.email ?: "Desconocido"

    private val activityIdRaw: String? = savedStateHandle.get<String>("activityId")
    private val activityId: String? = activityIdRaw?.let {
        try {
            URLDecoder.decode(it, "utf-8")
        } catch (ex: Exception) {
            Log.w(TAG, "No se pudo decodificar activityId='$it': ${ex.message}")
            it
        }
    }

    init {
        val id = activityId
        if (id.isNullOrBlank()) {
            Log.e(TAG, "activityId faltante en SavedStateHandle! raw=$activityIdRaw")
            _error.value = "activityId missing"
            _isLoading.value = false
        } else {
            Log.d(TAG, "ActivityDetailsViewModel inicializado con id=$id")
            fetchActivityDetailsStream(id)
            fetchActivityHistory(id) // Inicia la escucha del historial
        }
    }

    // -------------------------------------------------------------------------
    // LÓGICA DE ESCUCHA (REALTIME)
    // -------------------------------------------------------------------------

    /**
     * Establece un listener de Firestore para actualizar el estado de la actividad en tiempo real.
     * @param id El ID del documento de la actividad.
     */
    private fun fetchActivityDetailsStream(id: String) {
        _isLoading.value = true
        firestore.collection("activities").document(id)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    val code = (e as? FirebaseFirestoreException)?.code
                    Log.e(TAG, "Firestore listener error for id=$id code=$code msg=${e.message}")
                    _error.value = "Firestore error: ${code ?: e.message}"
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val activityObj = snapshot.toObject(ActivityEntity::class.java)
                        _activity.value = activityObj?.copy(id = snapshot.id)
                        _error.value = if (activityObj == null) "Deserialización fallida" else null
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error mapeando documento id=$id: ${ex.message}")
                        _error.value = "Error mapeando datos"
                    }
                } else {
                    Log.d(TAG, "Documento no existe para id=$id")
                    _activity.value = null
                    _error.value = "No encontrado"
                }
                _isLoading.value = false
            }
    }

    /**
     * Obtiene y escucha en tiempo real el historial de cambios de la actividad.
     * @param activityId El ID del documento de la actividad.
     */
    private fun fetchActivityHistory(activityId: String) {
        firestore.collection("activities")
            .document(activityId)
            .collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Más reciente primero
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Error al escuchar historial: $activityId", e)
                    return@addSnapshotListener
                }

                val historyList = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(HistoryEntry::class.java)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error mapeando HistoryEntry: ${ex.message}")
                        null
                    }
                } ?: emptyList()

                _history.value = historyList
            }
    }

    // -------------------------------------------------------------------------
    // AUDITORÍA (REGISTRO DE CAMBIOS)
    // -------------------------------------------------------------------------

    /**
     * Registra un cambio en un campo específico de la actividad en la subcolección 'history'.
     * @param activityId ID de la actividad a auditar.
     * @param field Nombre del campo modificado (ej: "progreso", "cantidadOk").
     * @param oldValue Valor antes del cambio.
     * @param newValue Nuevo valor establecido.
     */
    private fun logActivityChange(
        activityId: String,
        field: String,
        oldValue: Any?,
        newValue: Any?
    ) {
        val historyRef = firestore
            .collection("activities")
            .document(activityId)
            .collection("history")

        val logEntry = HistoryEntry(
            timestamp = Timestamp.now(),
            userId = currentUserId,
            userName = currentUserName,
            field = field,
            oldValue = oldValue,
            newValue = newValue
        )

        viewModelScope.launch {
            historyRef.add(logEntry)
                .addOnSuccessListener { Log.d("Auditoría", "Registro de cambio exitoso para $field.") }
                .addOnFailureListener { e -> Log.e("Auditoría", "Error al registrar cambio: ${e.message}") }
        }
    }

    // -------------------------------------------------------------------------
    // FUNCIONES DE EDICIÓN (CRUD - Update con Auditoría)
    // -------------------------------------------------------------------------

    /**
     * Actualiza los datos de control de ejecución y calidad (piezas OK/NO OK y horas).
     * También recalcula el progreso y el estado.
     */
    fun updateActivityData(okCount: Int, noOkCount: Int, hours: Int) {
        val id = activityId ?: run {
            Log.e(TAG, "Intento de updateActivityData sin activityId"); return
        }
        val currentActivity = _activity.value ?: return

        viewModelScope.launch {
            val totalRequired = currentActivity.cantidadTotal
            val totalCompleted = okCount + noOkCount

            // Lógica de cálculo de progreso y estado
            val newProgress = if (totalRequired > 0) {
                ((totalCompleted.toFloat() / totalRequired.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else 0
            val newStatus = if (newProgress >= 100) "Finalizado" else "En curso"

            val updates = mapOf(
                "cantidadOk" to okCount,
                "cantidadNoOk" to noOkCount,
                "horasAcumuladas" to hours,
                "progreso" to newProgress,
                "estado" to newStatus
            )

            firestore.collection("activities").document(id)
                .update(updates)
                .addOnSuccessListener {
                    Log.d(TAG, "Datos de calidad actualizados id=$id")

                    // 🚨 REGISTRO DE AUDITORÍA
                    if (currentActivity.cantidadOk != okCount) logActivityChange(id, "cantidadOk", currentActivity.cantidadOk, okCount)
                    if (currentActivity.cantidadNoOk != noOkCount) logActivityChange(id, "cantidadNoOk", currentActivity.cantidadNoOk, noOkCount)
                    if (currentActivity.horasAcumuladas != hours) logActivityChange(id, "horasAcumuladas", currentActivity.horasAcumuladas, hours)
                    if (currentActivity.progreso != newProgress) logActivityChange(id, "progreso", currentActivity.progreso, newProgress)
                    if (currentActivity.estado != newStatus) logActivityChange(id, "estado", currentActivity.estado, newStatus)
                }
                .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar datos de calidad id=$id: ${e.message}") }
        }
    }

    /**
     * Actualiza los detalles generales y las estimaciones de la actividad.
     */
    fun updateGeneralDetails(
        cantidadTotal: Int,
        estimadoHoras: String,
        estimadoCosto: String,
        defecto: String
    ) {
        val id = activityId ?: run { Log.e(TAG, "Intento de updateGeneralDetails sin activityId"); return }
        val currentActivity = _activity.value ?: return

        viewModelScope.launch {
            val updates = mapOf(
                "cantidadTotal" to cantidadTotal,
                "estimadoHoras" to estimadoHoras,
                "estimadoCosto" to estimadoCosto,
                "defecto" to defecto
            )

            firestore.collection("activities").document(id)
                .update(updates)
                .addOnSuccessListener {
                    Log.d(TAG, "Detalles generales y estimaciones actualizados id=$id.")

                    // 🚨 REGISTRO DE AUDITORÍA
                    if (currentActivity.cantidadTotal != cantidadTotal) logActivityChange(id, "cantidadTotal", currentActivity.cantidadTotal, cantidadTotal)
                    if (currentActivity.estimadoHoras != estimadoHoras) logActivityChange(id, "estimadoHoras", currentActivity.estimadoHoras, estimadoHoras)
                    if (currentActivity.estimadoCosto != estimadoCosto) logActivityChange(id, "estimadoCosto", currentActivity.estimadoCosto, estimadoCosto)
                    if (currentActivity.defecto != defecto) logActivityChange(id, "defecto", currentActivity.defecto, defecto)
                }
                .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar detalles generales id=$id: ${e.message}") }
        }
    }

    /**
     * Actualiza el progreso de la actividad mediante el slider/input directo.
     */
    fun updateProgress(newProgress: Int) {
        val id = activityId ?: run { Log.e(TAG, "Intento de updateProgress sin activityId"); return }
        val currentActivity = _activity.value ?: return

        if (currentActivity.progreso == newProgress) return // Evitar actualizar si no hay cambio

        viewModelScope.launch {
            val status = if (newProgress == 100) "Finalizado" else "En curso"

            firestore.collection("activities").document(id)
                .update("progreso", newProgress, "estado", status)
                .addOnSuccessListener {
                    Log.d(TAG, "Progreso actualizado id=$id")

                    // 🚨 REGISTRO DE AUDITORÍA
                    logActivityChange(id, "progreso", currentActivity.progreso, newProgress)
                    if (currentActivity.estado != status) logActivityChange(id, "estado", currentActivity.estado, status)
                }
                .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar progreso id=$id: ${e.message}") }
        }
    }

    /**
     * Elimina el documento de la actividad de Firestore.
     */
    fun deleteActivity(onSuccess: () -> Unit) {
        val id = activityId ?: run { Log.e(TAG, "Intento de deleteActivity sin activityId"); return }

        viewModelScope.launch {
            firestore.collection("activities").document(id)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Actividad eliminada id=$id")
                    onSuccess()
                }
                .addOnFailureListener { e -> Log.e(TAG, "Error al eliminar actividad id=$id: ${e.message}") }
        }
    }
}