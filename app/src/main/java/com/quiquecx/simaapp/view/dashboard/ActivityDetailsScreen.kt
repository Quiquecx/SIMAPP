package com.quiquecx.simaapp.view.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Timestamp // <<-- IMPORTACIÓN CLAVE AÑADIDA
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date // Necesaria para el formato de fecha de ActivityEntity

// -----------------------------------------------------------------------------
// ENUM DE PESTAÑAS
// -----------------------------------------------------------------------------

private enum class ActivityTab(val title: String) {
    DETAILS("Detalles"),
    HISTORY("Historial")
}

/**
 * Pantalla principal que muestra los detalles de una actividad, permitiendo su edición,
 * control de calidad, ajuste de progreso y eliminación, y la visualización de su historial.
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
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    // Estado local para controlar la visibilidad del diálogo de eliminación
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Estado local para la navegación por pestañas
    val tabs = ActivityTab.entries.toTypedArray()
    var selectedTabIndex by remember { mutableStateOf(0) }

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
                    IconButton(onClick = { showDeleteDialog = true }, enabled = activity != null) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color(0xFFEC221F))
                    }
                }
            )
        }
    ) { padding ->

        // Manejo de estados de carga y error
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Error al cargar la actividad: ${error.orEmpty()}", color = Color.Red)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // 1. TabRow para navegación
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(tab.title) }
                        )
                    }
                }

                // 2. Contenido Condicional por Pestaña
                when (tabs[selectedTabIndex]) {
                    ActivityTab.DETAILS -> {
                        activity?.let {
                            ActivityDetailsContent(activity = it, viewModel = viewModel)
                        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Actividad no disponible.")
                        }
                    }
                    ActivityTab.HISTORY -> {
                        ActivityHistoryList(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Diálogo de Confirmación de Eliminación
    if (showDeleteDialog && activity != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar la actividad ${activity!!.tipo} (${activity!!.materialId})? Esta acción es permanente.") },
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

// -----------------------------------------------------------------------------
// COMPONENTE DE PESTAÑA 1: DETALLES DE LA ACTIVIDAD (Contenido original refactorizado)
// -----------------------------------------------------------------------------

@Composable
fun ActivityDetailsContent(
    activity: ActivityEntity,
    viewModel: ActivityDetailsViewModel
) {
    // --- ESTADOS LOCALES PARA EDICIÓN DE CONTROL DE CALIDAD Y PROGRESO ---
    var currentProgress by remember { mutableStateOf(activity.progreso.toFloat()) }
    var currentOkCount by remember { mutableStateOf(activity.cantidadOk.toString()) }
    var currentNoOkCount by remember { mutableStateOf(activity.cantidadNoOk.toString()) }
    var currentHours by remember { mutableStateOf(activity.horasAcumuladas.toString()) }
    // ---------------------------------------------------------------------

    // --- ESTADOS LOCALES PARA EDICIÓN DE DETALLES Y ESTIMACIONES ---
    var currentTotalCount by remember { mutableStateOf(activity.cantidadTotal.toString()) }
    var currentEstimadoHoras by remember { mutableStateOf(activity.estimadoHoras) }
    var currentEstimadoCosto by remember { mutableStateOf(activity.estimadoCosto) }
    var currentDefecto by remember { mutableStateOf(activity.defecto) }
    // -------------------------------------------------------------

    // Efecto que se dispara cada vez que el objeto 'activity' cambia (actualizaciones de Firestore)
    LaunchedEffect(activity) {
        // Solo actualiza los estados locales si los valores del modelo han cambiado
        if (currentProgress.toInt() != activity.progreso) currentProgress = activity.progreso.toFloat()
        if (currentOkCount != activity.cantidadOk.toString()) currentOkCount = activity.cantidadOk.toString()
        if (currentNoOkCount != activity.cantidadNoOk.toString()) currentNoOkCount = activity.cantidadNoOk.toString()
        if (currentHours != activity.horasAcumuladas.toString()) currentHours = activity.horasAcumuladas.toString()
        if (currentTotalCount != activity.cantidadTotal.toString()) currentTotalCount = activity.cantidadTotal.toString()
        if (currentEstimadoHoras != activity.estimadoHoras) currentEstimadoHoras = activity.estimadoHoras
        if (currentEstimadoCosto != activity.estimadoCosto) currentEstimadoCosto = activity.estimadoCosto
        if (currentDefecto != activity.defecto) currentDefecto = activity.defecto
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()) // Habilita el desplazamiento vertical
    ) {
        // --- 1. DETALLES DE LA ACTIVIDAD (EDICIÓN DE ESTIMACIONES) ---
        EditableDetailsSection(
            currentActivity = activity,
            currentDefecto = currentDefecto,
            onDefectoChange = { currentDefecto = it },
            currentTotalCount = currentTotalCount,
            onTotalCountChange = { currentTotalCount = it.filter { char -> char.isDigit() } }, // Solo dígitos
            currentEstimadoHoras = currentEstimadoHoras,
            onEstimadoHorasChange = { currentEstimadoHoras = it },
            currentEstimadoCosto = currentEstimadoCosto,
            onEstimadoCostoChange = { currentEstimadoCosto = it },
            onSave = {
                // Llama a la función de guardado para detalles y estimaciones
                viewModel.updateGeneralDetails(
                    cantidadTotal = currentTotalCount.toIntOrNull() ?: activity.cantidadTotal,
                    estimadoHoras = currentEstimadoHoras,
                    estimadoCosto = currentEstimadoCosto,
                    defecto = currentDefecto
                )
            }
        )

        Spacer(Modifier.height(32.dp))

        // --- 2. ENTRADA DE DATOS (CONTROL DE CALIDAD) ---
        QualityControlSection(
            currentActivity = activity,
            currentOkCount = currentOkCount,
            onOkCountChange = { currentOkCount = it.filter { char -> char.isDigit() } },
            currentNoOkCount = currentNoOkCount,
            onNoOkCountChange = { currentNoOkCount = it.filter { char -> char.isDigit() } },
            currentHours = currentHours,
            onHoursChange = { currentHours = it.filter { char -> char.isDigit() } },
            onSave = {
                // Llama a la función de guardado para calidad, que recalcula el progreso
                viewModel.updateActivityData(
                    okCount = currentOkCount.toIntOrNull() ?: activity.cantidadOk,
                    noOkCount = currentNoOkCount.toIntOrNull() ?: activity.cantidadNoOk,
                    hours = currentHours.toIntOrNull() ?: activity.horasAcumuladas
                )
            }
        )

        Spacer(Modifier.height(32.dp))

        // --- 3. CONTROL DE PROGRESO (AJUSTE MANUAL CON SLIDER) ---
        ProgressControlSection(
            currentActivity = activity,
            currentProgress = currentProgress,
            onProgressChange = { currentProgress = it },
            onSave = { viewModel.updateProgress(currentProgress.toInt()) }
        )

        Spacer(Modifier.height(48.dp))
    }
}


// -----------------------------------------------------------------------------
// COMPONENTE DE PESTAÑA 2: LISTA DEL HISTORIAL (NUEVO)
// -----------------------------------------------------------------------------

@Composable
fun ActivityHistoryList(
    viewModel: ActivityDetailsViewModel
) {
    // Observa el estado del historial
    val historyList by viewModel.history.collectAsStateWithLifecycle()

    if (historyList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No hay registros de cambios para esta actividad.", color = Color.Gray)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 70.dp)
    ) {
        items(historyList) { entry ->
            HistoryCard(entry = entry)
        }
    }
}

/**
 * Tarjeta individual para mostrar un registro de auditoría.
 */
