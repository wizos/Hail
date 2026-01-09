package com.aistra.hail.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistra.hail.app.HailData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class PasswordEntry(val error: String? = null) : AuthState()
    object BiometricPrompt : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState = _uiState.asStateFlow()

    fun startAuthentication() {
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            if (!HailData.biometricLogin) {
                _uiState.value = AuthState.Authenticated
                return@launch
            }

            when (HailData.authType) {
                "biometric" -> _uiState.value = AuthState.BiometricPrompt
                "password" -> _uiState.value = AuthState.PasswordEntry()
                else -> _uiState.value = AuthState.Authenticated // No auth configured
            }
        }
    }

    fun onPasswordEntered(password: String) {
        viewModelScope.launch {
            if (AuthManager.authenticatePassword(password)) {
                _uiState.value = AuthState.Authenticated
            } else {
                _uiState.value = AuthState.PasswordEntry(error = "密码错误")
            }
        }
    }

    fun onBiometricAuthResult(success: Boolean, errorString: String?) {
        viewModelScope.launch {
            if (success) {
                _uiState.value = AuthState.Authenticated
            } else {
                // Fallback to password entry on biometric error/failure
                _uiState.value = AuthState.PasswordEntry(error = errorString)
            }
        }
    }
}