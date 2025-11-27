package com.quiquecx.simaapp.view.home

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quiquecx.simaapp.R
import com.quiquecx.simaapp.domain.entity.ProjectEntity
import com.quiquecx.simaapp.view.utils.WindowSizeClass
import com.quiquecx.simaapp.view.utils.rememberWindowSizeClass // 👈 Importamos la utilidad


// Componente principal
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onProjectClick: (String) -> Unit,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = rememberWindowSizeClass() // 👈 Detectamos el ancho de la pantalla

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(
                    horizontal = if (windowSizeClass != WindowSizeClass.Compact) 48.dp else 16.dp, // Más padding en tablet
                    vertical = 24.dp
                )
        ) {
            HeaderSection(windowSizeClass) // Pasamos el ancho para adaptar el título

            Spacer(modifier = Modifier.height(40.dp))

            // LÓGICA DE CARGA Y DISPOSICIÓN ADAPTABLE
            if (uiState.isLoading) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFEC221F))
                }
            } else if (uiState.projects.isEmpty() && uiState.errorMessage == null) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No hay proyectos activos para ${uiState.selectedCompanyId}", color = Color.Gray)
                }
            } else {
                // Si es Compact (Móvil), usamos cuadrícula vertical.
                if (windowSizeClass == WindowSizeClass.Compact) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2), // 2 columnas en móvil
                        contentPadding = PaddingValues(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(uiState.projects) { project ->
                            ProjectCard(project = project, onClick = { onProjectClick(project.id) }, isCompact = true)
                        }
                    }
                } else {
                    // Si es Medium o Expanded (Tablet), usamos LazyRow original (horizontal)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(uiState.projects) { project ->
                            ProjectCard(project = project, onClick = { onProjectClick(project.id) }, isCompact = false)
                        }
                    }
                }
            }

            // Botón Retroceder (Alineación siempre a la derecha)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC221F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Retroceder", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun HeaderSection(windowSizeClass: WindowSizeClass) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { /* Abrir Drawer */ }) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", modifier = Modifier.size(32.dp))
        }

        Column(
            modifier = Modifier.weight(1f), // Toma el espacio restante
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SELECCIONA UN PROYECTO",
                style = if (windowSizeClass == WindowSizeClass.Compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium.copy( // Título más pequeño en móvil
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "\"La mejora continua es mejor que la perfección retrasada\"",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )
        }

        Image(
            imageVector = Icons.Default.Person,
            contentDescription = "User Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(if (windowSizeClass == WindowSizeClass.Compact) 40.dp else 50.dp) // Avatar más pequeño en móvil
                .clip(CircleShape)
                .background(Color.LightGray)
        )
    }
}

@Composable
fun ProjectCard(project: ProjectEntity, onClick: () -> Unit, isCompact: Boolean) {

    // El tamaño de la tarjeta se adapta: pequeño en móvil, grande en tablet.
    val cardModifier = if (isCompact) {
        Modifier.fillMaxWidth().height(180.dp)
    } else {
        Modifier.width(280.dp).height(320.dp)
    }

    val imageRes = when (project.imageType.lowercase()) {
        "incoming" -> R.drawable.ic_launcher_background
        "cadenas" -> R.drawable.ic_launcher_background
        "vcts" -> R.drawable.ic_launcher_background
        else -> R.drawable.ic_launcher_background
    }

    Card(
        modifier = cardModifier
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Imagen del proyecto
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = project.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(if (isCompact) 16.dp else 24.dp)
                )

                // Nombre del proyecto
                Text(
                    text = project.name,
                    style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = if (isCompact) 16.dp else 32.dp)
                )
            }

            // Botón Rojo "+" Flotante
            FloatingActionButton(
                onClick = onClick,
                containerColor = Color(0xFFEC221F),
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(if (isCompact) 40.dp else 48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Entrar")
            }
        }
    }
}


// ---------------------------------------------------------
// PREVIEW SECTION (Adaptado para mostrar ambos tamaños)
// ---------------------------------------------------------

private val mockProjects = listOf(
    ProjectEntity(id = "proy_incoming", name = "Incoming", description = "", status = "Activo", responsible = "A", imageType = "incoming"),
    ProjectEntity(id = "proy_cadenas", name = "Cadenas", description = "", status = "Activo", responsible = "B", imageType = "cadenas"),
    ProjectEntity(id = "proy_vcts", name = "VCTS", description = "", status = "Activo", responsible = "C", imageType = "vcts"),
    ProjectEntity(id = "proy_test", name = "Test", description = "", status = "Activo", responsible = "D", imageType = "incoming")
)

@Preview(
    name = "1. Tablet/Expanded (Horizontal)",
    device = "spec:width=1280dp,height=800dp,dpi=480",
    showBackground = true
)
@Composable
fun PreviewHomeScreenTablet() {
    com.quiquecx.simaapp.ui.theme.SimaAppTheme {
        HomeScreenPreviewContent(WindowSizeClass.Expanded, mockProjects)
    }
}

@Preview(
    name = "2. Móvil/Compact (Vertical Grid)",
    device = "spec:width=411dp,height=891dp,dpi=420",
    showBackground = true
)
@Composable
fun PreviewHomeScreenMobile() {
    com.quiquecx.simaapp.ui.theme.SimaAppTheme {
        HomeScreenPreviewContent(WindowSizeClass.Compact, mockProjects)
    }
}

// Función auxiliar que renderiza el contenido usando el tamaño simulado
@Composable
private fun HomeScreenPreviewContent(sizeClass: WindowSizeClass, projects: List<ProjectEntity>) {
    Scaffold(containerColor = Color(0xFFF5F5F5)) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(
                    horizontal = if (sizeClass != WindowSizeClass.Compact) 48.dp else 16.dp,
                    vertical = 24.dp
                )
        ) {
            HeaderSection(sizeClass)
            Spacer(modifier = Modifier.height(40.dp))

            if (sizeClass == WindowSizeClass.Compact) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(projects) { project ->
                        ProjectCard(project = project, onClick = { }, isCompact = true)
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(projects) { project ->
                        ProjectCard(project = project, onClick = { }, isCompact = false)
                    }
                }
            }
            // Botón Retroceder
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC221F)), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Retroceder", color = Color.White)
                }
            }
        }
    }
}