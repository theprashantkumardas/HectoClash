package com.example.hectoclash.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hectoclash.data.local.TokenManager

import com.example.hectoclash.data.models.OnlineUserResponse
import com.example.hectoclash.data.models.ReceiveChallengeData
import com.example.hectoclash.data.repository.OnlineUsersRepository
import com.example.hectoclash.utils.SocketManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnlineUsersViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OnlineUsersRepository.getInstance(application)
    private val tokenManager = TokenManager.getInstance(application)

    // --- Use StateFlow Consistently ---
    private val _onlineUsers = MutableStateFlow<List<OnlineUserResponse>>(emptyList())
    val onlineUsers: StateFlow<List<OnlineUserResponse>> = _onlineUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _incomingChallenge = MutableStateFlow<ReceiveChallengeData?>(null)
    val incomingChallenge: StateFlow<ReceiveChallengeData?> = _incomingChallenge.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private var listenerJob: Job? = null

    init {
        Log.d("OnlineUsersViewModel", "Initializing and starting listeners.")
        startListeningToSocketEvents()
        fetchOnlineUsers() // Initial fetch
    }

    private fun startListeningToSocketEvents() {
        if (listenerJob?.isActive == true) {
            Log.d("OnlineUsersViewModel", "Listener job already active.")
            return
        }
        Log.d("OnlineUsersViewModel", "Starting SocketManager flow listeners.")
        listenerJob = viewModelScope.launch {
            // Observe Online Users
            launch {
                SocketManager.onlineUsersFlow
                    .catch { e -> Log.e("OnlineUsersViewModel", "Error onlineUsersFlow: ${e.message}") }
                    .collect { users ->
                        Log.d("OnlineUsersViewModel", "Received ${users.size} online users from flow")
                        _onlineUsers.value = users
                        _isLoading.value = false // Stop loading when data arrives via socket
                        _error.value = null // Clear error on success
                    }
            }
            // Observe Incoming Challenges
            launch {
                SocketManager.challengeReceivedFlow
                    .catch { e -> Log.e("OnlineUsersViewModel", "Error challengeReceivedFlow: ${e.message}") }
                    .collect { challenge ->
                        Log.d("OnlineUsersViewModel", "Challenge received from ${challenge.challengerName}")
                        _incomingChallenge.value = challenge
                    }
            }
            // Observe Challenge Rejections
            launch {
                SocketManager.challengeRejectedFlow
                    .catch { e -> Log.e("OnlineUsersViewModel", "Error challengeRejectedFlow: ${e.message}") }
                    .collect { rejection ->
                        Log.d("OnlineUsersViewModel", "Challenge rejected by ${rejection.opponentName}")
                        _feedbackMessage.value = "${rejection.opponentName} declined your challenge."
                    }
            }
            // Observe Challenge Failures
            launch {
                SocketManager.challengeFailedFlow
                    .catch { e -> Log.e("OnlineUsersViewModel", "Error challengeFailedFlow: ${e.message}") }
                    .collect { failure ->
                        Log.w("OnlineUsersViewModel", "Challenge failed: ${failure.reason}")
                        _feedbackMessage.value = "Challenge failed: ${failure.reason}"
                    }
            }
        }
    }

    // Fetch via HTTP for initial load or manual refresh
    fun fetchOnlineUsers() {
        if (_isLoading.value) return
        viewModelScope.launch {
            Log.d("OnlineUsersViewModel", "Fetching online users via HTTP GET...")
            _isLoading.value = true
            _error.value = null
            try {
                repository.getOnlineUsers().fold(
                    onSuccess = { users ->
                        // Don't necessarily overwrite if socket has already provided data,
                        // but useful for manual refresh. Check timestamp? Or just update.
                        _onlineUsers.value = users
                        Log.d("OnlineUsersViewModel", "HTTP GET successful: ${users.size} users")
                    },
                    onFailure = { exception ->
                        val errorMsg = exception.message ?: "Failed to fetch users"
                        _error.value = errorMsg
                        Log.e("OnlineUsersViewModel", "HTTP GET failed: $errorMsg")
                    }
                )
            } catch (e: Exception) {
                val errorMsg = "Network error: ${e.message}"
                _error.value = errorMsg
                Log.e("OnlineUsersViewModel", "HTTP GET exception: $errorMsg", e)
            } finally {
                // Only set loading false here if there was an error,
                // otherwise let the socket flow update handle it.
                if (_error.value != null) _isLoading.value = false
                // Or simply always set it false after HTTP attempt completes:
                // _isLoading.value = false
            }
        }
    }

    // Respond to an incoming challenge
    fun respondToChallenge(challengerId: String, accept: Boolean) {
        Log.d("OnlineUsersViewModel", "Responding to challenge from $challengerId. Accept: $accept")
        _incomingChallenge.value = null // Dismiss dialog
        SocketManager.emitRespondChallenge(challengerId, accept)
    }

    fun clearIncomingChallenge() { _incomingChallenge.value = null }
    fun clearFeedbackMessage() { _feedbackMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        Log.d("OnlineUsersViewModel", "ViewModel cleared. Cancelling listener job.")
        listenerJob?.cancel()
    }
}