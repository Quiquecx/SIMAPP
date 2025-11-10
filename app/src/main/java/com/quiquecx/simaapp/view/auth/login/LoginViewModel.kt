package com.quiquecx.simaapp.view.auth.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quiquecx.simaapp.domain.useCase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    // 📩 Cuando el usuario cambia el email
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
        verifyLogin()
    }

    // 🔒 Cuando el usuario cambia la contraseña
    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
        verifyLogin()
    }

    // 🚀 Ejecutar el inicio de sesión con Firebase
    fun onLoginClick() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        if (!isEmailValid(email) || !isPasswordValid(password)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            try {
                val user = loginUseCase(email, password)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Bienvenido ${user.name}"
                    )
                }

                // ✅ Limpia el mensaje después de un corto retraso
                kotlinx.coroutines.delay(100)
                _uiState.update { it.copy(successMessage = null) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }


    // Validaciones
    private fun verifyLogin() {
        val enabledLogin = isEmailValid(_uiState.value.email) &&
                isPasswordValid(_uiState.value.password)
        _uiState.update { it.copy(isLoginEnabled = enabledLogin) }
    }

    private fun isEmailValid(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun isPasswordValid(password: String): Boolean =
        password.length > 6
}

// 🧠 Estado de la UI (todo lo que la pantalla necesita saber)
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoginEnabled: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
