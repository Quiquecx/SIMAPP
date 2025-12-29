package com.quiquecx.simaapp.view.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.HistoryEntry
import com.quiquecx.simaapp.domain.entity.DefectEntry
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailsScreen(viewModel: ActivityDetailsViewModel, onBack: () -> Unit) {
    val activity by viewModel.activity.collectAsState()
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Detalles", "Calidad", "Tiempo", "Personal", "Historial")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity?.cpmId ?: "Detalles") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Regresar") } },
                actions = { IconButton(onClick = { viewModel.deleteActivity(onBack) }) { Icon(Icons.Default.Delete, "Eliminar", tint = Color.Red) } }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            activity?.let { act ->
                Column(Modifier.padding(padding)) {
                    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
                        tabs.forEachIndexed { index, title ->
                            Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                        }
                    }
                    when (selectedTab) {
                        0 -> GeneralDetailsTab(act, viewModel)
                        1 -> QualityControlTab(act, viewModel)
                        2 -> TimerTab(act, viewModel)
                        3 -> PeopleTab(act, viewModel)
                        4 -> HistoryTab(history)
                    }
                }
            }
        }
    }
}

@Composable
fun GeneralDetailsTab(activity: ActivityEntity, viewModel: ActivityDetailsViewModel) {
    var cpm by remember { mutableStateOf(activity.cpmId) }
    var total by remember { mutableStateOf(activity.cantidadTotal.toString()) }
    var horas by remember { mutableStateOf(activity.estimadoHoras) }
    var nota by remember { mutableStateOf(activity.defectoNota) }

    // Usamos fechaInicio que es de tipo Date en tu Entity
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {

        // --- TARJETA INFORMATIVA ---
        Card(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("ID Actividad: ${activity.id}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(Modifier.height(4.dp))

                // Usamos los IDs de material y proveedor definidos en tu Entity
                Text(
                    text = "Mat: ${activity.materialId} | Prov: ${activity.proveedorId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Responsable", style = MaterialTheme.typography.labelSmall)
                        Text(activity.responsable, style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Fecha Inicio", style = MaterialTheme.typography.labelSmall)
                        // Formateamos el campo fechaInicio
                        Text(sdf.format(activity.fechaInicio), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(8.dp).background(
                            if(activity.estado == "Finalizado") Color.Gray else Color(0xFF4CAF50),
                            RoundedCornerShape(50)
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Estado: ${activity.estado}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- FORMULARIO DE EDICIÓN ---
        Text("Editar Datos del Reporte", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(value = cpm, onValueChange = { cpm = it }, label = { Text("CPM ID") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = total,
            onValueChange = { if(it.all { c -> c.isDigit() }) total = it },
            label = { Text("Total Planificado (Cantidad)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(value = horas, onValueChange = { horas = it }, label = { Text("Est. Horas") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = nota,
            onValueChange = { nota = it },
            label = { Text("Notas y Observaciones") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.updateGeneralDetails(total.toIntOrNull() ?: 0, horas, "0", nota, cpm) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Guardar Cambios")
        }

        if (activity.estado != "Finalizado") {
            OutlinedButton(
                onClick = { viewModel.finalizeActivity() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.width(8.dp))
                Text("Finalizar Actividad")
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun QualityControlTab(activity: ActivityEntity, viewModel: ActivityDetailsViewModel) {
    var isEditMode by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Estado para el nuevo defecto que se quiera añadir
    var newDefectName by remember { mutableStateOf("") }

    var currentOkInput by remember { mutableStateOf("") }

    // Usamos deriveState o keys para que se actualice si la activity cambia
    val currentDefectsInput = remember(activity.defectos) {
        mutableStateMapOf<String, String>().apply {
            activity.defectos.forEach { put(it.name, "") }
        }
    }

    var adjustedOkTotal by remember { mutableStateOf(activity.cantidadOk.toString()) }
    val adjustedDefectsMap = remember(activity.defectos) {
        mutableStateMapOf<String, String>().apply {
            activity.defectos.forEach { put(it.name, it.count.toString()) }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("¿Confirmar Ajuste Manual?") },
            text = { Text("Esta acción sobrescribirá los totales actuales. Úselo solo para corregir errores.") },
            confirmButton = {
                TextButton(onClick = {
                    val okFixed = adjustedOkTotal.toIntOrNull() ?: 0
                    // Mapeamos los defectos ajustados
                    val defectsFixed = adjustedDefectsMap.map { (name, count) ->
                        DefectEntry(name, count.toIntOrNull() ?: 0)
                    }
                    viewModel.adjustQualityTotals(okFixed, defectsFixed)
                    showConfirmDialog = false
                    isEditMode = false
                }) { Text("SÍ, CORREGIR", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("CANCELAR") } }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Cabecera (Se mantiene igual)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Control de Calidad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Corrección", style = MaterialTheme.typography.labelSmall)
                Switch(checked = isEditMode, onCheckedChange = { isEditMode = it })
            }
        }

        // Cards de Totales (Se mantiene igual)
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL OK", fontSize = 10.sp, color = Color(0xFF2E7D32))
                    Text("${activity.cantidadOk}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
            }
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL NO OK", fontSize = 10.sp, color = Color.Red)
                    Text("${activity.cantidadNoOk}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.Red)
                }
            }
        }

        val animatedProgress by animateFloatAsState(targetValue = activity.progreso / 100f, label = "")
        LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Color(0xFF4CAF50), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)

        Spacer(Modifier.height(16.dp))

        // Contenido Principal (Registro o Ajuste)
        Text(
            text = if (!isEditMode) "Entrada de Nuevo Lote" else "Ajuste Manual de Totales",
            fontWeight = FontWeight.Bold,
            color = if (!isEditMode) MaterialTheme.colorScheme.primary else Color.Red
        )

        Card(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = if(isEditMode) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant)) {

            LazyColumn(Modifier.padding(12.dp)) {
                item {
                    if (isEditMode) {
                        Text("⚠️ Estos valores sobrescriben los actuales", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                        OutlinedTextField(value = adjustedOkTotal, onValueChange = { adjustedOkTotal = it }, label = { Text("Corregir Total OK") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    } else {
                        OutlinedTextField(value = currentOkInput, onValueChange = { if(it.all{c->c.isDigit()}) currentOkInput = it }, label = { Text("Piezas OK lote") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Defectos detectados:", style = MaterialTheme.typography.labelMedium)
                }

                // Lista Dinámica de Defectos (Combina los de la actividad + los nuevos añadidos en el mapa)
                val allDefectNames = if (isEditMode) adjustedDefectsMap.keys.toList() else currentDefectsInput.keys.toList()

                items(allDefectNames) { defectName ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(defectName, Modifier.weight(1f), fontSize = 14.sp)
                        OutlinedTextField(
                            value = if (isEditMode) (adjustedDefectsMap[defectName] ?: "0") else (currentDefectsInput[defectName] ?: ""),
                            onValueChange = {
                                if (it.all { c -> c.isDigit() }) {
                                    if (isEditMode) adjustedDefectsMap[defectName] = it else currentDefectsInput[defectName] = it
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }

                // 🚨 SECCIÓN PARA AÑADIR NUEVO DEFECTO
                item {
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newDefectName,
                            onValueChange = { newDefectName = it },
                            label = { Text("Nuevo tipo de defecto...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (newDefectName.isNotBlank()) {
                                    // Añadir a ambos mapas para que aparezca en ambos modos
                                    currentDefectsInput[newDefectName] = ""
                                    adjustedDefectsMap[newDefectName] = "0"
                                    newDefectName = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Añadir")
                        }
                    }
                }
            }
        }

        // Botón de Acción Final
        Button(
            onClick = {
                if (isEditMode) {
                    showConfirmDialog = true
                } else {
                    val okCount = currentOkInput.toIntOrNull() ?: 0
                    val defectsToCapture = currentDefectsInput.map { DefectEntry(it.key, it.value.toIntOrNull() ?: 0) }
                    viewModel.addQualityCapture(okCount, defectsToCapture)

                    // Limpiar campos
                    currentOkInput = ""
                    currentDefectsInput.keys.forEach { currentDefectsInput[it] = "" }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isEditMode) Color.Red else MaterialTheme.colorScheme.primary)
        ) {
            Text(if (isEditMode) "CONFIRMAR AJUSTE" else "REGISTRAR Y LIMPIAR")
        }
    }
}

@Composable
fun TimerTab(activity: ActivityEntity, viewModel: ActivityDetailsViewModel) {
    var secondsElapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(activity.timerActive, activity.timerStartTime) {
        if (activity.timerActive && activity.timerStartTime != null) {
            while (true) {
                secondsElapsed = maxOf(0L, System.currentTimeMillis() - activity.timerStartTime.time) / 1000
                delay(1000L)
            }
        } else secondsElapsed = 0L
    }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (activity.timerActive) "ACTIVO" else "PAUSADO", fontWeight = FontWeight.Bold, color = if (activity.timerActive) Color(0xFF4CAF50) else Color.Gray)
        Text(String.format("%02d:%02d:%02d", secondsElapsed / 3600, (secondsElapsed % 3600) / 60, secondsElapsed % 60), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!activity.timerActive) {
                Button(onClick = { viewModel.startTimerAndSetInProgress() }, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("INICIAR") }
            } else {
                Button(onClick = { viewModel.pauseTimerAndSave() }, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))) { Text("PAUSAR") }
            }
        }
        Spacer(Modifier.height(32.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Acumulado")
                Text("${String.format("%.4f", activity.horasAcumuladas)} hrs", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PeopleTab(activity: ActivityEntity, viewModel: ActivityDetailsViewModel) {
    var newPerson by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(value = newPerson, onValueChange = { newPerson = it }, label = { Text("Añadir Personal") }, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { if(newPerson.isNotBlank()){ viewModel.addPersonToActivity(newPerson); newPerson = "" } }) { Icon(Icons.Default.Add, null) } })
        LazyColumn { items(activity.people) { person -> ListItem(headlineContent = { Text(person) }, trailingContent = { IconButton(onClick = { viewModel.removePersonFromActivity(person) }) { Icon(Icons.Default.Delete, null, tint = Color.Gray) } }) } }
    }
}

@Composable
fun HistoryTab(history: List<HistoryEntry>) {
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    LazyColumn(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(history) { entry ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.userName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Text(sdf.format(entry.timestamp.toDate()), fontSize = 10.sp, color = Color.Gray)
                    }
                    Text("${entry.field}: ${entry.oldValue} -> ${entry.newValue}", fontSize = 12.sp)
                }
            }
        }
    }
}