package com.quiquecx.simaapp.view.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth // 🚨 NUEVA IMPORTACIÓN
import com.quiquecx.simaapp.domain.entity.CompanyEntity
import com.quiquecx.simaapp.domain.entity.ProjectEntity
import com.quiquecx.simaapp.domain.useCase.GetCompaniesUseCase
import com.quiquecx.simaapp.domain.useCase.GetProjectsUseCase
import com.quiquecx.simaapp.domain.useCase.GetSelectedCompanyUseCase
import com.quiquecx.simaapp.domain.useCase.SaveSelectedCompanyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Clase que encapsula todo el estado de la pantalla principal (Home).
 */
data class HomeUiState(
    // Estado general de la UI
    val isLoading: Boolean = true,
    val errorMessage: String? = null,

    // Datos principales de la pantalla
    val selectedCompanyId: String? = null,         // El ID de la compañía seleccionada.
    val companies: List<CompanyEntity> = emptyList(), // Lista de compañías para la selección inicial.
    val projects: List<ProjectEntity> = emptyList() // Lista de proyectos de la compañía seleccionada.
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    // Dependencias de Casos de Uso
    private val getCompaniesUseCase: GetCompaniesUseCase,
    private val getProjectsUseCase: GetProjectsUseCase,
    private val getSelectedCompanyUseCase: GetSelectedCompanyUseCase,
    private val saveSelectedCompanyUseCase: SaveSelectedCompanyUseCase,

    // 🚨 Dependencia de Autenticación para el Logout
    private val auth: FirebaseAuth
) : ViewModel() {

    // El único StateFlow que expone el estado completo de la UI
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        // Inicia el proceso de carga al crear el ViewModel
        loadInitialData()
    }

    // -------------------------------------------------------------------------
    // 1. GESTIÓN DEL ESTADO DE LA APLICACIÓN
    // -------------------------------------------------------------------------

    /**
     * Determina si debe cargar la lista de compañías (si no hay selección) o los proyectos (si ya hay selección).
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

    // -------------------------------------------------------------------------
    // 2. ACCIÓN: CERRAR SESIÓN (LOGOUT) 🚨 NUEVA FUNCIÓN
    // -------------------------------------------------------------------------

    /**
     * Cierra la sesión del usuario actual de Firebase Authentication.
     * * @param onSignOutSuccess Callback para ejecutar la navegación (ir a Login) tras el cierre exitoso.
     */
    fun signOut(onSignOutSuccess: () -> Unit) {
        viewModelScope.launch {
            // Limpia la sesión activa en Firebase Auth
            auth.signOut()

            // Llama al callback de navegación. Esto será manejado por NavigationWrapper.kt.
            onSignOutSuccess()
        }
    }

    // -------------------------------------------------------------------------
    // 3. LÓGICA DE CARGA DE DATOS
    // -------------------------------------------------------------------------

    /**
     * Carga los proyectos para una compañía específica.
     */
    private fun loadProjects(companyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
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
