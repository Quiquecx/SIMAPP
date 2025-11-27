package com.quiquecx.simaapp.view.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quiquecx.simaapp.domain.entity.CompanyEntity
import com.quiquecx.simaapp.domain.entity.ProjectEntity
import com.quiquecx.simaapp.domain.useCase.GetCompaniesUseCase
import com.quiquecx.simaapp.domain.useCase.GetProjectsUseCase // ⚠️ IMPORTANTE: Este use case debe existir
import com.quiquecx.simaapp.domain.useCase.GetSelectedCompanyUseCase
import com.quiquecx.simaapp.domain.useCase.SaveSelectedCompanyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Clase que encapsula todo el estado de la pantalla principal.
 * La UI se renderizará en base a los valores de esta clase.
 */
data class HomeUiState(
    // Estado general de la UI
    val isLoading: Boolean = true,
    val errorMessage: String? = null,

    // Datos principales de la pantalla
    val selectedCompanyId: String? = null,         // El ID de la compañía que se cargó desde DataStore.
    val companies: List<CompanyEntity> = emptyList(), // Lista de compañías para la selección inicial.
    val projects: List<ProjectEntity> = emptyList() // Lista de proyectos de la compañía seleccionada.
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCompaniesUseCase: GetCompaniesUseCase,
    private val getProjectsUseCase: GetProjectsUseCase, // ⚠️ Asegúrate de que este se inyecte
    private val getSelectedCompanyUseCase: GetSelectedCompanyUseCase,
    private val saveSelectedCompanyUseCase: SaveSelectedCompanyUseCase
) : ViewModel() {

    // 1. El único StateFlow que expone el estado completo de la UI (Usando el nombre que la UI espera: uiState)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState // ✅ HomeScreen.kt lee esta variable

    // 🛑 Hemos eliminado _companiesState, selectedCompanyId y CompaniesUiState

    init {
        // Inicia el proceso de carga al crear el ViewModel
        loadInitialData()
    }

    /**
     * Determina si debe cargar la lista de compañías (primera vez) o los proyectos (ya hay selección).
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val selectedId = getSelectedCompanyUseCase()

            if (selectedId != null) {
                // Si hay una compañía seleccionada, cargar sus proyectos.
                _uiState.update { it.copy(selectedCompanyId = selectedId) }
                loadProjects(selectedId)
            } else {
                // Si no hay selección, cargar la lista de todas las compañías.
                loadCompaniesForSelection()
            }
        }
    }

    /**
     * Carga los proyectos para una compañía específica.
     */
    private fun loadProjects(companyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // ⚠️ Aquí se usa GetProjectsUseCase, que no estaba en tu código anterior
                val projects = getProjectsUseCase(companyId)

                _uiState.update { currentState ->
                    currentState.copy(
                        projects = projects,
                        isLoading = false,
                        errorMessage = null,
                        companies = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar proyectos: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Carga todas las compañías disponibles para la pantalla de selección inicial.
     */
    private fun loadCompaniesForSelection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val companies = getCompaniesUseCase()
                _uiState.update { it.copy(
                    companies = companies,
                    isLoading = false,
                    selectedCompanyId = null,
                    projects = emptyList()
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error cargando lista de compañías.") }
            }
        }
    }

    /**
     * Guarda el ID de la compañía seleccionada por el usuario en DataStore y recarga los proyectos.
     */
    fun selectCompany(id: String) {
        viewModelScope.launch {
            // Guarda en DataStore
            saveSelectedCompanyUseCase(id)

            // Actualiza el estado y carga los proyectos
            _uiState.update { it.copy(selectedCompanyId = id) }
            loadProjects(id)
        }
    }
}
// 🛑 Eliminamos la sealed class CompaniesUiState