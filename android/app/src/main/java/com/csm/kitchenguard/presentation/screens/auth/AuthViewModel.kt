package com.csm.kitchenguard.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.csm.kitchenguard.domain.repository.AuthRepository
import com.csm.kitchenguard.utils.result.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Data class merangkum status layar UI (MVI Pattern). */
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

/**
 * ViewModel untuk menangani interaksi pengguna di layar Login.
 * Menyediakan fungsi login yang berkomunikasi dengan Domain Layer (AuthRepository).
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            authRepository.login(identifier, password).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }
                    }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false, error = null, isSuccess = true) }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = resource.message, isSuccess = false) }
                    }
                }
            }
        }
    }
    
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    /** Menampilkan pesan error validasi lokal (mis. field kosong) tanpa memanggil API. */
    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }
}
