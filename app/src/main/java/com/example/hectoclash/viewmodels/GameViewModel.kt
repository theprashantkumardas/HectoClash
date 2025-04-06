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
// ViewModel constructor accepts TokenManager
class GameViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val tokenManager: TokenManager // <<< ACCEPT TokenManager here
) : AndroidViewModel(application) {

    private val _gameId: String = savedStateHandle.get<String>("gameId") ?: "error_id"
    // Removed opponentId/Name from here, will get from flow

    // --- User & Player Role ---
    private val _currentUserId = MutableStateFlow<String?>(null)
    private val _isPlayer1 = MutableStateFlow<Boolean?>(null)
    val isPlayer1: StateFlow<Boolean?> = _isPlayer1 // Can be useful for UI logic if needed

    // Use imported data.models.PlayerInfo
    private val _player1Info = MutableStateFlow<com.example.hectoclash.data.models.PlayerInfo?>(null)
    private val _player2Info = MutableStateFlow<com.example.hectoclash.data.models.PlayerInfo?>(null)

    // Expose Own and Opponent Info derived from role
    val myPlayerInfo: StateFlow<com.example.hectoclash.data.models.PlayerInfo?> = combine(_isPlayer1, _player1Info, _player2Info) { isP1, p1, p2 ->
        when (isP1) { true -> p1; false -> p2; null -> null }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val opponentInfo: StateFlow<com.example.hectoclash.data.models.PlayerInfo?> = combine(_isPlayer1, _player1Info, _player2Info) { isP1, p1, p2 ->
        when (isP1) { true -> p2; false -> p1; null -> null }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Game State ---
    private val _currentRound = MutableStateFlow(0)
    val currentRound: StateFlow<Int> = _currentRound

    private val _totalRounds = MutableStateFlow(5)
    val totalRounds: StateFlow<Int> = _totalRounds

    private val _puzzle = MutableStateFlow("")
    val puzzle: StateFlow<String> = _puzzle

    // Raw scores (internal)
    private val _player1Score = MutableStateFlow(0)
    private val _player2Score = MutableStateFlow(0)

    // Computed Scores for UI (Corrected logic)
    val myScore: StateFlow<Int> = combine(_isPlayer1, _player1Score, _player2Score) { isP1, p1s, p2s ->
        when (isP1) { true -> p1s; false -> p2s; null -> 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val opponentScore: StateFlow<Int> = combine(_isPlayer1, _player1Score, _player2Score) { isP1, p1s, p2s ->
        when (isP1) { true -> p2s; false -> p1s; null -> 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Other states
    private val _solutionInput = MutableStateFlow("") // This will be managed by keypad interactions
    val solutionInput: StateFlow<String> = _solutionInput // UI can observe if needed, but keypad drives it

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
            _currentUserId.value = tokenManager.getUserId.first() // Get user ID first
            Log.d("GameViewModel", "Current User ID set: ${_currentUserId.value}")

            if (_gameId != "error_id" && !_currentUserId.value.isNullOrEmpty()) {
                observeSocketEvents() // Start listening ONLY if basic info is valid
            } else {
                Log.e("GameViewModel", "Initialization failed: Invalid gameId or userId.")
                _feedbackMessage.value = "Error: Invalid Game ID or User session."
                _isLoading.value = false
            }
        }
    }

    private fun observeSocketEvents() {
        Log.d("GameViewModel", "Observing socket events for gameId: $_gameId")

        // Observe Challenge Start
        viewModelScope.launch {
            SocketManager.challengeStartFlow
                .filter { it.gameId == _gameId }
                .collect { data ->
                    Log.d("GameViewModel", "[SUCCESS] Challenge Start Collected for ${data.gameId}")
                    _player1Info.value = data.player1
                    _player2Info.value = data.player2

                    val userId = _currentUserId.value
                    // Determine player role
                    _isPlayer1.value = when (userId) {
                        data.player1.id -> true
                        data.player2.id -> false
                        else -> { Log.e("GameViewModel", "User ID $userId not found!"); null }
                    }

                    if (_isPlayer1.value == null) {
                        _feedbackMessage.value = "Error determining player role."
                        _isLoading.value = false; return@collect
                    }
                    Log.d("GameViewModel", "User is Player ${if (_isPlayer1.value == true) "1" else "2"}")

                    _totalRounds.value = data.totalRounds
                    _roundTimeLimitMs.value = data.roundTimeLimitSeconds * 1000L
                    // Use the correct player scores from the event
                    updateRoundState(data.currentRound, data.puzzle, data.player1Score, data.player2Score)
                    _isLoading.value = false
                    Log.d("GameViewModel", "Set isLoading=false. MyScore=${myScore.value}, OpponentScore=${opponentScore.value}, Puzzle=${data.puzzle}")
                }
        }

        // Observe New Round
        viewModelScope.launch {
            SocketManager.newRoundFlow
                .filter { it.gameId == _gameId }
                .collect { data ->
                    Log.d("GameViewModel", "[SUCCESS] New Round Collected: ${data.roundNumber}")
                    _roundTimeLimitMs.value = data.roundTimeLimitSeconds * 1000L
                    updateRoundState(data.roundNumber, data.puzzle, data.player1Score, data.player2Score)
                }
        }

        // Observe Solution Result
        viewModelScope.launch {
            SocketManager.solutionResultFlow
                .filter { it.gameId == _gameId && it.round == _currentRound.value }
                .collect { data ->
                    Log.d("GameViewModel", "Solution Result Received: ${data.status}")
                    _isSubmitting.value = false
                    when (data.status) {
                        "correct" -> _feedbackMessage.value = "Correct! ${data.timeTakenMs?.let { "(Took ${it}ms)" } ?: ""}"
                        "incorrect" -> _feedbackMessage.value = "Incorrect: ${data.reason ?: "Try again"}"
                        "invalid" -> _feedbackMessage.value = "Invalid: ${data.reason ?: "Submission error"}"
                    }
                    // Clear feedback after delay
                    viewModelScope.launch { delay(3000); if (_feedbackMessage.value?.contains(data.status) == true) _feedbackMessage.value = null }
                }
        }

        // Observe Round Over
        viewModelScope.launch {
            SocketManager.roundOverFlow
                .filter { it.gameId == _gameId }
                .collect { data ->
                    Log.d("GameViewModel", "[SUCCESS] Round Over Collected: ${data.roundNumber}")
                    roundTimer?.cancel()
                    _roundResultInfo.value = data // Show overlay
                    // Update internal raw scores
                    _player1Score.value = data.player1Score
                    _player2Score.value = data.player2Score
                    Log.d("GameViewModel", "Round Over - Updated internal scores: P1=${data.player1Score}, P2=${data.player2Score}")
                    Log.d("GameViewModel", "Round Over - Computed scores: My=${myScore.value}, Opponent=${opponentScore.value}")

                    // Clear overlay after delay
                    roundTransitionJob?.cancel()
                    roundTransitionJob = viewModelScope.launch {
                        delay(ROUND_TRANSITION_DELAY_MS + 500) // Give time to see overlay
                        // Check if another round hasn't started already or game hasn't ended
                        if (_roundResultInfo.value?.roundNumber == data.roundNumber && _challengeResult.value == null) {
                            _roundResultInfo.value = null
                        }
                    }
                }
        }

        // Observe Challenge Over
        viewModelScope.launch {
            SocketManager.challengeOverFlow
                .filter { it.gameId == _gameId }
                .collect { data ->
                    Log.d("GameViewModel", "[SUCCESS] Challenge Over Collected: ${data.finalStatus}")
                    roundTimer?.cancel()
                    roundTransitionJob?.cancel()
                    _challengeResult.value = data // Trigger final overlay
                    _isLoading.value = false
                    _isSubmitting.value = false
                    _roundResultInfo.value = null // Ensure round overlay is hidden
                }
        }

        // Observe Game Start Failed
        viewModelScope.launch {
            // Assuming gameStartFailedFlow might still be relevant if DB fails on creation
            SocketManager.gameStartFailedFlow.collect { data ->
                Log.e("GameViewModel", "Game Start Failed received: ${data.reason}")
                // Check if it pertains to *this* potential game attempt if possible,
                // otherwise show a generic error if no game result is set yet.
                if (_challengeResult.value == null && !_gameId.startsWith("error")) { // Check if game seems valid but failed
                    _challengeResult.value = ChallengeOverData(
                        gameId = _gameId, finalStatus = "error", reason = "Failed to start: ${data.reason}",
                        challengeWinnerId = null, challengeLoserId = null, isDraw = false,
                        player1Score = 0, player2Score = 0, roundsData = null
                    )
                    _isLoading.value = false
                }
            }
        }
    }

    // Update internal raw scores
    private fun updateRoundState(round: Int, newPuzzle: String, p1Score: Int, p2Score: Int) {
        Log.d("GameViewModel", "Updating round state: Round=$round, P1Score=$p1Score, P2Score=$p2Score")
        roundTimer?.cancel()
        roundTransitionJob?.cancel()
        _roundResultInfo.value = null // Clear previous round result display
        _currentRound.value = round
        _puzzle.value = newPuzzle
        _player1Score.value = p1Score // Update raw P1 score
        _player2Score.value = p2Score // Update raw P2 score
        _solutionInput.value = "" // Clear keypad-driven solution for new round
        _isSubmitting.value = false
        // Only start timer if game is ongoing
        if (_challengeResult.value == null && round > 0) {
            startRoundTimer(_roundTimeLimitMs.value)
        }
    }

    // startRoundTimer remains the same
    private fun startRoundTimer(durationMs: Long) {
        roundTimer?.cancel()
        _roundTimeLeft.value = durationMs
        if (durationMs <= 0) return // Don't start timer if duration is zero or less
        Log.d("GameViewModel", "Starting round timer for round ${_currentRound.value} with ${durationMs}ms")
        roundTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) { _roundTimeLeft.value = millisUntilFinished }
            override fun onFinish() {
                _roundTimeLeft.value = 0
                Log.d("GameViewModel", "Local round timer finished for round ${_currentRound.value}")
                // Server handles timeout logic via 'round_over' event
            }
        }.start()
    }

    // Method for the UI (Keypad) to update the internal solution string
    fun updateSolutionInput(newSolution: String) {
        _solutionInput.value = newSolution
    }

    // submitSolution uses the internal _solutionInput.value
    fun submitSolution() {
        if (_solutionInput.value.isBlank() || _isSubmitting.value || _challengeResult.value != null) return
        _isSubmitting.value = true
        _feedbackMessage.value = null
        Log.d("GameViewModel", "Submitting solution: ${_solutionInput.value} for game: $_gameId")
        SocketManager.emitSubmitSolution(_gameId, _solutionInput.value)
    }

    fun clearFeedbackMessage() { _feedbackMessage.value = null }

    // Corrected Outcome Message Logic using currentUserId
    fun getChallengeOutcomeMessage(): String {
        val result = _challengeResult.value ?: return "Game Over"
        val userId = _currentUserId.value
        return when {
            result.finalStatus == "error" -> "Error: ${result.reason ?: "Unknown"}"
            result.finalStatus == "abandoned" && result.challengeLoserId == userId -> "You disconnected"
            result.finalStatus == "abandoned" && result.challengeLoserId != userId -> "Opponent disconnected"
            result.isDraw -> "Challenge Draw!"
            result.challengeWinnerId == userId -> "Challenge Won!"
            result.challengeLoserId == userId -> "Challenge Lost!"
            else -> "Challenge Over: ${result.finalStatus}" // Should not happen in normal flow
        }
    }

    // Use computed scores for result details
    fun getChallengeResultMessageDetails(): String {
        val result = _challengeResult.value ?: return ""
        val scoreString = "${myScore.value} - ${opponentScore.value}" // Use computed scores
        return when (result.finalStatus) {
            "completed", "timeout", "abandoned" -> "Final Score: $scoreString"
            "error" -> "" // Message already contains reason
            else -> "Status: ${result.finalStatus}"
        }
    }

    // Helpers for Round Overlay using computed score/role
    fun getRoundOutcomeMessage(roundResult: RoundOverData?): String {
        val result = roundResult ?: return ""
        val userId = _currentUserId.value
        val myScoreAfterRound = if (_isPlayer1.value == true) result.player1Score else result.player2Score
        val opponentScoreAfterRound = if (_isPlayer1.value == true) result.player2Score else result.player1Score

        return when {
            result.roundWinnerId == userId -> "Round Won!"
            result.reason == "timeout" -> "Round Timed Out"
            result.roundWinnerId != null && result.roundWinnerId != userId -> "Round Lost" // Opponent won
            else -> "Round Draw" // Both submitted incorrect / timeout with same outcome?
        } + " (Score: $myScoreAfterRound - $opponentScoreAfterRound)" // Add score context
    }

    fun getRoundOutcomeColor(roundResult: RoundOverData?): Color {
        val result = roundResult ?: return Color.Gray
        val userId = _currentUserId.value
        return when {
            result.roundWinnerId == userId -> Color(0xFF4CAF50) // Green for Win
            result.reason == "timeout" -> Color.Gray
            result.roundWinnerId != null && result.roundWinnerId != userId -> Color.Red // Red for Loss
            else -> Color.Yellow // Yellow/Orange for Draw
        }
    }

    //

    override fun onCleared() {
        super.onCleared()
        roundTimer?.cancel()
        roundTransitionJob?.cancel()
        Log.d("GameViewModel", "ViewModel Cleared. Timer cancelled.")
    }

    // REMOVED local PlayerInfo data class, using the one from data.models
}
// Add Factory for GameViewModel

// Factory MUST accept TokenManager now
class GameViewModelFactory(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val tokenManager: TokenManager // Accept TokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Pass TokenManager to ViewModel constructor
            return GameViewModel(application, savedStateHandle, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}