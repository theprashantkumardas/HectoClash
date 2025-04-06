package com.example.hectoclashapp.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hectoclash.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ProfileViewModel(private val tokenManager: TokenManager) : ViewModel() {

    private val _userName = MutableStateFlow<String?>(null)
    val userName = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail = _userEmail.asStateFlow()

    private val _playerId = MutableStateFlow<String?>(null)
    val playerId = _playerId.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _userName.value = tokenManager.getUserName.firstOrNull()
            _userEmail.value = tokenManager.getUserEmail.firstOrNull()
            _playerId.value = tokenManager.getPlayerId.firstOrNull()
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            tokenManager.clearData()
            onLogoutSuccess()  // Trigger navigation back to login screen
        }
    }
}

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(TokenManager.getInstance(context)) as T
    }
}