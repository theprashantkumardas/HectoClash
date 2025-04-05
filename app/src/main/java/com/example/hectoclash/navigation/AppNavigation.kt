// src/main/java/com/example/hectoclash/navigation/AppNavigation.kt
package com.example.hectoclash.navigation

import android.app.Application
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
// import androidx.compose.material3.CircularProgressIndicator // Remove if unused
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hectoclash.data.local.TokenManager
import com.example.hectoclash.ui.theme.screens.*
import com.example.hectoclash.utils.SocketManager
import com.example.hectoclash.viewmodels.UserProfileViewModelFactory
import kotlinx.coroutines.flow.firstOrNull
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


object Routes {
    // --- NEW: Add Splash Route ---
    const val SPLASH = "splash"

    // --- Top Level Routes ---
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val HOME = "home" // Root for screens with bottom nav
    const val PLAY_ONLINE = "play_online"
    const val FRIENDS_LIST = "friends_list" // Route name, usage depends on navigation structure

    const val USER_PROFILE = "user_profile/{userId}" // Route for user profile

    const val GAME = "game/{gameId}/{puzzle}/{opponentName}/{opponentId}/{timeLimitSeconds}"

    // --- Nested Routes for Home Bottom Navigation ---
    const val HOME_PLAY_TAB = "home/play"
    const val HOME_LEADERBOARD_TAB = "home/leaderboard"
    const val HOME_FRIENDS_TAB = "home/friends" // Ensure this matches BottomNavItem used in HomeScreen
    const val HOME_PROFILE_TAB = "home/profile"

    // --- Helper Functions ---

    // Use expression body (=) for implicit return and add error handling
    fun createGameRoute(
        gameId: String,
        puzzle: String,
        opponentName: String,
        opponentId: String,
        timeLimitSeconds: Int
    ): String =
        try {
            val encodedPuzzle = URLEncoder.encode(puzzle, StandardCharsets.UTF_8.toString())
            val encodedOpponentName = URLEncoder.encode(opponentName, StandardCharsets.UTF_8.toString())
            "game/$gameId/$encodedPuzzle/$encodedOpponentName/$opponentId/$timeLimitSeconds"
        } catch (e: Exception) {
            Log.e("Routes", "Error encoding game route parameters", e)
            "game/error/error/error/error/0" // Fallback route
        }

    // Use expression body (=) for implicit return
    fun createUserProfileRoute(userId: String): String =
        "user_profile/$userId"

    // Use expression body (=) for implicit return and add error handling
    fun decode(value: String): String =
        try {
            URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            Log.e("Routes", "Error decoding route parameter: $value", e)
            value // Return original value if decoding fails
        }
}


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager.getInstance(context) }

    // Listener for Game Start event to trigger navigation
    LaunchedEffect(key1 = SocketManager.gameStartFlow) {
        SocketManager.gameStartFlow.collect { gameData ->
            Log.d("AppNavigation", "Game Start Flow Collected: ${gameData.gameId}")
            val currentUserId = tokenManager.getUserId.firstOrNull() ?: ""
            val opponent = if (gameData.player1.id == currentUserId) gameData.player2 else gameData.player1

            // This call should now work correctly with the fixed function definition
            val route = Routes.createGameRoute(
                gameId = gameData.gameId,
                puzzle = gameData.puzzle,
                opponentName = opponent.name,
                opponentId = opponent.id,
                timeLimitSeconds = gameData.timeLimitSeconds
            )
            Log.d("AppNavigation", "Navigating to Game: $route")
            // Consider adding launchSingleTop = true if needed
            if (!route.contains("/error/")) { // Basic check if encoding failed
                navController.navigate(route)
            } else {
                Log.e("AppNavigation", "Failed to create valid game route, not navigating.")
                // Optionally show error to user (e.g., via Snackbar)
            }
        }
    }

    // --- Main Navigation Graph ---
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(navController = navController)
        }

        composable(Routes.SIGN_IN) {
            SignInScreen(navController)
        }
        composable(Routes.SIGN_UP) {
            SignUpScreen(navController)
        }
        composable(Routes.HOME) {
            HomeScreen(mainNavController = navController)
        }
        composable(Routes.PLAY_ONLINE) {
            PlayOnlineScreen(
                onBackClick = { navController.popBackStack() },
                onViewProfile = { userId ->
                    navController.navigate(Routes.createUserProfileRoute(userId))
                }
            )
        }

        // Removed the potentially duplicate top-level FRIENDS_LIST composable.
        // It should primarily be handled within HomeScreen's nested NavHost.

        composable(
            route = Routes.USER_PROFILE,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            if (userId == null) {
                Log.e("AppNavigation", "User ID missing for profile screen")
                navController.popBackStack()
            } else {
                UserProfileScreen(
                    navController = navController,
                    viewModel = viewModel(
                        factory = UserProfileViewModelFactory(
                            LocalContext.current.applicationContext as Application,
                            SavedStateHandle(mapOf("userId" to userId))
                        )
                    )
                )
            }
        }

        composable(
            route = Routes.GAME,
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
                navArgument("puzzle") { type = NavType.StringType },
                navArgument("opponentName") { type = NavType.StringType },
                navArgument("opponentId") { type = NavType.StringType },
                navArgument("timeLimitSeconds") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: "error_id"
            val puzzleEncoded = backStackEntry.arguments?.getString("puzzle") ?: "error_puzzle"
            val opponentNameEncoded = backStackEntry.arguments?.getString("opponentName") ?: "Opponent"
            val opponentId = backStackEntry.arguments?.getString("opponentId") ?: "error_opponent_id"
            val defaultTimeLimit = 60
            val timeLimit = backStackEntry.arguments?.getInt("timeLimitSeconds") ?: defaultTimeLimit

            // Use the robust decode function
            val puzzle = Routes.decode(puzzleEncoded)
            val opponentName = Routes.decode(opponentNameEncoded)

            // Check for errors from arguments OR potential decoding failures (if decode returns original encoded string on error)
            if (gameId == "error_id" || puzzle == "error_puzzle" || opponentId == "error_opponent_id" || puzzleEncoded == "error" || opponentNameEncoded == "error") {
                Log.e("AppNavigation", "Error receiving/decoding game arguments. gameId=$gameId, puzzle=$puzzle, opponentId=$opponentId. Cannot navigate to GameScreen.")
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error loading game data.") }
            } else {
                Log.d("AppNavigation", "Rendering GameScreen for gameId: $gameId, Opponent: $opponentName")
                GameScreen(
                    navController = navController,
                    gameId = gameId,
                    initialPuzzle = puzzle,
                    opponentName = opponentName,
                    opponentId = opponentId,
                    timeLimitSeconds = timeLimit
                )
            }
        }
    }
}