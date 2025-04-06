package com.example.hectoclashapp.ui.theme.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hectoclash.ui.theme.components.BottomNavBar
import com.example.hectoclash.ui.theme.components.BottomNavItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(mainNavController: NavController) {
    // Create a nested NavController for the home screen tabs
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            HomeBottomNavBar(navController)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // Nested NavHost for the home screen tabs
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Play.route
            ) {
                composable(BottomNavItem.Play.route) {
                    PlayScreen(mainNavController) // Pass the main NavController for navigation to FriendsList
                }
                composable(BottomNavItem.Leaderboard.route) {
                    LeaderboardScreen()
                }
                composable(BottomNavItem.Rewards.route) {
                    RewardsScreen()
                }
                composable(BottomNavItem.Profile.route) {
                    ProfileScreen(onLogout = {
                        mainNavController.navigate("sign_in") {
                            popUpTo("home") { inclusive = true } // Clears home and everything from the backstack
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun HomeBottomNavBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Play,
        BottomNavItem.Leaderboard,
        BottomNavItem.Rewards,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    BottomNavBar(items = items, currentDestination = currentDestination) { route ->
        navController.navigate(route) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }
}