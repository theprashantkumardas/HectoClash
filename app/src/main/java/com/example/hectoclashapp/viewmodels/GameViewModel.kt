package com.example.hectoclashapp.viewmodels

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

// Define the segment types
sealed interface PuzzleSegment {
    val char: Char
    data class Digit(override val char: Char) : PuzzleSegment
    data class Operator(override val char: Char) : PuzzleSegment
}

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
    // Puzzle represented as segments
    private val _puzzleSegments = MutableStateFlow<List<PuzzleSegment>>(emptyList())
    val puzzleSegments: StateFlow<List<PuzzleSegment>> = _puzzleSegments.asStateFlow()

    // Cursor position (index *between* segments)
    // 0 = before first segment, segments.size = after last segment
    private val _cursorPosition = MutableStateFlow(initialPuzzle.length) // Start cursor at the end
    val cursorPosition: StateFlow<Int> = _cursorPosition.asStateFlow()

    private val _timeLeft = MutableStateFlow(timeLimitSeconds * 1000L) // Time left in milliseconds
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    // Flag for submission state
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // Game result data
    private val _gameResult = MutableStateFlow<GameOverData?>(null)
    val gameResult: StateFlow<GameOverData?> = _gameResult.asStateFlow()

    // Feedback messages
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private var countdownTimer: CountDownTimer? = null
    private var socketListenerJob: Job? = null
    private var currentUserId: String? = null

    // Allowed operators for insertion
    private val allowedOperators = setOf('+', '-', '*', '/', '(', ')', '^')

    init {
        Log.d("GameViewModel", "Initializing for game: $gameId")
        // Initialize segments from the initial puzzle string
        _puzzleSegments.value = initialPuzzle.map { PuzzleSegment.Digit(it) }
        _cursorPosition.value = _puzzleSegments.value.size // Cursor initially after last digit

        viewModelScope.launch {
            currentUserId = tokenManager.getUserId.firstOrNull()
        }
        startTimer()
        listenToSocketEvents()
    }

    // --- Input Handling ---

    fun setCursorPosition(index: Int) {
        // Ensure cursor stays within valid bounds (0 to segments.size)
        _cursorPosition.value = index.coerceIn(0, _puzzleSegments.value.size)
        Log.d("GameViewModel", "Cursor position set to: ${_cursorPosition.value}")
    }

    fun insertOperator(operatorChar: Char) {
        // Skip if not an allowed operator or game is over
        if (!allowedOperators.contains(operatorChar) || gameResult.value != null) return

        val currentSegments = _puzzleSegments.value.toMutableList()
        val insertIndex = _cursorPosition.value

        // No validation - allow insertion anywhere
        currentSegments.add(insertIndex, PuzzleSegment.Operator(operatorChar))
        _puzzleSegments.value = currentSegments
        _cursorPosition.value = insertIndex + 1 // Move cursor after inserted operator
        Log.d("GameViewModel", "Inserted '$operatorChar' at $insertIndex. New segments: ${_puzzleSegments.value}")
    }

    fun handleBackspace() {
        if (_cursorPosition.value == 0 || gameResult.value != null) return

        val currentSegments = _puzzleSegments.value.toMutableList()
        val removalIndex = _cursorPosition.value - 1

        // Only remove if the segment *before* the cursor is an Operator
        if (currentSegments.getOrNull(removalIndex) is PuzzleSegment.Operator) {
            currentSegments.removeAt(removalIndex)
            _puzzleSegments.value = currentSegments
            _cursorPosition.value = removalIndex // Move cursor to where the operator was
            Log.d("GameViewModel", "Removed operator at $removalIndex. New segments: ${_puzzleSegments.value}")
        } else {
            Log.d("GameViewModel", "Backspace ignored: segment at $removalIndex is not an operator.")
        }
    }

    // --- Submission ---
    fun submitSolution() {
        val solutionString = _puzzleSegments.value.joinToString("") { it.char.toString() }
        if (solutionString == initialPuzzle || _isSubmitting.value || _gameResult.value != null) {
            Log.d("GameViewModel", "Submission skipped: No operators added, submitting, or game over.")
            return // Don't submit if same as initial, already submitting, or game is over
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _feedbackMessage.value = null // Clear previous feedback
            Log.d("GameViewModel", "Submitting solution: $solutionString")
            SocketManager.emitSubmitSolution(gameId, solutionString)
            // Timeout logic remains the same
            delay(5000) // 5 seconds
            if (_isSubmitting.value && _gameResult.value == null) {
                _isSubmitting.value = false
                _feedbackMessage.value = "Submission timed out."
            }
        }
    }


    // --- Timer and Socket Listeners (Mostly Unchanged) ---
    private fun startTimer() {
        // ... (timer logic is the same) ...
        countdownTimer?.cancel() // Cancel any existing timer
        countdownTimer = object : CountDownTimer(_timeLeft.value, 1000) { // Tick every second
            override fun onTick(millisUntilFinished: Long) {
                _timeLeft.value = millisUntilFinished
            }

            override fun onFinish() {
                _timeLeft.value = 0
                Log.d("GameViewModel", "Local timer finished for game $gameId")
            }
        }.start()
        Log.d("GameViewModel", "Timer started for $timeLimitSeconds seconds")
    }

    private fun listenToSocketEvents() {
        // ... (socket listening logic is the same) ...
        if (socketListenerJob?.isActive == true) return
        Log.d("GameViewModel", "Starting to listen to game-specific socket events")

        socketListenerJob = viewModelScope.launch {
            // Listen for Game Over
            launch {
                SocketManager.gameOverFlow
                    .filter { it.gameId == gameId }
                    .catch { e -> Log.e("GameViewModel", "Error in gameOverFlow: ${e.message}") }
                    .collect { result ->
                        Log.i("GameViewModel", "Game Over received for game $gameId: ${result.reason}")
                        countdownTimer?.cancel()
                        _isSubmitting.value = false
                        _gameResult.value = result
                    }
            }

            // Listen for Invalid Solution Feedback
            launch {
                SocketManager.solutionInvalidFlow
                    .filter { it.gameId == gameId }
                    .catch { e -> Log.e("GameViewModel", "Error in solutionInvalidFlow: ${e.message}") }
                    .collect { invalidInfo ->
                        Log.w("GameViewModel", "Invalid solution received: ${invalidInfo.reason}")
                        _isSubmitting.value = false
                        _feedbackMessage.value = formatInvalidReason(invalidInfo)
                        // Optional feedback clear delay
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

    // --- Result Formatting (Mostly Unchanged) ---
    private fun formatInvalidReason(info: SolutionInvalidData): String {
        // ... (same as before) ...
        return when (info.reason) {
            "digit_mismatch" -> "Incorrect digits or order used."
            "wrong_result" -> "Calculation does not equal 100." // Or target number
            "evaluation_error" -> "Invalid mathematical expression."
            "game_already_over" -> "Game has already ended."
            "already_submitted" -> "You already submitted a solution."
            else -> "Invalid solution (${info.reason})."
        }
    }

    fun getGameOutcomeMessage(): String? {
        // ... (same as before) ...
        val result = _gameResult.value ?: return null
        val isWinner = result.winnerId == currentUserId
        val isLoser = result.loserId == currentUserId

        return when (result.status) {
            "completed_win" -> if (isWinner) "You Won!" else "You Lost!"
            "timeout" -> "Time's Up!"
            "completed_draw" -> "It's a Draw!"
            "abandoned" -> if (isWinner) "Opponent Left!" else "Game Abandoned"
            else -> "Game Over (${result.status})"
        }
    }

    fun getResultMessageDetails(): String? {
        // ... (same as before, uses new player1Info/player2Info structure) ...
        val result = _gameResult.value ?: return null
        val opponentSolutionInfo = if (result.player1Info?.id == opponentId) result.player1Info else result.player2Info
        val yourSolutionInfo = if (result.player1Info?.id == currentUserId) result.player1Info else result.player2Info
        val opponentSolution = opponentSolutionInfo?.solution
        val yourSolution = yourSolutionInfo?.solution
        val yourSolutionText = "Your solution: ${yourSolution ?: "Not submitted"}"
        val opponentSolutionText = "$opponentName's solution: ${opponentSolution ?: "Not submitted"}"

        return when (result.reason) {
            "correct_solution" -> {
                val winnerName = if (result.winnerId == currentUserId) "You" else opponentName
                "$winnerName found the solution first.\n$yourSolutionText\n$opponentSolutionText"
            }
            "timeout" -> "Neither player found a solution in time.\n$yourSolutionText\n$opponentSolutionText"
            "opponent_disconnected" -> "$opponentName disconnected."
            else -> "Reason: ${result.reason}\n$yourSolutionText\n$opponentSolutionText"
        }
    }


    override fun onCleared() {
        // ... (same as before) ...
        super.onCleared()
        Log.d("GameViewModel", "ViewModel cleared for game $gameId. Cancelling timer and listeners.")
        countdownTimer?.cancel()
        socketListenerJob?.cancel()
    }
}

// --- ViewModel Factory (Unchanged) ---
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








//package com.example.hectoclash.viewmodels
//
//import android.app.Application
//import android.os.CountDownTimer
//import android.util.Log
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.SavedStateHandle
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewModelScope
//import com.example.hectoclash.data.local.TokenManager
//import com.example.hectoclash.data.models.GameOverData
//import com.example.hectoclash.data.models.SolutionInvalidData
//import com.example.hectoclash.utils.SocketManager
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//
//// Define the segment types
//sealed interface PuzzleSegment {
//    val char: Char
//    data class Digit(override val char: Char) : PuzzleSegment
//    data class Operator(override val char: Char) : PuzzleSegment
//}
//
//class GameViewModel(
//    application: Application,
//    savedStateHandle: SavedStateHandle // For receiving navigation arguments
//) : AndroidViewModel(application) {
//
//    private val tokenManager = TokenManager.getInstance(application)
//
//    // --- Navigation Arguments ---
//    val gameId: String = savedStateHandle["gameId"] ?: error("gameId not provided")
//    val initialPuzzle: String = savedStateHandle["puzzle"] ?: error("puzzle not provided")
//    val opponentName: String = savedStateHandle["opponentName"] ?: "Opponent"
//    val opponentId: String = savedStateHandle["opponentId"] ?: "opponent_id"
//    val timeLimitSeconds: Int = savedStateHandle["timeLimitSeconds"] ?: 60
//
//    // --- Game State ---
//    // Puzzle represented as segments
//    private val _puzzleSegments = MutableStateFlow<List<PuzzleSegment>>(emptyList())
//    val puzzleSegments: StateFlow<List<PuzzleSegment>> = _puzzleSegments.asStateFlow()
//
//    // Cursor position (index *between* segments)
//    // 0 = before first segment, segments.size = after last segment
//    private val _cursorPosition = MutableStateFlow(initialPuzzle.length) // Start cursor at the end
//    val cursorPosition: StateFlow<Int> = _cursorPosition.asStateFlow()
//
//    private val _timeLeft = MutableStateFlow(timeLimitSeconds * 1000L) // Time left in milliseconds
//    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()
//
//    // Flag for submission state
//    private val _isSubmitting = MutableStateFlow(false)
//    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()
//
//    // Game result data
//    private val _gameResult = MutableStateFlow<GameOverData?>(null)
//    val gameResult: StateFlow<GameOverData?> = _gameResult.asStateFlow()
//
//    // Feedback messages
//    private val _feedbackMessage = MutableStateFlow<String?>(null)
//    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()
//
//    private var countdownTimer: CountDownTimer? = null
//    private var socketListenerJob: Job? = null
//    private var currentUserId: String? = null
//
//    // Allowed operators for insertion
//    private val allowedOperators = setOf('+', '-', '*', '/', '(', ')', '^')
//
//    init {
//        Log.d("GameViewModel", "Initializing for game: $gameId")
//        // Initialize segments from the initial puzzle string
//        _puzzleSegments.value = initialPuzzle.map { PuzzleSegment.Digit(it) }
//        _cursorPosition.value = _puzzleSegments.value.size // Cursor initially after last digit
//
//        viewModelScope.launch {
//            currentUserId = tokenManager.getUserId.firstOrNull()
//        }
//        startTimer()
//        listenToSocketEvents()
//    }
//
//    // --- Input Handling ---
//
//    fun setCursorPosition(index: Int) {
//        // Ensure cursor stays within valid bounds (0 to segments.size)
//        _cursorPosition.value = index.coerceIn(0, _puzzleSegments.value.size)
//        Log.d("GameViewModel", "Cursor position set to: ${_cursorPosition.value}")
//    }
//
//    fun insertOperator(operatorChar: Char) {
//        if (!allowedOperators.contains(operatorChar) || gameResult.value != null) return
//
//        val currentSegments = _puzzleSegments.value.toMutableList()
//        val insertIndex = _cursorPosition.value
//
//        // Modified validation to allow multiple operators
//        val canInsert = when {
//            // Allow opening parenthesis anywhere
//            operatorChar == '(' -> true
//
//            // Allow closing parenthesis (with basic validation)
//            operatorChar == ')' -> {
//                // Basic check: ensure there's at least one opening parenthesis somewhere
//                currentSegments.any { it is PuzzleSegment.Operator && it.char == '(' }
//            }
//
//            // Don't allow operators at the very beginning
//            insertIndex == 0 -> false
//
//            // Don't allow operators at the very end
//            insertIndex == currentSegments.size -> false
//
//            // Allow operators between digits or after other operators
//            else -> true
//        }
//
//        if (canInsert) {
//            currentSegments.add(insertIndex, PuzzleSegment.Operator(operatorChar))
//            _puzzleSegments.value = currentSegments
//            _cursorPosition.value = insertIndex + 1 // Move cursor after inserted operator
//            Log.d("GameViewModel", "Inserted '$operatorChar' at $insertIndex. New segments: ${_puzzleSegments.value}")
//        } else {
//            Log.d("GameViewModel", "Insertion of '$operatorChar' at $insertIndex prevented by validation.")
//            _feedbackMessage.value = "Cannot insert operator at this position"
//        }
//    }
//
//    fun handleBackspace() {
//        if (_cursorPosition.value == 0 || gameResult.value != null) return
//
//        val currentSegments = _puzzleSegments.value.toMutableList()
//        val removalIndex = _cursorPosition.value - 1
//
//        // Only remove if the segment *before* the cursor is an Operator
//        if (currentSegments.getOrNull(removalIndex) is PuzzleSegment.Operator) {
//            currentSegments.removeAt(removalIndex)
//            _puzzleSegments.value = currentSegments
//            _cursorPosition.value = removalIndex // Move cursor to where the operator was
//            Log.d("GameViewModel", "Removed operator at $removalIndex. New segments: ${_puzzleSegments.value}")
//        } else {
//            Log.d("GameViewModel", "Backspace ignored: segment at $removalIndex is not an operator.")
//        }
//    }
//
//    // --- Submission ---
//    fun submitSolution() {
//        val solutionString = _puzzleSegments.value.joinToString("") { it.char.toString() }
//        if (solutionString == initialPuzzle || _isSubmitting.value || _gameResult.value != null) {
//            Log.d("GameViewModel", "Submission skipped: No operators added, submitting, or game over.")
//            return // Don't submit if same as initial, already submitting, or game is over
//        }
//
//        viewModelScope.launch {
//            _isSubmitting.value = true
//            _feedbackMessage.value = null // Clear previous feedback
//            Log.d("GameViewModel", "Submitting solution: $solutionString")
//            SocketManager.emitSubmitSolution(gameId, solutionString)
//            // Timeout logic remains the same
//            delay(5000) // 5 seconds
//            if (_isSubmitting.value && _gameResult.value == null) {
//                _isSubmitting.value = false
//                _feedbackMessage.value = "Submission timed out."
//            }
//        }
//    }
//
//
//    // --- Timer and Socket Listeners (Mostly Unchanged) ---
//    private fun startTimer() {
//        // ... (timer logic is the same) ...
//        countdownTimer?.cancel() // Cancel any existing timer
//        countdownTimer = object : CountDownTimer(_timeLeft.value, 1000) { // Tick every second
//            override fun onTick(millisUntilFinished: Long) {
//                _timeLeft.value = millisUntilFinished
//            }
//
//            override fun onFinish() {
//                _timeLeft.value = 0
//                Log.d("GameViewModel", "Local timer finished for game $gameId")
//            }
//        }.start()
//        Log.d("GameViewModel", "Timer started for $timeLimitSeconds seconds")
//    }
//
//    private fun listenToSocketEvents() {
//        // ... (socket listening logic is the same) ...
//        if (socketListenerJob?.isActive == true) return
//        Log.d("GameViewModel", "Starting to listen to game-specific socket events")
//
//        socketListenerJob = viewModelScope.launch {
//            // Listen for Game Over
//            launch {
//                SocketManager.gameOverFlow
//                    .filter { it.gameId == gameId }
//                    .catch { e -> Log.e("GameViewModel", "Error in gameOverFlow: ${e.message}") }
//                    .collect { result ->
//                        Log.i("GameViewModel", "Game Over received for game $gameId: ${result.reason}")
//                        countdownTimer?.cancel()
//                        _isSubmitting.value = false
//                        _gameResult.value = result
//                    }
//            }
//
//            // Listen for Invalid Solution Feedback
//            launch {
//                SocketManager.solutionInvalidFlow
//                    .filter { it.gameId == gameId }
//                    .catch { e -> Log.e("GameViewModel", "Error in solutionInvalidFlow: ${e.message}") }
//                    .collect { invalidInfo ->
//                        Log.w("GameViewModel", "Invalid solution received: ${invalidInfo.reason}")
//                        _isSubmitting.value = false
//                        _feedbackMessage.value = formatInvalidReason(invalidInfo)
//                        // Optional feedback clear delay
//                        launch {
//                            delay(3000)
//                            if (_feedbackMessage.value == formatInvalidReason(invalidInfo)) {
//                                _feedbackMessage.value = null
//                            }
//                        }
//                    }
//            }
//        }
//    }
//
//    // --- Result Formatting (Mostly Unchanged) ---
//    private fun formatInvalidReason(info: SolutionInvalidData): String {
//        // ... (same as before) ...
//        return when (info.reason) {
//            "digit_mismatch" -> "Incorrect digits or order used."
//            "wrong_result" -> "Calculation does not equal 100." // Or target number
//            "evaluation_error" -> "Invalid mathematical expression."
//            "game_already_over" -> "Game has already ended."
//            "already_submitted" -> "You already submitted a solution."
//            else -> "Invalid solution (${info.reason})."
//        }
//    }
//
//    fun getGameOutcomeMessage(): String? {
//        // ... (same as before) ...
//        val result = _gameResult.value ?: return null
//        val isWinner = result.winnerId == currentUserId
//        val isLoser = result.loserId == currentUserId
//
//        return when (result.status) {
//            "completed_win" -> if (isWinner) "You Won!" else "You Lost!"
//            "timeout" -> "Time's Up!"
//            "completed_draw" -> "It's a Draw!"
//            "abandoned" -> if (isWinner) "Opponent Left!" else "Game Abandoned"
//            else -> "Game Over (${result.status})"
//        }
//    }
//
//    fun getResultMessageDetails(): String? {
//        // ... (same as before, uses new player1Info/player2Info structure) ...
//        val result = _gameResult.value ?: return null
//        val opponentSolutionInfo = if (result.player1Info?.id == opponentId) result.player1Info else result.player2Info
//        val yourSolutionInfo = if (result.player1Info?.id == currentUserId) result.player1Info else result.player2Info
//        val opponentSolution = opponentSolutionInfo?.solution
//        val yourSolution = yourSolutionInfo?.solution
//        val yourSolutionText = "Your solution: ${yourSolution ?: "Not submitted"}"
//        val opponentSolutionText = "$opponentName's solution: ${opponentSolution ?: "Not submitted"}"
//
//        return when (result.reason) {
//            "correct_solution" -> {
//                val winnerName = if (result.winnerId == currentUserId) "You" else opponentName
//                "$winnerName found the solution first.\n$yourSolutionText\n$opponentSolutionText"
//            }
//            "timeout" -> "Neither player found a solution in time.\n$yourSolutionText\n$opponentSolutionText"
//            "opponent_disconnected" -> "$opponentName disconnected."
//            else -> "Reason: ${result.reason}\n$yourSolutionText\n$opponentSolutionText"
//        }
//    }
//
//
//    override fun onCleared() {
//        // ... (same as before) ...
//        super.onCleared()
//        Log.d("GameViewModel", "ViewModel cleared for game $gameId. Cancelling timer and listeners.")
//        countdownTimer?.cancel()
//        socketListenerJob?.cancel()
//    }
//}
//
//// --- ViewModel Factory (Unchanged) ---
//class GameViewModelFactory(
//    private val application: Application,
//    private val savedStateHandle: SavedStateHandle
//) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return GameViewModel(application, savedStateHandle) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
//    }
//}