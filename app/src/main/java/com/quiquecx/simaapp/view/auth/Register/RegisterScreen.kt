package com.quiquecx.simaapp.view.auth.register

import com.quiquecx.simaapp.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RegisterScreen(
    registerViewModel: RegisterViewModel = viewModel(),
    navigateBack: () -> Unit = {}
) {
    // 👇 Obtén el estado del ViewModel
    val uiState by registerViewModel.uiState.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (maxWidth < 600.dp) {
            // Celular
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                RegisterImage(modifier = Modifier.weight(1f).fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                RegisterForm(
                    uiState = uiState,
                    registerViewModel = registerViewModel,
                    modifier = Modifier.weight(2f),
                    navigateBack = navigateBack
                )
            }
        } else {
            // Tablet
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RegisterImage(modifier = Modifier.weight(1f))
                Spacer(Modifier.width(32.dp))
                RegisterForm(
                    uiState = uiState,
                    registerViewModel = registerViewModel,
                    modifier = Modifier.weight(1f),
                    navigateBack = navigateBack
                )
            }
        }
    }
}

@Composable
fun RegisterImage(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.simalogo),
        contentDescription = "Register Illustration",
        modifier = modifier.fillMaxHeight(0.5f)
    )
}

@Composable
fun RegisterForm(
    uiState: RegisterUiState,
    registerViewModel: RegisterViewModel,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Crear cuenta",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Únete y comienza a gestionar",
            style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
        )
        Spacer(Modifier.height(32.dp))

        // Campos conectados directamente al ViewModel
        OutlinedTextField(
            value = uiState.name,
            onValueChange = registerViewModel::onNameChange,
            placeholder = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = registerViewModel::onEmailChange,
            placeholder = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = registerViewModel::onPasswordChange,
            placeholder = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.role,
            onValueChange = registerViewModel::onRoleChange,
            placeholder = { Text("Rol (ej. Operador, Supervisor)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { /* Acción de registro */ },
            enabled = uiState.isRegisterEnabled,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC221F)),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(4.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Registrarse", color = Color.White)
        }

        Spacer(Modifier.height(1.dp))

        TextButton(
            onClick = navigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "¿Ya tienes cuenta? Inicia sesión",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Preview
@Composable
fun RegisterScreenPreview() {
    RegisterScreen()
}

