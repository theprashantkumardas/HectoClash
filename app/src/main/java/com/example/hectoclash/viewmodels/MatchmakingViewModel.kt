package com.example.hectoclash.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hectoclash.data.models.MatchmakingFailedData
import com.example.hectoclash.utils.SocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed class MatchmakingState {
    object Idle : MatchmakingState()
    object Searching : MatchmakingState()
    data class Failed(val reason: String) : MatchmakingState()
    object Canceled : MatchmakingState()
    // MatchFound state is not needed here, navigation handles it
}

class MatchmakingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<MatchmakingState>(MatchmakingState.Idle)
    val uiState: StateFlow<MatchmakingState> = _uiState

    init {
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            SocketManager.searchingForMatchFlow.collect {
                Log.d("MatchmakingViewModel", "Confirmed: Searching for match")
                _uiState.value = MatchmakingState.Searching
            }
        }
        viewModelScope.launch {
            SocketManager.leftMatchmakingFlow.collect {
                Log.d("MatchmakingViewModel", "Confirmed: Left matchmaking")
                if (_uiState.value == MatchmakingState.Searching) { // Only update if we were searching
                    _uiState.value = MatchmakingState.Canceled
                }
            }
        }
        viewModelScope.launch {
            SocketManager.matchmakingFailedFlow.collect { data ->
                Log.e("MatchmakingViewModel", "Matchmaking failed: ${data.reason}")
                _uiState.value = MatchmakingState.Failed(data.reason)
                // Maybe revert to Idle after a delay?
            }
        }
        // We don't need to collect challengeStartFlow here, AppNavigation handles it.
    }

    fun startMatchmaking() {
        Log.d("MatchmakingViewModel", "Attempting to enter matchmaking")
        // State might become Searching immediately based on server confirmation
        _uiState.value = MatchmakingState.Searching // Optimistic UI update? Or wait for server ack? Let's wait.
        SocketManager.emitEnterMatchmaking()
    }

    fun cancelMatchmaking() {
        Log.d("MatchmakingViewModel", "Attempting to leave matchmaking")
        if (_uiState.value == MatchmakingState.Searching) { // Only leave if currently searching
            SocketManager.emitLeaveMatchmaking()
            // UI state will update based on server confirmation via leftMatchmakingFlow
        }
    }

    override fun onCleared() {
        // If the user leaves the screen *before* cancelling, ensure they leave the queue
        if (_uiState.value == MatchmakingState.Searching) {
            Log.d("MatchmakingViewModel", "ViewModel cleared while searching, leaving queue.")
            SocketManager.emitLeaveMatchmaking()
        }
        super.onCleared()
    }
}