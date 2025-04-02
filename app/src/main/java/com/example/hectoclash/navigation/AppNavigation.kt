package com.example.hectoclash.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hectoclash.data.local.TokenManager
import com.example.hectoclash.ui.theme.screens.FriendsListScreen
import com.example.hectoclash.ui.theme.screens.GameScreen
import com.example.hectoclash.ui.theme.screens.HomeScreen
import com.example.hectoclash.ui.theme.screens.ProfileScreen
import com.example.hectoclash.ui.theme.screens.SignInScreen
import com.example.hectoclash.ui.theme.screens.SignUpScreen
import kotlinx.coroutines.flow.firstOrNull

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object SignUp : Screen("sign_up")
    object Home : Screen("home")
    object Play : Screen("play")
    object Leaderboard : Screen("leaderboard")
    object Rewards : Screen("rewards")
    object Profile : Screen("profile")
    object FriendsList : Screen("friends_list")
    object Game : Screen("game/{userId}/{username}/{points}") {
        fun createRoute(userId: String, username: String, points: Int): String {
            return "game/$userId/$username/$points"
        }
    }
}

//@Composable
//fun AppNavigation() {
//    val navController = rememberNavController()
//    NavHost(navController = navController, startDestination = Screen.SignIn.route) {
//        composable(Screen.SignIn.route) {
//            SignInScreen(navController)
//        }
//
//        composable(Screen.SignUp.route) {
//            SignUpScreen(navController)
//        }
//        composable(Screen.Home.route) {
//            HomeScreen(navController)
//        }
//
//        composable(Screen.Profile.route) {
//            ProfileScreen(onLogout = {
//                navController.navigate(Screen.SignIn.route) {
//                    popUpTo(Screen.Home.route) { inclusive = true }
//                }
//            })
//        }
//
//        composable(Screen.FriendsList.route) {
//            FriendsListScreen(navController)
//        }
//        composable(
//            Screen.Game.route,
//            arguments = listOf(
//                navArgument("userId") { type = NavType.StringType },
//                navArgument("username") { type = NavType.StringType },
//                navArgument("points") { type = NavType.IntType }
//            )
//        ) { backStackEntry ->
//            val userId = backStackEntry.arguments?.getString("userId") ?: ""
//            val username = backStackEntry.arguments?.getString("username") ?: ""
//            val points = backStackEntry.arguments?.getInt("points") ?: 0
//            GameScreen(userId, username, points)
//        }
//    }
//}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager.getInstance(context) }

    // Check login state
    var startDestination by remember { mutableStateOf(Screen.SignIn.route) }

    LaunchedEffect(Unit) {
        val token = tokenManager.getToken.firstOrNull()
        if (!token.isNullOrEmpty()) {
            startDestination = Screen.Home.route // Set start destination to Home if user is logged in
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.SignIn.route) {
            SignInScreen(navController)
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(navController)
        }

        composable(Screen.Home.route) {
            HomeScreen(navController)
        }

        composable(Screen.Profile.route) {
            ProfileScreen(onLogout = {
                navController.navigate(Screen.SignIn.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            })
        }

        composable(Screen.FriendsList.route) {
            FriendsListScreen(navController)
        }

        composable(
            Screen.Game.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType },
                navArgument("points") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val points = backStackEntry.arguments?.getInt("points") ?: 0
            GameScreen(userId, username, points)
        }
    }
}