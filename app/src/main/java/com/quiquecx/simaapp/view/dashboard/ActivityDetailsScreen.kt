package com.quiquecx.simaapp.view.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pantalla principal que muestra los detalles de una actividad, permitiendo su edición,
 * control de calidad, ajuste de progreso y eliminación.
 *
 * @param viewModel ViewModel inyectado que gestiona el estado de la actividad desde Firestore.
 * @param onBack Función de navegación para regresar a la pantalla anterior (Dashboard).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailsScreen(
    viewModel: ActivityDetailsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    // Escucha la actividad en tiempo real desde el ViewModel
    val activity by viewModel.activity.collectAsStateWithLifecycle()

    // Estado local para controlar la visibilidad del diálogo de eliminación
    var showDeleteDialog by remember { mutableStateOf(false) }

    // --- ESTADOS LOCALES PARA EDICIÓN DE CONTROL DE CALIDAD Y PROGRESO ---
    var currentProgress by remember { mutableStateOf(0f) }
    var currentOkCount by remember { mutableStateOf("") }
    var currentNoOkCount by remember { mutableStateOf("") }
    var currentHours by remember { mutableStateOf("") }
    // ---------------------------------------------------------------------

    // --- ESTADOS LOCALES PARA EDICIÓN DE DETALLES Y ESTIMACIONES ---
    var currentTotalCount by remember { mutableStateOf("") }
    var currentEstimadoHoras by remember { mutableStateOf("") }
    var currentEstimadoCosto by remember { mutableStateOf("") }
    var currentDefecto by remember { mutableStateOf("") }
    // -------------------------------------------------------------

    // Efecto que se dispara cada vez que el objeto 'activity' cambia (actualizaciones de Firestore)
    LaunchedEffect(activity) {
        activity?.let {
            // Inicialización de estados de Calidad y Progreso
            currentProgress = it.progreso.toFloat()
            currentOkCount = it.cantidadOk.toString()
            currentNoOkCount = it.cantidadNoOk.toString()
            currentHours = it.horasAcumuladas.toString()

            // Inicialización de estados de Detalles y Estimaciones
            currentTotalCount = it.cantidadTotal.toString()
            currentEstimadoHoras = it.estimadoHoras
            currentEstimadoCosto = it.estimadoCosto
            currentDefecto = it.defecto
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity?.tipo ?: "Detalles de Actividad") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color(0xFFEC221F))
                    }
                }
            )
        }
    ) { padding ->

        when (val currentActivity = activity) {
            null -> {
                // Muestra un cargador mientras se recupera la actividad de Firestore
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()) // Habilita el desplazamiento vertical
                ) {
                    // --- 1. DETALLES DE LA ACTIVIDAD (EDICIÓN DE ESTIMACIONES) ---
                    EditableDetailsSection(
                        currentActivity = currentActivity,
                        currentDefecto = currentDefecto,
                        onDefectoChange = { currentDefecto = it },
                        currentTotalCount = currentTotalCount,
                        onTotalCountChange = { currentTotalCount = it },
                        currentEstimadoHoras = currentEstimadoHoras,
                        onEstimadoHorasChange = { currentEstimadoHoras = it },
                        currentEstimadoCosto = currentEstimadoCosto,
                        onEstimadoCostoChange = { currentEstimadoCosto = it },
                        onSave = {
                            // Llama a la función de guardado para detalles y estimaciones
                            viewModel.updateGeneralDetails(
                                cantidadTotal = currentTotalCount.toIntOrNull() ?: currentActivity.cantidadTotal,
                                estimadoHoras = currentEstimadoHoras,
                                estimadoCosto = currentEstimadoCosto,
                                defecto = currentDefecto
                            )
                        }
                    )

                    Spacer(Modifier.height(32.dp))

                    // --- 2. ENTRADA DE DATOS (CONTROL DE CALIDAD) ---
                    QualityControlSection(
                        currentActivity = currentActivity,
                        currentOkCount = currentOkCount,
                        onOkCountChange = { currentOkCount = it },
                        currentNoOkCount = currentNoOkCount,
                        onNoOkCountChange = { currentNoOkCount = it },
                        currentHours = currentHours,
                        onHoursChange = { currentHours = it },
                        onSave = {
                            // Llama a la función de guardado para calidad, que recalcula el progreso
                            viewModel.updateActivityData(
                                okCount = currentOkCount.toIntOrNull() ?: currentActivity.cantidadOk,
                                noOkCount = currentNoOkCount.toIntOrNull() ?: currentActivity.cantidadNoOk,
                                hours = currentHours.toIntOrNull() ?: currentActivity.horasAcumuladas
                            )
                        }
                    )

                    Spacer(Modifier.height(32.dp))

                    // --- 3. CONTROL DE PROGRESO (AJUSTE MANUAL CON SLIDER) ---
                    ProgressControlSection(
                        currentActivity = currentActivity,
                        currentProgress = currentProgress,
                        onProgressChange = { currentProgress = it },
                        onSave = { viewModel.updateProgress(currentProgress.toInt()) }
                    )

                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }

    // Diálogo de Confirmación de Eliminación
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar la actividad ${activity?.tipo} (${activity?.materialId})? Esta acción es permanente.") },
            confirmButton = {
                Button(
                    onClick = {
                        // Elimina la actividad y navega de regreso si es exitoso
                        viewModel.deleteActivity(onSuccess = onBack)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC221F))
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// COMPONENTES REUTILIZABLES (SECCIONES)
// -----------------------------------------------------------------------------

/**
 * Sección de edición para Detalles y Estimaciones.
 * Muestra campos de texto para Cantidad Total, Horas Estimadas, Costo Estimado y Defecto.
 */
@Composable
fun EditableDetailsSection(
    currentActivity: ActivityEntity,
    currentDefecto: String,
    onDefectoChange: (String) -> Unit,
    currentTotalCount: String,
    onTotalCountChange: (String) -> Unit,
    currentEstimadoHoras: String,
    onEstimadoHorasChange: (String) -> Unit,
    currentEstimadoCosto: String,
    onEstimadoCostoChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val statusColor = if (currentActivity.estado == "Finalizado") Color.Green else Color.Red

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("ID: ${currentActivity.id}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text("Material: ${currentActivity.materialId} | Proveedor: ${currentActivity.proveedorId}", style = MaterialTheme.typography.bodyMedium)
        Text("Responsable: ${currentActivity.responsable}", style = MaterialTheme.typography.bodyLarge)
        Text("Fecha Inicio: ${formatter.format(currentActivity.fechaInicio)}", style = MaterialTheme.typography.bodyLarge)
        Text("Estado: ${currentActivity.estado}", style = MaterialTheme.typography.bodyLarge.copy(color = statusColor))

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        // --- CAMPOS EDITABLES ---

        // Defecto/Nota
        OutlinedTextField(
            value = currentDefecto,
            onValueChange = onDefectoChange,
            label = { Text("Defecto/Nota") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        // Cantidad Total (Piezas)
        OutlinedTextField(
            value = currentTotalCount,
            onValueChange = onTotalCountChange,
            label = { Text("Cantidad Total (Piezas)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        // Horas Estimadas
        OutlinedTextField(
            value = currentEstimadoHoras,
            onValueChange = onEstimadoHorasChange,
            label = { Text("Horas Estimadas") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        // Costo Estimado
        OutlinedTextField(
            value = currentEstimadoCosto,
            onValueChange = onEstimadoCostoChange,
            label = { Text("Costo Estimado") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        // Botón de Guardar
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            // Habilita el botón solo si al menos un campo ha sido modificado.
            enabled = currentActivity.cantidadTotal.toString() != currentTotalCount ||
                    currentActivity.estimadoHoras != currentEstimadoHoras ||
                    currentActivity.estimadoCosto != currentEstimadoCosto ||
                    currentActivity.defecto != currentDefecto
        ) {
            Text("Guardar Detalles y Estimaciones")
        }
    }
}


/**
 * Sección para el ajuste manual del Progreso (%) mediante un Slider.
 */
@Composable
fun ProgressControlSection(
    currentActivity: ActivityEntity,
    currentProgress: Float,
    onProgressChange: (Float) -> Unit,
    onSave: () -> Unit
) {
    Text("Ajuste de Progreso", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
    Text("Progreso: ${currentProgress.toInt()}%", modifier = Modifier.padding(vertical = 8.dp))

    Slider(
        value = currentProgress,
        onValueChange = onProgressChange,
        valueRange = 0f..100f,
        steps = 99, // Permite valores enteros de 0 a 100
        modifier = Modifier.fillMaxWidth()
    )

    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        // Habilita solo si el progreso local difiere del valor de Firestore
        enabled = currentActivity.progreso.toFloat().toInt() != currentProgress.toInt()
    ) {
        Text("Guardar Progreso (Slider)")
    }
}

/**
 * Sección para la entrada de datos de control de calidad (Piezas OK/NO OK y Horas).
 */
@Composable
fun QualityControlSection(
    currentActivity: ActivityEntity,
    currentOkCount: String,
    onOkCountChange: (String) -> Unit,
    currentNoOkCount: String,
    onNoOkCountChange: (String) -> Unit,
    currentHours: String,
    onHoursChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Control de Ejecución y Calidad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))

            // Piezas OK
            OutlinedTextField(
                value = currentOkCount,
                onValueChange = onOkCountChange,
                label = { Text("Piezas OK (Actual: ${currentActivity.cantidadOk})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Piezas NO OK
            OutlinedTextField(
                value = currentNoOkCount,
                onValueChange = onNoOkCountChange,
                label = { Text("Piezas NO OK (Actual: ${currentActivity.cantidadNoOk})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Horas Trabajadas
            OutlinedTextField(
                value = currentHours,
                onValueChange = onHoursChange,
                label = { Text("Horas Acumuladas (Actual: ${currentActivity.horasAcumuladas})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                // Se habilita si al menos un campo de calidad/horas ha sido modificado.
                enabled = currentActivity.cantidadOk.toString() != currentOkCount ||
                        currentActivity.cantidadNoOk.toString() != currentNoOkCount ||
                        currentActivity.horasAcumuladas.toString() != currentHours
            ) {
                Text("Guardar Conteo y Horas")
            }
        }
    }
}

// -----------------------------------------------------------------------------
// PREVIEW
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewActivityDetailsContent(activity: ActivityEntity) {
    // Definición del Scaffold para el Preview
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity.tipo) },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color(0xFFEC221F))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- ESTADOS LOCALES SIMULADOS PARA PREVIEW ---
            var mockDefecto by remember { mutableStateOf(activity.defecto) }
            var mockTotalCount by remember { mutableStateOf(activity.cantidadTotal.toString()) }
            var mockEstimadoHoras by remember { mutableStateOf(activity.estimadoHoras) }
            var mockEstimadoCosto by remember { mutableStateOf(activity.estimadoCosto) }

            // 1. Detalles Editables
            EditableDetailsSection(
                currentActivity = activity,
                currentDefecto = mockDefecto,
                onDefectoChange = { mockDefecto = it },
                currentTotalCount = mockTotalCount,
                onTotalCountChange = { mockTotalCount = it },
                currentEstimadoHoras = mockEstimadoHoras,
                onEstimadoHorasChange = { mockEstimadoHoras = it },
                currentEstimadoCosto = mockEstimadoCosto,
                onEstimadoCostoChange = { mockEstimadoCosto = it },
                onSave = { /* No hace nada en Preview */ }
            )
            Spacer(Modifier.height(32.dp))

            // 2. Control de Calidad
            var mockOk by remember { mutableStateOf(activity.cantidadOk.toString()) }
            var mockNoOk by remember { mutableStateOf(activity.cantidadNoOk.toString()) }
            var mockHours by remember { mutableStateOf(activity.horasAcumuladas.toString()) }
            QualityControlSection(
                currentActivity = activity,
                currentOkCount = mockOk,
                onOkCountChange = { mockOk = it },
                currentNoOkCount = mockNoOk,
                onNoOkCountChange = { mockNoOk = it },
                currentHours = mockHours,
                onHoursChange = { mockHours = it },
                onSave = { /* No hace nada en Preview */ }
            )
            Spacer(Modifier.height(32.dp))

            // 3. Control de Progreso
            var mockProgress by remember { mutableStateOf(activity.progreso.toFloat()) }
            ProgressControlSection(
                currentActivity = activity,
                currentProgress = mockProgress,
                onProgressChange = { mockProgress = it },
                onSave = { /* No hace nada en Preview */ }
            )
            Spacer(Modifier.height(48.dp))
        }
    }
}

/**
 * Función principal del Preview con datos simulados.
 */
@Preview(showBackground = true, name = "Detalles de Actividad Cargados")
@Composable
fun ActivityDetailsScreenPreview() {

    val mockActivity = ActivityEntity(
        id = "ABC-12345",
        tipo = "Inspección de Lote",
        proveedorId = "PROV-X",
        materialId = "MAT-001",
        responsable = "Ana García",
        cantidadTotal = 5000,
        cantidadOk = 1250,
        cantidadNoOk = 20,
        estado = "En curso",
        progreso = 25,
        estimadoHoras = "48",
        estimadoCosto = "5000",
        defecto = "Necesita inspección detallada de soldaduras"
    )

    MaterialTheme {
        PreviewActivityDetailsContent(activity = mockActivity)
    }
}