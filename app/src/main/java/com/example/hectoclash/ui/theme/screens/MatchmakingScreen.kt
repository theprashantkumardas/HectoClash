package com.example.hectoclash.ui.theme.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.hectoclash.viewmodels.MatchmakingState
import com.example.hectoclash.viewmodels.MatchmakingViewModel
import kotlinx.coroutines.delay

@Composable
fun MatchmakingScreen(
    navController: NavController, // To navigate back on cancel/failure
    viewModel: MatchmakingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Automatically start matchmaking when the screen is entered
    LaunchedEffect(Unit) {
        viewModel.startMatchmaking()
    }

    // Handle leaving the screen on cancel or failure
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is MatchmakingState.Canceled -> {
                snackbarHostState.showSnackbar("Matchmaking canceled")
                navController.popBackStack()
            }
            is MatchmakingState.Failed -> {
                snackbarHostState.showSnackbar("Matchmaking failed: ${state.reason}")
                // Delay slightly before popping back to allow snackbar to show
                delay(1500)
                navController.popBackStack()
            }
            else -> {} // Searching or Idle (initial state)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            when (uiState) {
                is MatchmakingState.Searching, MatchmakingState.Idle -> {
                    SearchingAnimation() // Display searching animation
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Searching for an opponent...",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = { viewModel.cancelMatchmaking() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Cancel Search")
                    }
                }
                else -> {
                    // Error/Canceled states are handled by LaunchedEffect popping back
                    // You could show a brief message here too if needed before pop
                    CircularProgressIndicator() // Placeholder while navigating back
                }
            }
        }
    }
}

@Composable
fun SearchingAnimation() {
    // Simple pulsing animation example
    val infiniteTransition = rememberInfiniteTransition(label = "Searching Pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Pulse Alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Pulse Scale"
    )

    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(120.dp),
            strokeWidth = 6.dp
        )
        // Example using graphicsLayer for pulse effect
        Icon(
            Icons.Filled.PersonSearch, // Or a game controller icon
            contentDescription = "Searching Icon",
            modifier = Modifier
                .size(60.dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                ),
            tint = MaterialTheme.colorScheme.primary
        )

        // Alternative: Use a Lottie animation if you have one
        // val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.your_search_animation))
        // LottieAnimation(composition = composition, iterations = LottieConstants.IterateForever)
    }
}