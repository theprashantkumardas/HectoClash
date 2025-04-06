package com.example.hectoclash.navigation

import android.app.Application
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.hectoclash.ui.theme.screens.FriendsListScreen
//import com.example.hectoclash.ui.theme.screens.FriendsListScreen
import com.example.hectoclash.ui.theme.screens.GameScreen
import com.example.hectoclash.ui.theme.screens.HomeScreen
import com.example.hectoclash.ui.theme.screens.MatchmakingScreen
import com.example.hectoclash.ui.theme.screens.PlayOnlineScreen
import com.example.hectoclash.ui.theme.screens.ProfileScreen
import com.example.hectoclash.ui.theme.screens.SignInScreen
import com.example.hectoclash.ui.theme.screens.SignUpScreen
import com.example.hectoclash.ui.theme.screens.UserProfileScreen
import com.example.hectoclash.utils.SocketManager
import com.example.hectoclash.viewmodels.UserProfileViewModelFactory
import kotlinx.coroutines.flow.firstOrNull
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


object Routes {
    // --- Top Level Routes ---
    const val SPLASH = "splash"
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val HOME = "home" // Root for screens with bottom nav
    const val PLAY_ONLINE = "play_online"
    const val FRIENDS_LIST = "friends_list"

    const val USER_PROFILE = "user_profile/{userId}" // Route for user profile

    const val GAME = "game/{gameId}/{opponentName}/{opponentId}"


    // ... other routes ...
    const val MATCHMAKING = "matchmaking" // <<< NEW Route
    // ... other routes ..


    // --- Nested Routes for Home Bottom Navigation ---
    // Define the actual string values used in HomeScreen's NavHost
    const val HOME_PLAY_TAB = "home/play"
    const val HOME_LEADERBOARD_TAB = "home/leaderboard"
    const val HOME_REWARDS_TAB = "home/rewards"
    const val HOME_PROFILE_TAB = "home/profile"


    // --- Helper Functions ---
    fun createGameRoute(
        gameId: String,
//        puzzle: String,
        opponentName: String,
        opponentId: String,
//        timeLimitSeconds: Int
    ): String {
        // Encoding remains the same
//        val encodedPuzzle = URLEncoder.encode(puzzle, StandardCharsets.UTF_8.toString())
        val encodedOpponentName = URLEncoder.encode(opponentName, StandardCharsets.UTF_8.toString())
        return "game/$gameId/$encodedOpponentName/$opponentId"
    }

    // *** ADD THIS FUNCTION BACK ***
    fun createUserProfileRoute(userId: String): String {
        // Takes the USER_PROFILE pattern and replaces {userId} with the actual id
        return "user_profile/$userId"
    }

    // Optional: Add decode function if needed elsewhere, or keep it local to where args are read
    fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.toString())

}


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager.getInstance(context) }
    var startDestination by remember { mutableStateOf<String?>(null) } // Start as null until checked

    // Determine start destination based on login state
    LaunchedEffect(key1 = tokenManager) {
        val token = tokenManager.getToken.firstOrNull()
        startDestination = if (!token.isNullOrEmpty()) {
            // If logged in, connect socket immediately (if not already)
            val userId = tokenManager.getUserId.firstOrNull()
            if (!userId.isNullOrEmpty()) {
                SocketManager.connect(userId) // Ensure connection on app start if logged in
            }
            Routes.HOME
        } else {
            Routes.SIGN_IN
        }
        Log.d("AppNavigation", "Start destination set to: $startDestination")
    }

    // Listener for Game Start event to trigger navigation
