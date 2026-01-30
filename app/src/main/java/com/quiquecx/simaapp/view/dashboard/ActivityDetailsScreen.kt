package com.quiquecx.simaapp.view.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quiquecx.simaapp.domain.entity.*
import java.text.SimpleDateFormat
import java.util.*

// Función auxiliar para convertir milisegundos a formato de reloj 00:00:00
fun formatMillisToClock(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailsScreen(viewModel: ActivityDetailsViewModel, onBack: () -> Unit) {
    val activity by viewModel.activity.collectAsState()
    val history by viewModel.history.collectAsState()
    val productivityLogs by viewModel.productivityLogs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Calidad", "Personal", "Historial")

    var showDeleteActivityDialog by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<ProductivityEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity?.cpmId ?: "Detalles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteActivityDialog = true }) {
                        Icon(Icons.Default.Delete, "Eliminar", tint = Color.Red)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            activity?.let { act ->
                Column(Modifier.padding(padding)) {
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }
                    when (selectedTab) {
                        0 -> GeneralDetailsTab(act, viewModel)
                        1 -> QualityControlTab(act, productivityLogs, viewModel) { logToDelete = it }
                        2 -> PersonnelTab(act, viewModel)
                        3 -> HistoryTab(history)
                    }
                }
            }
        }
    }

    if (showDeleteActivityDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteActivityDialog = false },
            title = { Text("¿Eliminar actividad?") },
            text = { Text("Esta acción es permanente y borrará todo el historial. ¿Continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteActivity(onBack)
                    showDeleteActivityDialog = false
                }) { Text("ELIMINAR", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteActivityDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    logToDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Eliminar registro") },
            text = { Text("Se restarán ${log.cantidadOk} piezas OK del total. ¿Confirmar?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProductivityLog(log)
                    logToDelete = null
                }) { Text("BORRAR", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) { Text("CANCELAR") }
            }
        )
    }
}

// --- PESTAÑA: PERSONAL ---
@Composable
fun PersonnelTab(activity: ActivityEntity, viewModel: ActivityDetailsViewModel) {
    var newPersonName by remember { mutableStateOf("") }
    val serverTime by viewModel.currentTime.collectAsState()

    val totalHoursRunning = activity.workers.sumOf { worker ->
        val liveSession = if (worker.isTimerActive && worker.startTime != null) {
            (serverTime - worker.startTime.toDate().time).toDouble() / 3600000.0
        } else 0.0
        worker.accumulatedHours + liveSession
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TIEMPO TOTAL DE OPERACIÓN", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = "${String.format("%.4f", totalHoursRunning)} hrs",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = newPersonName,
            onValueChange = { newPersonName = it },
            label = { Text("Nombre del Trabajador") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = {
                    if(newPersonName.isNotBlank()){
                        viewModel.addPersonToActivity(newPersonName)
                        newPersonName = ""
                    }
                }) { Icon(Icons.Default.Add, null) }
            }
        )
        Spacer(Modifier.height(16.dp))
        Text("Equipo en Planta", fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f)) {
            items(activity.workers) { worker ->
                WorkerItemRow(
                    worker = worker,
                    serverTime = serverTime,
                    onToggle = { viewModel.toggleWorkerTimer(worker.name) },
                    onDelete = { viewModel.removePersonFromActivity(worker) }
                )
            }
        }
    }
}

