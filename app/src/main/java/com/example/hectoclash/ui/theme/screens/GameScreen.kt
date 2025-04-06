package com.example.hectoclash.ui.theme.screens

import android.app.Application
import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.Backspace
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
import com.example.hectoclash.data.local.TokenManager
import com.example.hectoclash.data.models.*
import com.example.hectoclash.ui.theme.*
import com.example.hectoclash.viewmodels.GameViewModel
import com.example.hectoclash.viewmodels.GameViewModelFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay


// Data model for puzzle segments (from friend's code)
sealed class PuzzleSegment {
    abstract val char: Char
    data class Digit(override val char: Char) : PuzzleSegment()
    data class Operator(override val char: Char) : PuzzleSegment()
}
@OptIn(ExperimentalMaterial3Api::class)
/// Helper function to create puzzle segments (from friend's code)
// Takes the raw puzzle string and the solution string built by the keypad
fun createPuzzleSegments(puzzleDigits: String, solutionWithOperators: String): List<PuzzleSegment> {
    val segments = mutableListOf<PuzzleSegment>()
    var digitIndex = 0 // Tracks which digit from the original puzzle we're at

    for (char in solutionWithOperators) {
        // Check if the character corresponds to the *next expected digit* from the original puzzle
        if (digitIndex < puzzleDigits.length && char == puzzleDigits[digitIndex]) {
            segments.add(PuzzleSegment.Digit(char))
            digitIndex++
        } else if (!char.isDigit()) { // It must be an operator (or parenthesis etc.)
            segments.add(PuzzleSegment.Operator(char))
        } else {
            // This case should ideally not happen if input logic is correct,
            // but it means a digit appeared in the solution string that wasn't the next expected puzzle digit.
            // Could happen if digits are somehow inserted incorrectly.
            // We might ignore it or add it as an 'error' segment if needed.
            // For now, let's assume operators are added correctly and skip unexpected digits.
            // Log.w("createPuzzleSegments", "Skipping unexpected digit '$char' in solution.")
        }
    }

    // After processing the solution string, add any remaining digits from the original puzzle
    while (digitIndex < puzzleDigits.length) {
        segments.add(PuzzleSegment.Digit(puzzleDigits[digitIndex]))
        digitIndex++
    }

    return segments
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavController,
    gameId: String,
    // opponentName & opponentId are now primarily handled by ViewModel state derived from challenge_start
    // We keep them in the factory call for potential fallback if needed
    opponentName: String, // Fallback name from nav args
    opponentId: String,
    viewModel: GameViewModel = viewModel(
        factory = GameViewModelFactory(
            LocalContext.current.applicationContext as Application,
            SavedStateHandle(mapOf(
                "gameId" to gameId,
                "opponentName" to opponentName, // Pass fallback name
                "opponentId" to opponentId
            )),
            TokenManager.getInstance(LocalContext.current) // PROVIDE TokenManager
        )
    )
) {
    // Collect States from ViewModel
    val currentRound by viewModel.currentRound.collectAsState()
    val totalRounds by viewModel.totalRounds.collectAsState()
    val puzzle by viewModel.puzzle.collectAsState() // The raw puzzle digits (e.g., "123456")
    val roundTimeLeft by viewModel.roundTimeLeft.collectAsState()
    val solutionInput by viewModel.solutionInput.collectAsState() // The current solution string with operators
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val roundResultInfo by viewModel.roundResultInfo.collectAsState()
    val challengeResult by viewModel.challengeResult.collectAsState()
    val myScore by viewModel.myScore.collectAsState()
    val opponentScore by viewModel.opponentScore.collectAsState()
    val myPlayerInfo by viewModel.myPlayerInfo.collectAsState() // Get own info
    val opponentInfo by viewModel.opponentInfo.collectAsState() // Get opponent info

    // --- Local UI State (for Friend's UI components) ---
    var cursorPosition by remember { mutableStateOf(0) }
    var showCursor by remember { mutableStateOf(true) }
    val hScrollState = rememberScrollState() // Horizontal scroll for puzzle display

    // Calculate segments for RichPuzzleDisplay based on raw puzzle and current solution string
    val puzzleSegments = remember(puzzle, solutionInput) {
        createPuzzleSegments(puzzle, solutionInput) // Use helper
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Show feedback in snackbar
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearFeedbackMessage()
        }
    }

    // Blinking cursor effect
    LaunchedEffect(key1 = challengeResult) { // Stop blinking when game is over
        while (challengeResult == null) {
            delay(500)
            showCursor = !showCursor
        }
        showCursor = false // Hide cursor when game ends
    }

    // Auto-scroll RichPuzzleDisplay to cursor (might need refinement)
    LaunchedEffect(cursorPosition, puzzleSegments.size) {
        // Estimate width - this is tricky without measuring text. Adjust '20' as needed.
        val estimatedCharWidthPx = 20
        val targetScrollPx = (cursorPosition * estimatedCharWidthPx - (hScrollState.viewportSize / 2)) // Try centering cursor
            .coerceIn(0, hScrollState.maxValue)
        hScrollState.animateScrollTo(targetScrollPx)
    }


    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            // Use Friend's Top Bar
            GameTopBarWithPlayers(
                yourName = myPlayerInfo?.name ?: "You", // Use info from VM, fallback
                opponentName = opponentInfo?.name ?: opponentName, // Use info from VM, fallback
                // yourImageUrl = myPlayerInfo?.imageUrl, // Pass URLs if available
                // opponentImageUrl = opponentInfo?.imageUrl,
                timeLeft = roundTimeLeft,
                currentRound = currentRound,
                totalRounds = totalRounds,
                myScore = myScore, // Pass computed scores
                opponentScore = opponentScore
            )
        },
        containerColor = MaterialTheme.colorScheme.background // Friend's UI background suggestion
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        // Removed vertical scroll from main column, keypad is fixed at bottom
                        .padding(bottom = 0.dp), // Remove bottom padding if keypad handles it
                    horizontalAlignment = Alignment.CenterHorizontally
                    // Let content fill space, keypad will be at bottom
                    // verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(16.dp)) // Space below top bar

                    // Question number display (Optional, friend's has it in top bar now)
                    /* if (currentRound > 0 && totalRounds > 0) { ... } */

                    // Use Friend's Rich Puzzle Display
                    RichPuzzleDisplay(
                        segments = puzzleSegments,
                        cursorPosition = cursorPosition,
                        onCursorPositionChange = { newPosition ->
                            // Ensure cursor stays within valid bounds (0 to size)
                            cursorPosition = newPosition.coerceIn(0, puzzleSegments.size)
                        },
                        showCursor = showCursor && challengeResult == null, // Show only if game ongoing
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .weight(1f), // Let puzzle display take available vertical space
                        scrollState = hScrollState
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Instruction text (Optional)
                    Text(
                        text = "Tap between numbers to place operator",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray, // Use a less prominent color
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Use Friend's Operator Keypad
                    OperatorKeypad(
                        onOperatorClick = { operator ->
                            val currentSolution = solutionInput // Get current solution string
                            val pos = calculateStringIndex(puzzle, currentSolution, cursorPosition) // Calculate real string index

                            if (pos != -1) {
                                // Insert operator into the string at the calculated position
                                val newSolution = currentSolution.substring(0, pos) + operator + currentSolution.substring(pos)
                                viewModel.updateSolutionInput(newSolution) // Update ViewModel state
                                cursorPosition++ // Move cursor after the inserted operator
                            } else {
                                Log.w("GameScreen", "Could not determine string index for cursor position $cursorPosition")
                                // Maybe show feedback?
                            }
                        },
                        onBackspaceClick = {
                            val currentSolution = solutionInput
                            // Calculate string index *before* the cursor
                            val pos = calculateStringIndex(puzzle, currentSolution, cursorPosition)

                            if (cursorPosition > 0 && pos > 0) { // Need pos > 0 to backspace something
                                // Find the character in the *solution string* just before the cursor's effective position
                                val charToRemove = currentSolution.getOrNull(pos - 1)

                                // Only allow backspacing operators or parentheses
                                if (charToRemove != null && !charToRemove.isDigit()) {
                                    val newSolution = currentSolution.substring(0, pos - 1) + currentSolution.substring(pos)
                                    viewModel.updateSolutionInput(newSolution)
                                    cursorPosition-- // Move cursor back
                                } else {
                                    Log.d("GameScreen", "Backspace ignored: Tried to delete digit or at start.")
                                    // Optional: Add haptic feedback or visual cue
                                }
                            } else {
                                Log.d("GameScreen", "Backspace ignored: At start of input.")
                            }
                        },
                        onSubmitClick = viewModel::submitSolution,
                        enabled = challengeResult == null && !isSubmitting && currentRound > 0
                    )
                    // Add small padding at the very bottom if needed
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // --- Round Over Brief Overlay ---
            AnimatedVisibility(
                visible = roundResultInfo != null && challengeResult == null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                roundResultInfo?.let { result ->
                    // Use the ViewModel helper for message and color
                    RoundResultOverlay(
                        roundNumber = result.roundNumber,
                        outcomeMessage = viewModel.getRoundOutcomeMessage(result),
                        outcomeColor = viewModel.getRoundOutcomeColor(result),
                        // Pass computed scores directly
                        myScore = myScore,
                        opponentScore = opponentScore
                    )
                }
            }

            // --- Challenge Over Overlay ---
            challengeResult?.let { result ->
                // Use Friend's GameOverOverlay (ensure it matches structure)
                GameOverOverlay( // Assuming friend's overlay name is GameOverOverlay
                    result = result,
                    viewModel = viewModel, // Pass VM for messages
                    onPlayAgain = { /* TODO if needed */ }, // Friend's overlay might have this
                    onExit = { navController.popBackStack() } // Navigate back on OK/Exit
                )
            }

            // --- Loading indicator during submission ---
            if (isSubmitting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(enabled = false) {}, // Prevent clicks during submit
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// --- Helper Function to map cursor position in segments to string index in solutionInput ---
// This is crucial for inserting/deleting operators correctly
fun calculateStringIndex(puzzleDigits: String, solutionWithOperators: String, cursorPosition: Int): Int {
    var stringIndex = 0
    var segmentsProcessed = 0
    var digitIndex = 0

    // Iterate through the solution string, correlating with puzzle digits
    for (char in solutionWithOperators) {
        if (segmentsProcessed == cursorPosition) {
            return stringIndex // Found the string index corresponding to the cursor position
        }

        stringIndex++ // Increment string index for this character
        segmentsProcessed++ // Count this segment (digit or operator)

        // Track if it was a digit from the original puzzle
        if (digitIndex < puzzleDigits.length && char == puzzleDigits[digitIndex]) {
            digitIndex++
        }
    }

    // If cursor is at the very end
    if (segmentsProcessed == cursorPosition) {
        return stringIndex
    }

    // Should not happen if cursorPosition is valid (0 to segments.size)
    Log.e("calculateStringIndex", "Failed to find string index for cursor $cursorPosition")
    return -1 // Indicate error
}


// --- Friend's UI Components (Copied and adapted) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTopBarWithPlayers(
    yourName: String,
    opponentName: String,
    yourImageUrl: String? = null, // Keep placeholders for images
    opponentImageUrl: String? = null,
    timeLeft: Long,
    currentRound: Int,
    totalRounds: Int,
    myScore: Int,       // Added score params
    opponentScore: Int // Added score params
) {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val timeColor = if (timeLeft <= 10000L && timeLeft > 0L) MaterialTheme.colorScheme.error else LocalContentColor.current

    TopAppBar(
        title = { /* Empty title, info moved to nav/actions */ },
        navigationIcon = { PlayerScoreInfo(name = yourName, score = myScore, imageUrl = yourImageUrl, color = ProfilePink) },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Timer and Round
                Icon( Icons.Default.Timer, contentDescription = "Time Left", tint = timeColor )
                Spacer(Modifier.width(4.dp))
                Text( text = timeFormatted, fontWeight = FontWeight.Bold, color = timeColor )
                Spacer(Modifier.width(8.dp))
                if (currentRound > 0 && totalRounds > 0) {
                    Text( "R: $currentRound/$totalRounds", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                // Opponent Info on the right
                PlayerScoreInfo(name = opponentName, score = opponentScore, imageUrl = opponentImageUrl, color = PurpleFriend)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            // containerColor = Color.Gray.copy(alpha = 0.1f) // Friend used Gray
            containerColor = MaterialTheme.colorScheme.surfaceVariant // Use theme color
        )
    )
}

// Modified Player Info to include Score
@Composable
fun PlayerScoreInfo(name: String, score: Int, imageUrl: String?, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        // Score Badge
        Box(
            modifier = Modifier
                .size(24.dp) // Smaller score badge
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score.toString(),
                color = Color.White, // Ensure text is visible
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        // Player Icon/Image (Placeholder)
        Box(
            modifier = Modifier
                .size(36.dp) // Slightly smaller icon
                .clip(CircleShape)
                .background(color.copy(alpha=0.5f)), // Lighter background for icon itself
            contentAlignment = Alignment.Center
        ) {
            // TODO: Use CoilAsyncImage if imageUrl is provided
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "Profile picture of $name",
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        // Player Name
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium, // Adjusted style
            fontWeight = FontWeight.SemiBold,
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
    scrollState: androidx.compose.foundation.ScrollState // Use foundation ScrollState
) {
    val cursorColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            // .background(MaterialTheme.colorScheme.surfaceVariant) // Removed background for cleaner look maybe?
            .padding(vertical = 8.dp) // Add some vertical padding
    ) {
        // Optional: Grid background (comment out if not desired)
        // DrawGridBackground(Modifier.matchParentSize())

        Surface( // Card-like appearance for the puzzle area
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth() // Take full width
                // .heightIn(min = 60.dp, max = 80.dp) // Control height
                .wrapContentHeight() // Adjust height based on content
                .padding(horizontal = 8.dp), // Padding around the surface
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp), // Padding inside the surface
                contentAlignment = Alignment.CenterStart // Align content to start
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(scrollState) // Enable horizontal scrolling
                        .fillMaxWidth(), // Allow row to take width for scrolling
                    // horizontalArrangement = Arrangement.Center, // Center content within the scrollable row
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cursor at the very beginning (index 0)
                    ClickableCursorArea(
                        index = 0,
                        currentCursorPosition = cursorPosition,
                        showCursor = showCursor,
                        cursorColor = cursorColor,
                        onClick = { onCursorPositionChange(0) },
                    )

                    // Display segments and cursors between them
                    segments.forEachIndexed { index, segment ->
                        Text(
                            text = segment.char.toString(),
                            style = when (segment) {
                                is PuzzleSegment.Digit -> MaterialTheme.typography.headlineLarge.copy( // Larger font
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                is PuzzleSegment.Operator -> MaterialTheme.typography.headlineLarge.copy( // Larger font
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.primary // Operators in primary color
                                )
                            },
                            modifier = Modifier
                                .clickable { onCursorPositionChange(index + 1) } // Click text to place cursor after it
                                .padding(horizontal = 2.dp) // Small padding around chars
                        )

                        // Cursor after this segment (index + 1)
                        ClickableCursorArea(
                            index = index + 1,
                            currentCursorPosition = cursorPosition,
                            showCursor = showCursor,
                            cursorColor = cursorColor,
                            onClick = { onCursorPositionChange(index + 1) },
                        )
                    } // End forEachIndexed
                } // End Row
            } // End Box (inner content box)
        } // End Surface
    } // End Box (outer container)
}


@Composable
fun ClickableCursorArea(
    index: Int,
    currentCursorPosition: Int,
    showCursor: Boolean,
    cursorColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp, // Height for the clickable area and cursor line
    width: Dp = 8.dp    // Width of the clickable area around the cursor line
) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clickable(onClick = onClick), // Make the area clickable
        contentAlignment = Alignment.Center
    ) {
        // Draw the blinking cursor line if this is the active position
        if (index == currentCursorPosition && showCursor) {
            Divider(
                color = cursorColor,
                modifier = Modifier
                    .fillMaxHeight(0.7f) // Adjust cursor line height relative to area
                    .width(2.dp)         // Cursor line thickness
            )
        }
    }
}

