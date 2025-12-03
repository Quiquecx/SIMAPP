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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailsScreen(
    viewModel: ActivityDetailsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val activity by viewModel.activity.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // --- NUEVOS ESTADOS PARA CONTROL DE CALIDAD ---
    var currentProgress by remember { mutableStateOf(0f) }
    var currentOkCount by remember { mutableStateOf("") }
    var currentNoOkCount by remember { mutableStateOf("") }
    var currentHours by remember { mutableStateOf("") }
    // ---------------------------------------------

    // Inicializa estados cuando la actividad se carga o actualiza
    LaunchedEffect(activity) {
        activity?.let {
            currentProgress = it.progreso.toFloat()
            // Inicializamos los campos con los valores actuales (o 0 si son nulos/vacíos)
            currentOkCount = it.cantidadOk.toString()
            currentNoOkCount = it.cantidadNoOk.toString()
            currentHours = it.horasAcumuladas.toString()
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
                        .verticalScroll(rememberScrollState()) // Añadimos scroll
                ) {
                    // --- 1. DETALLES DE LA ACTIVIDAD ---
                    DetailsSection(currentActivity)

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
                            // Llamada a la nueva función de guardado en el ViewModel
                            viewModel.updateActivityData(
                                okCount = currentOkCount.toIntOrNull() ?: currentActivity.cantidadOk,
                                noOkCount = currentNoOkCount.toIntOrNull() ?: currentActivity.cantidadNoOk,
                                hours = currentHours.toIntOrNull() ?: currentActivity.horasAcumuladas
                            )
                        }
                    )

                    Spacer(Modifier.height(32.dp))

                    // --- 3. CONTROL DE PROGRESO
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

    // Diálogo de Confirmación de Eliminación (Existente)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar la actividad ${activity?.tipo} (${activity?.materialId})? Esta acción es permanente.") },
            confirmButton = {
                Button(
                    onClick = {
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

// --- Componentes Reutilizables ---

@Composable
fun DetailsSection(currentActivity: ActivityEntity) {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val statusColor = if (currentActivity.estado == "Finalizado") Color.Green else Color.Red

    Text("ID: ${currentActivity.id}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
    Text("Material: ${currentActivity.materialId} | Proveedor: ${currentActivity.proveedorId}", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(16.dp))

    Text("Defecto: ${currentActivity.defecto}", style = MaterialTheme.typography.bodyLarge)
    Text("Responsable: ${currentActivity.responsable}", style = MaterialTheme.typography.bodyLarge)
    Text("Fecha Inicio: ${formatter.format(currentActivity.fechaInicio)}", style = MaterialTheme.typography.bodyLarge)
    Text("Estado: ${currentActivity.estado}", style = MaterialTheme.typography.bodyLarge.copy(color = statusColor))

    Spacer(Modifier.height(16.dp))

    // Muestra datos de estimación y totales
    Text("Estimado Horas: ${currentActivity.estimadoHoras} | Costo: ${currentActivity.estimadoCosto}", fontWeight = FontWeight.SemiBold)
    Text("Cant. Total a Producir: ${currentActivity.cantidadTotal}", fontWeight = FontWeight.SemiBold)
}

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
        steps = 99,
        modifier = Modifier.fillMaxWidth()
    )

    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        // Habilita solo si el progreso ha cambiado respecto al valor de Firestore
        enabled = currentActivity.progreso.toFloat().toInt() != currentProgress.toInt()
    ) {
        Text("Guardar Progreso (Slider)")
    }
}

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
                // Se habilita si al menos un campo ha cambiado respecto a Firestore
                enabled = currentActivity.cantidadOk.toString() != currentOkCount ||
                        currentActivity.cantidadNoOk.toString() != currentNoOkCount ||
                        currentActivity.horasAcumuladas.toString() != currentHours
            ) {
                Text("Guardar Conteo y Horas")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewActivityDetailsContent(activity: ActivityEntity) {
    // Usamos el mismo diseño del Scaffold para simular la pantalla
    Scaffold(
        topBar = {
            TopAppBar(
                // 🚨 CORRECCIÓN CLAVE: Usamos 'activity.tipo' directamente (sin '?')
                // ya que la función PreviewActivityDetailsContent garantiza que 'activity' no es nulo.
                title = { Text(activity.tipo) },
                navigationIcon = {
                    IconButton(onClick = {}) {
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Llamamos a los componentes internos con los datos simulados
            DetailsSection(activity)
            Spacer(Modifier.height(32.dp))

            // Creamos estados locales simulados para los TextFields
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

            // Estado de progreso simulado
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

// FUNCIÓN PRINCIPAL DEL PREVIEW

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
        estimadoCosto = "5000"
    )

    MaterialTheme {
        PreviewActivityDetailsContent(activity = mockActivity)
    }
}