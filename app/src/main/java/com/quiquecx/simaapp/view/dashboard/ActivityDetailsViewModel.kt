package com.quiquecx.simaapp.view.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.toObject
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

// Etiqueta para el logging profesional
private const val TAG = "ActivityDetailsVM"

/**
 * ViewModel responsable de la lógica de la pantalla de detalles de la actividad.
 *
 * Se encarga de:
 * 1. Recuperar el ID de la actividad de la navegación.
 * 2. Escuchar la actividad en tiempo real desde Firestore (stream).
 * 3. Implementar la lógica para actualizar el progreso y los datos de calidad.
 * 4. Implementar la lógica para actualizar los detalles generales y estimaciones.
 * 5. Implementar la lógica para eliminar la actividad.
 */
@HiltViewModel
class ActivityDetailsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // -------------------------------------------------------------------------
    // ESTADOS DE UI Y DATOS
    // -------------------------------------------------------------------------

    // Estado para indicar si la actividad está cargando inicialmente.
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Estado para manejar y exponer errores a la UI.
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Contenedor de la actividad actual (null si no se ha cargado o no existe).
    private val _activity = MutableStateFlow<ActivityEntity?>(null)
    val activity: StateFlow<ActivityEntity?> = _activity.asStateFlow()

    // -------------------------------------------------------------------------
    // INICIALIZACIÓN Y MANEJO DE ARGUMENTOS
    // -------------------------------------------------------------------------

    // 1. Intenta leer el activityId de SavedStateHandle.
    private val activityIdRaw: String? = savedStateHandle.get<String>("activityId")

    // 2. Decodifica el ID para manejar URLs o argumentos complejos (aunque el ID de Firestore es simple).
    private val activityId: String? = activityIdRaw?.let {
        try {
            URLDecoder.decode(it, "utf-8")
        } catch (ex: Exception) {
            Log.w(TAG, "No se pudo decodificar activityId='$it': ${ex.message}")
            it
        }
    }

    init {
        if (activityId.isNullOrBlank()) {
            // Manejo de error si el argumento de navegación está ausente.
            Log.e(TAG, "activityId faltante en SavedStateHandle! raw=$activityIdRaw")
            _error.value = "activityId missing"
            _isLoading.value = false
        } else {
            Log.d(TAG, "ActivityDetailsViewModel inicializado con id=$activityId (raw=$activityIdRaw)")
            // Inicia la escucha en tiempo real de los detalles.
            fetchActivityDetailsStream(activityId)
        }
    }

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
                        if (activityObj != null) {
                            // Asignamos el id del documento para tenerlo en el modelo.
                            _activity.value = activityObj.copy(id = snapshot.id)
                            _error.value = null
                        } else {
                            // Si toObject falla, reportamos un error interno.
                            Log.w(TAG, "toObject devolvió null para id=$id")
                            _activity.value = null
                            _error.value = "Deserialización fallida"
                        }
                    } catch (ex: Exception) {
                        // Captura errores de mapeo o formato de datos.
                        Log.e(TAG, "Error mapeando documento id=$id: ${ex.message}")
                        _activity.value = null
                        _error.value = "Error mapeando datos"
                    }
                } else {
                    // El documento no existe (fue eliminado o nunca existió).
                    Log.d(TAG, "Documento no existe para id=$id (snapshot=null o !exists)")
                    _activity.value = null
                    _error.value = "No encontrado"
                }
                _isLoading.value = false
            }
    }

    // -------------------------------------------------------------------------
    // FUNCIONES DE EDICIÓN (CRUD - Update)
    // -------------------------------------------------------------------------

    /**
     * Actualiza los datos de control de ejecución y calidad (piezas OK/NO OK y horas).
     * También recalcula el progreso y el estado basados en la cantidad total requerida.
     */
    fun updateActivityData(okCount: Int, noOkCount: Int, hours: Int) {
        val id = activityId ?: run {
            Log.e(TAG, "Intento de updateActivityData sin activityId")
            return
        }

        viewModelScope.launch {
            // Obtenemos la cantidad total para calcular el progreso.
            val totalRequired = _activity.value?.cantidadTotal ?: 1
            val totalCompleted = okCount + noOkCount

            // Calculamos el nuevo progreso y lo restringimos entre 0 y 100.
            val newProgress = if (totalRequired > 0) {
                ((totalCompleted.toFloat() / totalRequired.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else 0

            // Determinamos el nuevo estado.
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
                .addOnSuccessListener { Log.d(TAG, "Datos de calidad actualizados id=$id") }
                .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar datos de calidad id=$id: ${e.message}") }
        }
    }

    /**
     * Actualiza los detalles generales y las estimaciones de la actividad.
     * (Cantidad total, horas y costo estimado, y el defecto).
     */
    fun updateGeneralDetails(
        cantidadTotal: Int,
        estimadoHoras: String,
        estimadoCosto: String,
        defecto: String
    ) {
        val id = activityId ?: run {
            Log.e(TAG, "Intento de updateGeneralDetails sin activityId")
            return
        }

        viewModelScope.launch {
            val updates = mapOf(
                "cantidadTotal" to cantidadTotal,
                "estimadoHoras" to estimadoHoras,
                "estimadoCosto" to estimadoCosto,
                "defecto" to defecto
            )

            firestore.collection("activities").document(id)
                .update(updates)
                .addOnSuccessListener { Log.d(TAG, "Detalles generales y estimaciones actualizados id=$id.") }
                .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar detalles generales id=$id: ${e.message}") }
        }
    }

    /**
     * Actualiza el progreso de la actividad mediante el slider.
     * @param newProgress El nuevo porcentaje de progreso.
     */
    fun updateProgress(newProgress: Int) {
        val id = activityId ?: run {
            Log.e(TAG, "Intento de updateProgress sin activityId")
            return
        }

        viewModelScope.launch {
            val status = if (newProgress == 100) "Finalizado" else "En curso"

            firestore.collection("activities").document(id)
                // Se usa varargs para actualizar múltiples campos de forma concisa.
                .update("progreso", newProgress, "estado", status)
                .addOnSuccessListener { Log.d(TAG, "Progreso actualizado id=$id") }
                .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar progreso id=$id: ${e.message}") }
        }
    }

    /**
     * Elimina el documento de la actividad de Firestore.
     * @param onSuccess Callback que se ejecuta tras la eliminación exitosa (usado para la navegación 'onBack').
     */
    fun deleteActivity(onSuccess: () -> Unit) {
        val id = activityId ?: run {
            Log.e(TAG, "Intento de deleteActivity sin activityId")
            return
        }

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