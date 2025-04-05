package com.example.hectoclash.ui.theme.screens

import android.app.Application
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.hectoclash.R
import com.example.hectoclash.data.models.GameOverData
import com.example.hectoclash.viewmodels.GameViewModel
import com.example.hectoclash.viewmodels.GameViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun GameScreen(
//    opponentId: String,
//    opponentName: String,
//    opponentPoints: Int
//) {
//    // Generate a 6-digit number for the game
//    val digits = remember { List(6) { (0..9).random() } }
//    val digitString = remember { digits.joinToString("") }
//
//    // State for the user's answer
//    var userAnswer by remember { mutableStateOf(digitString) }
//
//    // Timer state
//    var remainingTime by remember { mutableStateOf(60) }
//    val formattedTime = remember(remainingTime) {
//        String.format("%02d:%02d", remainingTime / 60, remainingTime % 60)
//    }
//
//    // Current question state
//    val currentQuestion = 1
//    val totalQuestions = 10
//
//    // Timer effect
//    val coroutineScope = rememberCoroutineScope()
//    LaunchedEffect(key1 = true) {
//        coroutineScope.launch {
//            while (remainingTime > 0) {
//                delay(1000)
//                remainingTime--
//            }
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Math Game") },
//                navigationIcon = {
//                    IconButton(onClick = { /* Handle back */ }) {
//                        Icon(
//                            imageVector = Icons.Default.ArrowBack,
//                            contentDescription = "Back"
//                        )
//                    }
//                }
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .padding(16.dp)
//        ) {
//            // Top section with player profiles and game info
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                // Your profile
//                PlayerProfile(
//                    imageUrl = "https://static.vecteezy.com/system/resources/thumbnails/054/078/735/small_2x/gamer-avatar-with-headphones-and-controller-vector.jpg", // Replace with actual URL
//                    name = "You",
//                    points = 1500
//                )
//
//                // Timer and question counter
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Text(
//                        text = formattedTime,
//                        style = MaterialTheme.typography.headlineMedium,
//                        fontWeight = FontWeight.Bold,
//                        color = if (remainingTime <= 10) Color.Red else MaterialTheme.colorScheme.onSurface
//                    )
//
//                    Spacer(modifier = Modifier.height(4.dp))
//
//                    Text(
//                        text = "$currentQuestion/$totalQuestions",
//                        style = MaterialTheme.typography.bodyLarge
//                    )
//                }
//
//                // Opponent profile
//                PlayerProfile(
//                    imageUrl = "https://play-lh.googleusercontent.com/7YVozI6b-RaUAcAL7zBGv2XW_i3clzOgYwEsN3uKezWt-u8UfkGnf7WtmcIuKvGYjcE", // Replace with actual URL
//                    name = opponentName,
//                    points = opponentPoints
//                )
//            }
//
//            Spacer(modifier = Modifier.height(48.dp))
//
//            // Game content
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp),
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .padding(16.dp)
//                        .fillMaxWidth(),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Text(
//                        text = "Insert operators between digits to create a valid expression:",
//                        style = MaterialTheme.typography.bodyLarge,
//                        textAlign = TextAlign.Center
//                    )
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    Text(
//                        text = digitString,
//                        style = MaterialTheme.typography.headlineLarge,
//                        fontWeight = FontWeight.Bold
//                    )
//
//                    Spacer(modifier = Modifier.height(24.dp))
//
//                    OutlinedTextField(
//                        value = userAnswer,
//                        onValueChange = { userAnswer = it },
//                        label = { Text("Insert operators (e.g., 1+2*3-4/5+6)") },
//                        modifier = Modifier.fillMaxWidth(),
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
//                    )
//
//                    Spacer(modifier = Modifier.height(24.dp))
//
//                    Button(
//                        onClick = { /* Handle submission */ },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(50.dp)
//                    ) {
//                        Text("Submit")
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun PlayerProfile(
//    imageUrl: String,
//    name: String,
//    points: Int
//) {
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        AsyncImage(
//            model = ImageRequest.Builder(LocalContext.current)
//                .data(imageUrl)
//                .crossfade(true)
//                .build(),
//            contentDescription = "Profile picture of $name",
//            modifier = Modifier
//                .size(60.dp)
//                .clip(CircleShape),
//            contentScale = ContentScale.Crop,
//            error = painterResource(id = R.drawable.default_profile)
//        )
//
//        Spacer(modifier = Modifier.height(4.dp))
//
//        Text(
//            text = name,
//            style = MaterialTheme.typography.bodyMedium,
//            fontWeight = FontWeight.Bold
//        )
//
//        Text(
//            text = "$points pts",
//            style = MaterialTheme.typography.bodySmall
//        )
//    }
//}

// Make GameScreen accept the new arguments
@Composable
fun GameScreen(
    navController: NavController,
    gameId: String,
    initialPuzzle: String,
    opponentName: String,
    opponentId: String, // Keep if needed for display/logic
    timeLimitSeconds: Int,
    // Use factory for ViewModel instantiation
    viewModel: GameViewModel = viewModel(
        factory = GameViewModelFactory(
            LocalContext.current.applicationContext as Application,
            SavedStateHandle(mapOf( // Pass nav args to SavedStateHandle
                "gameId" to gameId,
                "puzzle" to initialPuzzle,
                "opponentName" to opponentName,
                "opponentId" to opponentId,
                "timeLimitSeconds" to timeLimitSeconds
            ))
        )
    )
) {
    val puzzle by viewModel.puzzle.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val solutionInput by viewModel.solutionInput.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val gameResult by viewModel.gameResult.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show feedback (invalid solution) in snackbar
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            // ViewModel handles clearing the message state
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { GameTopBar(opponentName = opponentName, timeLeft = timeLeft) }
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()) // Make content scrollable
                    .animateContentSize(), // Animate size changes
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween // Pushes input to bottom
            ) {
                // Top Section: Puzzle
                PuzzleDisplay(puzzle = puzzle)

                Spacer(modifier = Modifier.height(24.dp))

                // Middle Section: Input Area
                SolutionInputArea(
                    solution = solutionInput,
                    onSolutionChange = viewModel::onSolutionInputChange,
                    onSubmit = viewModel::submitSolution,
                    enabled = gameResult == null && !isSubmitting, // Disable input when game over or submitting
                    isSubmitting = isSubmitting
                )

                // Add some flexible space
                Spacer(modifier = Modifier.weight(1f))

            }

            // --- Game Over Overlay ---
            gameResult?.let { result ->
                GameOverOverlay(
                    result = result,
                    viewModel = viewModel, // Pass viewModel to get outcome message
                    onPlayAgain = { /* TODO: Implement Play Again logic if needed */ },
                    onExit = {
                        navController.popBackStack() // Go back to previous screen (e.g., PlayOnline)
                        // Or navigate to Home: navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTopBar(opponentName: String, timeLeft: Long) {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val timeColor = if (timeLeft <= 10000 && timeLeft > 0) MaterialTheme.colorScheme.error else LocalContentColor.current


    TopAppBar(
        title = { Text("vs $opponentName", maxLines = 1) },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = "Time Left", tint = timeColor)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = timeFormatted,
                    fontWeight = FontWeight.Bold,
                    color = timeColor,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }
    )
}

@Composable
fun PuzzleDisplay(puzzle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Puzzle:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = puzzle.chunked(1).joinToString(" "), // Add spaces between digits
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp // Space out digits
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Make 100 using +, -, *, /, ()", // Add parentheses to instructions
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolutionInputArea(
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
            label = { Text("Enter your solution (e.g., 1*2+...)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            enabled = enabled && solution.isNotBlank(), // Also disable if input is blank
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Submit Solution", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}


@Composable
fun GameOverOverlay(
    result: GameOverData,
    viewModel: GameViewModel, // Get message from ViewModel
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val outcomeMessage = viewModel.getGameOutcomeMessage() ?: "Game Over"
    val resultDetails = viewModel.getResultMessageDetails() ?: ""

    // Semi-transparent background overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) {}, // Consume clicks
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
                        outcomeMessage.contains("Won") -> Color(0xFF4CAF50) // Green for win
                        outcomeMessage.contains("Lost") -> MaterialTheme.colorScheme.error // Red for loss
                        else -> LocalContentColor.current
                    }
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = resultDetails,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                // Optionally display solutions
//                Text("Your solution: ${result.player1Solution ?: result.player2Solution ?: "Not submitted"}", style = MaterialTheme.typography.bodyMedium) // Adjust logic based on who is player1/player2
                // Text("Opponent's solution: ${opponentSolution ?: "Not submitted"}", style = MaterialTheme.typography.bodyMedium)


                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
//                    Button(onClick = onPlayAgain, enabled = false) { // TODO: Implement play again
//                        Icon(Icons.Default.Replay, contentDescription = null)
//                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
//                        Text("Play Again")
//                    }
                    Button(onClick = onExit) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Exit Game")
                    }
                }
            }
        }
    }
}