@Composable
fun WorkerItemRow(worker: WorkerEntity, serverTime: Long, onToggle: () -> Unit, onDelete: () -> Unit) {
    val cardColor by animateColorAsState(
        if (worker.isTimerActive) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface, label = ""
    )

    // Calculamos el tiempo de la sesión actual en milisegundos para el reloj 00:00:00
    val sessionMillis = if (worker.isTimerActive && worker.startTime != null) {
        maxOf(0L, serverTime - worker.startTime.toDate().time)
    } else 0L

    // Calculamos las horas totales (acumuladas + sesión actual) en formato decimal
    val totalHoursDecimal = worker.accumulatedHours + (sessionMillis.toDouble() / 3600000.0)

    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = if(worker.isTimerActive) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50)) else null
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(worker.name, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Muestra el reloj 00:00:00 solo si está activo
                    if (worker.isTimerActive) {
                        Surface(
                            color = Color(0xFF4CAF50),
                            shape = CircleShape,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = formatMillisToClock(sessionMillis),
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Total: ${String.format("%.4f", totalHoursDecimal)} hrs",
                        fontSize = 12.sp,
                        color = if(worker.isTimerActive) Color(0xFF2E7D32) else Color.Gray
                    )
                }
            }
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (worker.isTimerActive) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = if (worker.isTimerActive) Color.Red else Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// --- PESTAÑA: CALIDAD ---
@Composable
fun QualityControlTab(
    activity: ActivityEntity,
    productivityLogs: List<ProductivityEntity>,
    viewModel: ActivityDetailsViewModel,
    onDeleteLogRequest: (ProductivityEntity) -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }
    var currentOkInput by remember { mutableStateOf("") }
    var selectedTurno by remember { mutableStateOf("Mañana") }
    var newDefectName by remember { mutableStateOf("") }
    val turnos = listOf("Mañana", "Tarde", "Noche")

    val currentDefectsInput = remember(activity.defectos) {
        mutableStateMapOf<String, String>().apply {
            activity.defectos.forEach { put(it.name, "") }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Control de Producción", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Modo Ajuste", fontSize = 10.sp)
                Switch(checked = isEditMode, onCheckedChange = { isEditMode = it })
            }
        }

        if (!isEditMode) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                turnos.forEach { turno ->
                    FilterChip(
                        selected = selectedTurno == turno,
                        onClick = { selectedTurno = turno },
                        label = { Text(turno) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OK ACUMULADO", fontSize = 10.sp)
                    Text("${activity.cantidadOk}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NO OK TOTAL", fontSize = 10.sp)
                    Text("${activity.cantidadNoOk}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Red)
                }
            }
        }

        LazyColumn(Modifier.weight(1f)) {
            item {
                OutlinedTextField(
                    value = currentOkInput,
                    onValueChange = { if(it.all{c->c.isDigit()}) currentOkInput = it },
                    label = { Text(if(isEditMode) "Corregir Total OK" else "Piezas OK del Lote") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(12.dp))
                Text("Desglose de Defectos:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }

            items(activity.defectos) { defect ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.removeDefectType(defect) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.RemoveCircleOutline, null, tint = Color.LightGray)
                    }
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(defect.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Acumulado: ${defect.count}", fontSize = 11.sp, color = Color.Gray)
                    }
                    OutlinedTextField(
                        value = currentDefectsInput[defect.name] ?: "",
                        onValueChange = { if(it.all{c->c.isDigit()}) currentDefectsInput[defect.name] = it },
                        modifier = Modifier.width(90.dp),
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newDefectName,
                        onValueChange = { newDefectName = it },
                        label = { Text("Nuevo tipo de defecto...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (newDefectName.isNotBlank()) {
                            viewModel.addNewDefectType(newDefectName)
                            newDefectName = ""
                        }
                    }) { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.height(24.dp))
                Text("HISTORIAL DE TURNOS", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
            }
            items(productivityLogs) { log -> ProductivityLogItem(log, onDelete = { onDeleteLogRequest(log) }) }
        }

        Button(
            onClick = {
                val defects = currentDefectsInput.map { DefectEntry(it.key, it.value.toIntOrNull() ?: 0) }
                if (isEditMode) {
                    // 1. Guardar el ajuste
                    viewModel.adjustQualityTotals(currentOkInput.toIntOrNull() ?: activity.cantidadOk, defects)

                    // 2. SALIR EN AUTOMÁTICO del modo ajuste
                    isEditMode = false

                    // 3. Limpiar inputs para que queden listos para el siguiente registro
                    currentOkInput = ""
                    currentDefectsInput.keys.forEach { currentDefectsInput[it] = "" }
                } else {
                    viewModel.addQualityCaptureWithShift(currentOkInput.toIntOrNull() ?: 0, defects, selectedTurno)
                    currentOkInput = ""
                    currentDefectsInput.keys.forEach { currentDefectsInput[it] = "" }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if(isEditMode) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary // Cambié a Verde al ser exitoso/guardado
            )
        ) {
            Icon(if(isEditMode) Icons.Default.CheckCircle else Icons.Default.Send, null)
            Spacer(Modifier.width(8.dp))
            Text(if(isEditMode) "CONFIRMAR Y CERRAR AJUSTE" else "REGISTRAR LOTE - $selectedTurno")
        }
    }
}

// --- PESTAÑA: GENERAL ---
@Composable
fun GeneralDetailsTab(activity: ActivityEntity, viewModel: ActivityDetailsViewModel) {
    var cpm by remember(activity.id) { mutableStateOf(activity.cpmId) }
    var total by remember(activity.id) { mutableStateOf(activity.cantidadTotal.toString()) }
    var nota by remember(activity.id) { mutableStateOf(activity.defectoNota) }
    var horasEst by remember(activity.id) { mutableStateOf(activity.estimadoHoras) }

    val serverTime by viewModel.currentTime.collectAsState()

    val totalHoursReal = activity.workers.sumOf { worker ->
        val liveSession = if (worker.isTimerActive && worker.startTime != null) {
            (serverTime - worker.startTime.toDate().time).toDouble() / 3600000.0
        } else 0.0
        worker.accumulatedHours + liveSession
    }

    val horasTeoricasPlanificadas = horasEst.replace(",", ".").toDoubleOrNull() ?: 0.0
    val balance = horasTeoricasPlanificadas - totalHoursReal

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Rendimiento Operativo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Card(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (balance >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Column {
                        Text("TIEMPO REAL (PERSONAL)", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "${String.format("%.2f", totalHoursReal)}h",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TIEMPO ESTIMADO", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "${String.format("%.2f", horasTeoricasPlanificadas)}h",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (balance >= 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                        null,
                        tint = if (balance >= 0) Color(0xFF2E7D32) else Color.Red
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (balance >= 0) "Restante: ${String.format("%.2f", balance)}h"
                        else "Excedido: ${String.format("%.2f", Math.abs(balance))}h",
                        fontWeight = FontWeight.Bold,
                        color = if (balance >= 0) Color(0xFF2E7D32) else Color.Red
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Responsable: ${activity.responsable}")

                val colorEstado = when (activity.estado) {
                    "Finalizado" -> Color(0xFF2E7D32)
                    "En curso" -> Color(0xFF1976D2)
                    else -> Color.Gray
                }

                Text(
                    text = "Estado: ${activity.estado.uppercase()}",
                    fontWeight = FontWeight.Black,
                    color = colorEstado
                )

                Spacer(Modifier.height(12.dp))

                // --- CAMBIO AQUÍ: USA activity.cantidadTotal EN LUGAR DE total ---
                val procesadasActual = (activity.cantidadOk + activity.cantidadNoOk).toDouble()
                val progresoReal = if (activity.cantidadTotal > 0) {
                    ((procesadasActual / activity.cantidadTotal) * 100).toInt().coerceIn(0, 100)
                } else 0

                LinearProgressIndicator(
                    progress = { progresoReal / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    strokeCap = StrokeCap.Round,
                    color = if (progresoReal >= 100) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
                Text("Progreso Real: $progresoReal%", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        OutlinedTextField(value = cpm, onValueChange = { cpm = it }, label = { Text("CPM ID") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = total,
                onValueChange = { if(it.all { c -> c.isDigit() }) total = it },
                label = { Text("Lote Total") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = horasEst,
                onValueChange = { horasEst = it },
                label = { Text("Tiempo Estimado (h)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = nota, onValueChange = { nota = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

        Button(
            onClick = {
                viewModel.updateGeneralDetails(
                    cantidadTotal = total.toIntOrNull() ?: 0,
                    estHoras = horasEst,
                    estCosto = "0",
                    nota = nota,
                    cpm = cpm
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 16.dp)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text("ACTUALIZAR PLAN")
        }
    }
}

// --- PESTAÑA: HISTORIAL ---
@Composable
fun HistoryTab(history: List<HistoryEntry>) {
    val sdf = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
        items(history) { entry ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(entry.userName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(sdf.format(entry.timestamp.toDate()), fontSize = 10.sp, color = Color.Gray)
                    }
                    Text("${entry.field}: ${entry.oldValue} -> ${entry.newValue}", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ProductivityLogItem(log: ProductivityEntity, onDelete: () -> Unit) {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Badge { Text(log.turno) }
                Text(sdf.format(log.timestamp.toDate()), fontSize = 11.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${log.cantidadOk} OK", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Text("${log.defectos.sumOf { it.count }} DEF", fontSize = 11.sp, color = Color.Red)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) }
        }
    }
}
