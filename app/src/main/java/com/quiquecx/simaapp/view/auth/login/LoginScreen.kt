package com.quiquecx.simaapp.view.auth.login

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quiquecx.simaapp.R
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = hiltViewModel(),
    navigateToRegister: () -> Unit = {},
    navigateToHome: () -> Unit = {}
) {
    val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()


    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
        uiState.successMessage?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(it)
                navigateToHome() // 🔹 Aquí podrías navegar a otra pantalla
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            if (maxWidth < 600.dp) {
                // Pantalla de celular
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    LoginImage(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    LoginForm(
                        uiState = uiState,
                        loginViewModel = loginViewModel,
                        navigateToRegister = navigateToRegister,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            } else {
                // Pantalla de tablet
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LoginImage(modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(32.dp))
                    LoginForm(
                        uiState = uiState,
                        loginViewModel = loginViewModel,
                        navigateToRegister = navigateToRegister,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun LoginImage(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.logologin),
        contentDescription = "Login Illustration",
        modifier = modifier.fillMaxHeight(0.5f)
    )
}

@Composable
fun LoginForm(
    uiState: LoginUiState,
    loginViewModel: LoginViewModel,
    modifier: Modifier = Modifier,
    navigateToRegister: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Inicio de sesión",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Creando éxitos con valor",
            style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = { loginViewModel.onEmailChange(it) },
            placeholder = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = { loginViewModel.onPasswordChange(it) },
            placeholder = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { loginViewModel.onLoginClick() },
            enabled = uiState.isLoginEnabled && !uiState.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC221F)),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(4.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Comencemos", color = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { navigateToRegister() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(4.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Registrarse", color = Color.White)
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}
