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

private const val TAG = "ActivityDetailsVM"

@HiltViewModel
class ActivityDetailsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Exponer flags para UI
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Estado de la actividad
    private val _activity = MutableStateFlow<ActivityEntity?>(null)
    val activity: StateFlow<ActivityEntity?> = _activity.asStateFlow()

    // Intentamos leer el activityId de SavedStateHandle y decodificarlo
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
        if (activityId.isNullOrBlank()) {
            Log.e(TAG, "activityId faltante en SavedStateHandle! raw=$activityIdRaw")
            _error.value = "activityId missing"
            _isLoading.value = false
        } else {
            Log.d(TAG, "ActivityDetailsViewModel inicializado con id=$activityId (raw=$activityIdRaw)")
            fetchActivityDetailsStream(activityId)
        }
    }

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
                            // Asignamos el id del documento (snapshot.id)
                            _activity.value = activityObj.copy(id = snapshot.id)
                            _error.value = null
                        } else {
                            Log.w(TAG, "toObject devolvió null para id=$id")
                            _activity.value = null
                            _error.value = "Deserialización fallida"
                        }
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error mapeando documento id=$id: ${ex.message}")
                        _activity.value = null
                        _error.value = "Error mapeando datos"
                    }
                } else {
                    Log.d(TAG, "Documento no existe para id=$id (snapshot=null o !exists)")
                    _activity.value = null
                    _error.value = "No encontrado"
                }
                _isLoading.value = false
            }
    }

    fun updateActivityData(okCount: Int, noOkCount: Int, hours: Int) {
        val id = activityId ?: run {
            Log.e(TAG, "Intento de updateActivityData sin activityId")
            return
        }

        viewModelScope.launch {
            val totalRequired = _activity.value?.cantidadTotal ?: 1
            val totalCompleted = okCount + noOkCount
            val newProgress = if (totalRequired > 0) {
                ((totalCompleted.toFloat() / totalRequired.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else 0
            val newStatus = if (newProgress == 100) "Finalizado" else "En curso"

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

    fun updateProgress(newProgress: Int) {
        val id = activityId ?: run {
            Log.e(TAG, "Intento de updateProgress sin activityId")
            return
        }

        viewModelScope.launch {
            val status = if (newProgress == 100) "Finalizado" else "En curso"

            firestore.collection("activities").document(id)
                .update("progreso", newProgress, "estado", status)
                .addOnSuccessListener { Log.d(TAG, "Progreso actualizado id=$id") }
                .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar progreso id=$id: ${e.message}") }
        }
    }

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