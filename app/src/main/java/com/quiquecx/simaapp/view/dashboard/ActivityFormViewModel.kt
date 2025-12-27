package com.quiquecx.simaapp.view.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.DefectEntry
import com.quiquecx.simaapp.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

// --- ESTADO DE LA UI ACTUALIZADO ---
data class ActivityUiState(
    val tipo: String = "",
    val proveedorId: String = "",
    val materialId: String = "",
    val responsable: String = "Equipo Sima",
    val cantidadTotal: String = "",

    // [ ] Añadir campo de CPM
    val cpmId: String = "",

    // [ ] Añadir campo de personas (como String separado por comas para el input)
    val personasInput: String = "",

    // [ ] Manejar 2 defectos diferentes
    val nombreDefecto1: String = "",
    val cantidadDefecto1: String = "0",
    val nombreDefecto2: String = "",
    val cantidadDefecto2: String = "0",

    val defectoNota: String = "",
    val estimadoHoras: String = "0",
    val estimadoCosto: String = "0",
    val fechaInicio: Date = Date(),

    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: Boolean = false
)

@HiltViewModel
class ActivityFormViewModel @Inject constructor(
    private val repository: DashboardRepository // Usamos el repositorio, no firestore directamente
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    // --- MANEJADORES DE ESTADO ---
    fun updateTipo(newValue: String) { _uiState.update { it.copy(tipo = newValue) } }
    fun updateProveedorId(newValue: String) { _uiState.update { it.copy(proveedorId = newValue) } }
    fun updateMaterialId(newValue: String) { _uiState.update { it.copy(materialId = newValue) } }
    fun updateResponsable(newValue: String) { _uiState.update { it.copy(responsable = newValue) } }
    fun updateCpmId(newValue: String) { _uiState.update { it.copy(cpmId = newValue) } }
    fun updatePersonasInput(newValue: String) { _uiState.update { it.copy(personasInput = newValue) } }

    fun updateCantidadTotal(newValue: String) {
        _uiState.update { it.copy(cantidadTotal = newValue.filter { c -> c.isDigit() }) }
    }

    // Manejadores para los 2 defectos
    fun updateDefecto1(nombre: String, cantidad: String) {
        _uiState.update { it.copy(nombreDefecto1 = nombre, cantidadDefecto1 = cantidad.filter { c -> c.isDigit() }) }
    }
    fun updateDefecto2(nombre: String, cantidad: String) {
        _uiState.update { it.copy(nombreDefecto2 = nombre, cantidadDefecto2 = cantidad.filter { c -> c.isDigit() }) }
    }

    fun updateDefectoNota(newValue: String) { _uiState.update { it.copy(defectoNota = newValue) } }
    fun updateEstimadoHoras(newValue: String) { _uiState.update { it.copy(estimadoHoras = newValue) } }
    fun updateEstimadoCosto(newValue: String) { _uiState.update { it.copy(estimadoCosto = newValue) } }

    fun updateFechaInicio(newDate: Date) {
        _uiState.update { it.copy(fechaInicio = newDate) }
    }

    fun saveActivity() {
        val state = _uiState.value

        if (state.tipo.isBlank() || state.cantidadTotal.toIntOrNull() == null) {
            _uiState.update { it.copy(saveError = true) }
            return
        }

        // 1. Procesar Personas (String a Lista)
        val listaPersonas = state.personasInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        // 2. Procesar Defectos (Crear lista de DefectEntry)
        val listaDefectos = mutableListOf<DefectEntry>()
        if (state.nombreDefecto1.isNotBlank()) {
            listaDefectos.add(DefectEntry(state.nombreDefecto1, state.cantidadDefecto1.toIntOrNull() ?: 0))
        }
        if (state.nombreDefecto2.isNotBlank()) {
            listaDefectos.add(DefectEntry(state.nombreDefecto2, state.cantidadDefecto2.toIntOrNull() ?: 0))
        }

        // 3. Calcular cantidadNoOk automáticamente
        val totalNoOk = listaDefectos.sumOf { it.count }

        val newActivity = ActivityEntity(
            id = "", // El Repositorio/Firestore generará el ID
            tipo = state.tipo.trim(),
            proveedorId = state.proveedorId.trim(),
            materialId = state.materialId.trim(),
            responsable = state.responsable.trim(),
            cpmId = state.cpmId.trim(),
            people = listaPersonas,
            defectos = listaDefectos,
            defectoNota = state.defectoNota,
            cantidadTotal = state.cantidadTotal.toIntOrNull() ?: 0,
            cantidadOk = 0,
            cantidadNoOk = totalNoOk,
            fechaInicio = state.fechaInicio,
            estimadoHoras = state.estimadoHoras,
            estimadoCosto = state.estimadoCosto,
            estado = "En curso",
            progreso = 0
        )

        _uiState.update { it.copy(isSaving = true, saveError = false) }

        viewModelScope.launch {
            val result = repository.saveActivity(newActivity)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } else {
                _uiState.update { it.copy(isSaving = false, saveError = true) }
            }
        }
    }
}