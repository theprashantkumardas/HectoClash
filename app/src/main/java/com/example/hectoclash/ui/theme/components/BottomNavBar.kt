package com.example.hectoclash.ui.theme.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.hectoclash.navigation.Routes


// Define BottomNavItem using the Routes constants
sealed class BottomNavItem(
    val route: String, // This route MUST match the routes used in HomeScreen's NavHost
    val title: String,
    val icon: ImageVector
) {
    // Use Routes.ROUTE_NAME
    object Play : BottomNavItem(Routes.HOME_PLAY_TAB, "Play", Icons.Default.Home) // Or SportsEsports
    object Leaderboard : BottomNavItem(Routes.HOME_LEADERBOARD_TAB, "Leaderboard", Icons.Filled.EmojiEvents)
//    object Rewards : BottomNavItem(Routes.HOME_REWARDS_TAB, "Rewards", Icons.Filled.Star)
    object Profile : BottomNavItem(Routes.HOME_PROFILE_TAB, "Profile", Icons.Filled.Person)

    // RENAME REWARDS to FRIENDS
    object Friends : BottomNavItem(Routes.FRIENDS_LIST, "Friends", Icons.Filled.People) // Use FRIENDS_LIST route

}

@Composable
fun BottomNavBar(
    // items parameter is no longer needed if defined within the composable
    currentDestination: NavDestination?,
    onItemClick: (String) -> Unit
) {
    // Define items list directly here using the sealed class objects
    val items = listOf(
        BottomNavItem.Play,
        BottomNavItem.Leaderboard,
//        BottomNavItem.Rewards,
        BottomNavItem.Friends, // Use FRIENDS_LIST route
        BottomNavItem.Profile
    )

    NavigationBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            // Check if the current destination's route matches the item's route
            val selected = currentDestination?.hierarchy?.any { navDest ->
                navDest.route == item.route
            } == true

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = selected,
                onClick = { onItemClick(item.route) } // onItemClick receives the correct route (e.g., "home/play")
            )
        }
    }
}