package com.example.hectoclashapp.ui.theme.screens

import android.app.Application
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
import com.example.hectoclash.data.models.GameOverData
import com.example.hectoclash.ui.theme.*
import com.example.hectoclash.viewmodels.GameViewModel
import com.example.hectoclash.viewmodels.GameViewModelFactory
import com.example.hectoclash.viewmodels.PuzzleSegment
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavController,
    gameId: String,
    initialPuzzle: String, // Still needed for factory
    opponentName: String,
    opponentId: String, // Keep if needed for display/logic
    timeLimitSeconds: Int,
    // Use factory for ViewModel instantiation
    viewModel: GameViewModel = viewModel(
        factory = GameViewModelFactory(
            LocalContext.current.applicationContext as Application,
            SavedStateHandle(mapOf( // Pass nav args to SavedStateHandle
                "gameId" to gameId,
                "puzzle" to initialPuzzle, // Pass the original string here
                "opponentName" to opponentName,
                "opponentId" to opponentId,
                "timeLimitSeconds" to timeLimitSeconds
            ))
        )
    )
) {
    // Collect state from ViewModel
    val puzzleSegments by viewModel.puzzleSegments.collectAsState()
    val cursorPosition by viewModel.cursorPosition.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val gameResult by viewModel.gameResult.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current // Needed for PlayerInfo default image

    // Show feedback (invalid solution) in snackbar
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        // Use custom top bar with player info
        topBar = {
            GameTopBarWithPlayers(
                // Fetch current user name from TokenManager or pass it? For now, hardcode "You"
                // Ideally, fetch from ViewModel if it gets user data
                yourName = "You",
                opponentName = opponentName,
                // Add opponent image URL if available, otherwise default
                opponentImageUrl = null, // Replace with actual URL if you have it
                timeLeft = timeLeft
            )
        },
        // Background matches theme
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Apply Scaffold padding
                    .padding(bottom = 16.dp), // Add some bottom padding
                horizontalAlignment = Alignment.CenterHorizontally
                // No verticalArrangement needed, keypad pushes content up
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Custom Puzzle Display Area
                RichPuzzleDisplay(
                    segments = puzzleSegments,
                    cursorPosition = cursorPosition,
                    onCursorPositionChange = viewModel::setCursorPosition,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .weight(1f) // Takes up available vertical space
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Placeholder for user input feedback
                Text(
                    text = "Type out your answer", // Or maybe display current expression?
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnDarkSecondary, // Use secondary text color
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Operator Keypad
                OperatorKeypad(
                    onOperatorClick = viewModel::insertOperator,
                    onBackspaceClick = viewModel::handleBackspace,
                    onSubmitClick = viewModel::submitSolution,
                    enabled = gameResult == null // Disable keypad when game is over
                )
            }

            // --- Game Over Overlay ---
            gameResult?.let { result ->
                GameOverOverlay( // Existing overlay composable
                    result = result,
                    viewModel = viewModel,
                    onPlayAgain = { /* TODO */ },
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

// --- Top Bar with Player Info ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTopBarWithPlayers(
    yourName: String,
    opponentName: String,
    yourImageUrl: String? = null, // Optional image URLs
    opponentImageUrl: String? = null,
    timeLeft: Long
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
            containerColor = MaterialTheme.colorScheme.surface
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
                .size(40.dp) // Smaller avatar
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            // Use Coil or another image loader if you have URLs
            // For now, just use default icon
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
            style = MaterialTheme.typography.labelSmall, // Smaller label
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
    modifier: Modifier = Modifier
) {
    val cursorColor = MaterialTheme.colorScheme.primary
    var showCursor by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    // Blinking cursor effect
    LaunchedEffect(key1 = cursorPosition) {
        showCursor = true
        while (true) {
            delay(500)
            showCursor = !showCursor
        }
    }

    // Auto-scroll to ensure cursor is visible
    LaunchedEffect(cursorPosition) {
        // This is a heuristic to scroll to keep cursor visible
        // You may need to adjust this based on actual character width
        val characterWidth = 20 // estimated average character width in pixels
        val targetScroll = (cursorPosition * characterWidth)
            .coerceAtMost(scrollState.maxValue)
        scrollState.animateScrollTo(targetScroll)
    }

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
                .fillMaxWidth(0.95f) // Increased width (95% of parent)
                .heightIn(max = 70.dp) // Reduced max height even more
                .padding(horizontal = 8.dp), // Reduced horizontal padding
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), // Reduced padding
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
                        height = 30.dp, // Reduced cursor height
                        width = 4.dp   // Reduced cursor width
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
                                    fontSize = 22.sp // Even smaller font
                                )
                                is PuzzleSegment.Operator -> MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 22.sp // Even smaller font
                                )
                            },
                            modifier = Modifier
                                .clickable { onCursorPositionChange(index + 1) }
                                .padding(horizontal = 0.dp) // No horizontal padding to compress
                        )

                        // Cursor after the segment
                        ClickableCursorArea(
                            index = index + 1,
                            currentCursorPosition = cursorPosition,
                            showCursor = showCursor,
                            cursorColor = cursorColor,
                            onClick = { onCursorPositionChange(index + 1) },
                            height = 30.dp, // Reduced cursor height
                            width = 4.dp    // Reduced cursor width
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
            .size(width = width, height = height) // Adjustable dimensions
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Draw blinking cursor if this is the active position
        if (index == currentCursorPosition && showCursor) {
            Divider(
                color = cursorColor,
                modifier = Modifier
                    .fillMaxHeight(0.8f) // Adjust cursor height
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
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) // Optional dashed lines

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
                strokeWidth = strokeWidth,
                // pathEffect = pathEffect // Uncomment for dashed lines
            )
        }

        // Draw horizontal lines
        for (i in 0..horizontalLines) {
            val startY = i * cellSize.toPx()
            drawLine(
                color = color,
                start = Offset(0f, startY),
                end = Offset(size.width, startY),
                strokeWidth = strokeWidth,
                // pathEffect = pathEffect // Uncomment for dashed lines
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
        listOf("^", "←", "Enter") // Using text for Backspace and Enter
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp), // Padding for the keypad area
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing between rows
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally) // Spacing between buttons
            ) {
                row.forEach { btnText ->
                    val buttonModifier = Modifier
                        .weight(1f) // Equal width buttons
                        .height(56.dp) // Fixed height

                    when (btnText) {
                        "←" -> KeypadButton(
                            text = btnText, // Display text/icon
                            onClick = onBackspaceClick,
                            enabled = enabled,
                            modifier = buttonModifier,
                            isIcon = true // Treat as icon for styling if needed
                        )
                        "Enter" -> KeypadButton(
                            text = btnText,
                            onClick = onSubmitClick,
                            enabled = enabled, // Submit might have separate logic?
                            modifier = buttonModifier,
                            containerColor = MaterialTheme.colorScheme.primary, // Highlight Enter
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                        else -> KeypadButton(
                            text = btnText,
                            onClick = { onOperatorClick(btnText[0]) }, // Get char from string
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
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant, // Darker button background
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isIcon: Boolean = false // Flag if it's primarily an icon
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp), // Slightly rounded buttons
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(0.dp) // Remove default padding for custom content alignment
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
                fontSize = if (text == "Enter") 16.sp else 20.sp, // Smaller text for "Enter"
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// --- Game Over Overlay (Mostly Unchanged) ---
@Composable
fun GameOverOverlay(
    result: GameOverData,
    viewModel: GameViewModel, // Get message from ViewModel
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    // ... (same as before) ...
    val outcomeMessage = viewModel.getGameOutcomeMessage() ?: "Game Over"
    val resultDetails = viewModel.getResultMessageDetails() ?: ""

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
                        else -> LocalContentColor.current
                    }
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = resultDetails,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly // Or Arrangement.Center if only Exit
                ) {
                    Button(onClick = onExit) {
                        Icon(Icons.Default.Check, contentDescription = null) // Changed icon to Check
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("OK") // Changed text to OK
                    }
                }
            }
        }
    }
}



