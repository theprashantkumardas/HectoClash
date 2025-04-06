package com.example.hectoclash.ui.theme.screens

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hectoclash.navigation.Routes
import com.example.hectoclash.ui.theme.components.BottomNavBar
import com.example.hectoclash.ui.theme.components.BottomNavItem
import com.example.hectoclash.utils.SocketManager
import com.example.hectoclash.viewmodels.ProfileViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun HomeScreen(mainNavController: NavHostController) { // Correct parameter type
    val nestedNavController = rememberNavController()

    val snackbarHostState = remember { SnackbarHostState() } // For friend request notifications

    val scope = rememberCoroutineScope() // <<< Get a coroutine scope here

    // --- Friend Request Notification Listener ---
    LaunchedEffect(key1 = SocketManager.friendRequestReceivedFlow) {
        SocketManager.friendRequestReceivedFlow.collect { data ->
            snackbarHostState.showSnackbar(
                message = "${data.senderName} sent you a friend request!",
                actionLabel = "View", // Optional action to navigate to friends list
                duration = SnackbarDuration.Long
            ).let { result ->
                if (result == SnackbarResult.ActionPerformed) {
                    // Navigate to the Friends tab within the nested controller
                    nestedNavController.navigate(BottomNavItem.Friends.route) { // Assuming Friends tab exists
                        popUpTo(nestedNavController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }, // Add SnackbarHost

        // Call the imported BottomNavBar component
        bottomBar = {
            // Get current destination for highlighting the selected tab
            val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val bottomNavItems = listOf(
                BottomNavItem.Play,
                BottomNavItem.Leaderboard,
                BottomNavItem.Friends, // Use the new Friends item
                BottomNavItem.Profile
            )

            NavigationBar ( containerColor = Color.Black){ // Use Material3 NavigationBar

                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selected,
                        onClick = {
                            nestedNavController.navigate(item.route) {
                                popUpTo(nestedNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }



        },
                // --- <<< NEW Floating Action Button >>> ---
        floatingActionButtonPosition = FabPosition.Center,// Position above BottomNav
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Play Now") },
                icon = { Icon(Icons.Filled.SportsEsports, contentDescription = "Play Now") },
                onClick = {
                    // Navigate to the new Matchmaking screen using the *main* NavController
                    mainNavController.navigate(Routes.MATCHMAKING)
                },
                modifier = Modifier.padding(bottom = 0.dp) // Adjust padding if needed so it doesn't overlap bottom nav too much
            )
        }


//

    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(
                navController = nestedNavController,
                // Use the routes defined in BottomNavItem (which use Routes constants)
                startDestination = BottomNavItem.Play.route
            ) {
                composable(BottomNavItem.Play.route) {
                    PlayScreen(mainNavController = mainNavController)
                }
                composable(BottomNavItem.Leaderboard.route) { LeaderboardScreen() }
//                composable(BottomNavItem.Rewards.route) { RewardsScreen() }
                
                composable(BottomNavItem.Profile.route) {
                    ProfileScreen(
                        onLogout = {
                            SocketManager.disconnect()
                            // Use mainNavController to navigate outside HOME scope
                            mainNavController.navigate(Routes.SIGN_IN) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        },
                        nestedNavController = nestedNavController, // For navigating within home tabs if needed
                        mainNavController =  mainNavController,     // For navigating outside (logout)
                        profileViewModel = viewModel(
                            factory = ProfileViewModelFactory(LocalContext.current.applicationContext as Application)
                        )
                    )
                }

                // Use the correct route for FriendsListScreen
                composable(BottomNavItem.Friends.route) { // Assumes Friends item uses Routes.FRIENDS_LIST
                    FriendsListScreen(
                        navController = nestedNavController, // Use nested for internal nav like back
                        onViewProfile = { userId ->
                            // Use MAIN NavController to go outside Home's scope
                            mainNavController.navigate(Routes.createUserProfileRoute(userId))
                        },
                        onChallengeFriend = { friendId, friendName ->
                            // Emit challenge
                            SocketManager.emitChallengeUser(friendId)
                            // Show feedback in HomeScreen's snackbar
                            scope.launch {
                                snackbarHostState.showSnackbar("Challenge sent to $friendName")
                            }
                        }
                    )
                }
            }
        }
    }
}