@Composable
fun HistoryCard(entry: HistoryEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Quién y Cuándo
            Text(
                text = "${entry.userName}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
            // Formato de fecha
            // Utilizamos .toDate() de Firebase Timestamp, que ahora está importado.
            val time: Date = entry.timestamp.toDate()
            Text(
                text = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(time),
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
            )
            Spacer(Modifier.height(8.dp))

            // Qué cambió
            Text(
                text = "Campo modificado: ${entry.field.uppercase()}",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFEC221F), fontWeight = FontWeight.SemiBold)
            )

            // Valores
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Anterior:",
                    modifier = Modifier.width(80.dp),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = entry.formatValue(entry.oldValue),
                    style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough)
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Nuevo:",
                    modifier = Modifier.width(80.dp),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = entry.formatValue(entry.newValue),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}


// -----------------------------------------------------------------------------
// COMPONENTES REUTILIZABLES (SECCIONES EXISTENTES)
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
    val statusColor = if (currentActivity.estado == "Finalizado") Color(0xFF1E88E5) else Color(0xFFEC221F)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("ID: ${currentActivity.id}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text("Material: ${currentActivity.materialId} | Proveedor: ${currentActivity.proveedorId}", style = MaterialTheme.typography.bodyMedium)
        Text("Responsable: ${currentActivity.responsable}", style = MaterialTheme.typography.bodyLarge)
        // CORRECCIÓN DE SINTAXIS: Se eliminó el paréntesis extra al final de la línea.
        Text("Fecha Inicio: ${formatter.format(currentActivity.fechaInicio)}", style = MaterialTheme.typography.bodyLarge)
        Text("Estado: ${currentActivity.estado}", style = MaterialTheme.typography.bodyLarge.copy(color = statusColor, fontWeight = FontWeight.SemiBold))

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        // --- CAMPOS EDITABLES ---

        // Defecto/Nota
        OutlinedTextField(
            value = currentDefecto,
            onValueChange = onDefectoChange,
            label = { Text("Defecto/Nota") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
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
        val isModified = currentActivity.cantidadTotal.toString() != currentTotalCount ||
                currentActivity.estimadoHoras != currentEstimadoHoras ||
                currentActivity.estimadoCosto != currentEstimadoCosto ||
                currentActivity.defecto != currentDefecto

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = isModified
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

    val isModified = currentActivity.progreso.toFloat().toInt() != currentProgress.toInt()
    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        enabled = isModified
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

            val isModified = currentActivity.cantidadOk.toString() != currentOkCount ||
                    currentActivity.cantidadNoOk.toString() != currentNoOkCount ||
                    currentActivity.horasAcumuladas.toString() != currentHours

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = isModified
            ) {
                Text("Guardar Conteo y Horas")
            }
        }
    }
}