//package com.example.hectoclash.ui.theme.screens
//
//import android.app.Application
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.horizontalScroll
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.AccountCircle
//import androidx.compose.material.icons.filled.Backspace
//import androidx.compose.material.icons.filled.Check
//import androidx.compose.material.icons.filled.Timer
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.PathEffect
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.SavedStateHandle
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import com.example.hectoclash.data.models.GameOverData
//import com.example.hectoclash.ui.theme.*
//import com.example.hectoclash.viewmodels.GameViewModel
//import com.example.hectoclash.viewmodels.GameViewModelFactory
//import com.example.hectoclash.viewmodels.PuzzleSegment
//import java.util.concurrent.TimeUnit
//import kotlinx.coroutines.delay
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun GameScreen(
//    navController: NavController,
//    gameId: String,
//    initialPuzzle: String, // Still needed for factory
//    opponentName: String,
//    opponentId: String, // Keep if needed for display/logic
//    timeLimitSeconds: Int,
//    // Use factory for ViewModel instantiation
//    viewModel: GameViewModel = viewModel(
//        factory = GameViewModelFactory(
//            LocalContext.current.applicationContext as Application,
//            SavedStateHandle(mapOf( // Pass nav args to SavedStateHandle
//                "gameId" to gameId,
//                "puzzle" to initialPuzzle, // Pass the original string here
//                "opponentName" to opponentName,
//                "opponentId" to opponentId,
//                "timeLimitSeconds" to timeLimitSeconds
//            ))
//        )
//    )
//) {
//    // Collect state from ViewModel
//    val puzzleSegments by viewModel.puzzleSegments.collectAsState()
//    val cursorPosition by viewModel.cursorPosition.collectAsState()
//    val timeLeft by viewModel.timeLeft.collectAsState()
//    val isSubmitting by viewModel.isSubmitting.collectAsState()
//    val gameResult by viewModel.gameResult.collectAsState()
//    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
//
//    val snackbarHostState = remember { SnackbarHostState() }
//    val context = LocalContext.current // Needed for PlayerInfo default image
//
//    // Show feedback (invalid solution) in snackbar
//    LaunchedEffect(feedbackMessage) {
//        feedbackMessage?.let {
//            snackbarHostState.showSnackbar(
//                message = it,
//                duration = SnackbarDuration.Short
//            )
//        }
//    }
//
//    Scaffold(
//        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
//        // Use custom top bar with player info
//        topBar = {
//            GameTopBarWithPlayers(
//                // Fetch current user name from TokenManager or pass it? For now, hardcode "You"
//                // Ideally, fetch from ViewModel if it gets user data
//                yourName = "You",
//                opponentName = opponentName,
//                // Add opponent image URL if available, otherwise default
//                opponentImageUrl = null, // Replace with actual URL if you have it
//                timeLeft = timeLeft
//            )
//        },
//        // Background matches theme
//        containerColor = MaterialTheme.colorScheme.background
//    ) { paddingValues ->
//
//        Box(modifier = Modifier.fillMaxSize()) {
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(paddingValues) // Apply Scaffold padding
//                    .padding(bottom = 16.dp), // Add some bottom padding
//                horizontalAlignment = Alignment.CenterHorizontally
//                // No verticalArrangement needed, keypad pushes content up
//            ) {
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Custom Puzzle Display Area
//                RichPuzzleDisplay(
//                    segments = puzzleSegments,
//                    cursorPosition = cursorPosition,
//                    onCursorPositionChange = viewModel::setCursorPosition,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 16.dp)
//                        .weight(1f) // Takes up available vertical space
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Placeholder for user input feedback
//                Text(
//                    text = "Type out your answer", // Or maybe display current expression?
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = TextOnDarkSecondary, // Use secondary text color
//                    modifier = Modifier.padding(bottom = 8.dp)
//                )
//
//                // Operator Keypad
//                OperatorKeypad(
//                    onOperatorClick = viewModel::insertOperator,
//                    onBackspaceClick = viewModel::handleBackspace,
//                    onSubmitClick = viewModel::submitSolution,
//                    enabled = gameResult == null // Disable keypad when game is over
//                )
//            }
//
//            // --- Game Over Overlay ---
//            gameResult?.let { result ->
//                GameOverOverlay( // Existing overlay composable
//                    result = result,
//                    viewModel = viewModel,
//                    onPlayAgain = { /* TODO */ },
//                    onExit = { navController.popBackStack() }
//                )
//            }
//
//            // --- Loading indicator during submission ---
//            if (isSubmitting) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(Color.Black.copy(alpha = 0.5f)),
//                    contentAlignment = Alignment.Center
//                ) {
//                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
//                }
//            }
//        }
//    }
//}
//
//// --- Top Bar with Player Info ---
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun GameTopBarWithPlayers(
//    yourName: String,
//    opponentName: String,
//    yourImageUrl: String? = null, // Optional image URLs
//    opponentImageUrl: String? = null,
//    timeLeft: Long
//) {
//    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft)
//    val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60
//    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
//    val timeColor = if (timeLeft <= 10000 && timeLeft > 0) MaterialTheme.colorScheme.error else LocalContentColor.current
//
//    TopAppBar(
//        title = {
//            // Center timer in title area
//            Row(
//                horizontalArrangement = Arrangement.Center,
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Icon(
//                    Icons.Default.Timer,
//                    contentDescription = "Time Left",
//                    tint = timeColor
//                )
//                Spacer(Modifier.width(4.dp))
//                Text(
//                    text = timeFormatted,
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold,
//                    color = timeColor
//                )
//            }
//        },
//        navigationIcon = { PlayerInfo(name = yourName, imageUrl = yourImageUrl, color = ProfilePink) },
//        actions = {
//            // Only opponent info on the right
//            PlayerInfo(name = opponentName, imageUrl = opponentImageUrl, color = PurpleFriend)
//        },
//        colors = TopAppBarDefaults.topAppBarColors(
//            containerColor = MaterialTheme.colorScheme.surface
//        )
//    )
//}
//
//@Composable
//fun PlayerInfo(name: String, imageUrl: String?, color: Color) {
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        modifier = Modifier.padding(horizontal = 8.dp)
//    ) {
//        Box(
//            modifier = Modifier
//                .size(40.dp) // Smaller avatar
//                .clip(CircleShape)
//                .background(color),
//            contentAlignment = Alignment.Center
//        ) {
//            // Use Coil or another image loader if you have URLs
//            // For now, just use default icon
//            Icon(
//                imageVector = Icons.Filled.AccountCircle,
//                contentDescription = "Profile picture of $name",
//                modifier = Modifier.size(36.dp),
//                tint = Color.White
//            )
//        }
//        Spacer(modifier = Modifier.height(2.dp))
//        Text(
//            text = name,
//            style = MaterialTheme.typography.labelSmall, // Smaller label
//            maxLines = 1,
//            color = MaterialTheme.colorScheme.onSurface
//        )
//    }
//}
//
//
//@Composable
//fun RichPuzzleDisplay(
//    segments: List<PuzzleSegment>,
//    cursorPosition: Int,
//    onCursorPositionChange: (Int) -> Unit,
//    modifier: Modifier = Modifier
//) {
//    val cursorColor = MaterialTheme.colorScheme.primary
//    var showCursor by remember { mutableStateOf(true) }
//
//    // Blinking cursor effect
//    LaunchedEffect(key1 = cursorPosition) {
//        showCursor = true
//        while (true) {
//            delay(500)
//            showCursor = !showCursor
//        }
//    }
//
//    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
//        DrawGridBackground(
//            modifier = Modifier.fillMaxSize(),
//            color = Color.Gray.copy(alpha = 0.3f),
//            strokeWidth = 1.dp.value,
//            cellSize = 30.dp
//        )
//
//        Surface(
//            modifier = Modifier
//                .align(Alignment.Center)
//                .fillMaxWidth(0.9f) // Increased width (90% of parent)
//                .heightIn(max = 80.dp) // Reduced max height
//                .padding(horizontal = 16.dp),
//            shape = RoundedCornerShape(20.dp),
//            color = MaterialTheme.colorScheme.surface,
//            shadowElevation = 4.dp
//        ) {
//            Box(
//                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), // Reduced vertical padding
//                contentAlignment = Alignment.Center
//            ) {
//                Row(
//                    modifier = Modifier
//                        .horizontalScroll(rememberScrollState())
//                        .wrapContentSize(),
//                    horizontalArrangement = Arrangement.Center,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    // First cursor position
//                    ClickableCursorArea(
//                        index = 0,
//                        currentCursorPosition = cursorPosition,
//                        showCursor = showCursor,
//                        cursorColor = cursorColor,
//                        onClick = { onCursorPositionChange(0) },
//                        height = 32.dp // Reduced cursor height
//                    )
//
//                    // All segments with their cursor positions
//                    segments.forEachIndexed { index, segment ->
//                        // The segment itself
//                        Text(
//                            text = segment.char.toString(),
//                            style = when (segment) {
//                                is PuzzleSegment.Digit -> MaterialTheme.typography.headlineMedium.copy( // Smaller font
//                                    fontWeight = FontWeight.Bold,
//                                    color = MaterialTheme.colorScheme.onSurface,
//                                    fontSize = 24.sp // Explicit smaller size
//                                )
//                                is PuzzleSegment.Operator -> MaterialTheme.typography.headlineMedium.copy( // Smaller font
//                                    fontWeight = FontWeight.Normal,
//                                    color = MaterialTheme.colorScheme.primary,
//                                    fontSize = 24.sp // Explicit smaller size
//                                )
//                            },
//                            modifier = Modifier
//                                .clickable { onCursorPositionChange(index + 1) }
//                                .padding(horizontal = 2.dp)
//                        )
//
//                        // Cursor after the segment
//                        ClickableCursorArea(
//                            index = index + 1,
//                            currentCursorPosition = cursorPosition,
//                            showCursor = showCursor,
//                            cursorColor = cursorColor,
//                            onClick = { onCursorPositionChange(index + 1) },
//                            height = 32.dp // Reduced cursor height
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun ClickableCursorArea(
//    index: Int,
//    currentCursorPosition: Int,
//    showCursor: Boolean,
//    cursorColor: Color,
//    onClick: () -> Unit,
//    modifier: Modifier = Modifier,
//    height: Dp = 40.dp // Parameterized height
//) {
//    Box(
//        modifier = modifier
//            .size(width = 8.dp, height = height) // Adjustable height
//            .clickable(onClick = onClick),
//        contentAlignment = Alignment.Center
//    ) {
//        // Draw blinking cursor if this is the active position
//        if (index == currentCursorPosition && showCursor) {
//            Divider(
//                color = cursorColor,
//                modifier = Modifier
//                    .fillMaxHeight(0.8f) // Adjust cursor height
//                    .width(2.dp)
//            )
//        }
//    }
//}
//
//// Simple Grid Background Composable
//@Composable
//fun DrawGridBackground(
//    modifier: Modifier = Modifier,
//    color: Color = Color.Gray,
//    strokeWidth: Float = 1f,
//    cellSize: Dp = 20.dp
//) {
//    Canvas(modifier = modifier) {
//        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) // Optional dashed lines
//
//        // Calculate number of lines based on size and cell size
//        val verticalLines = (size.width / cellSize.toPx()).toInt()
//        val horizontalLines = (size.height / cellSize.toPx()).toInt()
//
//        // Draw vertical lines
//        for (i in 0..verticalLines) {
//            val startX = i * cellSize.toPx()
//            drawLine(
//                color = color,
//                start = Offset(startX, 0f),
//                end = Offset(startX, size.height),
//                strokeWidth = strokeWidth,
//                // pathEffect = pathEffect // Uncomment for dashed lines
//            )
//        }
//
//        // Draw horizontal lines
//        for (i in 0..horizontalLines) {
//            val startY = i * cellSize.toPx()
//            drawLine(
//                color = color,
//                start = Offset(0f, startY),
//                end = Offset(size.width, startY),
//                strokeWidth = strokeWidth,
//                // pathEffect = pathEffect // Uncomment for dashed lines
//            )
//        }
//    }
//}
//
//
//// --- Operator Keypad ---
//@Composable
//fun OperatorKeypad(
//    onOperatorClick: (Char) -> Unit,
//    onBackspaceClick: () -> Unit,
//    onSubmitClick: () -> Unit,
//    enabled: Boolean,
//    modifier: Modifier = Modifier
//) {
//    val buttons = listOf(
//        listOf("(", ")", "+"),
//        listOf("/", "*", "-"),
//        listOf("^", "←", "Enter") // Using text for Backspace and Enter
//    )
//
//    Column(
//        modifier = modifier
//            .fillMaxWidth()
//            .padding(horizontal = 8.dp), // Padding for the keypad area
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing between rows
//    ) {
//        buttons.forEach { row ->
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally) // Spacing between buttons
//            ) {
//                row.forEach { btnText ->
//                    val buttonModifier = Modifier
//                        .weight(1f) // Equal width buttons
//                        .height(56.dp) // Fixed height
//
//                    when (btnText) {
//                        "←" -> KeypadButton(
//                            text = btnText, // Display text/icon
//                            onClick = onBackspaceClick,
//                            enabled = enabled,
//                            modifier = buttonModifier,
//                            isIcon = true // Treat as icon for styling if needed
//                        )
//                        "Enter" -> KeypadButton(
//                            text = btnText,
//                            onClick = onSubmitClick,
//                            enabled = enabled, // Submit might have separate logic?
//                            modifier = buttonModifier,
//                            containerColor = MaterialTheme.colorScheme.primary, // Highlight Enter
//                            contentColor = MaterialTheme.colorScheme.onPrimary
//                        )
//                        else -> KeypadButton(
//                            text = btnText,
//                            onClick = { onOperatorClick(btnText[0]) }, // Get char from string
//                            enabled = enabled,
//                            modifier = buttonModifier
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun KeypadButton(
//    text: String,
//    onClick: () -> Unit,
//    enabled: Boolean,
//    modifier: Modifier = Modifier,
//    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant, // Darker button background
//    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
//    isIcon: Boolean = false // Flag if it's primarily an icon
//) {
//    Button(
//        onClick = onClick,
//        modifier = modifier,
//        enabled = enabled,
//        shape = RoundedCornerShape(12.dp), // Slightly rounded buttons
//        colors = ButtonDefaults.buttonColors(
//            containerColor = containerColor,
//            contentColor = contentColor,
//            disabledContainerColor = containerColor.copy(alpha = 0.5f),
//            disabledContentColor = contentColor.copy(alpha = 0.5f)
//        ),
//        contentPadding = PaddingValues(0.dp) // Remove default padding for custom content alignment
//    ) {
//        if (isIcon && text == "←") {
//            Icon(
//                imageVector = Icons.Default.Backspace,
//                contentDescription = "Backspace",
//                modifier = Modifier.size(24.dp)
//            )
//        } else {
//            Text(
//                text = text,
//                fontSize = if (text == "Enter") 16.sp else 20.sp, // Smaller text for "Enter"
//                fontWeight = FontWeight.Medium
//            )
//        }
//    }
//}
//
//
//// --- Game Over Overlay (Mostly Unchanged) ---
//@Composable
//fun GameOverOverlay(
//    result: GameOverData,
//    viewModel: GameViewModel, // Get message from ViewModel
//    onPlayAgain: () -> Unit,
//    onExit: () -> Unit
//) {
//    // ... (same as before) ...
//    val outcomeMessage = viewModel.getGameOutcomeMessage() ?: "Game Over"
//    val resultDetails = viewModel.getResultMessageDetails() ?: ""
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.Black.copy(alpha = 0.7f))
//            .clickable(enabled = false) {},
//        contentAlignment = Alignment.Center
//    ) {
//        Card(
//            shape = RoundedCornerShape(16.dp),
//            modifier = Modifier
//                .fillMaxWidth(0.9f)
//                .wrapContentHeight(),
//            elevation = CardDefaults.cardElevation(8.dp)
//        ) {
//            Column(
//                modifier = Modifier.padding(24.dp),
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center
//            ) {
//                Text(
//                    text = outcomeMessage,
//                    style = MaterialTheme.typography.headlineMedium,
//                    fontWeight = FontWeight.Bold,
//                    color = when {
//                        outcomeMessage.contains("Won") -> Color(0xFF4CAF50)
//                        outcomeMessage.contains("Lost") -> MaterialTheme.colorScheme.error
//                        else -> LocalContentColor.current
//                    }
//                )
//                Spacer(Modifier.height(16.dp))
//                Text(
//                    text = resultDetails,
//                    style = MaterialTheme.typography.bodyLarge,
//                    textAlign = TextAlign.Center
//                )
//                Spacer(Modifier.height(24.dp))
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceEvenly // Or Arrangement.Center if only Exit
//                ) {
//                    Button(onClick = onExit) {
//                        Icon(Icons.Default.Check, contentDescription = null) // Changed icon to Check
//                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
//                        Text("OK") // Changed text to OK
//                    }
//                }
//            }
//        }
//    }
//}