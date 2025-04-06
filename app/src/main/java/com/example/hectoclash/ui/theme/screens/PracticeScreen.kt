package com.example.hectoclash.ui.theme.screens

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.hectoclash.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    navController: NavController,
    viewModel: PracticeViewModel = viewModel(
        factory = PracticeViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    // Collect state from ViewModel
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val totalQuestionsCompleted by viewModel.totalQuestionsCompleted.collectAsState()
    val totalQuestionsSkipped by viewModel.totalQuestionsSkipped.collectAsState()
    val currentPuzzle by viewModel.currentPuzzle.collectAsState()
    val solutionInput by viewModel.solutionInput.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()

    // Track cursor position state
    var cursorPosition by remember { mutableStateOf(0) }
    var showCursor by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    // Generate puzzle segments with operators from solution input
    val puzzleSegments = remember(currentPuzzle, solutionInput) {
        createPracticeSegments(currentPuzzle, solutionInput)
    }

    // Reset cursor position when puzzle changes
    LaunchedEffect(currentPuzzle) {
        cursorPosition = 0
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Show feedback in snackbar
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearFeedbackMessage()
        }
    }

    // Auto-advance to next question after correct answer
    LaunchedEffect(isCorrect) {
        if (isCorrect) {
            delay(1500) // Show success message briefly
            viewModel.loadNextPuzzle()
        }
    }

    // Blinking cursor effect
    LaunchedEffect(key1 = Unit) {
        while (true) {
            delay(500)
            showCursor = !showCursor
        }
    }

    // Auto-scroll to ensure cursor is visible
    LaunchedEffect(cursorPosition) {
        val characterWidth = 20 // estimated average character width in pixels
        val targetScroll = (cursorPosition * characterWidth)
            .coerceAtMost(scrollState.maxValue)
        scrollState.animateScrollTo(targetScroll)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PracticeTopBar(
                totalQuestionsCompleted = totalQuestionsCompleted,
                totalQuestionsSkipped = totalQuestionsSkipped,
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Question counter display
//                Text(
//                    text = "Puzzle ${currentQuestionIndex + 1}",
//                    style = MaterialTheme.typography.titleMedium,
//                    color = MaterialTheme.colorScheme.primary,
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier.padding(bottom = 8.dp)
//                )

                // Rich Puzzle Display with Cursor
                PracticeRichPuzzleDisplay(
                    segments = puzzleSegments,
                    cursorPosition = cursorPosition,
                    onCursorPositionChange = { newPosition ->
                        cursorPosition = newPosition.coerceIn(0, puzzleSegments.size)
                    },
                    showCursor = showCursor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .weight(1f),
                    scrollState = scrollState
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Instruction text
//                Text(
//                    text = "Add operators to make 100",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
//                    modifier = Modifier.padding(bottom = 8.dp)
//                )

                // Skip button
                Button(
                    onClick = { viewModel.skipPuzzle() },
                    enabled = !isSubmitting && !isCorrect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Skip")
                    Spacer(Modifier.width(8.dp))
                    Text("Skip This Puzzle")
                }

                Spacer(Modifier.height(8.dp))

                // Operator Keypad
                PracticeOperatorKeypad(
                    onOperatorClick = { operator ->
                        // Insert operator at cursor position
                        viewModel.insertOperatorAtCursor(operator, cursorPosition)
                        cursorPosition++ // Move cursor after inserted operator
                    },
                    onBackspaceClick = {
                        if (cursorPosition > 0 && viewModel.deleteOperatorBeforeCursor(cursorPosition)) {
                            cursorPosition-- // Move cursor back if deletion successful
                        }
                    },
                    onSubmitClick = { viewModel.submitSolution() },
                    enabled = !isSubmitting && !isCorrect
                )
            }

            // --- Success Overlay ---
            AnimatedVisibility(
                visible = isCorrect,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x880A7E07)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Correct",
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                        Text(
                            "Correct!",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- Loading indicator during submission ---
            if (isSubmitting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeTopBar(
    totalQuestionsCompleted: Int,
    totalQuestionsSkipped: Int,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = { Text("Practice Mode") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Show completed questions count
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$totalQuestionsCompleted",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.width(16.dp))

                // Show skipped questions count
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Skipped",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$totalQuestionsSkipped",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Gray // Same as game screen
        )
    )
}

@Composable
fun PracticeRichPuzzleDisplay(
    segments: List<PracticePuzzleSegment>,
    cursorPosition: Int,
    onCursorPositionChange: (Int) -> Unit,
    showCursor: Boolean,
    modifier: Modifier = Modifier,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val cursorColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        PracticeDrawGridBackground(
            modifier = Modifier.fillMaxSize(),
            color = Color.Gray.copy(alpha = 0.3f),
            strokeWidth = 1.dp.value,
            cellSize = 30.dp
        )

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.95f)
                .heightIn(max = 70.dp)
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .wrapContentSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // First cursor position
                    PracticeClickableCursorArea(
                        index = 0,
                        currentCursorPosition = cursorPosition,
                        showCursor = showCursor,
                        cursorColor = cursorColor,
                        onClick = { onCursorPositionChange(0) },
                        height = 30.dp,
                        width = 4.dp
                    )

                    // All segments with their cursor positions
                    segments.forEachIndexed { index, segment ->
                        // The segment itself
                        Text(
                            text = segment.char.toString(),
                            style = when (segment) {
                                is PracticePuzzleSegment.Digit -> MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 22.sp
                                )
                                is PracticePuzzleSegment.Operator -> MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 22.sp
                                )
                            },
                            modifier = Modifier
                                .clickable { onCursorPositionChange(index + 1) }
                                .padding(horizontal = 0.dp)
                        )

                        // Cursor after the segment
                        PracticeClickableCursorArea(
                            index = index + 1,
                            currentCursorPosition = cursorPosition,
                            showCursor = showCursor,
                            cursorColor = cursorColor,
                            onClick = { onCursorPositionChange(index + 1) },
                            height = 30.dp,
                            width = 4.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeClickableCursorArea(
    index: Int,
    currentCursorPosition: Int,
    showCursor: Boolean,
    cursorColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    width: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (index == currentCursorPosition && showCursor) {
            Divider(
                color = cursorColor,
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .width(2.dp)
            )
        }
    }
}

// Simple Grid Background Composable
@Composable
fun PracticeDrawGridBackground(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray,
    strokeWidth: Float = 1f,
    cellSize: Dp = 20.dp
) {
    Canvas(modifier = modifier) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

        // Calculate number of lines based on size and cell size
        val verticalLines = (size.width / cellSize.toPx()).toInt()
        val horizontalLines = (size.height / cellSize.toPx()).toInt()

        // Draw vertical lines
        for (i in 0..verticalLines) {
            val startX = i * cellSize.toPx()
            drawLine(
                color = color,
                start = Offset(startX, 0f),
                end = Offset(startX, size.height),
                strokeWidth = strokeWidth
            )
        }

        // Draw horizontal lines
        for (i in 0..horizontalLines) {
            val startY = i * cellSize.toPx()
            drawLine(
                color = color,
                start = Offset(0f, startY),
                end = Offset(size.width, startY),
                strokeWidth = strokeWidth
            )
        }
    }
}

// --- Operator Keypad ---
@Composable
fun PracticeOperatorKeypad(
    onOperatorClick: (Char) -> Unit,
    onBackspaceClick: () -> Unit,
    onSubmitClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val buttons = listOf(
        listOf("(", ")", "+"),
        listOf("/", "*", "-"),
        listOf("^", "←", "Enter")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { btnText ->
                    val buttonModifier = Modifier
                        .weight(1f)
                        .height(56.dp)

                    when (btnText) {
                        "←" -> PracticeKeypadButton(
                            text = btnText,
                            onClick = onBackspaceClick,
                            enabled = enabled,
                            modifier = buttonModifier,
                            isIcon = true
                        )
                        "Enter" -> PracticeKeypadButton(
                            text = btnText,
                            onClick = onSubmitClick,
                            enabled = enabled,
                            modifier = buttonModifier,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                        else -> PracticeKeypadButton(
                            text = btnText,
                            onClick = { onOperatorClick(btnText[0]) },
                            enabled = enabled,
                            modifier = buttonModifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeKeypadButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isIcon: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (isIcon && text == "←") {
            Icon(
                imageVector = Icons.Default.Backspace,
                contentDescription = "Backspace",
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = text,
                fontSize = if (text == "Enter") 16.sp else 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// --- Data model for puzzle segments (different name to avoid conflict) ---
sealed class PracticePuzzleSegment {
    abstract val char: Char
    data class Digit(override val char: Char) : PracticePuzzleSegment()
    data class Operator(override val char: Char) : PracticePuzzleSegment()
}

// Helper function (different name to avoid conflict)
fun createPracticeSegments(puzzle: String, solutionInput: String): List<PracticePuzzleSegment> {
    val segments = mutableListOf<PracticePuzzleSegment>()
    var digitIndex = 0

    // Process the solution input to create segments
    var i = 0
    while (i < solutionInput.length) {
        if (solutionInput[i].isDigit()) {
            // This is a digit from the original puzzle
            if (digitIndex < puzzle.length) {
                segments.add(PracticePuzzleSegment.Digit(puzzle[digitIndex]))
                digitIndex++
            }
        } else {
            // This is an operator
            segments.add(PracticePuzzleSegment.Operator(solutionInput[i]))
        }
        i++
    }

    // Add any remaining digits from the puzzle
    while (digitIndex < puzzle.length) {
        segments.add(PracticePuzzleSegment.Digit(puzzle[digitIndex]))
        digitIndex++
    }

    return segments
}

// ViewModel for Practice Mode
class PracticeViewModel(application: Application) : AndroidViewModel(application) {
    // Practice state
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex

    private val _totalQuestionsCompleted = MutableStateFlow(0)
    val totalQuestionsCompleted: StateFlow<Int> = _totalQuestionsCompleted

    private val _totalQuestionsSkipped = MutableStateFlow(0)
    val totalQuestionsSkipped: StateFlow<Int> = _totalQuestionsSkipped

    private val _currentPuzzle = MutableStateFlow(generateRandomDigits(6))
    val currentPuzzle: StateFlow<String> = _currentPuzzle

    private val _solutionInput = MutableStateFlow("")
    val solutionInput: StateFlow<String> = _solutionInput

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage

    private val _isCorrect = MutableStateFlow(false)
    val isCorrect: StateFlow<Boolean> = _isCorrect

    init {
        loadNextPuzzle()
    }

    fun updateSolutionInput(input: String) {
        _solutionInput.value = input
    }

    // Insert operator at cursor position
    fun insertOperatorAtCursor(operator: Char, cursorPosition: Int) {
        val segments = createPracticeSegments(_currentPuzzle.value, _solutionInput.value)
        val segmentsBefore = segments.take(cursorPosition)
        val segmentsAfter = segments.drop(cursorPosition)

        // Build the new solution
        var newSolution = ""
        var digitsAdded = 0

        // Add segments before cursor
        segmentsBefore.forEach { segment ->
            if (segment is PracticePuzzleSegment.Digit) {
                if (digitsAdded < _currentPuzzle.value.length) {
                    newSolution += _currentPuzzle.value[digitsAdded]
                    digitsAdded++
                }
            } else {
                newSolution += segment.char
            }
        }

        // Add the new operator
        newSolution += operator

        // Add segments after cursor
        segmentsAfter.forEach { segment ->
            if (segment is PracticePuzzleSegment.Digit) {
                if (digitsAdded < _currentPuzzle.value.length) {
                    newSolution += _currentPuzzle.value[digitsAdded]
                    digitsAdded++
                }
            } else {
                newSolution += segment.char
            }
        }

        // Add any remaining digits
        while (digitsAdded < _currentPuzzle.value.length) {
            newSolution += _currentPuzzle.value[digitsAdded]
            digitsAdded++
        }

        _solutionInput.value = newSolution
    }

    // Delete operator before cursor
    fun deleteOperatorBeforeCursor(cursorPosition: Int): Boolean {
        val segments = createPracticeSegments(_currentPuzzle.value, _solutionInput.value)
        if (cursorPosition <= 0 || cursorPosition > segments.size) return false

        val segmentToRemove = segments.getOrNull(cursorPosition - 1)
        // Only remove operators, not digits
        if (segmentToRemove is PracticePuzzleSegment.Operator) {
            var newSolution = ""
            var digitsAdded = 0

            segments.forEachIndexed { index, segment ->
                if (index != cursorPosition - 1) { // Skip the segment to remove
                    if (segment is PracticePuzzleSegment.Digit) {
                        if (digitsAdded < _currentPuzzle.value.length) {
                            newSolution += _currentPuzzle.value[digitsAdded]
                            digitsAdded++
                        }
                    } else {
                        newSolution += segment.char
                    }
                }
            }

            // Add any remaining digits
            while (digitsAdded < _currentPuzzle.value.length) {
                newSolution += _currentPuzzle.value[digitsAdded]
                digitsAdded++
            }

            _solutionInput.value = newSolution
            return true
        }
        return false
    }

    fun submitSolution() {
        if (_solutionInput.value.isBlank() || _isSubmitting.value || _isCorrect.value) return

        _isSubmitting.value = true

        // Simple evaluation for demo purposes
        // In a real app, you'd use a proper expression evaluator
        try {
            // This is a simplistic evaluation that doesn't handle all cases
            // But it's sufficient for the UI demo
            val expression = _solutionInput.value

            // For simplicity, we'll just check if certain expressions are correct
            val isValid = when (_currentPuzzle.value) {
                "123456" -> expression == "1+2+3+4*5*6" || expression == "1*2*3*4+5+6" || expression == "(1+2+3)*4*5-6"
                "234567" -> expression == "2+3+4*5*6+7" || expression == "2*3*4+56+7" || expression == "(2+3)*45-6-7"
                "345678" -> expression == "3*4*(5+6+7)+8" || expression == "3+4+5+6*7+8" || expression == "3*4*5+6*7+8"
                else -> false
            }

            if (isValid) {
                _isCorrect.value = true
                _totalQuestionsCompleted.value += 1
                _feedbackMessage.value = "Correct! You made 100."
            } else {
                _feedbackMessage.value = "Not correct. Try again!"
            }
        } catch (e: Exception) {
            _feedbackMessage.value = "Invalid expression"
        }

        _isSubmitting.value = false
    }

    fun skipPuzzle() {
        _totalQuestionsSkipped.value += 1
        loadNextPuzzle()
    }

    fun loadNextPuzzle() {
        _currentQuestionIndex.value += 1
        _currentPuzzle.value = generateRandomDigits(6)
        _solutionInput.value = ""
        _isCorrect.value = false
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }

    private fun generateRandomDigits(count: Int): String {
        // For demo purposes, rotate through a few predefined puzzles
        return when (_currentQuestionIndex.value % 3) {
            0 -> "123456"
            1 -> "234567"
            else -> "345678"
        }
    }
}

class PracticeViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PracticeViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}