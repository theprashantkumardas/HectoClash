package com.example.hectoclash.ui.theme.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.*
import com.example.hectoclash.R
import com.example.hectoclash.data.local.TokenManager
import com.example.hectoclash.navigation.Routes
import com.example.hectoclash.utils.SocketManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull

// Define a minimum splash display time (e.g., 3 seconds)
private const val SPLASH_MIN_DURATION_MS = 3500L

@Composable
fun SplashScreen(navController: NavHostController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager.getInstance(context) }
    var targetRoute by remember { mutableStateOf<String?>(null) }

    // --- Lottie Animation Setup ---
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_animation))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    // --- Logic to determine next screen and handle delay (Unchanged) ---
    LaunchedEffect(key1 = Unit) {
        val startTime = System.currentTimeMillis()

        // 1. Check login state
        val token = tokenManager.getToken.firstOrNull()
        val destination = if (!token.isNullOrEmpty()) {
            val userId = tokenManager.getUserId.firstOrNull()
            if (!userId.isNullOrEmpty()) {
                SocketManager.connect(userId)
                Log.d("SplashScreen", "User logged in ($userId), connecting socket.")
            } else {
                Log.w("SplashScreen", "User logged in but userId is null/empty.")
            }
            Routes.HOME
        } else {
            Log.d("SplashScreen", "User not logged in.")
            Routes.SIGN_IN
        }
        Log.d("SplashScreen", "Target destination determined: $destination")

        // 2. Ensure minimum splash duration
        val elapsedTime = System.currentTimeMillis() - startTime
        val remainingTime = SPLASH_MIN_DURATION_MS - elapsedTime
        if (remainingTime > 0) {
            Log.d("SplashScreen", "Waiting for remaining splash duration: ${remainingTime}ms")
            delay(remainingTime)
        }

        // 3. Set the target route to trigger navigation
        targetRoute = destination
        Log.d("SplashScreen", "Splash finished, ready to navigate.")
    }

    // --- Navigation (Unchanged) ---
    LaunchedEffect(key1 = targetRoute) {
        targetRoute?.let { route ->
            Log.d("SplashScreen", "Navigating to $route")
            navController.navigate(route) {
                popUpTo(Routes.SPLASH) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // --- UI ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center // Set the Box's content alignment to Center
    ) {
        // Lottie Animation - Centered in the Box
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .size(300.dp) // Set a fixed size for the animation (adjust as needed)
                .align(Alignment.Center), // Explicitly align in the center
            contentScale = ContentScale.Fit // Use Fit to ensure the animation is fully visible
        )

        // Text - Aligned to the bottom center of the Box
        Text(
            text = "HectoClash",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        )
    }
}