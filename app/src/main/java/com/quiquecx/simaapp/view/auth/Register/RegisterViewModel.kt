package com.quiquecx.simaapp.view.auth.register

import android.util.Patterns
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun onNameChange(newName: String) {
        _uiState.value = _uiState.value.copy(name = newName)
        validateForm()
    }

    fun onEmployeeNumberChange(newNumber: String) {
        _uiState.value = _uiState.value.copy(employeeNumber = newNumber)
        validateForm()
    }

    fun onEmailChange(newEmail: String) {
        _uiState.value = _uiState.value.copy(email = newEmail)
        validateForm()
    }

    fun onRoleChange(newRole: String) {
        _uiState.value = _uiState.value.copy(role = newRole)
        validateForm()
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.value = _uiState.value.copy(password = newPassword)
        validateForm()
    }

    fun onCodeChange(newCode: String) {
        _uiState.value = _uiState.value.copy(code = newCode)
        validateForm()
    }

    private fun validateForm() {
        val state = _uiState.value
        val isValid = state.name.isNotBlank() &&
                state.employeeNumber.isNotBlank() &&
                state.email.contains("@") &&
                state.role.isNotBlank() && // 🔹 Debe elegir un rol
                state.password.length >= 6 &&
                state.code.isNotBlank()

        _uiState.value = _uiState.value.copy(isRegisterEnabled = isValid)
    }
}

data class RegisterUiState(
    val name: String = "",
    val employeeNumber: String = "",
    val email: String = "",
    val role: String = "", // 🔹 Guardamos el rol seleccionado
    val password: String = "",
    val code: String = "",
    val isRegisterEnabled: Boolean = false,

)
