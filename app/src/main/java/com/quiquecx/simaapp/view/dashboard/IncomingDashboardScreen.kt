package com.quiquecx.simaapp.view.dashboard

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingDashboardScreen(
    viewModel: IncomingDashboardViewModel = hiltViewModel(),
    onNavigateToCreate: () -> Unit,
    onNavigateToDetails: (activityId: String) -> Unit,
    onBack: () -> Unit
) {
    // Escuchamos actividades filtradas, KPIs y la consulta de búsqueda
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val kpis by viewModel.kpis.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // Ordenamiento: Activas primero, finalizadas al último (dentro de lo filtrado)
    val sortedActivities = remember(activities) {
        activities.sortedWith(
            compareByDescending<ActivityEntity> { it.timerActive }
                .thenBy { it.estado == "Finalizado" }
        )
    }

    // Buscamos si existe alguna actividad corriendo para mostrar el Banner
    val activeActivity = activities.find { it.timerActive }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SIMA - Incoming", fontWeight = FontWeight.Bold) },
                // 2. Agregamos el icono de navegación hacia atrás
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        floatingActionButton = {
            // Animamos la posición del FAB para que no choque con el banner de tiempo
            val fabPadding by animateDpAsState(
                targetValue = if (activeActivity != null) 80.dp else 0.dp,
                label = "fabAnimation"
            )
            FloatingActionButton(
                onClick = onNavigateToCreate,
                modifier = Modifier.padding(bottom = fabPadding)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Crear")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 800.dp) // Optimización para TABLET
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Sección de Indicadores
                KpiSection(kpis = kpis)

                // 2. Barra de Búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    placeholder = { Text("Buscar por ID, proveedor o material...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Título dinámico con contador
                Text(
                    text = if (searchQuery.isEmpty()) "Actividades Recientes" else "Resultados encontrados (${activities.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Lista de Actividades
                if (activities.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isEmpty()) "No hay actividades registradas." else "No hay coincidencias con la búsqueda.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Padding inferior amplio para que la última tarjeta no quede bajo el Banner
                        contentPadding = PaddingValues(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedActivities, key = { it.id }) { activity ->
                            ActivityListItem(
                                activity = activity,
                                onClick = { onNavigateToDetails(activity.id) }
                            )
                        }
                    }
                }
            }

            // 5. Banner Flotante (Mini Player de tiempo)
            AnimatedVisibility(
                visible = activeActivity != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                activeActivity?.let {
                    ActiveTimerBanner(
                        activity = it,
                        onClick = { onNavigateToDetails(it.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityListItem(activity: ActivityEntity, onClick: () -> Unit) {
    var secondsElapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(activity.timerActive, activity.timerStartTime) {
        if (activity.timerActive && activity.timerStartTime != null) {
            while (true) {
                val now = System.currentTimeMillis()
                val start = activity.timerStartTime.time
                secondsElapsed = (now - start) / 1000
                delay(1000L)
            }
        }
    }

    val statusColor = if (activity.estado == "Finalizado") Color.Green else Color.Red
    val isRunning = activity.timerActive

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = activity.tipo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Mat: ${activity.materialId} | Prov: ${activity.proveedorId}", style = MaterialTheme.typography.bodySmall)
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (isRunning) {
                        Surface(color = Color(0xFF6200EE), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = String.format("%02d:%02d:%02d", secondsElapsed / 3600, (secondsElapsed % 3600) / 60, secondsElapsed % 60),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(text = "${activity.progreso}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = statusColor)
                    }
                    Text(text = activity.estado, style = MaterialTheme.typography.labelSmall, color = statusColor)
                }
            }

            LinearProgressIndicator(
                progress = { activity.progreso / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = statusColor,
                trackColor = ProgressIndicatorDefaults.linearTrackColor
            )
        }
    }
}

@Composable
fun ActiveTimerBanner(activity: ActivityEntity, onClick: () -> Unit) {
    var seconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(activity.timerStartTime) {
        while (true) {
            seconds = (System.currentTimeMillis() - (activity.timerStartTime?.time ?: System.currentTimeMillis())) / 1000
            delay(1000L)
        }
    }

    Surface(
        modifier = Modifier
            .padding(16.dp)
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() },
        color = Color(0xFF6200EE),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Actividad en curso", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text(activity.cpmId, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(
                text = String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun KpiSection(kpis: KpiData) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        KpiCard(title = "Total", value = kpis.totalActivities.toString(), modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        KpiCard(title = "En Curso", value = kpis.inProgressActivities.toString(), modifier = Modifier.weight(1f), color = Color(0xFF6200EE))
        Spacer(Modifier.width(8.dp))
        KpiCard(title = "Promedio %", value = "${kpis.avgProgress}%", modifier = Modifier.weight(1f), color = Color(0xFF00C853))
    }
}

@Composable
fun KpiCard(title: String, value: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = color)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}