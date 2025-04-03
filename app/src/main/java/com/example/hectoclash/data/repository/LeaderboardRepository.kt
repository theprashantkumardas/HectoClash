package com.example.hectoclash.data.repository

import android.content.Context
import com.example.hectoclash.data.models.LeaderboardEntryResponse
import com.example.hectoclash.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LeaderboardRepository(private val context: Context) {

    private val _leaderboardData = MutableStateFlow<List<LeaderboardEntryResponse>>(emptyList())
    val leaderboardData: StateFlow<List<LeaderboardEntryResponse>> = _leaderboardData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    companion object {
        @Volatile
        private var INSTANCE: LeaderboardRepository? = null
        
        fun getInstance(context: Context): LeaderboardRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LeaderboardRepository(context).also { INSTANCE = it }
            }
        }
    }


    suspend fun fetchLeaderboard() {
        _isLoading.value = true
        _error.value = null
        
        withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient
                    .getAuthenticatedApiService(context)
                    .getLeaderboard()
                
                if (response.isSuccessful) {
                    response.body()?.let { leaderboardResponse ->
                        if (leaderboardResponse.success) {
                            _leaderboardData.value = leaderboardResponse.topPlayers
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


}