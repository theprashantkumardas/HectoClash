package com.example.hectoclash.viewmodels

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hectoclash.data.local.TokenManager
import com.example.hectoclash.data.models.User
import com.example.hectoclash.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle

// --- Define UI State ---
sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: User) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(application: Application, private val tokenManager: TokenManager) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow() // Use asStateFlow

    private val _userName = MutableStateFlow<String?>(null)
    val userName = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail = _userEmail.asStateFlow()

    private val _playerId = MutableStateFlow<String?>(null)
    val playerId = _playerId.asStateFlow()

    val cachedUserName: StateFlow<String?> = tokenManager.getUserName.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val cachedPlayerId: StateFlow<String?> = tokenManager.getPlayerId.stateIn(viewModelScope, SharingStarted.Lazily, null)

//    init {
//        loadUserData()
//    }
    init {
        fetchUserProfile()
    }


//    private fun loadUserData() {
//        viewModelScope.launch {
//            _userName.value = tokenManager.getUserName.firstOrNull()
//            _userEmail.value = tokenManager.getUserEmail.firstOrNull()
//            _playerId.value = tokenManager.getPlayerId.firstOrNull()
//        }
//    }
//
//    fun logout(onLogoutSuccess: () -> Unit) {
//        viewModelScope.launch {
//            tokenManager.clearData()
//            onLogoutSuccess()  // Trigger navigation back to login screen
//        }
//    }
//}
//
//class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        return ProfileViewModel(TokenManager.getInstance(context)) as T
//    }
//}

    fun fetchUserProfile() {
        Log.d("ProfileViewModel", "Attempting to fetch user profile...")
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                // Use the AUTHENTICATED service
                val apiService = RetrofitClient.getAuthenticatedApiService(getApplication())
                val response = apiService.getMyProfile() // Ensure this uses the correct endpoint

                if (response.isSuccessful && response.body() != null) {
                    val userProfile = response.body()!!
                    Log.d("ProfileViewModel", "Profile fetched successfully: ${userProfile.name}")
                    _uiState.value = ProfileUiState.Success(userProfile)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to fetch profile (${response.code()})"
                    Log.e("ProfileViewModel", "Error fetching profile: $errorMsg")
                    _uiState.value = ProfileUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception fetching profile", e)
                _uiState.value = ProfileUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            Log.d("ProfileViewModel", "Clearing user data and logging out.")
            tokenManager.clearData()
            onLogoutComplete()
        }
    }

    // Helper to format date safely
    @RequiresApi(Build.VERSION_CODES.O)
    fun formatJoinDate(isoDate: String?): String {
        if (isoDate == null) return "N/A"
        return try {
            OffsetDateTime.parse(isoDate)
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        } catch (e: DateTimeParseException) {
            Log.w("ProfileViewModel", "Error parsing date: $isoDate", e)
            "Invalid Date" // Or return the original string, or "N/A"
        } catch (e: Exception) { // Catch other potential errors
            Log.e("ProfileViewModel", "Unexpected error formatting date: $isoDate", e)
            "Error"
        }
    }

    // --- Calculation Helpers (can be expanded) ---
    fun calculateWinRate(wins: Int, totalGames: Int): Float? {
        if (totalGames == 0) return null
        return (wins.toFloat() / totalGames.toFloat()) * 100
    }
}

// --- Updated ProfileViewModelFactory ---
class ProfileViewModelFactory(
    private val application: Application // Context is enough via Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Get TokenManager instance here
            return ProfileViewModel(application, TokenManager.getInstance(application)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}