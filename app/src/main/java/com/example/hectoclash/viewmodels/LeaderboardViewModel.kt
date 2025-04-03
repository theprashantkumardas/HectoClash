package com.example.hectoclash.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hectoclash.R
import com.example.hectoclash.data.models.LeaderboardEntry
import com.example.hectoclash.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _leaderboardEntries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = _leaderboardEntries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Load initial dummy data while we wait for the API
        _leaderboardEntries.value = getDummyLeaderboardEntries()

        // Load real data from API
        fetchLeaderboard()
    }

    fun fetchLeaderboard() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = RetrofitClient
                    .getAuthenticatedApiService(getApplication())
                    .getLeaderboard()

                if (response.isSuccessful) {
                    response.body()?.let { leaderboardResponse ->
                        if (leaderboardResponse.success) {
                            // Map server response to UI model and sort by score (points)
                            val entries = leaderboardResponse.topPlayers.mapIndexed { index, data ->
                                LeaderboardEntry(
                                    rank = index + 1, // Calculate rank based on sorted position
                                    playerId = data.user.playerId,
                                    profilePicRes = R.drawable.default_profile, // Use default profile pic
                                    matches = data.totalgames,
                                    won = data.totalwins,
                                    loss = data.totalgames - data.totalwins,
                                    points = data.score
                                )
                            }
                            _leaderboardEntries.value = entries
                        } else {
                            _error.value = "Failed to fetch leaderboard data"
                        }
                    } ?: run {
                        _error.value = "Empty response body"
                    }
                } else {
                    _error.value = "Error ${response.code()}: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Dummy data for initial display
    private fun getDummyLeaderboardEntries(): List<LeaderboardEntry> {
        return listOf(
            LeaderboardEntry(1, "Pro_Player1", R.drawable.default_profile, 120, 85, 35, 2500),
            LeaderboardEntry(2, "GameMaster", R.drawable.default_profile, 110, 75, 35, 2350),
            LeaderboardEntry(3, "ChampionGirl", R.drawable.default_profile, 95, 68, 27, 2100),
            LeaderboardEntry(4, "WinnerX", R.drawable.default_profile, 105, 65, 40, 1950),
            LeaderboardEntry(5, "TopPlayer", R.drawable.default_profile, 90, 60, 30, 1800),
            LeaderboardEntry(6, "GamerKid", R.drawable.default_profile, 80, 52, 28, 1650),
            LeaderboardEntry(7, "MonuGit9", R.drawable.default_profile, 75, 48, 27, 1500),
            LeaderboardEntry(8, "LeaderPro", R.drawable.default_profile, 70, 42, 28, 1350),
            LeaderboardEntry(9, "StarGamer", R.drawable.default_profile, 65, 38, 27, 1200),
            LeaderboardEntry(10, "ProGamer123", R.drawable.default_profile, 60, 35, 25, 1050)
        )
    }
}