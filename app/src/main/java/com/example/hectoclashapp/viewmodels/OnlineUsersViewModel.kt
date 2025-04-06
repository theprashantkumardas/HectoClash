package com.example.hectoclashapp.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hectoclash.data.local.TokenManager

import com.example.hectoclash.data.models.OnlineUserResponse
import com.example.hectoclash.data.repository.OnlineUsersRepository
import com.example.hectoclash.utils.SocketManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnlineUsersViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OnlineUsersRepository.getInstance(application)
    private val tokenManager = TokenManager.getInstance(application)
    private val userPreferences = TokenManager.getInstance(application)

    private val _onlineUsers = MutableLiveData<List<OnlineUserResponse>>()
    val onlineUsers: LiveData<List<OnlineUserResponse>> = _onlineUsers

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        // Setup socket callback
        // Setup socket callback
        SocketManager.setOnlineUsersCallback { users -> // Callback now receives List<OnlineUserResponse>
            Log.d("OnlineUsersViewModel", "Received users from socket: ${users.size}")
            // Directly update the LiveData with the list from the socket
            _onlineUsers.postValue(users) // Use postValue if called from background thread in SocketManager
            _isLoading.postValue(false) // Ensure loading is stopped
            _error.postValue(null) // Clear any previous error
        }

        // Connect to socket if we have userId
        viewModelScope.launch {
            val userId = userPreferences.getUserId.first()
            if (!userId.isNullOrEmpty()) {
                SocketManager.connect(userId)
            }
        }
    }

    // Keep fetchOnlineUsers for initial load or manual refresh
    fun fetchOnlineUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d("OnlineUsersViewModel", "Fetching online users via HTTP GET")
            try {
                repository.getOnlineUsers().fold(
                    onSuccess = { users ->
                        _onlineUsers.value = users
                        Log.d("OnlineUsersViewModel", "HTTP GET successful: ${users.size} users")
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to fetch users"
                        Log.e("OnlineUsersViewModel", "HTTP GET failed: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                Log.e("OnlineUsersViewModel", "HTTP GET exception: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // No need to disconnect here as SocketManager is an object
        // and might be used by other screens
    }
}