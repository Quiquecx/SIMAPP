// archivo: ActivityFormViewModel.kt (VERSIÓN DEFINITIVA CON ESTADO)

package com.quiquecx.simaapp.view.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

// --- ESTADO DE LA UI AMPLIADO ---
data class ActivityUiState(
    val tipo: String = "",
    val proveedorId: String = "",
    val materialId: String = "",
    val responsable: String = "", // NUEVO
    val cantidadTotal: String = "",
    val defecto: String = "Ninguno", // NUEVO
    val estimadoHoras: String = "", // NUEVO
    val estimadoCosto: String = "", // NUEVO
    val fechaInicio: Date = Date(), // NUEVO
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: Boolean = false
)

@HiltViewModel
class ActivityFormViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    // --- MANEJADORES DE ESTADO (Para el Composable) ---
    fun updateTipo(newValue: String) { _uiState.update { it.copy(tipo = newValue) } }
    fun updateProveedorId(newValue: String) { _uiState.update { it.copy(proveedorId = newValue) } }
    fun updateMaterialId(newValue: String) { _uiState.update { it.copy(materialId = newValue) } }
    fun updateResponsable(newValue: String) { _uiState.update { it.copy(responsable = newValue) } }
    fun updateCantidadTotal(newValue: String) { _uiState.update { it.copy(cantidadTotal = newValue.filter { char -> char.isDigit() }) } } // Filtra solo dígitos
    fun updateDefecto(newValue: String) { _uiState.update { it.copy(defecto = newValue) } }
    fun updateEstimadoHoras(newValue: String) { _uiState.update { it.copy(estimadoHoras = newValue.filter { char -> char.isDigit() || char == '.' }) } }
    fun updateEstimadoCosto(newValue: String) { _uiState.update { it.copy(estimadoCosto = newValue.filter { char -> char.isDigit() || char == '.' }) } }
    fun updateFechaInicio(newDate: Date) { _uiState.update { it.copy(fechaInicio = newDate) } }


    fun saveActivity() {
        val state = _uiState.value

        // 🚨 Es crucial que todos los campos requeridos estén llenos aquí
        if (state.tipo.isBlank() || state.cantidadTotal.toIntOrNull() == null || state.materialId.isBlank()) {
            _uiState.update { it.copy(saveError = true) }
            return
        }

        // 1. Mapear estado de UI a ActivityEntity
        val newActivity = ActivityEntity(
            // ... (Mapeo de todos los campos, como en la versión anterior)
            tipo = state.tipo.trim(),
            proveedorId = state.proveedorId.trim(),
            materialId = state.materialId.trim(),
            responsable = state.responsable.trim(),
            cantidadTotal = state.cantidadTotal.toIntOrNull() ?: 0,
            defecto = state.defecto.trim(),
            estimadoHoras = state.estimadoHoras,
            estimadoCosto = state.estimadoCosto,
            fechaInicio = state.fechaInicio,

            // Valores iniciales fijos
            id = "",
            cantidadOk = 0,
            cantidadNoOk = 0,
            horasAcumuladas = 0,
            estado = "En curso",
            progreso = 0
        )

        _uiState.update { it.copy(isSaving = true, saveError = false) }

        // 2. Guardar en Firestore
        viewModelScope.launch {
            firestore.collection("activities")
                .add(newActivity) // Firestore asignará el ID y lo guardará
                .addOnSuccessListener {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                .addOnFailureListener { e ->
                    println("❌ Error al guardar actividad: $e")
                    _uiState.update { it.copy(isSaving = false, saveError = true) }
                }
        }
    }
}