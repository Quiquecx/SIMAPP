package com.quiquecx.simaapp.view.reports

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.Timestamp
import com.quiquecx.simaapp.domain.entity.ReportFormat
import com.quiquecx.simaapp.domain.entity.TimeFilter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportConfigScreen(
    activityId: String? = null,
    activityContext: Context,
    onBack: () -> Unit,
    viewModel: ReportConfigViewModel = hiltViewModel()
) {
    LaunchedEffect(activityId) {
        viewModel.setActivityId(activityId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val config = uiState.config
    val isSpecificMode = activityId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSpecificMode) "Exportar esta actividad" else "Generar reporte personalizado"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.generateAndShare(activityContext) },
                        enabled = !uiState.isGenerating
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Print, contentDescription = "Generar reporte")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isSpecificMode) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Text(
                            text = "Generando reporte para la actividad actual. Selecciona los datos que deseas incluir y el formato.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // ✅ Filtro de tiempo
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Filtro de Tiempo", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))

                            ExposedDropdownMenuBox(
                                expanded = uiState.timeFilterExpanded,
                                onExpandedChange = { viewModel.toggleTimeFilterMenu() }
                            ) {
                                OutlinedTextField(
                                    value = when (uiState.selectedTimeFilter) {
                                        TimeFilter.TODAY -> "Hoy"
                                        TimeFilter.LAST_7_DAYS -> "Últimos 7 días"
                                        TimeFilter.LAST_30_DAYS -> "Últimos 30 días"
                                        TimeFilter.ALL -> "Todo"
                                        TimeFilter.CUSTOM -> "Rango personalizado"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Período") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.timeFilterExpanded) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = uiState.timeFilterExpanded,
                                    onDismissRequest = { viewModel.toggleTimeFilterMenu() }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("🔷 Hoy") },
                                        onClick = { viewModel.selectTimeFilter(TimeFilter.TODAY) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("📅 Últimos 7 días") },
                                        onClick = { viewModel.selectTimeFilter(TimeFilter.LAST_7_DAYS) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("📆 Últimos 30 días") },
                                        onClick = { viewModel.selectTimeFilter(TimeFilter.LAST_30_DAYS) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("⏳ Todo") },
                                        onClick = { viewModel.selectTimeFilter(TimeFilter.ALL) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("📝 Rango personalizado") },
                                        onClick = { viewModel.selectTimeFilter(TimeFilter.CUSTOM) }
                                    )
                                }
                            }

                            // Mostrar fechas seleccionadas
                            if (uiState.startDate != null && uiState.endDate != null) {
                                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Del ${dateFormat.format(uiState.startDate!!.toDate())} al ${dateFormat.format(uiState.endDate!!.toDate())}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                // Filtros de empresa y proyecto
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Filtros Adicionales", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))

                            ExposedDropdownMenuBox(
                                expanded = uiState.companyMenuExpanded,
                                onExpandedChange = { viewModel.toggleCompanyMenu() }
                            ) {
                                OutlinedTextField(
                                    value = uiState.selectedCompanyName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Empresa") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.companyMenuExpanded) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = uiState.companyMenuExpanded,
                                    onDismissRequest = { viewModel.toggleCompanyMenu() }
                                ) {
                                    uiState.companies.forEach { company ->
                                        DropdownMenuItem(
                                            text = { Text(company.name) },
                                            onClick = {
                                                viewModel.selectCompany(company)
                                                viewModel.toggleCompanyMenu()
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            ExposedDropdownMenuBox(
                                expanded = uiState.projectMenuExpanded,
                                onExpandedChange = { viewModel.toggleProjectMenu() }
                            ) {
                                OutlinedTextField(
                                    value = uiState.selectedProjectName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Proyecto (opcional)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.projectMenuExpanded) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = uiState.projectMenuExpanded,
                                    onDismissRequest = { viewModel.toggleProjectMenu() }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Todos los proyectos") },
                                        onClick = {
                                            viewModel.selectProject(null)
                                            viewModel.toggleProjectMenu()
                                        }
                                    )
                                    uiState.projects.forEach { project ->
                                        DropdownMenuItem(
                                            text = { Text(project.name) },
                                            onClick = {
                                                viewModel.selectProject(project)
                                                viewModel.toggleProjectMenu()
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Productividad mínima: ${config.minProductivity}%")
                            Slider(
                                value = config.minProductivity.toFloat(),
                                onValueChange = { viewModel.updateMinProductivity(it.toInt()) },
                                valueRange = 0f..100f,
                                steps = 10
                            )
                        }
                    }
                }
            }

            // Checklist con "Historial"
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Incluir en el reporte", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        ReportCheckboxItem(
                            label = "Datos generales",
                            checked = config.includeGeneralInfo,
                            onCheckedChange = { viewModel.updateConfig { it.copy(includeGeneralInfo = !it.includeGeneralInfo) } }
                        )

                        ReportCheckboxItem(
                            label = "Personal y tiempos",
                            checked = config.includeWorkers,
                            onCheckedChange = { viewModel.updateConfig { it.copy(includeWorkers = !it.includeWorkers) } }
                        )

                        ReportCheckboxItem(
                            label = "Defectos",
                            checked = config.includeDefects,
                            onCheckedChange = { viewModel.updateConfig { it.copy(includeDefects = !it.includeDefects) } }
                        )

                        ReportCheckboxItem(
                            label = "Productividad",
                            checked = config.includeProductivity,
                            onCheckedChange = { viewModel.updateConfig { it.copy(includeProductivity = !it.includeProductivity) } }
                        )

                        ReportCheckboxItem(
                            label = "Costos",
                            checked = config.includeCosts,
                            onCheckedChange = { viewModel.updateConfig { it.copy(includeCosts = !it.includeCosts) } }
                        )

                        ReportCheckboxItem(
                            label = "Historial detallado (horas por día/persona)",
                            checked = config.includeHistory,
                            onCheckedChange = { viewModel.updateConfig { it.copy(includeHistory = !it.includeHistory) } }
                        )
                    }
                }
            }

            // Formato de salida
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Formato de salida", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        ReportFormatOption(
                            label = "HTML (vista web)",
                            selected = config.format == ReportFormat.PDF,
                            onClick = { viewModel.updateConfig { it.copy(format = ReportFormat.PDF) } }
                        )

                        ReportFormatOption(
                            label = "CSV",
                            selected = config.format == ReportFormat.CSV,
                            onClick = { viewModel.updateConfig { it.copy(format = ReportFormat.CSV) } }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📌 HTML se abre en navegador y puedes guardarlo como PDF. CSV es ideal para Excel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Mensaje de error
            if (uiState.error != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            text = "❌ ${uiState.error}",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

        }
    }

    // ✅ DatePicker CORRECTO
    if (!isSpecificMode && uiState.showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.hideStartDatePicker() },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.startDatePickerState.selectedDateMillis?.let {
                        viewModel.setStartDate(Timestamp(Date(it)))
                    }
                    viewModel.hideStartDatePicker()
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideStartDatePicker() }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = viewModel.startDatePickerState)
        }
    }

    if (!isSpecificMode && uiState.showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.hideEndDatePicker() },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.endDatePickerState.selectedDateMillis?.let {
                        viewModel.setEndDate(Timestamp(Date(it)))
                    }
                    viewModel.hideEndDatePicker()
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideEndDatePicker() }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = viewModel.endDatePickerState)
        }
    }
}

// ✅ Componentes auxiliares
@Composable
fun ReportCheckboxItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun ReportFormatOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}
