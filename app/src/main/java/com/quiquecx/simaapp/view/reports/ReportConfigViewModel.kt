package com.quiquecx.simaapp.view.reports

import android.content.Context
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.quiquecx.simaapp.domain.entity.CompanyEntity
import com.quiquecx.simaapp.domain.entity.ProjectEntity
import com.quiquecx.simaapp.domain.entity.ReportConfig
import com.quiquecx.simaapp.domain.repository.DashboardRepository
import com.quiquecx.simaapp.domain.useCase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@HiltViewModel
class ReportConfigViewModel @Inject constructor(
    private val getCompaniesUseCase: GetCompaniesUseCase,
    private val getProjectsUseCase: GetProjectsUseCase,
    private val getSelectedCompanyUseCase: GetSelectedCompanyUseCase,
    private val getFilteredActivitiesUseCase: GetFilteredActivitiesForReportUseCase,
    private val generateReportUseCase: GenerateReportUseCase,
    private val shareReportUseCase: ShareReportUseCase,
    private val dashboardRepository: DashboardRepository, // 👈 Nuevo: para obtener actividad específica
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class UiState(
        val config: ReportConfig = ReportConfig(companyId = ""),
        val companies: List<CompanyEntity> = emptyList(),
        val projects: List<ProjectEntity> = emptyList(),
        val selectedCompanyName: String = "",
        val selectedProjectName: String = "Todos los proyectos",
        val companyMenuExpanded: Boolean = false,
        val projectMenuExpanded: Boolean = false,
        val isGenerating: Boolean = false,
        val error: String? = null,
        val showStartDatePicker: Boolean = false,
        val showEndDatePicker: Boolean = false,
        val startDate: Timestamp? = null,
        val endDate: Timestamp? = null,
        val isSpecificActivity: Boolean = false   // 👈 Nuevo flag
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    val startDatePickerState = DatePickerState(locale = java.util.Locale.getDefault())
    val endDatePickerState = DatePickerState(locale = java.util.Locale.getDefault())

    private var specificActivityId: String? = null

    // Inicialización normal (modo general)
    init {
        loadInitialData()
    }

    /**
     * Llama a este método si quieres inicializar el ViewModel en modo "actividad específica".
     * @param activityId ID de la actividad a exportar, o null para modo general.
     */
    fun setActivityId(activityId: String?) {
        android.util.Log.d("ReportConfig", "setActivityId llamado con: $activityId")
        specificActivityId = activityId
        if (activityId != null) {
            _uiState.value = _uiState.value.copy(
                isSpecificActivity = true,
                config = _uiState.value.config.copy(activityId = activityId)
            )
            android.util.Log.d("ReportConfig", "Modo específico activado, ID guardado: $specificActivityId")
        } else {
            _uiState.value = _uiState.value.copy(isSpecificActivity = false)
            if (_uiState.value.companies.isEmpty()) {
                loadInitialData()
            }
        }
    }


    private fun loadInitialData() {
        viewModelScope.launch {
            val companies = getCompaniesUseCase()
            val selectedCompanyId = getSelectedCompanyUseCase()
            val selectedCompany = companies.find { it.id == selectedCompanyId }
            _uiState.value = _uiState.value.copy(
                companies = companies,
                selectedCompanyName = selectedCompany?.name ?: "",
                config = _uiState.value.config.copy(companyId = selectedCompanyId ?: ""),
                isSpecificActivity = false
            )
            selectedCompanyId?.let { loadProjects(it) }
        }
    }

    private suspend fun loadProjects(companyId: String) {
        val projects = getProjectsUseCase(companyId)
        _uiState.value = _uiState.value.copy(projects = projects)
    }

    fun selectCompany(company: CompanyEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                config = _uiState.value.config.copy(companyId = company.id, projectId = null),
                selectedCompanyName = company.name,
                selectedProjectName = "Todos los proyectos"
            )
            loadProjects(company.id)
        }
    }

    fun selectProject(project: ProjectEntity?) {
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(projectId = project?.id),
            selectedProjectName = project?.name ?: "Todos los proyectos"
        )
    }

    fun updateMinProductivity(value: Int) {
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(minProductivity = value)
        )
    }

    fun updateConfig(update: (ReportConfig) -> ReportConfig) {
        _uiState.value = _uiState.value.copy(config = update(_uiState.value.config))
    }

    fun toggleCompanyMenu() {
        _uiState.value = _uiState.value.copy(companyMenuExpanded = !_uiState.value.companyMenuExpanded)
    }

    fun toggleProjectMenu() {
        _uiState.value = _uiState.value.copy(projectMenuExpanded = !_uiState.value.projectMenuExpanded)
    }

    fun showStartDatePicker() {
        _uiState.value = _uiState.value.copy(showStartDatePicker = true)
    }

    fun hideStartDatePicker() {
        _uiState.value = _uiState.value.copy(showStartDatePicker = false)
    }

    fun showEndDatePicker() {
        _uiState.value = _uiState.value.copy(showEndDatePicker = true)
    }

    fun hideEndDatePicker() {
        _uiState.value = _uiState.value.copy(showEndDatePicker = false)
    }

    fun setStartDate(date: Timestamp) {
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(startDate = date),
            startDate = date
        )
    }

    fun setEndDate(date: Timestamp) {
        _uiState.value = _uiState.value.copy(
            config = _uiState.value.config.copy(endDate = date),
            endDate = date
        )
    }

    // ReportConfigViewModel.kt
    fun generateAndShare(activityContext: Context) {  // ← Recibe activityContext
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null)
            try {
                val config = _uiState.value.config
                val activities = if (specificActivityId != null) {
                    // Modo específico: obtener una sola actividad
                    val activity = dashboardRepository.getActivityDetails(specificActivityId!!)
                    if (activity != null) listOf(activity) else emptyList()
                } else {
                    // Modo general: validar empresa y aplicar filtros
                    if (config.companyId.isBlank()) {
                        throw Exception("Debe seleccionar una empresa")
                    }
                    getFilteredActivitiesUseCase(config)
                }

                if (activities.isEmpty()) {
                    throw Exception("No se encontró la actividad con ID: $specificActivityId")
                }

                val file = generateReportUseCase(activities, config)
                shareReportUseCase(file, config.format, activityContext)
                _uiState.value = _uiState.value.copy(isGenerating = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isGenerating = false, error = e.message)
            }
        }
    }
}