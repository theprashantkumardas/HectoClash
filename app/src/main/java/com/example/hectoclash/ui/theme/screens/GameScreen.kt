package com.example.hectoclash.ui.theme.screens

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Import all
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Auto-mirrored back icon
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.hectoclash.data.models.* // Import models
import com.example.hectoclash.viewmodels.GameViewModel
import com.example.hectoclash.viewmodels.GameViewModelFactory
import com.example.hectoclash.viewmodels.ROUND_TRANSITION_DELAY_MS
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavController,
    gameId: String,
    // Removed initialPuzzle, timeLimitSeconds
    opponentName: String,
    opponentId: String,
    // Use factory for ViewModel instantiation
    viewModel: GameViewModel = viewModel(
        factory = GameViewModelFactory(
            LocalContext.current.applicationContext as Application,
            SavedStateHandle(mapOf( // Pass nav args to SavedStateHandle
                "gameId" to gameId,
                "opponentName" to opponentName, // Keep opponent info if needed by ViewModel directly
                "opponentId" to opponentId
            ))
        )
    )
) {
    // Collect StateFlows
    val currentRound by viewModel.currentRound.collectAsState()
    val totalRounds by viewModel.totalRounds.collectAsState()
    val puzzle by viewModel.puzzle.collectAsState()
    val player1Score by viewModel.player1Score.collectAsState() // TODO: Determine player perspective
    val player2Score by viewModel.player2Score.collectAsState()
    val roundTimeLeft by viewModel.roundTimeLeft.collectAsState()
    val solutionInput by viewModel.solutionInput.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val roundResultInfo by viewModel.roundResultInfo.collectAsState() // Result of the *last* round
    val challengeResult by viewModel.challengeResult.collectAsState() // Final challenge result

    val snackbarHostState = remember { SnackbarHostState() }

    // Show feedback (incorrect solution, etc.) in snackbar
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearFeedbackMessage() // Clear after showing
        }
    }

    // Determine player perspective (assuming player 1 is 'us' for now)
    // TODO: Get actual user ID and compare with player1/player2 IDs from challenge data if needed
    val myScore = player1Score
    val opponentScore = player2Score
    val opponentInfo = viewModel.opponentInfo // Get from ViewModel

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ChallengeTopBar(
                opponentName = opponentInfo.name,
                currentRound = currentRound,
                totalRounds = totalRounds,
                myScore = myScore,
                opponentScore = opponentScore,
                timeLeft = roundTimeLeft // Show round timer
            )
        }
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .animateContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Section: Puzzle
                    PuzzleDisplay(puzzle = puzzle, round = currentRound, totalRounds = totalRounds)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Middle Section: Input Area
                    SolutionInputArea(
                        solution = solutionInput,
                        onSolutionChange = viewModel::onSolutionInputChange,
                        onSubmit = viewModel::submitSolution,
                        enabled = challengeResult == null && !isSubmitting && currentRound > 0, // Disable if challenge over or submitting
                        isSubmitting = isSubmitting
                    )

                    Spacer(modifier = Modifier.weight(1f)) // Pushes input area down
                }
            }

            // --- Round Over Brief Overlay ---
            AnimatedVisibility(
                visible = roundResultInfo != null && challengeResult == null, // Show only if round ended BUT challenge hasn't
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                roundResultInfo?.let { result ->
                    // Simple text overlay, could be more elaborate
                    RoundResultOverlay(result = result)
                }
            }

            // --- Challenge Over Overlay ---
            challengeResult?.let { result ->
                ChallengeOverOverlay( // Renamed from GameOverOverlay
                    result = result,
                    viewModel = viewModel, // Pass viewModel to get outcome message
                    onExit = {
                        navController.popBackStack() // Go back
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeTopBar(
    opponentName: String,
    currentRound: Int,
    totalRounds: Int,
    myScore: Int,
    opponentScore: Int,
    timeLeft: Long
) {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val timeColor = if (timeLeft <= 10000 && timeLeft > 0) MaterialTheme.colorScheme.error else LocalContentColor.current

    TopAppBar(
        title = {
            // Show score and opponent name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("You ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("$myScore - $opponentScore ")
                Text(opponentName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Show Round Number
                if (currentRound > 0 && totalRounds > 0) {
                    Text(
                        "R: $currentRound/$totalRounds",
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Show Timer
                Icon(Icons.Default.Timer, contentDescription = "Time Left", tint = timeColor)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = timeFormatted,
                    fontWeight = FontWeight.Bold,
                    color = timeColor,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant // Slightly different background
        )
    )
}

@Composable
fun PuzzleDisplay(puzzle: String, round: Int, totalRounds: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (round > 0) {
                Text(
                    text = "Round $round of $totalRounds",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                Text(
                    text = "Waiting for challenge to start...",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedContent( // Animate puzzle change
                targetState = puzzle,
                transitionSpec = {
                    //(slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                    fadeIn() togetherWith fadeOut() // Simpler fade
                }
            ) { currentPuzzle ->
                Text(
                    text = currentPuzzle.chunked(1).joinToString(" "), // Add spaces
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = "Make 100 using +, -, *, /, ()",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolutionInputArea( // Mostly unchanged
    solution: String,
    onSolutionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean,
    isSubmitting: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = solution,
            onValueChange = onSolutionChange,
            label = { Text("Enter your solution") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled && !isSubmitting, // Ensure enabled also considers submitting state
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii) // Allow symbols
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            enabled = enabled && solution.isNotBlank() && !isSubmitting, // Disable if blank or submitting
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(Modifier.size(24.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
            } else {
                Text("Submit Solution", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun RoundResultOverlay(result: RoundOverData) {
    // Simple overlay example - could be made more elaborate
    // This appears *briefly* between rounds
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)) // Semi-transparent background
            .clickable(enabled = false) {}, // Consume clicks
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
                    result.roundWinnerId != null -> "Round Won!" // TODO: Check if winner is 'us'
                    result.reason == "timeout" -> "Round Timed Out!"
                    else -> "Round Draw!" // Assuming draw if no winner and not timeout
                }
                // TODO: Determine if winner ID is current user
                val outcomeColor = when {
                    result.roundWinnerId != null -> Color(0xFF4CAF50) // Green
                    result.reason == "timeout" -> Color.Gray
                    else -> MaterialTheme.colorScheme.secondary
                }

                Text(roundOutcome, style = MaterialTheme.typography.titleLarge, color = outcomeColor)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Score: ${result.player1Score} - ${result.player2Score}", // TODO: Show from player perspective
                    style = MaterialTheme.typography.titleMedium
                )
                // Optionally show solutions if needed
                // Text("Correct solution was related to: ${result.puzzle}")
            }
        }
    }
}


@Composable
fun ChallengeOverOverlay( // Renamed from GameOverOverlay
    result: ChallengeOverData,
    viewModel: GameViewModel, // Get message from ViewModel
    onExit: () -> Unit
) {
    val outcomeMessage = viewModel.getChallengeOutcomeMessage() // Use new helper
    val resultDetails = viewModel.getChallengeResultMessageDetails() // Use new helper

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
                    color = when { // Determine color based on outcome string
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
                // Optionally display final score breakdown or round history if needed from result.roundsData

                Spacer(Modifier.height(24.dp))

                Button(onClick = onExit) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) // Use AutoMirrored
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Back to Lobby")
                }
            }
        }
    }
}