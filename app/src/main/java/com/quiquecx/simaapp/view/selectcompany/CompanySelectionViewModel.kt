package com.quiquecx.simaapp.view.selectcompany

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quiquecx.simaapp.domain.useCase.SaveSelectedCompanyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompanySelectionViewModel @Inject constructor(
    private val saveSelectedCompanyUseCase: SaveSelectedCompanyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanySelectionUiState())
    val uiState: StateFlow<CompanySelectionUiState> = _uiState

    fun setCompanies(companies: List<String>) {
        _uiState.value = _uiState.value.copy(companies = companies)
        // auto-select if only 1
        if (companies.size == 1) {
            _uiState.value = _uiState.value.copy(selectedCompany = companies.first())
        }
    }

    fun onCompanySelected(companyId: String) {
        _uiState.value = _uiState.value.copy(selectedCompany = companyId)
    }

    fun confirmSelection(onDone: () -> Unit) {
        val selected = _uiState.value.selectedCompany ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                saveSelectedCompanyUseCase(selected)
                _uiState.value = _uiState.value.copy(isLoading = false)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }
}

data class CompanySelectionUiState(
    val companies: List<String> = emptyList(),
    val selectedCompany: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)