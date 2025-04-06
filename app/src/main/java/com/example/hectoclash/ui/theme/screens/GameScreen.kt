package com.example.hectoclash.ui.theme.screens

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.hectoclash.data.models.*
import com.example.hectoclash.ui.theme.*
import com.example.hectoclash.viewmodels.GameViewModel
import com.example.hectoclash.viewmodels.GameViewModelFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavController,
    gameId: String,
    opponentName: String,
    opponentId: String,
    // Use factory for ViewModel instantiation
    viewModel: GameViewModel = viewModel(
        factory = GameViewModelFactory(
            LocalContext.current.applicationContext as Application,
            SavedStateHandle(mapOf( // Pass nav args to SavedStateHandle
                "gameId" to gameId,
                "opponentName" to opponentName,
                "opponentId" to opponentId
            ))
        )
    )
) {
    // Collect StateFlows
    val currentRound by viewModel.currentRound.collectAsState()
    val totalRounds by viewModel.totalRounds.collectAsState()
    val puzzle by viewModel.puzzle.collectAsState()
    val roundTimeLeft by viewModel.roundTimeLeft.collectAsState()
    val solutionInput by viewModel.solutionInput.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val roundResultInfo by viewModel.roundResultInfo.collectAsState()
    val challengeResult by viewModel.challengeResult.collectAsState()

    // Track cursor position state
    var cursorPosition by remember { mutableStateOf(0) }
    var showCursor by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    // Generate puzzle segments with operators from solution input
    val puzzleSegments = remember(puzzle, solutionInput) {
        createPuzzleSegments(puzzle, solutionInput)
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
            GameTopBarWithPlayers(
                yourName = "You",
                opponentName = opponentName,
                timeLeft = roundTimeLeft,
                currentRound = currentRound,
                totalRounds = totalRounds
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Question number display at top of screen
                    if (currentRound > 0 && totalRounds > 0) {
                        Text(
                            text = "Question $currentRound of $totalRounds",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Rich Puzzle Display with Cursor
                    RichPuzzleDisplay(
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
                    Text(
                        text = "Type out your answer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextOnDarkSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Operator Keypad
                    OperatorKeypad(
                        onOperatorClick = { operator ->
                            // Fixed: Insert at actual cursor position
                            val segmentsBefore = puzzleSegments.take(cursorPosition)
                            val segmentsAfter = puzzleSegments.drop(cursorPosition)

                            // Build the new solution by reconstructing the string with the operator inserted
                            var newSolution = ""
                            var digitsAdded = 0

                            // Add segments before cursor
                            segmentsBefore.forEach { segment ->
                                if (segment is PuzzleSegment.Digit) {
                                    if (digitsAdded < puzzle.length) {
                                        newSolution += puzzle[digitsAdded]
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
                                if (segment is PuzzleSegment.Digit) {
                                    if (digitsAdded < puzzle.length) {
                                        newSolution += puzzle[digitsAdded]
                                        digitsAdded++
                                    }
                                } else {
                                    newSolution += segment.char
                                }
                            }

                            // Add any remaining digits
                            while (digitsAdded < puzzle.length) {
                                newSolution += puzzle[digitsAdded]
                                digitsAdded++
                            }

                            viewModel.onSolutionInputChange(newSolution)
                            cursorPosition++ // Move cursor after inserted operator
                        },
                        onBackspaceClick = {
                            if (cursorPosition > 0) {
                                // Get the segment before the cursor
                                val segmentToRemove = puzzleSegments.getOrNull(cursorPosition - 1)

                                // Only remove operators, not digits
                                if (segmentToRemove is PuzzleSegment.Operator) {
                                    // Build the new solution without this operator
                                    var newSolution = ""
                                    var digitsAdded = 0

                                    puzzleSegments.forEachIndexed { index, segment ->
                                        if (index != cursorPosition - 1) { // Skip the segment to remove
                                            if (segment is PuzzleSegment.Digit) {
                                                if (digitsAdded < puzzle.length) {
                                                    newSolution += puzzle[digitsAdded]
                                                    digitsAdded++
                                                }
                                            } else {
                                                newSolution += segment.char
                                            }
                                        }
                                    }

                                    // Add any remaining digits
                                    while (digitsAdded < puzzle.length) {
                                        newSolution += puzzle[digitsAdded]
                                        digitsAdded++
                                    }

                                    viewModel.onSolutionInputChange(newSolution)
                                    cursorPosition-- // Move cursor back
                                }
                            }
                        },
                        onSubmitClick = viewModel::submitSolution,
                        enabled = challengeResult == null && !isSubmitting && currentRound > 0
                    )
                }
            }

            // --- Round Over Brief Overlay ---
            AnimatedVisibility(
                visible = roundResultInfo != null && challengeResult == null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                roundResultInfo?.let { result ->
                    RoundResultOverlay(result = result)
                }
            }

            // --- Challenge Over Overlay ---
            challengeResult?.let { result ->
                GameOverOverlay(
                    result = result,
                    viewModel = viewModel,
                    onPlayAgain = { /* Not implemented */ },
                    onExit = { navController.popBackStack() }
                )
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

// Helper function to create puzzle segments with operators from solution input
fun createPuzzleSegments(puzzle: String, solutionInput: String): List<PuzzleSegment> {
    val segments = mutableListOf<PuzzleSegment>()
    var digitIndex = 0

    // Process the solution input to create segments
    var i = 0
    while (i < solutionInput.length) {
        if (solutionInput[i].isDigit()) {
            // This is a digit from the original puzzle
            if (digitIndex < puzzle.length) {
                segments.add(PuzzleSegment.Digit(puzzle[digitIndex]))
                digitIndex++
            }
        } else {
            // This is an operator
            segments.add(PuzzleSegment.Operator(solutionInput[i]))
        }
        i++
    }

    // Add any remaining digits from the puzzle
    while (digitIndex < puzzle.length) {
        segments.add(PuzzleSegment.Digit(puzzle[digitIndex]))
        digitIndex++
    }

    return segments
}

// --- Data model for puzzle segments ---
sealed class PuzzleSegment {
    abstract val char: Char
    data class Digit(override val char: Char) : PuzzleSegment()
    data class Operator(override val char: Char) : PuzzleSegment()
}

// --- Top Bar with Player Info ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTopBarWithPlayers(
    yourName: String,
    opponentName: String,
    yourImageUrl: String? = null,
    opponentImageUrl: String? = null,
    timeLeft: Long,
    currentRound: Int,
    totalRounds: Int
) {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val timeColor = if (timeLeft <= 10000 && timeLeft > 0) MaterialTheme.colorScheme.error else LocalContentColor.current

    TopAppBar(
        title = {
            // Center timer in title area
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Display question number in the title area
                if (currentRound > 0 && totalRounds > 0) {
                    Text(
                        text = "Q$currentRound/$totalRounds",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }

                Icon(
                    Icons.Default.Timer,
                    contentDescription = "Time Left",
                    tint = timeColor
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = timeColor
                )
            }
        },
        navigationIcon = { PlayerInfo(name = yourName, imageUrl = yourImageUrl, color = ProfilePink) },
        actions = {
            // Only opponent info on the right
            PlayerInfo(name = opponentName, imageUrl = opponentImageUrl, color = PurpleFriend)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Gray // Changed to Gray as requested
        )
    )
}

@Composable
fun PlayerInfo(name: String, imageUrl: String?, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "Profile picture of $name",
                modifier = Modifier.size(36.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun RichPuzzleDisplay(
    segments: List<PuzzleSegment>,
    cursorPosition: Int,
    onCursorPositionChange: (Int) -> Unit,
    showCursor: Boolean,
    modifier: Modifier = Modifier,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val cursorColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        DrawGridBackground(
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
                    ClickableCursorArea(
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
                                is PuzzleSegment.Digit -> MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 22.sp
                                )
                                is PuzzleSegment.Operator -> MaterialTheme.typography.headlineMedium.copy(
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
                        ClickableCursorArea(
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
fun ClickableCursorArea(
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
fun DrawGridBackground(
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
fun OperatorKeypad(
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
                        "←" -> KeypadButton(
                            text = btnText,
                            onClick = onBackspaceClick,
                            enabled = enabled,
                            modifier = buttonModifier,
                            isIcon = true
                        )
                        "Enter" -> KeypadButton(
                            text = btnText,
                            onClick = onSubmitClick,
                            enabled = enabled,
                            modifier = buttonModifier,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                        else -> KeypadButton(
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
fun KeypadButton(
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

@Composable
fun RoundResultOverlay(result: RoundOverData) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Round ${result.roundNumber} Over",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                val roundOutcome = when {
                    result.roundWinnerId != null -> "Round Won!"
                    result.reason == "timeout" -> "Round Timed Out!"
                    else -> "Round Draw!"
                }
                val outcomeColor = when {
                    result.roundWinnerId != null -> Color(0xFF4CAF50)
                    result.reason == "timeout" -> Color.Gray
                    else -> MaterialTheme.colorScheme.secondary
                }

                Text(roundOutcome, style = MaterialTheme.typography.titleLarge, color = outcomeColor)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Score: ${result.player1Score} - ${result.player2Score}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    result: ChallengeOverData,
    viewModel: GameViewModel,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val outcomeMessage = viewModel.getChallengeOutcomeMessage()
    val resultDetails = viewModel.getChallengeResultMessageDetails()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = outcomeMessage,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        outcomeMessage.contains("Won") -> Color(0xFF4CAF50)
                        outcomeMessage.contains("Lost") -> MaterialTheme.colorScheme.error
                        outcomeMessage.contains("Draw") -> MaterialTheme.colorScheme.secondary
                        outcomeMessage.contains("Error") -> MaterialTheme.colorScheme.error
                        else -> LocalContentColor.current
                    },
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = resultDetails,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                Button(onClick = onExit) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("OK")
                }
            }
        }
    }
}