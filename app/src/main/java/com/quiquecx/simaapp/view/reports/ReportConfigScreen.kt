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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportConfigScreen(
    activityId: String? = null,
    activityContext: Context,          // ← Contexto de actividad (pasado desde el padre)
    onBack: () -> Unit,
    viewModel: ReportConfigViewModel = hiltViewModel()
) {
    // Inicializar el ViewModel con el activityId (modo específico o normal)
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
                        onClick = { viewModel.generateAndShare(activityContext) }, // ← Pasar contexto
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
            // Si es modo específico, mostrar un mensaje informativo y ocultar filtros
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
                // Filtros completos (solo en modo normal)
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Filtros", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Empresa
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

                            // Proyecto
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

                            // Rango de fechas
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = uiState.startDate?.let { dateFormat.format(it.toDate()) } ?: "",
                                    onValueChange = {},
                                    label = { Text("Fecha inicio") },
                                    readOnly = true,
                                    modifier = Modifier.weight(1f),
                                    trailingIcon = {
                                        IconButton(onClick = { viewModel.showStartDatePicker() }) {
                                            Icon(Icons.Default.DateRange, null)
                                        }
                                    }
                                )
                                OutlinedTextField(
                                    value = uiState.endDate?.let { dateFormat.format(it.toDate()) } ?: "",
                                    onValueChange = {},
                                    label = { Text("Fecha fin") },
                                    readOnly = true,
                                    modifier = Modifier.weight(1f),
                                    trailingIcon = {
                                        IconButton(onClick = { viewModel.showEndDatePicker() }) {
                                            Icon(Icons.Default.DateRange, null)
                                        }
                                    }
                                )
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

            // Checklist (siempre visible)
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Incluir en el reporte", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = config.includeGeneralInfo,
                                onCheckedChange = { viewModel.updateConfig { it.copy(includeGeneralInfo = !it.includeGeneralInfo) } }
                            )
                            Text("Datos generales")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = config.includeWorkers,
                                onCheckedChange = { viewModel.updateConfig { it.copy(includeWorkers = !it.includeWorkers) } }
                            )
                            Text("Personal y tiempos")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = config.includeDefects,
                                onCheckedChange = { viewModel.updateConfig { it.copy(includeDefects = !it.includeDefects) } }
                            )
                            Text("Defectos")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = config.includeProductivity,
                                onCheckedChange = { viewModel.updateConfig { it.copy(includeProductivity = !it.includeProductivity) } }
                            )
                            Text("Productividad")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = config.includeCosts,
                                onCheckedChange = { viewModel.updateConfig { it.copy(includeCosts = !it.includeCosts) } }
                            )
                            Text("Costos")
                        }
                    }
                }
            }

            // Formato de salida
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Formato de salida", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = config.format == ReportFormat.PDF,
                                    onClick = { viewModel.updateConfig { it.copy(format = ReportFormat.PDF) } }
                                )
                                Text("HTML (vista web)")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = config.format == ReportFormat.EXCEL,
                                    onClick = { viewModel.updateConfig { it.copy(format = ReportFormat.EXCEL) } }
                                )
                                Text("CSV")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = config.format == ReportFormat.CSV,
                                    onClick = { viewModel.updateConfig { it.copy(format = ReportFormat.CSV) } }
                                )
                                Text("CSV")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "El formato HTML se abre en el navegador y puedes guardarlo como PDF desde el menú del navegador.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Mensaje de error si existe
            if (uiState.error != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            text = uiState.error!!,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }

    // DatePicker diálogos (solo se muestran en modo normal cuando se necesita filtrar por fechas)
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