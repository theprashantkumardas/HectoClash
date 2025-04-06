package com.example.hectoclash.viewmodels

import android.app.Application
import androidx.compose.ui.graphics.Color // Import Compose Color
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.*
import com.example.hectoclash.data.local.TokenManager // <<< Import TokenManager
// <<< Import ALL data models, including the correct PlayerInfo >>>
import com.example.hectoclash.data.models.*
import com.example.hectoclash.utils.SocketManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

const val ROUND_TRANSITION_DELAY_MS = 3000L

// <<< Corrected Constructor: Added tokenManager >>>
class GameViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val tokenManager: TokenManager // <<< ACCEPT TokenManager here
) : AndroidViewModel(application) {

    private val _gameId: String = savedStateHandle.get<String>("gameId") ?: "error_id"
    // Keep initial name as fallback until state loads
    private val initialOpponentName: String = savedStateHandle.get<String>("opponentName") ?: "Opponent"

    // --- User & Player Role ---
    private val _currentUserId = MutableStateFlow<String?>(null)
    // Exposing userId might not be necessary for the UI
    // val currentUserId: StateFlow<String?> = _currentUserId

    private val _isPlayer1 = MutableStateFlow<Boolean?>(null)
    // Exposing isPlayer1 can be useful if UI needs role-specific logic
    val isPlayer1: StateFlow<Boolean?> = _isPlayer1

    // <<< Use imported data.models.PlayerInfo >>>
    private val _player1Info = MutableStateFlow<com.example.hectoclash.data.models.PlayerInfo?>(null)
    private val _player2Info = MutableStateFlow<com.example.hectoclash.data.models.PlayerInfo?>(null)

    // Computed opponent info based on user role
    // <<< Use imported data.models.PlayerInfo >>>
    val opponentInfo: StateFlow<com.example.hectoclash.data.models.PlayerInfo?> = combine(_isPlayer1, _player1Info, _player2Info) { isP1, p1, p2 ->
        when (isP1) {
            true -> p2 // If I am P1, opponent is P2
            false -> p1 // If I am P2, opponent is P1
            null -> null // Role not yet determined
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    // --- Game State ---
    private val _currentRound = MutableStateFlow(0)
    val currentRound: StateFlow<Int> = _currentRound

    private val _totalRounds = MutableStateFlow(5)
    val totalRounds: StateFlow<Int> = _totalRounds

    private val _puzzle = MutableStateFlow("")
    val puzzle: StateFlow<String> = _puzzle

    // Raw scores (keep internal)
    private val _player1Score = MutableStateFlow(0)
    private val _player2Score = MutableStateFlow(0)

    // Computed Scores for UI
    val myScore: StateFlow<Int> = combine(_isPlayer1, _player1Score, _player2Score) { isP1, p1s, p2s ->
        when (isP1) {
            true -> p1s; false -> p2s; null -> 0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val opponentScore: StateFlow<Int> = combine(_isPlayer1, _player1Score, _player2Score) { isP1, p1s, p2s ->
        when (isP1) {
            true -> p2s; false -> p1s; null -> 0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Other states
    private val _solutionInput = MutableStateFlow("")
    val solutionInput: StateFlow<String> = _solutionInput

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _roundTimeLimitMs = MutableStateFlow(60000L)
    private val _roundTimeLeft = MutableStateFlow(0L)
    val roundTimeLeft: StateFlow<Long> = _roundTimeLeft

    private var roundTimer: CountDownTimer? = null
    private var roundTransitionJob: Job? = null

    // --- Feedback & Results ---
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage

    private val _roundResultInfo = MutableStateFlow<RoundOverData?>(null)
    val roundResultInfo: StateFlow<RoundOverData?> = _roundResultInfo

    private val _challengeResult = MutableStateFlow<ChallengeOverData?>(null)
    val challengeResult: StateFlow<ChallengeOverData?> = _challengeResult

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading


    init {
        Log.d("GameViewModel", "Initializing for game: $_gameId")
        viewModelScope.launch {
            // <<< Use the injected tokenManager >>>
            _currentUserId.value = tokenManager.getUserId.first()
            Log.d("GameViewModel", "Current User ID set: ${_currentUserId.value}")
            // Check validity before observing
            if (_gameId != "error_id" && !_currentUserId.value.isNullOrEmpty()) {
                observeSocketEvents()
            } else {
                Log.e("GameViewModel", "Initialization failed: Invalid gameId ($_gameId) or userId (${_currentUserId.value})")
                _feedbackMessage.value = "Error: Invalid Game ID or User session"
                _isLoading.value = false
            }
        }
    }

    private fun observeSocketEvents() {
        Log.d("GameViewModel", "Observing socket events for gameId: $_gameId")
        viewModelScope.launch {
            SocketManager.challengeStartFlow
                .filter { it.gameId == _gameId }
                .collect { data ->
                    Log.d("GameViewModel", "[SUCCESS] Challenge Start Collected for ${data.gameId}")

                    // <<< Assign directly - types should now match data.models.PlayerInfo >>>
                    _player1Info.value = data.player1
                    _player2Info.value = data.player2

                    val userId = _currentUserId.value
                    // Determine player role
                    _isPlayer1.value = when (userId) {
                        data.player1.id -> true
                        data.player2.id -> false
                        else -> {
                            Log.e("GameViewModel", "User ID $userId not found in player list! P1=${data.player1.id}, P2=${data.player2.id}")
                            null // Error case
                        }
                    }
                    if (_isPlayer1.value == null) { // Handle error case explicitly
                        _feedbackMessage.value = "Error determining player role."
                        _isLoading.value = false
                        return@collect
                    }
                    Log.d("GameViewModel", "User is Player ${if (_isPlayer1.value == true) "1" else "2"}")


                    _totalRounds.value = data.totalRounds
                    _roundTimeLimitMs.value = data.roundTimeLimitSeconds * 1000L
                    updateRoundState(data.currentRound, data.puzzle, data.player1Score, data.player2Score)
                    _isLoading.value = false
                    Log.d("GameViewModel", "Set isLoading=false. P1Score=${data.player1Score}, P2Score=${data.player2Score}, Puzzle=${data.puzzle}")
                }
        }
        // Collect other flows (newRoundFlow, solutionResultFlow, roundOverFlow, challengeOverFlow, gameStartFailedFlow)
        // as they were in the previous correct version...
        viewModelScope.launch {
            SocketManager.newRoundFlow.filter { it.gameId == _gameId }.collect { data ->
                Log.d("GameViewModel", "[SUCCESS] New Round Collected: ${data.roundNumber}")
                _roundTimeLimitMs.value = data.roundTimeLimitSeconds * 1000L
                updateRoundState(data.roundNumber, data.puzzle, data.player1Score, data.player2Score)
            }
        }
        viewModelScope.launch {
            SocketManager.solutionResultFlow.filter { it.gameId == _gameId && it.round == _currentRound.value }.collect { data ->
                Log.d("GameViewModel", "Solution Result Received: ${data.status}")
                _isSubmitting.value = false
                when (data.status) { /* ... feedback logic ... */ }
                viewModelScope.launch { /* ... clear feedback delay ... */ }
            }
        }
        viewModelScope.launch {
            SocketManager.roundOverFlow.filter { it.gameId == _gameId }.collect { data ->
                Log.d("GameViewModel", "[SUCCESS] Round Over Collected: ${data.roundNumber}")
                roundTimer?.cancel()
                _roundResultInfo.value = data
                _player1Score.value = data.player1Score
                _player2Score.value = data.player2Score
                Log.d("GameViewModel", "Round Over - Updated scores: P1=${data.player1Score}, P2=${data.player2Score}")
                roundTransitionJob?.cancel()
                roundTransitionJob = viewModelScope.launch {
                    delay(ROUND_TRANSITION_DELAY_MS + 500)
                    if (_roundResultInfo.value?.roundNumber == data.roundNumber) {
                        _roundResultInfo.value = null
                    }
                }
            }
        }
        viewModelScope.launch {
            SocketManager.challengeOverFlow.filter { it.gameId == _gameId }.collect { data ->
                Log.d("GameViewModel", "[SUCCESS] Challenge Over Collected: ${data.finalStatus}")
                roundTimer?.cancel()
                roundTransitionJob?.cancel()
                _challengeResult.value = data
                _isLoading.value = false
                _isSubmitting.value = false
                _roundResultInfo.value = null
            }
        }
        viewModelScope.launch {
            SocketManager.gameStartFailedFlow.collect { data ->
                Log.e("GameViewModel", "Game Start Failed: ${data.reason}")
                if (_challengeResult.value == null) { /* ... error handling ... */ }
            }
        }
    }

    // updateRoundState uses raw scores
    private fun updateRoundState(round: Int, newPuzzle: String, p1Score: Int, p2Score: Int) {
        Log.d("GameViewModel", "Updating round state: Round=$round, P1Score=$p1Score, P2Score=$p2Score, Puzzle=$newPuzzle")
        roundTimer?.cancel()
        roundTransitionJob?.cancel()
        _roundResultInfo.value = null
        _currentRound.value = round
        _puzzle.value = newPuzzle
        _player1Score.value = p1Score // Update raw P1 score
        _player2Score.value = p2Score // Update raw P2 score
        _solutionInput.value = ""
        _isSubmitting.value = false
        if (_challengeResult.value == null) { // Only start timer if game is ongoing
            startRoundTimer(_roundTimeLimitMs.value)
        }
    }

    // startRoundTimer remains the same
    private fun startRoundTimer(durationMs: Long) {
        roundTimer?.cancel()
        _roundTimeLeft.value = durationMs
        if (durationMs <= 0) return
        Log.d("GameViewModel", "Starting round timer for ${_currentRound.value} with ${durationMs}ms")
        roundTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) { _roundTimeLeft.value = millisUntilFinished }
            override fun onFinish() {
                _roundTimeLeft.value = 0
                Log.d("GameViewModel", "Local round timer finished for round ${_currentRound.value}")
            }
        }.start()
    }

    // onSolutionInputChange, submitSolution, clearFeedbackMessage remain the same
    fun onSolutionInputChange(input: String) { _solutionInput.value = input }
    fun submitSolution() {
        if (_solutionInput.value.isBlank() || _isSubmitting.value || _challengeResult.value != null) return
        _isSubmitting.value = true
        _feedbackMessage.value = null
        SocketManager.emitSubmitSolution(_gameId, _solutionInput.value)
    }
    fun clearFeedbackMessage() { _feedbackMessage.value = null }

    // Corrected Outcome Message Logic using currentUserId
    fun getChallengeOutcomeMessage(): String {
        val result = _challengeResult.value ?: return "Game Over"
        val userId = _currentUserId.value // Use internal state
        return when {
            result.finalStatus == "error" -> "Error: ${result.reason ?: "Unknown"}"
            result.finalStatus == "abandoned" && result.challengeLoserId == userId -> "You disconnected"
            result.finalStatus == "abandoned" && result.challengeLoserId != userId -> "Opponent disconnected"
            result.isDraw -> "Challenge Draw!"
            result.challengeWinnerId == userId -> "Challenge Won!"
            result.challengeLoserId == userId -> "Challenge Lost!"
            else -> "Challenge Over: ${result.finalStatus}"
        }
    }

    fun getChallengeResultMessageDetails(): String {
        val result = _challengeResult.value ?: return ""
        // Use computed scores directly via their state flows' values
        val scoreString = "${myScore.value} - ${opponentScore.value}"
        return when (result.finalStatus) {
            "completed", "timeout", "abandoned" -> "Final Score: $scoreString"
            "error" -> ""
            else -> "Status: ${result.finalStatus}"
        }
    }

    // Corrected Round Outcome Helpers using currentUserId
    fun getRoundOutcomeMessage(roundResult: RoundOverData?): String {
        val result = roundResult ?: return ""
        val userId = _currentUserId.value // Use internal state
        return when {
            result.roundWinnerId == userId -> "Round Won!"
            result.reason == "timeout" -> "Round Timed Out"
            result.roundWinnerId != null && result.roundWinnerId != userId -> "Round Lost"
            else -> "Round Draw"
        }
    }

    fun getRoundOutcomeColor(roundResult: RoundOverData?): Color {
        val result = roundResult ?: return Color.Gray
        val userId = _currentUserId.value // Use internal state
        return when {
            result.roundWinnerId == userId -> Color(0xFF4CAF50) // Green
            result.reason == "timeout" -> Color.Gray
            result.roundWinnerId != null && result.roundWinnerId != userId -> Color.Red // Red
            else -> Color.Gray
        }
    }

    override fun onCleared() {
        super.onCleared()
        roundTimer?.cancel()
        roundTransitionJob?.cancel()
        Log.d("GameViewModel", "ViewModel Cleared. Timer cancelled.")
    }

    // <<< REMOVED local PlayerInfo data class >>>
}

// Add Factory for GameViewModel

class GameViewModelFactory(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val tokenManager: TokenManager // Accept TokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(application, savedStateHandle, tokenManager) as T // Pass TokenManager
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}