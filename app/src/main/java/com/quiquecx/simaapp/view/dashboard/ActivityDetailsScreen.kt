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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailsScreen(
    viewModel: ActivityDetailsViewModel,
    onBack: () -> Unit
) {
    val activity by viewModel.activity.collectAsState()
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Detalles", "Calidad", "Tiempo", "Personal", "Historial")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity?.cpmId ?: "Detalles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.deleteActivity(onBack) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
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

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Card(Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Column(Modifier.padding(12.dp)) {
                Text("ID: ${activity.id}", style = MaterialTheme.typography.labelSmall)
                Text("Responsable: ${activity.responsable}", fontWeight = FontWeight.Bold)
                Text("Estado: ${activity.estado}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        OutlinedTextField(value = cpm, onValueChange = { cpm = it }, label = { Text("CPM ID") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = total, onValueChange = { if(it.all{c->c.isDigit()}) total = it }, label = { Text("Cantidad Total Planificada") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = horas, onValueChange = { horas = it }, label = { Text("Estimado de Horas") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = nota, onValueChange = { nota = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.updateGeneralDetails(total.toIntOrNull() ?: 0, horas, "0", nota, cpm) }, modifier = Modifier.fillMaxWidth()) {
            Text("Actualizar Información")
        }

        if (activity.estado != "Finalizado") {
            OutlinedButton(onClick = { viewModel.finalizeActivity() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) {
                Text("Finalizar Actividad")
            }
        }
    }
}

@Composable
fun QualityControlTab(activity: ActivityEntity, viewModel: ActivityDetailsViewModel) {
    var okCountStr by remember { mutableStateOf(activity.cantidadOk.toString()) }
    val editedDefects = remember(activity.defectos) {
        mutableStateMapOf<String, String>().apply {
            activity.defectos.forEach { put(it.name, it.count.toString()) }
        }
    }
    var newDefectName by remember { mutableStateOf("") }
    var isAddingDefect by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // 1. INDICADORES SUPERIORES
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Piezas OK", fontSize = 12.sp, color = Color(0xFF2E7D32))
                    Text("${activity.cantidadOk}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Piezas NO OK", fontSize = 12.sp, color = Color.Red)
                    Text("${activity.cantidadNoOk}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Red)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 2. BARRA DE PROGRESO
        val animatedProgress by animateFloatAsState(targetValue = activity.progreso / 100f, label = "progreso")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Progreso del Sorteo", style = MaterialTheme.typography.titleSmall)
            Text("${activity.progreso}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = Color(0xFF4CAF50),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        Spacer(Modifier.height(24.dp))

        // 3. DESGLOSE DE DEFECTOS
        Text("Desglose de Defectos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

        LazyColumn(Modifier.weight(1f)) {
            items(activity.defectos) { defect ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(defect.name, Modifier.weight(1f))
                    OutlinedTextField(
                        value = editedDefects[defect.name] ?: "0",
                        onValueChange = { if (it.all { c -> c.isDigit() }) editedDefects[defect.name] = it },
                        modifier = Modifier.width(100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
            item {
                if (isAddingDefect) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(value = newDefectName, onValueChange = { newDefectName = it }, label = { Text("Nuevo Defecto") }, modifier = Modifier.weight(1f))
                        IconButton(onClick = { if (newDefectName.isNotBlank()){ viewModel.addNewDefectType(newDefectName); isAddingDefect=false; newDefectName="" } }) {
                            Icon(Icons.Default.Check, contentDescription = "Guardar", tint = Color.Green)
                        }
                    }
                } else {
                    TextButton(onClick = { isAddingDefect = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Añadir tipo de defecto")
                    }
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        // 4. CONTADOR MANUAL DE PIEZAS OK
        Text("Captura de Producción", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = okCountStr,
            onValueChange = { if (it.all { c -> c.isDigit() }) okCountStr = it },
            label = { Text("Total de Piezas OK") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50)) }
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val okInt = okCountStr.toIntOrNull() ?: 0
                val defectList = activity.defectos.map { it.copy(count = editedDefects[it.name]?.toIntOrNull() ?: 0) }
                viewModel.updateQualityData(okInt, defectList)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Guardar Cambios de Calidad")
        }
    }
}

@Composable
fun TimerTab(activity: ActivityEntity, viewModel: ActivityDetailsViewModel) {
    // Calculamos el tiempo transcurrido localmente basándonos en la hora de inicio de Firestore
    var secondsElapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(activity.timerActive, activity.timerStartTime) {
        if (activity.timerActive && activity.timerStartTime != null) {
            while (true) {
                val now = System.currentTimeMillis()
                val start = activity.timerStartTime.time
                secondsElapsed = (now - start) / 1000
                delay(1000L)
            }
        } else {
            secondsElapsed = 0L
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (activity.timerActive) "TRABAJO EN CURSO" else "SESIÓN PAUSADA",
            style = MaterialTheme.typography.labelLarge,
            color = if (activity.timerActive) Color(0xFF4CAF50) else Color.Gray
        )

        Text(
            text = String.format("%02d:%02d:%02d", secondsElapsed / 3600, (secondsElapsed % 3600) / 60, secondsElapsed % 60),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!activity.timerActive) {
                Button(
                    onClick = { viewModel.startTimerAndSetInProgress() },
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Iniciar Cronómetro")
                }
            } else {
                Button(
                    onClick = { viewModel.pauseTimerAndSave() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pausar y Registrar")
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Horas Acumuladas", style = MaterialTheme.typography.titleMedium)
                // Mostramos con decimales (Double)
                Text(
                    text = "${String.format("%.4f", activity.horasAcumuladas)} hrs",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun PeopleTab(activity: ActivityEntity, viewModel: ActivityDetailsViewModel) {
    var newPerson by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = newPerson,
            onValueChange = { newPerson = it },
            label = { Text("Nombre del Inspector") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { if(newPerson.isNotBlank()){ viewModel.addPersonToActivity(newPerson); newPerson = "" } }) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir")
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        Text("Personal Asignado", fontWeight = FontWeight.Bold)
        LazyColumn {
            items(activity.people) { person ->
                ListItem(
                    headlineContent = { Text(person) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.removePersonFromActivity(person) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HistoryTab(history: List<HistoryEntry>) {
    val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
        items(history) { entry ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(entry.userName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Text(sdf.format(entry.timestamp.toDate()), fontSize = 10.sp, color = Color.Gray)
                    }
                    Text("${entry.field}: ${entry.oldValue} -> ${entry.newValue}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}