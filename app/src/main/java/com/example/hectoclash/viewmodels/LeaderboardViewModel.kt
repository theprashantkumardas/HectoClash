package com.example.hectoclash.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hectoclash.data.models.LeaderboardEntry
import com.example.hectoclash.data.network.RetrofitClient // Assuming API is here
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LeaderboardUiState {
    object Loading : LeaderboardUiState()
    data class Success(val leaderboard: List<LeaderboardEntry>) : LeaderboardUiState()
    data class Error(val message: String) : LeaderboardUiState()
}

class LeaderboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val uiState: StateFlow<LeaderboardUiState> = _uiState

    init {
        fetchHectoChallengeLeaderboard() // Fetch on init
    }

    // Renamed function
    fun fetchHectoChallengeLeaderboard() {
        _uiState.value = LeaderboardUiState.Loading
        viewModelScope.launch {
            try {
                // Assuming non-authenticated for global leaderboard
                // Use getAuthenticatedApiService if auth is required
                val apiService = RetrofitClient.apiService  // Or your singleton access

                // Call the NEW API endpoint
                val response = apiService.getHectocChallengeLeaderboard(limit = 50) // Call new function

                if (response.isSuccessful) {
                    _uiState.value = LeaderboardUiState.Success(response.body() ?: emptyList())
                    Log.d("LeaderboardViewModel", "Fetched ${response.body()?.size ?: 0} entries")
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to fetch leaderboard"
                    Log.e("LeaderboardViewModel", "Error fetching leaderboard: $errorMsg")
                    _uiState.value = LeaderboardUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("LeaderboardViewModel", "Exception fetching leaderboard", e)
                _uiState.value = LeaderboardUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}