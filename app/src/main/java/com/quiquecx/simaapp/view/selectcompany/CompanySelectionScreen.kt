package com.quiquecx.simaapp.view.selectcompany

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quiquecx.simaapp.domain.entity.CompanyEntity // 👈 IMPORTAR ESTO
import com.quiquecx.simaapp.ui.theme.SimaAppTheme

@Composable
fun CompanySelectionScreen(
    // 🛑 Eliminamos 'companiesFromLogin' ya que el VM carga los datos
    onNavigateToHome: () -> Unit,
    // ✅ Usamos el nombre del ViewModel que carga desde la BD
    viewModel: SelectCompanyViewModel = hiltViewModel()
) {
    // ✅ Usamos SelectCompanyUiState
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 💡 Lógica de navegación: Navega cuando el VM indica que la selección está completa.
    LaunchedEffect(uiState.selectionComplete) {
        if (uiState.selectionComplete) {
            onNavigateToHome()
        }
    }

    CompanySelectionContent(
        uiState = uiState,
        // ✅ Ahora enviamos el objeto CompanyEntity completo a la función de selección
        onCompanySelected = { viewModel.selectCompany(it) }
        // 🛑 Eliminamos onConfirm, ya que la selección/navegación sucede al hacer clic en la tarjeta
    )
}

@Composable
fun CompanySelectionContent(
    // ✅ Usamos SelectCompanyUiState
    uiState: SelectCompanyUiState,
    // ✅ La selección recibe el objeto CompanyEntity
    onCompanySelected: (CompanyEntity) -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7F7))
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SELECCIONA UNA EMPRESA",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "\"La calidad no es un acto, es un hábito\"",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )
            Spacer(Modifier.height(24.dp))

            // 🛑 Lógica de estado y error
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFEC221F))
                }
            } else if (uiState.errorMessage != null) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Error: ${uiState.errorMessage}", color = Color.Red)
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // ✅ Iteramos sobre objetos CompanyEntity
                    items(uiState.companies) { company ->
                        // La selección se compara con el ID
                        val isSelected = uiState.selectedCompany == company.id
                        CompanyCard(
                            company = company,
                            isSelected = isSelected,
                            onClick = { onCompanySelected(company) } // Enviamos el objeto
                        )
                    }
                }

                // Si la selección ocurre al hacer clic en la tarjeta, el botón 'Continuar' no es necesario.
                // Si lo quieres mantener, se activa si hay una compañía seleccionada.
                Spacer(Modifier.height(32.dp))
                uiState.errorMessage?.let { msg ->
                    Spacer(Modifier.height(8.dp))
                    Text(text = msg, color = Color.Red)
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// COMPONENTE AUXILIAR (Extraído para claridad y reuso)
// --------------------------------------------------------------------------

@Composable
fun CompanyCard(company: CompanyEntity, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(160.dp)
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFEC221F) else Color.White
        )
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = company.name.uppercase(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isSelected) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}


// --------------------------------------------------------------------------
// PREVIEW
// --------------------------------------------------------------------------

private val mockCompanies = listOf(
    // ✅ CORRECCIÓN: Agregar el parámetro 'responsible' (o el que falte)
    CompanyEntity(id = "1", name = "BorgWarner", responsible = "Quique"),
    CompanyEntity(id = "2", name = "HELLA", responsible = "Quique"),
    CompanyEntity(id = "3", name = "CHENSON", responsible = "Quique")
)

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun CompanySelectionPreview() {
    SimaAppTheme {
        CompanySelectionContent(
            uiState = SelectCompanyUiState(
                companies = mockCompanies,
                isLoading = false,
                selectedCompany = "2"
            ),
            onCompanySelected = {}
        )
    }
}