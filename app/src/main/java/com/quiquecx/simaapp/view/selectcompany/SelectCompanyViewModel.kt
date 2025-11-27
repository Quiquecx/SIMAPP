package com.quiquecx.simaapp.view.selectcompany

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quiquecx.simaapp.domain.entity.CompanyEntity
import com.quiquecx.simaapp.domain.useCase.GetCompaniesUseCase // 👈 Nuevo Use Case
import com.quiquecx.simaapp.domain.useCase.SaveSelectedCompanyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectCompanyViewModel @Inject constructor(
    private val getCompaniesUseCase: GetCompaniesUseCase, // 👈 Inyectado
    private val saveSelectedCompanyUseCase: SaveSelectedCompanyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectCompanyUiState())
    val uiState: StateFlow<SelectCompanyUiState> = _uiState

    init {
        loadCompanies()
    }

    private fun loadCompanies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // LLAMADA REAL A LA BASE DE DATOS
                val companies = getCompaniesUseCase()

                _uiState.value = _uiState.value.copy(
                    companies = companies,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error al cargar empresas: ${e.message}"
                )
            }
        }
    }

    fun selectCompany(company: CompanyEntity) {
        viewModelScope.launch {
            // Guardar solo el ID de la empresa en DataStore
            saveSelectedCompanyUseCase(company.id)
            _uiState.value = _uiState.value.copy(
                selectionComplete = true,
                selectedCompany = company.id
            )
        }
    }
}

data class SelectCompanyUiState(
    val companies: List<CompanyEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedCompany: String? = null,
    val selectionComplete: Boolean = false
)