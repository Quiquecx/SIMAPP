package com.quiquecx.simaapp.view.selectcompany

import com.quiquecx.simaapp.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quiquecx.simaapp.domain.entity.CompanyEntity // 👈 IMPORTAR ESTO
import com.quiquecx.simaapp.ui.theme.SimaAppTheme

@Composable
fun CompanySelectionScreen(
    onNavigateToHome: () -> Unit,
    viewModel: SelectCompanyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navegación automática al seleccionar
    LaunchedEffect(uiState.selectionComplete) {
        if (uiState.selectionComplete) {
            onNavigateToHome()
        }
    }

    // Llamamos a la UI real
    CompanySelectionContent(
        uiState = uiState,
        onCompanySelected = { viewModel.selectCompany(it) }
    )
}

// 2. ESTA ES LA UI PURA
@Composable
fun CompanySelectionContent(
    uiState: SelectCompanyUiState,
    onCompanySelected: (CompanyEntity) -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Image(
                painter = painterResource(id = R.drawable.simalogo), // Sin la extensión .png
                contentDescription = "Logo SIMA",
                modifier = Modifier
                    .size(120.dp) // Ajusté un poco el tamaño para que luzca mejor un logo
                    .padding(bottom = 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Bienvenido a SIMA",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                text = "Selecciona tu centro de trabajo",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )

            Spacer(Modifier.height(32.dp))

            if (uiState.isLoading) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFEC221F))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.companies) { company ->
                        val isSelected = uiState.selectedCompany == company.id
                        EnhancedCompanyCard(
                            company = company,
                            isSelected = isSelected,
                            onClick = { onCompanySelected(company) }
                        )
                    }
                }
            }

            Text(
                text = "\"La calidad no es un acto, es un hábito\"",
                style = MaterialTheme.typography.labelMedium.copy(color = Color.LightGray),
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }
    }
}

@Composable
fun EnhancedCompanyCard(company: CompanyEntity, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Color(0xFFEC221F) else Color.Transparent
    val backgroundColor = if (isSelected) Color(0xFFFFF1F0) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, borderColor) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFFEC221F) else Color(0xFFF1F1F1)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = company.name.take(1).uppercase(),
                        color = if (isSelected) Color.White else Color.DarkGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(company.name, fontWeight = FontWeight.Bold)
                Text(
                    "Responsable: ${company.responsible}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFEC221F))
            }
        }
    }
}
