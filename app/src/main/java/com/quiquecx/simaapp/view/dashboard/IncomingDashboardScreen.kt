package com.quiquecx.simaapp.view.dashboard

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quiquecx.simaapp.domain.entity.ActivityEntity

private const val TAG = "IncomingDashboard"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingDashboardScreen(
    viewModel: IncomingDashboardViewModel = hiltViewModel(),
    // Funciones de navegación
    onNavigateToCreate: () -> Unit,
    onNavigateToDetails: (activityId: String) -> Unit
) {
    // Escuchar la lista de actividades y los KPIs en tiempo real
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val kpis by viewModel.kpis.collectAsStateWithLifecycle()

    // Filtrar actividades en curso para mostrarlas primero
    val sortedActivities = activities.sortedBy { it.estado == "Finalizado" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SIMA - Incoming (Supervisor)") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(Icons.Filled.Add, contentDescription = "Crear nueva actividad")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // --- 1. Indicadores (KPIs) ---
            KpiSection(kpis = kpis)

            Spacer(modifier = Modifier.height(16.dp))

            // --- 2. Lista de Actividades en Tiempo Real ---
            Text(
                "Actividades Recientes (${activities.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (activities.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay actividades registradas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedActivities, key = { it.id }) { activity ->
                        ActivityListItem(
                            activity = activity,
                            onClick = {
                                // Logueamos para asegurar que el click funciona y qué id se pasa
                                Log.d(TAG, "Clicked activity id=${activity.id}")
                                // Navegación: pasamos el id crudo aquí; NavigationWrapper debería codificarlo (URLEncoder) antes de formar la ruta
                                onNavigateToDetails(activity.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KpiSection(kpis: KpiData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
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

@Composable
fun ActivityListItem(activity: ActivityEntity, onClick: () -> Unit) {
    val statusColor = if (activity.estado == "Finalizado") Color.Green else Color.Red

    // Card con onClick (Material3 soporta esto). Si usas otra versión, usa Modifier.clickable.
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.tipo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Material: ${activity.materialId} | Proveedor: ${activity.proveedorId}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${activity.progreso}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                    Text(
                        text = activity.estado,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }

            // Barra de progreso visual CORREGIDA: pasar Float directo (no lambda), altura mayor para visibilidad
            LinearProgressIndicator(
                progress = activity.progreso / 100f,
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = statusColor,
                trackColor = ProgressIndicatorDefaults.linearTrackColor
            )
        }
    }
}