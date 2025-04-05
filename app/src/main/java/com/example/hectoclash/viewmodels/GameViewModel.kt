package com.example.hectoclash.viewmodels

import android.app.Application
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hectoclash.data.local.TokenManager
import com.example.hectoclash.data.models.GameOverData
import com.example.hectoclash.data.models.SolutionInvalidData
import com.example.hectoclash.utils.SocketManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle // For receiving navigation arguments
) : AndroidViewModel(application) {

    private val tokenManager = TokenManager.getInstance(application)

    // --- Navigation Arguments ---
    val gameId: String = savedStateHandle["gameId"] ?: error("gameId not provided")
    val initialPuzzle: String = savedStateHandle["puzzle"] ?: error("puzzle not provided")
    val opponentName: String = savedStateHandle["opponentName"] ?: "Opponent"
    val opponentId: String = savedStateHandle["opponentId"] ?: "opponent_id"
    val timeLimitSeconds: Int = savedStateHandle["timeLimitSeconds"] ?: 60

    // --- Game State ---
    private val _puzzle = MutableStateFlow(initialPuzzle)
    val puzzle: StateFlow<String> = _puzzle.asStateFlow()

    private val _timeLeft = MutableStateFlow(timeLimitSeconds * 1000L) // Time left in milliseconds
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    private val _solutionInput = MutableStateFlow("")
    val solutionInput: StateFlow<String> = _solutionInput.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _gameResult = MutableStateFlow<GameOverData?>(null)
    val gameResult: StateFlow<GameOverData?> = _gameResult.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null) // For "Invalid solution" etc.
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private var countdownTimer: CountDownTimer? = null
    private var socketListenerJob: Job? = null
    private var currentUserId: String? = null

    init {
        Log.d("GameViewModel", "Initializing for game: $gameId")
        viewModelScope.launch {
            currentUserId = tokenManager.getUserId.firstOrNull()
        }
        startTimer()
        listenToSocketEvents()
    }

    fun onSolutionInputChange(newValue: String) {
        _solutionInput.value = newValue
    }

    fun submitSolution() {
        if (_solutionInput.value.isBlank() || _isSubmitting.value || _gameResult.value != null) {
            return // Don't submit if empty, already submitting, or game is over
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            _feedbackMessage.value = null // Clear previous feedback
            Log.d("GameViewModel", "Submitting solution: ${_solutionInput.value}")
            SocketManager.emitSubmitSolution(gameId, _solutionInput.value)
            // Timeout for submission feedback (in case server doesn't respond quickly)
            delay(5000) // 5 seconds
            if (_isSubmitting.value && _gameResult.value == null) {
                _isSubmitting.value = false
                _feedbackMessage.value = "Submission timed out."
            }
        }
    }

    private fun startTimer() {
        countdownTimer?.cancel() // Cancel any existing timer
        countdownTimer = object : CountDownTimer(_timeLeft.value, 1000) { // Tick every second
            override fun onTick(millisUntilFinished: Long) {
                _timeLeft.value = millisUntilFinished
            }

            override fun onFinish() {
                _timeLeft.value = 0
                // Timer finished locally. The server determines the actual timeout result.
                Log.d("GameViewModel", "Local timer finished for game $gameId")
                // Optionally disable input field here if game hasn't ended via socket yet
            }
        }.start()
        Log.d("GameViewModel", "Timer started for $timeLimitSeconds seconds")
    }

    private fun listenToSocketEvents() {
        if (socketListenerJob?.isActive == true) return
        Log.d("GameViewModel", "Starting to listen to game-specific socket events")

        socketListenerJob = viewModelScope.launch {
            // Listen for Game Over
            launch {
                SocketManager.gameOverFlow
                    .filter { it.gameId == gameId } // Only process events for *this* game
                    .catch { e -> Log.e("GameViewModel", "Error in gameOverFlow: ${e.message}") }
                    .collect { result ->
                        Log.i("GameViewModel", "Game Over received for game $gameId: ${result.reason}")
                        countdownTimer?.cancel() // Stop local timer
                        _isSubmitting.value = false
                        _gameResult.value = result // Update UI to show results
                    }
            }

            // Listen for Invalid Solution Feedback
            launch {
                SocketManager.solutionInvalidFlow
                    .filter { it.gameId == gameId } // Only process events for *this* game
                    .catch { e -> Log.e("GameViewModel", "Error in solutionInvalidFlow: ${e.message}") }
                    .collect { invalidInfo ->
                        Log.w("GameViewModel", "Invalid solution received: ${invalidInfo.reason}")
                        _isSubmitting.value = false // Re-enable submit button
                        _feedbackMessage.value = formatInvalidReason(invalidInfo)
                        // Optionally clear feedback after a delay
                        launch {
                            delay(3000)
                            if (_feedbackMessage.value == formatInvalidReason(invalidInfo)) {
                                _feedbackMessage.value = null
                            }
                        }
                    }
            }
        }
    }

    private fun formatInvalidReason(info: SolutionInvalidData): String {
        return when (info.reason) {
            "digit_mismatch" -> "Incorrect digits or order used."
            "wrong_result" -> "Calculation does not equal 100."
            "evaluation_error" -> "Invalid mathematical expression."
            "game_already_over" -> "Game has already ended."
            "already_submitted" -> "You already submitted a solution."
            else -> "Invalid solution (${info.reason})."
        }
    }

    fun getGameOutcomeMessage(): String? {
        val result = _gameResult.value ?: return null
        val isWinner = result.winnerId == currentUserId
        val isLoser = result.loserId == currentUserId

        return when (result.status) {
            "completed_win" -> if (isWinner) "You Won!" else "You Lost!"
            "timeout" -> "Time's Up!"
            "completed_draw" -> "It's a Draw!" // If you implement draws
            "abandoned" -> if (isWinner) "Opponent Left!" else "Game Abandoned" // Should ideally only be seen by winner
            else -> "Game Over (${result.status})"
        }
    }

    fun getResultMessageDetails(): String? {
        val result = _gameResult.value ?: return null

        // Find solutions using the new structure
        val opponentSolutionInfo = if (result.player1Info?.id == opponentId) {
            result.player1Info
        } else if (result.player2Info?.id == opponentId) {
            result.player2Info
        } else {
            null // Opponent info not found in payload? Log error maybe.
        }

        val yourSolutionInfo = if (result.player1Info?.id == currentUserId) {
            result.player1Info
        } else if (result.player2Info?.id == currentUserId) {
            result.player2Info
        } else {
            null // Your info not found? Log error maybe.
        }

        val opponentSolution = opponentSolutionInfo?.solution
        val yourSolution = yourSolutionInfo?.solution

        // Prepare solution strings for display
        val yourSolutionText = "Your solution: ${yourSolution ?: "Not submitted"}"
        // Use opponentName property from ViewModel
        val opponentSolutionText = "$opponentName's solution: ${opponentSolution ?: "Not submitted"}"

        return when (result.reason) {
            "correct_solution" -> {
                val winnerName = if (result.winnerId == currentUserId) "You" else opponentName
                "$winnerName found the solution first.\n$yourSolutionText\n$opponentSolutionText" // Append solutions
            }
            "timeout" -> "Neither player found a solution in time.\n$yourSolutionText\n$opponentSolutionText"
            "opponent_disconnected" -> "$opponentName disconnected." // Solutions might be less relevant here
            else -> "Reason: ${result.reason}\n$yourSolutionText\n$opponentSolutionText" // Default case includes solutions
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("GameViewModel", "ViewModel cleared for game $gameId. Cancelling timer and listeners.")
        countdownTimer?.cancel()
        socketListenerJob?.cancel()
        // Do NOT disconnect SocketManager here
    }
}

// Add a ViewModel Factory if not using Hilt
class GameViewModelFactory(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(application, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