//    LaunchedEffect(key1 = SocketManager.gameStartFlow) {
//        SocketManager.gameStartFlow.collect { gameData ->
//            Log.d("AppNavigation", "Game Start Flow Collected: ${gameData.gameId}")
//            // Determine opponent details based on current user ID
//            val currentUserId = tokenManager.getUserId.firstOrNull() ?: ""
//            val opponent =
//                if (gameData.player1.id == currentUserId) gameData.player2 else gameData.player1
//
//            val route = Routes.createGameRoute(
//                gameId = gameData.gameId,
////                puzzle = gameData.puzzle,
//                opponentName = opponent.name,
//                opponentId = opponent.id,
////                timeLimitSeconds = gameData.timeLimitSeconds
//            )
//            Log.d("AppNavigation", "Navigating to Game: $route")
//            navController.navigate(route) {
//                // Optional: Pop up to home or clear back stack if needed
//                // popUpTo(Routes.HOME)
//            }
//        }
//    }

    // *** CORRECTED LaunchedEffect ***
    LaunchedEffect(key1 = SocketManager.challengeStartFlow) { // <-- LISTEN TO NEW FLOW
        SocketManager.challengeStartFlow.collect { challengeData -> // <-- COLLECT FROM NEW FLOW, use correct variable name
            Log.d("AppNavigation", "[Challenge Start] Flow Collected: ${challengeData.gameId}") // <-- Updated Log Message
            // Determine opponent details based on current user ID
            val currentUserId = tokenManager.getUserId.firstOrNull() ?: ""
            val opponent =
                if (challengeData.player1.id == currentUserId) challengeData.player2 else challengeData.player1

            val route = Routes.createGameRoute( // Use correct helper
                gameId = challengeData.gameId,
                opponentName = opponent.name,
                opponentId = opponent.id
            )
            Log.d("AppNavigation", "[Challenge Start] Navigating to Game: $route") // <-- Updated Log Message
            navController.navigate(route) {
                // Optional: Pop up logic
            }
        }
    }


    // Render NavHost only after startDestination is determined
    if (startDestination != null) {
        NavHost(navController = navController, startDestination = startDestination!!) {
            composable(Routes.SIGN_IN) {
                SignInScreen(navController) // Assumes SignInScreen takes NavController
            }
            composable(Routes.SIGN_UP) {
                SignUpScreen(navController) // Assumes SignUpScreen takes NavController
            }
            composable(Routes.HOME) {
                // HomeScreen now manages its own internal navigation via Bottom Nav
                // It needs the main NavController to navigate *outside* the home tabs (like to PlayOnline)
                HomeScreen(mainNavController = navController)
            }
            composable(Routes.PLAY_ONLINE) {
                // PlayOnlineScreen now only needs the onBackClick lambda
                PlayOnlineScreen(
                    onBackClick = { navController.popBackStack() },
                    // NEW: Add navigation to user profile
                    onViewProfile = { userId ->
                        navController.navigate(Routes.createUserProfileRoute(userId))
                    }
                )
                // The logic previously in the onChallengeUser lambda
                // is now handled inside the onClick of the UserItem's Button
                // within PlayOnlineScreen.kt, which directly calls SocketManager.emitChallengeUser.
            }

            // Update Friends List composable
            composable(Routes.FRIENDS_LIST) {
                FriendsListScreen(
                    navController = navController, // Pass NavController if needed for navigation from this screen
                    onViewProfile = { userId -> // Add callback to view profile from friends list
                        navController.navigate(Routes.createUserProfileRoute(userId))
                    },
                    onChallengeFriend = { friendId, friendName ->
                        // Directly emit challenge via SocketManager
                        Log.d("AppNavigation", "Challenging friend: $friendName ($friendId)")
                        SocketManager.emitChallengeUser(friendId)
                        // Show feedback? (e.g., Snackbar "Challenge sent to $friendName")
                    }
                )
            }

            // Add User Profile composable
            composable(
                route = Routes.USER_PROFILE,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")
                if (userId == null) {
                    Log.e("AppNavigation", "User ID missing for profile screen")
                    // Handle error, maybe pop back stack
                    navController.popBackStack()
                } else {
                    UserProfileScreen(
                        navController = navController, // Pass if needed for back navigation etc.
                        // Instantiate ViewModel using Factory and SavedStateHandle
                        viewModel = viewModel(
                            factory = UserProfileViewModelFactory(
                                LocalContext.current.applicationContext as Application,
                                SavedStateHandle(mapOf("userId" to userId)) // Pass nav arg
                            )
                        )
                    )
                }
            }
            // <<< NEW Composable for Matchmaking >>>
            composable(Routes.MATCHMAKING) {
                MatchmakingScreen(navController = navController)
            }
            // <<< END NEW >>>


            composable(
                // *** PROBLEM AREA 3: Route definition in NavHost ***
                // Use the corrected Routes.GAME constant
                route = Routes.GAME,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.StringType },
                    // *** Remove arguments no longer in the route constant ***
                    // navArgument("puzzle") { type = NavType.StringType },
                    navArgument("opponentName") { type = NavType.StringType }, // Encoded
                    navArgument("opponentId") { type = NavType.StringType },
                    // navArgument("timeLimitSeconds") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                // Decode arguments safely
                val gameId = backStackEntry.arguments?.getString("gameId") ?: "error_id"
//                val puzzleEncoded = backStackEntry.arguments?.getString("puzzle") ?: "error_puzzle"
                val opponentNameEncoded =
                    backStackEntry.arguments?.getString("opponentName") ?: "Opponent"
                val opponentId =
                    backStackEntry.arguments?.getString("opponentId") ?: "error_opponent_id"
                // Default time limit (make sure GAME_TIME_LIMIT_SECONDS is accessible here or pass default)
//                val defaultTimeLimit = 60
//                val timeLimit = backStackEntry.arguments?.getInt("timeLimitSeconds") ?: defaultTimeLimit

                // Handle potential decoding errors
//                val puzzle = try {
//                    URLDecoder.decode(puzzleEncoded, StandardCharsets.UTF_8.toString())
//                } catch (e: Exception) {
//                    Log.e("AppNavigation", "Puzzle decode error", e); "error"
//                }
                val opponentName = try {
//                    URLDecoder.decode(opponentNameEncoded, StandardCharsets.UTF_8.toString())
                    Routes.decode(opponentNameEncoded)
                } catch (e: Exception) {
                    Log.e("AppNavigation", "Opponent Name decode error", e); "Opponent"
                }

                if (gameId == "error_id" || opponentId == "error_opponent_id") {
                    Log.e(
                        "AppNavigation",
                        "Error receiving game arguments. Cannot navigate to GameScreen."
                    )
                    // Optionally show an error message or navigate back
                    // navController.popBackStack() // Example: Go back if args are missing
                    Box(Modifier.fillMaxSize()) { Text("Error loading game data.") } // Show error UI
                } else {
                    Log.d(
                        "AppNavigation",
                        "Rendering GameScreen for gameId: $gameId, Opponent: $opponentName"
                    )
                    // *** CORRECTED: Pass all arguments to GameScreen ***
                    // Pass only the necessary arguments
                    GameScreen(
                        navController = navController,
                        gameId = gameId,
                        opponentName = opponentName,
                        opponentId = opponentId
                    )
                }
            }
        }
    }
}