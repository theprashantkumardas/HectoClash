package com.example.hectoclash.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hectoclash.data.local.TokenManager
import com.example.hectoclash.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.getInstance(application)
    private val tokenManager = TokenManager.getInstance(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            if (email.isEmpty() || password.isEmpty()) {
                _authState.value = AuthState.Error("Email and password cannot be empty")
                return@launch
            }

            try {
                val result = authRepository.signIn(email, password)
                result.fold(
                    onSuccess = { response ->
                        tokenManager.saveUserData(
                            userId = response._id,
                            name = response.name,
                            email = response.email,
                            playerId = response.playerId,
                            token = response.token
                        )
                        // Connect WebSocket when logged in
                        SocketManager.connect(response._id)

                        _authState.value = AuthState.Success("Sign in successful")
                    },
                    onFailure = { exception ->
                        _authState.value = AuthState.Error(exception.message ?: "Sign in failed")
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }


    fun signUp(name: String, playerId: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            // Basic validation
            val invalidFields = mutableListOf<String>()
            if (name.isEmpty()) invalidFields.add("Name")
            if (playerId.isEmpty()) invalidFields.add("Player ID")
            if (email.isEmpty()) invalidFields.add("Email")
            if (password.isEmpty()) invalidFields.add("Password")

            if (invalidFields.isNotEmpty()) {
                _authState.value = AuthState.Error("${invalidFields.joinToString(", ")} cannot be empty")
                return@launch
            }

            try {
                val result = authRepository.signUp(name, playerId, email, password)
                result.fold(
                    onSuccess = { response ->
                        tokenManager.saveUserData(
                            userId = response._id,
                            name = response.name,
                            email = response.email,
                            playerId = response.playerId,
                            token = response.token
                        )

                        // Connect WebSocket when logged in
                        SocketManager.connect(response._id)

                        _authState.value = AuthState.Success("Account created successfully")
                    },
                    onFailure = { exception ->
                        _authState.value = AuthState.Error(exception.message ?: "Sign up failed")
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearData()
            _authState.value = AuthState.Idle
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}