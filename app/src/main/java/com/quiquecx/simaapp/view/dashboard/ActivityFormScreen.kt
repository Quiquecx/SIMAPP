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

    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.fechaInicio.time
    )

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
                    Text("Información General", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                item {
                    OutlinedTextField(
                        value = state.tipo,
                        onValueChange = viewModel::updateTipo,
                        label = { Text("Tipo de Actividad (Ej: Sorteo)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 🚨 NUEVO: Campo CPM
                item {
                    OutlinedTextField(
                        value = state.cpmId,
                        onValueChange = viewModel::updateCpmId,
                        label = { Text("ID de CPM (Identificador de Pago)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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

                // 🚨 NUEVO: Campo Personas
                item {
                    OutlinedTextField(
                        value = state.personasInput,
                        onValueChange = viewModel::updatePersonasInput,
                        label = { Text("Personas Asignadas (separadas por comas)") },
                        placeholder = { Text("Ej: Juan, Pedro, Maria") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = state.responsable,
                        onValueChange = viewModel::updateResponsable,
                        label = { Text("Responsable") },
                        placeholder = { Text("Nombre de quien supervisa") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = state.cantidadTotal,
                        onValueChange = viewModel::updateCantidadTotal,
                        label = { Text("Cantidad Total de Piezas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    OutlinedTextField(
                        value = dateFormat.format(state.fechaInicio),
                        onValueChange = {},
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

                // --- SECCIÓN 2: CONTROL DE DEFECTOS (2 TIPOS) ---
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Control de Defectos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                // Defecto 1
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.nombreDefecto1,
                            onValueChange = { viewModel.updateDefecto1(it, state.cantidadDefecto1) },
                            label = { Text("Defecto inicial") },
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }


                item {
                    OutlinedTextField(
                        value = state.defectoNota,
                        onValueChange = viewModel::updateDefectoNota,
                        label = { Text("Notas adicionales") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                // --- SECCIÓN 3: ESTIMACIONES ---
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Estimaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.estimadoHoras,
                            onValueChange = viewModel::updateEstimadoHoras,
                            label = { Text("Horas Est.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.estimadoCosto,
                            onValueChange = viewModel::updateEstimadoCosto,
                            label = { Text("Costo Est.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }

                if (state.saveError) {
                    item {
                        Text("Error al guardar. Verifique los campos obligatorios.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Button(
                onClick = { viewModel.saveActivity() }, // Lambda
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
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
                            viewModel.updateFechaInicio(java.util.Date(millis))
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