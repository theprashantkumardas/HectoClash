package com.example.hectoclash.ui.theme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.hectoclash.R
import com.example.hectoclash.navigation.Routes
import com.example.hectoclash.ui.theme.DarkBackgroundEnd
import com.example.hectoclash.ui.theme.DarkBackgroundStart
import com.example.hectoclash.ui.theme.GreenAccent
import com.example.hectoclash.ui.theme.ProfilePink
import com.example.hectoclash.ui.theme.PurpleFriend
import com.example.hectoclash.ui.theme.TextOnDarkSecondary

// Define data class for friend status (replace with your actual data model)
data class FriendStatus(val id: String, val name: String, val isOnline: Boolean)

@Composable
fun PlayScreen(
    mainNavController: NavHostController,
) {
    // Observe username from ViewModel (replace with your actual state management)
    val username = "HectoClash" // Replace with actual username from ViewModel

    // Define the background gradient
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(DarkBackgroundStart, DarkBackgroundEnd)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()) // Make content scrollable
    ) {
        Spacer(modifier = Modifier.height(16.dp)) // Top padding

        // Top Bar: Username and Profile Icon
        TopAppBar(
            username = username ?: "Player", // Use fetched username or default
            onProfileClick = {
                // Using the fixed approach: navigate to the HOME_PROFILE_TAB directly
                mainNavController.navigate(Routes.HOME_PROFILE_TAB)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Spacer(modifier = Modifier.height(32.dp))

        // "Explore Duels" Heading
        Text(
            text = "EXPLORE DUELS",
            style = MaterialTheme.typography.labelMedium, // Use label style for subheadings
            fontWeight = FontWeight.Bold,
            color = TextOnDarkSecondary,
            letterSpacing = 1.1.sp, // Add some letter spacing
            modifier = Modifier.padding(start = 4.dp) // Align slightly with cards
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Duel Option Cards
        DuelOptionCard(
            title = "ONLINE DUELS",
            subtitle = "Quick-fire hectoc duel",
            onClick = { mainNavController.navigate(Routes.PLAY_ONLINE) }
        )
        DuelOptionCard(
            title = "PLAY A FRIEND",
            subtitle = "Battle your friend",
            onClick = {
                // Navigate to the nested Friends Tab first
                mainNavController.navigate(Routes.FRIENDS_LIST) {
                    launchSingleTop = true
                }
            }
        )
        DuelOptionCard(
            title = "PRACTICE",
            subtitle = "Learn hectoc",
            onClick = { mainNavController.navigate(Routes.PRACTICE) }
        )
        DuelOptionCard(
            title = "WATCH LIVE",
            subtitle = "Spectate a match",
            onClick = { /* TODO: Navigate to Watch Live Screen */ }
        )

        Spacer(modifier = Modifier.height(16.dp)) // Bottom padding before nav bar
    }
}

// --- Reusable Composables for PlayScreen ---

@Composable
fun TopAppBar(username: String, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = username, // Display dynamic username
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape) // Clip the box to make background circular
                .background(ProfilePink) // Use theme color or specific color
                .clickable(onClick = onProfileClick), // Add clickable modifier with onProfileClick callback
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.profile_pic), // Replace with your profile image
                contentDescription = "Profile",
                modifier = Modifier
                    .size(44.dp) // Image slightly smaller than background
                    .clip(CircleShape), // Ensure the image itself is also clipped to circle
                contentScale = ContentScale.Crop // Ensures the image fills the space properly
            )
        }
    }
}

@Composable
fun ActiveFriendsRow(
    friends: List<FriendStatus>,
    onFriendClick: (String) -> Unit,
    onMoreClick: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(friends, key = { it.id }) { friend ->
            FriendIconWithStatus(
                isOnline = friend.isOnline,
                onClick = { onFriendClick(friend.id) }
            )
        }
        item { // "More" Button
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .size(40.dp) // Match friend icon size
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "More Friends",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun FriendIconWithStatus(isOnline: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.BottomEnd, // Align status dot to bottom-end
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
    ) {
        // Background circle with user icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PurpleFriend)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "Friend",
                modifier = Modifier.size(36.dp), // Slightly smaller than background
                tint = Color.White
            )
        }

        // Status indicator dot
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .border(2.dp, DarkBackgroundEnd, CircleShape) // Border color that matches the background
                    .background(GreenAccent) // Use GreenAccent directly
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun DuelOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface( // Use Surface for elevation simulation and background
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp) // Slightly less vertical padding
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp), // Gamified rounded corners
        color = MaterialTheme.colorScheme.surface, // Use theme surface color
        shadowElevation = 2.dp // Subtle shadow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Text content
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnDarkSecondary // Use secondary text color
                )
            }
            // Play Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f)), // Dark semi-transparent background
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = title, // Accessibility: "Play [title]"
                    tint = GreenAccent, // Use theme's primary green
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}