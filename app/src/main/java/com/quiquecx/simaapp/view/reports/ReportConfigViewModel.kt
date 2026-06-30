package com.quiquecx.simaapp.view.reports

import android.content.Context
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.quiquecx.simaapp.domain.entity.*
import com.quiquecx.simaapp.domain.repository.DashboardRepository
import com.quiquecx.simaapp.domain.useCase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
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
    private val dashboardRepository: DashboardRepository,
    private val firestore: FirebaseFirestore,
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
        val timeFilterExpanded: Boolean = false,
        val selectedTimeFilter: TimeFilter = TimeFilter.ALL,
        val isGenerating: Boolean = false,
        val error: String? = null,
        val showStartDatePicker: Boolean = false,
        val showEndDatePicker: Boolean = false,
        val startDate: Timestamp? = null,
        val endDate: Timestamp? = null,
        val isSpecificActivity: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    val startDatePickerState = DatePickerState(locale = Locale.getDefault())
    val endDatePickerState = DatePickerState(locale = Locale.getDefault())

    private var specificActivityId: String? = null

    init {
        loadInitialData()
    }

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

    fun selectTimeFilter(filter: TimeFilter) {
        val (startDate, endDate) = when (filter) {
            TimeFilter.TODAY -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                Pair(Timestamp(start.time), Timestamp(end.time))
            }
            TimeFilter.LAST_7_DAYS -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                }
                val end = Calendar.getInstance()
                Pair(Timestamp(start.time), Timestamp(end.time))
            }
            TimeFilter.LAST_30_DAYS -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                    set(Calendar.HOUR_OF_DAY, 0)
                }
                val end = Calendar.getInstance()
                Pair(Timestamp(start.time), Timestamp(end.time))
            }
            TimeFilter.ALL -> {
                Pair(null, null)
            }
            TimeFilter.CUSTOM -> {
                return
            }
        }

        _uiState.value = _uiState.value.copy(
            selectedTimeFilter = filter,
            config = _uiState.value.config.copy(
                timeFilter = filter,
                startDate = startDate,
                endDate = endDate
            ),
            startDate = startDate,
            endDate = endDate,
            timeFilterExpanded = false
        )
    }

    fun toggleTimeFilterMenu() {
        _uiState.value = _uiState.value.copy(timeFilterExpanded = !_uiState.value.timeFilterExpanded)
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

    fun generateAndShare(activityContext: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null)
            try {
                val config = _uiState.value.config
                val activities = if (specificActivityId != null) {
                    val activity = dashboardRepository.getActivityDetails(specificActivityId!!)
                    if (activity != null) listOf(activity) else emptyList()
                } else {
                    if (config.companyId.isBlank()) {
                        throw Exception("Debe seleccionar una empresa")
                    }
                    getFilteredActivitiesUseCase(config)
                }

                if (activities.isEmpty()) {
                    val errorMsg = if (specificActivityId != null) {
                        "No se encontró la actividad con ID: $specificActivityId"
                    } else {
                        "No se encontraron actividades para los filtros seleccionados"
                    }
                    throw Exception(errorMsg)
                }

                // ✅ OBTENER WORKER_SESSIONS (NO productivity_logs)
                val activitySessionsMap = mutableMapOf<String, List<WorkerSessionLog>>()

                activities.forEach { activity ->
                    try {
                        var query = firestore.collection("activities")
                            .document(activity.id)
                            .collection("worker_sessions")
                            .orderBy("timestamp", Query.Direction.ASCENDING)

                        // Aplicar filtros de fecha
                        config.startDate?.let {
                            query = query.whereGreaterThanOrEqualTo("timestamp", it)
                        }
                        config.endDate?.let {
                            query = query.whereLessThanOrEqualTo("timestamp", it)
                        }

                        val snapshot = query.get().await()
                        val sessions = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(WorkerSessionLog::class.java)?.copy(id = doc.id)
                        }
                        android.util.Log.d("ReportConfigVM", "Activity ${activity.id} - Sesiones encontradas: ${sessions.size}")
                        activitySessionsMap[activity.id] = sessions
                    } catch (e: Exception) {
                        android.util.Log.e("ReportConfigVM", "Error obteniendo worker_sessions para ${activity.id}", e)
                        activitySessionsMap[activity.id] = emptyList()
                    }
                }

                // ✅ PASAR WORKER_SESSIONS AL GENERADOR
                val file = generateReportUseCase(
                    activities = activities,
                    config = config,
                    activitySessions = activitySessionsMap
                )

                shareReportUseCase(file, config.format, activityContext)
                _uiState.value = _uiState.value.copy(isGenerating = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = e.message ?: "Error desconocido"
                )
                android.util.Log.e("ReportConfigVM", "Error generando reporte", e)
            }
        }
    }
}