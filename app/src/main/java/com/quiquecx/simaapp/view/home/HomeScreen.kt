package com.quiquecx.simaapp.view.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.quiquecx.simaapp.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quiquecx.simaapp.domain.entity.CompanyEntity
import com.quiquecx.simaapp.domain.entity.ProjectEntity
import com.quiquecx.simaapp.view.utils.WindowSizeClass
import com.quiquecx.simaapp.view.utils.rememberWindowSizeClass
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onProjectClick: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = rememberWindowSizeClass()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = Color.White
            ) {
                DrawerContent(
                    companies = uiState.companies,
                    selectedCompanyId = uiState.selectedCompanyId,
                    onCompanyClick = { company ->
                        viewModel.changeCompany(company) {
                            scope.launch { drawerState.close() }
                        }
                    },
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFF5F5F5)
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(
                        horizontal = if (windowSizeClass != WindowSizeClass.Compact) 48.dp else 16.dp,
                        vertical = 24.dp
                    )
            ) {
                HeaderSection(
                    windowSizeClass = windowSizeClass,
                    viewModel = viewModel,
                    onSignOut = onSignOut,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )

                Spacer(modifier = Modifier.height(40.dp))

                if (uiState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFEC221F))
                    }
                } else if (uiState.projects.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay proyectos para esta empresa", color = Color.Gray)
                    }
                } else {
                    ProjectsGridOrRow(
                        uiState = uiState,
                        windowSizeClass = windowSizeClass,
                        onProjectClick = onProjectClick
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    companies: List<CompanyEntity>,
    selectedCompanyId: String?,
    onCompanyClick: (CompanyEntity) -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MIS EMPRESAS", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(companies) { company ->
                val isSelected = company.id == selectedCompanyId
                NavigationDrawerItem(
                    label = { Text(company.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    selected = isSelected,
                    onClick = { onCompanyClick(company) },
                    icon = { Icon(Icons.Default.Business, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFFFFF1F0),
                        selectedIconColor = Color(0xFFEC221F),
                        selectedTextColor = Color(0xFFEC221F)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Divider(color = Color.LightGray, thickness = 0.5.dp)
        Text(
            text = "SIMA App v1.0.2",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 16.dp, start = 8.dp),
            color = Color.LightGray
        )
    }
}

@Composable
fun HeaderSection(
    windowSizeClass: WindowSizeClass,
    viewModel: HomeViewModel,
    onSignOut: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.Menu, contentDescription = "Menú", modifier = Modifier.size(32.dp))
        }

        Text(
            text = "MIS PROYECTOS",
            style = if (windowSizeClass == WindowSizeClass.Compact)
                MaterialTheme.typography.titleLarge
            else
                MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable { viewModel.signOut(onSignOutSuccess = onSignOut) }
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(Icons.Default.Person, contentDescription = "Logout", modifier = Modifier.align(Alignment.Center))
            Icon(
                Icons.Default.ExitToApp,
                contentDescription = null,
                tint = Color(0xFFEC221F),
                modifier = Modifier.size(16.dp).align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun ProjectsGridOrRow(
    uiState: HomeUiState,
    windowSizeClass: WindowSizeClass,
    onProjectClick: (String) -> Unit
) {
    if (windowSizeClass == WindowSizeClass.Compact) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.projects) { project ->
                // Asumo que tienes tu ProjectCard definida
                ProjectCard(project, onClick = { onProjectClick(project.id) }, isCompact = true)
            }
        }
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.projects) { project ->
                ProjectCard(project, onClick = { onProjectClick(project.id) }, isCompact = false)
            }
        }
    }
}

@Composable
fun ProjectCard(project: ProjectEntity, onClick: () -> Unit, isCompact: Boolean) {

    val cardModifier = if (isCompact) {
        Modifier.fillMaxWidth().height(180.dp)
    } else {
        Modifier.width(280.dp).height(320.dp)
    }

    val imageRes = when (project.imageType.lowercase()) {
        "incoming" -> R.drawable.imgincomingcard
        "cadenas" -> R.drawable.imgcadenascard
        "vcts" -> R.drawable.imgvctscard
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

