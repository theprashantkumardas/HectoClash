package com.example.hectoclashapp.ui.theme.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
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
import com.example.hectoclash.navigation.Screen

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Play : BottomNavItem(Screen.Play.route, "Play", Icons.Filled.SportsEsports)
    object Leaderboard : BottomNavItem(Screen.Leaderboard.route, "Leaderboard", Icons.Filled.EmojiEvents)
    object Rewards : BottomNavItem(Screen.Rewards.route, "Rewards", Icons.Filled.Star)
    object Profile : BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.Person)
}

@Composable
fun BottomNavBar(
    items: List<BottomNavItem>,
    currentDestination: NavDestination?,
    onItemClick: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = selected,
                onClick = { onItemClick(item.route) }
            )
        }
    }
}