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
import com.quiquecx.simaapp.ui.theme.SimaAppTheme

@Composable
fun CompanySelectionScreen(
    companiesFromLogin: List<String>,
    onNavigateToHome: () -> Unit,
    viewModel: CompanySelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.setCompanies(companiesFromLogin)
        // If auto-selected (single company), confirm automatically
        uiState.selectedCompany?.let {
            if (companiesFromLogin.size == 1) {
                viewModel.confirmSelection(onNavigateToHome)
            }
        }
    }

    CompanySelectionContent(
        uiState = uiState,
        onCompanySelected = { viewModel.onCompanySelected(it) },
        onConfirm = { viewModel.confirmSelection(onNavigateToHome) }
    )
}

@Composable
fun CompanySelectionContent(
    uiState: CompanySelectionUiState,
    onCompanySelected: (String) -> Unit,
    onConfirm: () -> Unit
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

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.companies) { company ->
                    val isSelected = uiState.selectedCompany == company
                    Card(
                        modifier = Modifier
                            .width(200.dp)
                            .height(160.dp)
                            .shadow(6.dp, RoundedCornerShape(12.dp))
                            .clickable { onCompanySelected(company) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFEC221F) else Color.White
                        )
                    ) {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Reemplazar Text por Image si tienes logo
                            Text(
                                text = company.uppercase(),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = if (isSelected) Color.White else Color.Black
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onConfirm,
                enabled = uiState.selectedCompany != null && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC221F)),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Continuar", color = Color.White)
                }
            }
            uiState.errorMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(text = msg, color = Color.Red)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun CompanySelectionPreview() {
    SimaAppTheme {
        CompanySelectionContent(
            uiState = CompanySelectionUiState(
                companies = listOf("BorgWarner", "HELLA", "CHENSON"),
                selectedCompany = null,
                isLoading = false
            ),
            onCompanySelected = {},
            onConfirm = {}
        )
    }
}