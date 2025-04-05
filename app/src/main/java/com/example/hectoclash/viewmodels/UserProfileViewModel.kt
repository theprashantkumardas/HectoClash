package com.example.hectoclash.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.*

import com.example.hectoclash.data.models.UserProfileResponse
import com.example.hectoclash.data.network.GenericResponse
import com.example.hectoclash.data.repository.FriendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class UserProfileViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle // To get userId from navigation args
) : AndroidViewModel(application) {

    private val friendRepository = FriendRepository.getInstance(application)
    val userId: String = savedStateHandle["userId"] ?: error("userId not found in SavedStateHandle")

    private val _profile = MutableStateFlow<UserProfileResponse?>(null)
    val profile: StateFlow<UserProfileResponse?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // For temporary feedback (e.g., "Request Sent", "Friend Removed")
    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    init {
        fetchUserProfile()
    }

    fun fetchUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            friendRepository.getUserProfile(userId).fold(
                onSuccess = { _profile.value = it },
                onFailure = { handleApiError("Failed to fetch profile", it) }
            )
            _isLoading.value = false
        }
    }

    fun sendFriendRequest() = performFriendAction("Sending request...") {
        friendRepository.sendFriendRequest(userId)
    }

    fun acceptFriendRequest() = performFriendAction("Accepting request...") {
        friendRepository.acceptFriendRequest(userId)
    }

    // Handles reject, cancel, remove
    fun removeFriend(actionVerb: String = "Removing friend...") = performFriendAction(actionVerb) {
        friendRepository.removeFriend(userId)
    }

    private fun performFriendAction(loadingMessage: String, action: suspend () -> Result<GenericResponse>) {
        viewModelScope.launch {
            _isLoading.value = true // Indicate loading specifically for the action
            _error.value = null
            _actionFeedback.value = loadingMessage // Show immediate feedback

            action().fold(
                onSuccess = {
                    _actionFeedback.value = it.message // Show success message from server
                    fetchUserProfile() // Refresh profile to update friendship status
                },
                onFailure = { handleApiError("Action failed", it) }
            )
            _isLoading.value = false // Action finished
            // Optional: Clear feedback after a delay
            kotlinx.coroutines.delay(3000)
            if (_actionFeedback.value?.startsWith(loadingMessage.substringBefore("...")) == true) {
                _actionFeedback.value = null
            }
        }
    }

    private fun handleApiError(defaultMessage: String, throwable: Throwable) {
        val errorMsg = if (throwable is HttpException) {
            try {
                // Attempt to parse error body, fallback to default message
                val errorJson = throwable.response()?.errorBody()?.string()
                errorJson?.let { org.json.JSONObject(it).getString("message") } ?: throwable.message()
            } catch (e: Exception) {
                throwable.message() ?: defaultMessage
            }
        } else {
            throwable.message ?: defaultMessage
        }
        _error.value = errorMsg
        Log.e("UserProfileViewModel", "$defaultMessage: $errorMsg", throwable)
    }

    fun clearError() { _error.value = null }
    fun clearActionFeedback() { _actionFeedback.value = null }
}

// Factory for UserProfileViewModel
class UserProfileViewModelFactory(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserProfileViewModel(application, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}