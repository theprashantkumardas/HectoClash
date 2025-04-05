package com.example.hectoclash.viewmodels

import android.app.Application
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.*
import com.example.hectoclash.data.models.*
import com.example.hectoclash.utils.SocketManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

const val ROUND_TRANSITION_DELAY_MS = 3000L // Delay before starting next round

class GameViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle // Use SavedStateHandle to get nav args
) : AndroidViewModel(application) {

    private val _gameId: String = savedStateHandle.get<String>("gameId") ?: "error_id"
    private val _opponentName: String = savedStateHandle.get<String>("opponentName") ?: "Opponent"
    private val _opponentId: String = savedStateHandle.get<String>("opponentId") ?: "error_opponent_id"

    // --- Game State ---
    private val _currentRound = MutableStateFlow(0) // 0=Not Started, 1-5 during game
    val currentRound: StateFlow<Int> = _currentRound

    private val _totalRounds = MutableStateFlow(5) // Default, updated on challenge start
    val totalRounds: StateFlow<Int> = _totalRounds

    private val _puzzle = MutableStateFlow("")
    val puzzle: StateFlow<String> = _puzzle

    private val _player1Score = MutableStateFlow(0)
    val player1Score: StateFlow<Int> = _player1Score

    private val _player2Score = MutableStateFlow(0)
    val player2Score: StateFlow<Int> = _player2Score

    private val _solutionInput = MutableStateFlow("")
    val solutionInput: StateFlow<String> = _solutionInput

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _roundTimeLimitMs = MutableStateFlow(60000L) // Default, updated per round/challenge
    private val _roundTimeLeft = MutableStateFlow(0L)
    val roundTimeLeft: StateFlow<Long> = _roundTimeLeft // Time left in CURRENT round

    private var roundTimer: CountDownTimer? = null
    private var roundTransitionJob: Job? = null

    // --- Feedback & Results ---
    private val _feedbackMessage = MutableStateFlow<String?>(null) // For temporary messages (e.g., incorrect solution)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage

    private val _roundResultInfo = MutableStateFlow<RoundOverData?>(null) // Stores result of the last completed round
    val roundResultInfo: StateFlow<RoundOverData?> = _roundResultInfo

    private val _challengeResult = MutableStateFlow<ChallengeOverData?>(null) // Final challenge result
    val challengeResult: StateFlow<ChallengeOverData?> = _challengeResult

    private val _isLoading = MutableStateFlow(true) // Indicate initial loading
    val isLoading: StateFlow<Boolean> = _isLoading

    // Store opponent info
    val opponentInfo = PlayerInfo(_opponentId, _opponentName) // Simple data class for opponent

    init {
        Log.d("GameViewModel", "Initializing for game: $_gameId, Opponent: $_opponentName")
        if (_gameId != "error_id") {
            observeSocketEvents()
            // Initial state will be set by challenge_start listener
        } else {
            _feedbackMessage.value = "Error: Invalid Game ID"
            _isLoading.value = false
        }
    }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            SocketManager.challengeStartFlow
                .filter { it.gameId == _gameId }
                .collect { data ->
                    Log.d("GameViewModel", "Challenge Start Received: ${data.gameId}")
                    _totalRounds.value = data.totalRounds
                    _roundTimeLimitMs.value = data.roundTimeLimitSeconds * 1000L
                    updateRoundState(data.currentRound, data.puzzle, data.player1Score, data.player2Score)
                    _isLoading.value = false // No longer loading initial state
                }
        }
        viewModelScope.launch {
            SocketManager.newRoundFlow
                .filter { it.gameId == _gameId }
                .collect { data ->
                    Log.d("GameViewModel", "New Round Received: ${data.roundNumber}")
                    _roundTimeLimitMs.value = data.roundTimeLimitSeconds * 1000L // Update time limit if needed
                    updateRoundState(data.roundNumber, data.puzzle, data.player1Score, data.player2Score)
                }
        }
        viewModelScope.launch {
            SocketManager.solutionResultFlow
                .filter { it.gameId == _gameId && it.round == _currentRound.value } // Only process for current round
                .collect { data ->
                    Log.d("GameViewModel", "Solution Result Received: ${data.status}")
                    _isSubmitting.value = false // No longer submitting
                    when (data.status) {
                        "correct" -> {
                            _feedbackMessage.value = "Correct! (Took ${data.timeTakenMs ?: "N/A"} ms)"
                            // Round will end via round_over event from server
                        }
                        "incorrect" -> {
                            _feedbackMessage.value = "Incorrect: ${data.reason ?: "Try again"}"
                            // Clear input? Optionally. _solutionInput.value = ""
                        }
                        "invalid" -> {
                            _feedbackMessage.value = "Invalid: ${data.reason ?: "Submission error"}"
                        }
                    }
                    // Clear feedback message after a delay
                    viewModelScope.launch {
                        delay(3000)
                        if (_feedbackMessage.value?.startsWith(data.status.replaceFirstChar { it.titlecase() }) == true) {
                            _feedbackMessage.value = null
                        }
                    }
                }
        }
        viewModelScope.launch {
            SocketManager.roundOverFlow
                .filter { it.gameId == _gameId }
                .collect { data ->
                    Log.d("GameViewModel", "Round Over Received: ${data.roundNumber}, Winner: ${data.roundWinnerId}")
                    roundTimer?.cancel() // Stop timer for the completed round
                    _roundResultInfo.value = data // Store round result for potential display
                    _player1Score.value = data.player1Score // Update scores
                    _player2Score.value = data.player2Score
                    // Don't immediately start next round here, wait for new_round or challenge_over
                    // Optional: Show round result overlay briefly
                    // Clear round result after delay?
                    viewModelScope.launch {
                        delay(ROUND_TRANSITION_DELAY_MS)
                        // _roundResultInfo.value = null // Clear overlay trigger
                    }
                }
        }
        viewModelScope.launch {
            SocketManager.challengeOverFlow
                .filter { it.gameId == _gameId }
                .collect { data ->
                    Log.d("GameViewModel", "Challenge Over Received: ${data.finalStatus}")
                    roundTimer?.cancel()
                    roundTransitionJob?.cancel()
                    _challengeResult.value = data // Set final result
                    _isLoading.value = false
                    _isSubmitting.value = false
                    // Game is finished
                }
        }
        // Handle potential game start failures
        viewModelScope.launch {
            SocketManager.gameStartFailedFlow
                // .filter { it.gameId == _gameId } // gameId might not be available here yet
                .collect { data ->
                    Log.e("GameViewModel", "Game Start Failed: ${data.reason}")
                    _challengeResult.value = ChallengeOverData( // Use ChallengeOverData to show error state
                        gameId = _gameId, // Use the one we have
                        finalStatus = "error",
                        reason = "Failed to start: ${data.reason}",
                        challengeWinnerId = null, challengeLoserId = null, isDraw = false,
                        player1Score = 0, player2Score = 0, roundsData = null
                    )
                    _isLoading.value = false
                }
        }
    }

    private fun updateRoundState(round: Int, newPuzzle: String, p1Score: Int, p2Score: Int) {
        roundTimer?.cancel() // Cancel previous timer
        roundTransitionJob?.cancel() // Cancel any pending transition

        _roundResultInfo.value = null // Clear previous round result display
        _currentRound.value = round
        _puzzle.value = newPuzzle
        _player1Score.value = p1Score
        _player2Score.value = p2Score
        _solutionInput.value = "" // Clear input for new round
        _isSubmitting.value = false

        startRoundTimer(_roundTimeLimitMs.value)
    }


    private fun startRoundTimer(durationMs: Long) {
        roundTimer?.cancel() // Ensure no double timers
        _roundTimeLeft.value = durationMs
        roundTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _roundTimeLeft.value = millisUntilFinished
            }

            override fun onFinish() {
                _roundTimeLeft.value = 0
                // Timeout logic is handled by server sending 'round_over' or 'challenge_over'
                Log.d("GameViewModel", "Local round timer finished for round ${_currentRound.value}")
                // Optionally show a local "Time's Up!" message if server event is delayed
                // _feedbackMessage.value = "Time's up for this round!"
            }
        }.start()
    }

    fun onSolutionInputChange(input: String) {
        // Basic validation? Only allow numbers and operators?
        _solutionInput.value = input
    }

    fun submitSolution() {
        if (_solutionInput.value.isBlank() || _isSubmitting.value || _challengeResult.value != null) return

        _isSubmitting.value = true
        _feedbackMessage.value = null // Clear previous feedback
        SocketManager.emitSubmitSolution(_gameId, _solutionInput.value)
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }

    // Helper to get outcome message for the final result overlay
    fun getChallengeOutcomeMessage(): String {
        val result = _challengeResult.value ?: return "Game Over"
        // Determine current user ID (needs access, maybe pass from Composable or use TokenManager)
        // For simplicity, assume we know if player 1 is 'us'
        // val currentUserId = ... TokenManager.getUserId.first() ...
        // Simplified: Check winner/loser fields
        return when {
            result.finalStatus == "error" -> "Error: ${result.reason ?: "Unknown"}"
            result.finalStatus == "abandoned" -> "Opponent disconnected" // Or "You disconnected"
            result.isDraw -> "Challenge Draw!"
            result.challengeWinnerId != null -> "Challenge Won!" // Need to check if winnerId is us
            result.challengeLoserId != null -> "Challenge Lost!" // Need to check if loserId is us
            else -> "Challenge Over: ${result.finalStatus}"
        }
        // TODO: Refine this logic based on knowing the actual current user's ID vs winner/loser IDs
    }

    fun getChallengeResultMessageDetails(): String {
        val result = _challengeResult.value ?: return ""
        val score = "${result.player1Score} - ${result.player2Score}" // TODO: Show score from player's perspective (You - Opponent)
        return when (result.finalStatus) {
            "completed" -> "Final Score: $score"
            "timeout" -> "Challenge timed out. Final Score: $score"
            "abandoned" -> "Game abandoned. Score: $score"
            "error" -> "" // Message already contains reason
            else -> "Status: ${result.finalStatus}"
        }
    }


    override fun onCleared() {
        super.onCleared()
        roundTimer?.cancel()
        roundTransitionJob?.cancel()
        Log.d("GameViewModel", "ViewModel Cleared. Timer cancelled.")
        // Optionally disconnect socket if game screen is the only place it's used? Unlikely.
    }

    // Simple data class used locally
    data class PlayerInfo(val id: String, val name: String)
}

// Add Factory for GameViewModel
class GameViewModelFactory(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(application, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}