// DrawGridBackground (optional, can keep from friend's code if desired)
@Composable
fun DrawGridBackground() { /* ... as before ... */ }

// Operator Keypad (Use friend's layout)
@Composable
fun OperatorKeypad(
    onOperatorClick: (Char) -> Unit,
    onBackspaceClick: () -> Unit,
    onSubmitClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    // Friend's button layout structure
    val buttons = listOf(
        listOf("(", ")", "+"),
        listOf("/", "*", "-"),
        // listOf("^", "←", "Enter") // Friend had caret, adjust if needed
        listOf(" ", "←", "Enter") // Replace caret with space if not used
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)) // Keypad background
            .padding(horizontal = 8.dp, vertical = 12.dp), // Padding around keypad
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp) // Space between rows
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { btnText ->
                    val buttonModifier = Modifier
                        .weight(1f) // Equal weight distribution
                        .height(52.dp) // Slightly smaller buttons

                    when (btnText) {
                        " " -> Spacer(modifier = buttonModifier) // Use spacer for empty slot if needed
                        "←" -> KeypadButton( // Backspace
                            text = btnText,
                            onClick = onBackspaceClick,
                            enabled = enabled,
                            modifier = buttonModifier,
                            isIcon = true, // Use icon for backspace
                            containerColor = MaterialTheme.colorScheme.secondaryContainer, // Different color
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        "Enter" -> KeypadButton( // Submit
                            text = btnText,
                            onClick = onSubmitClick,
                            enabled = enabled,
                            modifier = buttonModifier,
                            isIcon = true, // Use checkmark icon
                            containerColor = MaterialTheme.colorScheme.primary, // Primary color
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                        else -> KeypadButton( // Operators
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

// Keypad Button (Use friend's styling)
@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant, // Default button color
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant, // Default text color
    isIcon: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp), // More rounded
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.3f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(4.dp) // Adjust padding if needed
    ) {
        if (isIcon) {
            when (text) {
                "←" -> Icon(Icons.AutoMirrored.Filled.Backspace, "Backspace", modifier = Modifier.size(24.dp)) // Use AutoMirrored
                "Enter" -> Icon(Icons.Default.Check, "Submit", modifier = Modifier.size(24.dp))
                // Add other icons if needed
            }
        } else {
            Text(
                text = text,
                fontSize = 22.sp, // Larger operator font size
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Use friend's Round Result Overlay (Modified to use VM helpers)
@Composable
fun RoundResultOverlay(
    roundNumber: Int,
    outcomeMessage: String,
    outcomeColor: Color,
    myScore: Int,
    opponentScore: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(horizontal = 40.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(horizontal= 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Round $roundNumber Result",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                // Outcome message already includes score context from ViewModel helper
                Text(outcomeMessage, style = MaterialTheme.typography.headlineSmall, color = outcomeColor, textAlign = TextAlign.Center)
                // Spacer(Modifier.height(16.dp))
                // Text( // Score display might be redundant if included in message
                //     "Score: $myScore - $opponentScore",
                //     style = MaterialTheme.typography.titleMedium
                // )
            }
        }
    }
}


// Use friend's Game Over Overlay
@Composable
fun GameOverOverlay(
    result: ChallengeOverData,
    viewModel: GameViewModel,
    onPlayAgain: () -> Unit, // Keep param even if not used yet
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
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    color = when { // Use same color logic
                        outcomeMessage.contains("Won") -> Color(0xFF4CAF50)
                        outcomeMessage.contains("Lost") -> Color.Red // Use Theme color
                        outcomeMessage.contains("Draw") -> Color.Yellow // Use Theme color
                        outcomeMessage.contains("Error") -> MaterialTheme.colorScheme.error
                        else -> LocalContentColor.current
                    },
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = resultDetails, // Includes final score
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // Friend's version used "OK" button
                Button(onClick = onExit) {
                    Icon(Icons.Default.Check, contentDescription = null) // Use checkmark
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("OK")
                }
            }
        }
    }
}