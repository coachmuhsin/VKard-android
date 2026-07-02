package com.vkard.pro.presentation.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vkard.pro.domain.repository.AuthRepository
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    data class Success(val role: String) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var uiState by mutableStateOf<LoginUiState>(LoginUiState.Idle)
        private set
        
    fun login() {
        if (email.isBlank() || password.isBlank()) {
            uiState = LoginUiState.Error("Email and password cannot be empty.")
            return
        }
        
        uiState = LoginUiState.Loading
        viewModelScope.launch {
            authRepository.login(email.trim(), password)
                .onSuccess { user ->
                    uiState = LoginUiState.Success(user.role)
                }
                .onFailure { error ->
                    uiState = LoginUiState.Error(error.message ?: "Authentication failed.")
                }
        }
    }
    
    fun clearError() {
        uiState = LoginUiState.Idle
    }
}
