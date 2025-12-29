package com.quiquecx.simaapp.view.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedCompanyId: String? = null,
    val companies: List<CompanyEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCompaniesUseCase: GetCompaniesUseCase,
    private val getProjectsUseCase: GetProjectsUseCase,
    private val getSelectedCompanyUseCase: GetSelectedCompanyUseCase,
    private val saveSelectedCompanyUseCase: SaveSelectedCompanyUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Cargamos la lista de empresas (siempre necesaria para el Drawer)
            val allCompanies = try { getCompaniesUseCase() } catch (e: Exception) { emptyList() }

            // 2. Obtenemos la selección guardada en DataStore
            val selectedId = getSelectedCompanyUseCase()

            _uiState.update { it.copy(
                companies = allCompanies,
                selectedCompanyId = selectedId
            ) }

            // 3. Si hay una empresa seleccionada, cargamos sus proyectos
            if (selectedId != null) {
                loadProjects(selectedId)
            } else {
                // Si no hay selección previa, dejamos de cargar para mostrar estado vacío o invitar a seleccionar
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Función para cambiar de empresa desde el Drawer
     */
    fun changeCompany(company: CompanyEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedCompanyId = company.id) }

            // Guardar nueva selección y cargar proyectos
            saveSelectedCompanyUseCase(company.id)
            loadProjects(company.id)

            // Notificar a la UI (para cerrar el drawer)
            onComplete()
        }
    }

    private fun loadProjects(companyId: String) {
        viewModelScope.launch {
            try {
                val projects = getProjectsUseCase(companyId)
                _uiState.update { it.copy(
                    projects = projects,
                    isLoading = false,
                    errorMessage = null
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "Error al cargar proyectos: ${e.message}"
                ) }
            }
        }
    }

    fun signOut(onSignOutSuccess: () -> Unit) {
        viewModelScope.launch {
            auth.signOut()
            onSignOutSuccess()
        }
    }
}