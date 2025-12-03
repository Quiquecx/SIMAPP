package com.quiquecx.simaapp.view.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityFormScreen(
    viewModel: ActivityFormViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Estado local para mostrar u ocultar DatePickerDialog
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.fechaInicio.time
    )

    // Efecto para manejar navegación y mostrar Toast al guardar con éxito
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            Toast.makeText(context, "Actividad creada con éxito!", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Nueva Actividad") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- SECCIÓN 1: INFORMACIÓN GENERAL ---
                item {
                    Text(
                        "Información General",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 1. Tipo de Actividad
                item {
                    OutlinedTextField(
                        value = state.tipo,
                        onValueChange = viewModel::updateTipo,
                        label = { Text("Tipo de Actividad (Ej: Sorteo, Inspección)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. Proveedor y Material
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.proveedorId,
                            onValueChange = viewModel::updateProveedorId,
                            label = { Text("ID Proveedor") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.materialId,
                            onValueChange = viewModel::updateMaterialId,
                            label = { Text("ID Material") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. Responsable 👈 NUEVO CAMPO
                item {
                    OutlinedTextField(
                        value = state.responsable,
                        onValueChange = viewModel::updateResponsable,
                        label = { Text("Responsable de la Tarea") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4. Cantidad Total
                item {
                    OutlinedTextField(
                        value = state.cantidadTotal,
                        onValueChange = viewModel::updateCantidadTotal,
                        label = { Text("Cantidad Total de Piezas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 5. Fecha de Inicio 👈 NUEVO CAMPO (Date Picker)
                item {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    OutlinedTextField(
                        value = dateFormat.format(state.fechaInicio),
                        onValueChange = {}, // Campo de solo lectura
                        label = { Text("Fecha de Inicio") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePickerDialog = true }) {
                                Icon(Icons.Filled.DateRange, contentDescription = "Seleccionar Fecha")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // --- SECCIÓN 2: ESTIMACIONES Y DETALLES ---
                item {
                    Text(
                        "Estimaciones y Detalles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 6. Estimado de Horas y Costo 👈 NUEVOS CAMPOS
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.estimadoHoras,
                            onValueChange = viewModel::updateEstimadoHoras,
                            label = { Text("Horas Estimadas") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.estimadoCosto,
                            onValueChange = viewModel::updateEstimadoCosto,
                            label = { Text("Costo Estimado") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 7. Defecto Inicial 👈 NUEVO CAMPO
                item {
                    OutlinedTextField(
                        value = state.defecto,
                        onValueChange = viewModel::updateDefecto,
                        label = { Text("Defecto Inicial Detectado") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 3
                    )
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }

                // Mostrar error de guardado
                if (state.saveError) {
                    item {
                        Text(
                            "Error al guardar la actividad. Revise los campos.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Botón de guardar
            Button(
                onClick = viewModel::saveActivity,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Guardar Nueva Actividad")
                }
            }
        }
    }

    // --- Date Picker Dialog ---
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Convertir milisegundos a objeto Date
                            viewModel.updateFechaInicio(Date(millis))
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}