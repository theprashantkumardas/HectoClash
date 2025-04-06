package com.example.hectoclash.ui.theme.screens


import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.hectoclash.navigation.Routes
import com.example.hectoclash.utils.SocketManager
import com.example.hectoclash.viewmodels.ProfileUiState
import com.example.hectoclash.viewmodels.ProfileViewModel
import com.example.hectoclash.viewmodels.ProfileViewModelFactory


import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.* // Keep relevant icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage // Import Coil for potential image loading
import coil.request.ImageRequest
import com.example.hectoclash.R // Ensure you have a default avatar drawable
import com.example.hectoclash.data.models.User
import com.example.hectoclash.ui.theme.DarkSurface
import com.example.hectoclash.ui.theme.GreenAccent
import com.example.hectoclash.ui.theme.components.BottomNavItem
import java.util.Locale
import kotlin.math.roundToInt


//@Composable
//fun ProfileScreen(
//    nestedNavController: NavController,
//    mainNavController: NavController, // For logout
//    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(LocalContext.current)),
//    // Removed onLogout lambda, handle directly in VM or here
////    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(LocalContext.current)),
//    onLogout: () -> Unit
//) {
//    val userName by profileViewModel.userName.collectAsState()
//    val userEmail by profileViewModel.userEmail.collectAsState()
//    val playerId by profileViewModel.playerId.collectAsState()
//
//    Column(
//        modifier = Modifier.fillMaxSize().padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center,
//    ) {
//        Text(text = "My Profile", style = MaterialTheme.typography.headlineMedium)
//        Icon(Icons.Filled.AccountCircle, contentDescription = "My Avatar", modifier = Modifier.size(60.dp))
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Text(text = "Name: ${userName ?: "Loading..."}", style = MaterialTheme.typography.bodyLarge)
//        Text(text = "Email: ${userEmail ?: "Loading..."}", style = MaterialTheme.typography.bodyLarge)
//        Text(text = "Player ID: ${playerId ?: "Loading..."}", style = MaterialTheme.typography.bodyLarge)
//
//        Spacer(modifier = Modifier.height(32.dp))
//
//        // TODO: Display stats (Wins, Losses, etc.) here - requires fetching
//
//        // Button to go to Friends List
//        Button(onClick = { nestedNavController.navigate(Routes.FRIENDS_LIST) }) {
//            Icon(Icons.Default.People, contentDescription = null)
//            Spacer(Modifier.width(8.dp))
//            Text("View Friends & Requests")
//        }
//
//        Spacer(modifier = Modifier.height(16.dp)) // Add space before logout
//
//        Button(onClick = { profileViewModel.logout { onLogout() } }) {
//            Text(text = "Logout")
//        }
//
//        // Logout Button
//        Button(
//            onClick = {
//                profileViewModel.logout {
//                    // Use mainNavController to navigate outside HOME scope
//                    SocketManager.disconnect() // Ensure socket disconnects on logout
//                    mainNavController.navigate(Routes.SIGN_IN) {
//                        popUpTo(Routes.HOME) { inclusive = true } // Clear back stack up to Home
//                    }
//                }
//            },
//            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
//        ) {
//            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
//            Spacer(Modifier.width(8.dp))
//            Text(text = "Logout")
//        }
//    }
//
//}


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    nestedNavController: NavController,
    mainNavController: NavController,
    profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by profileViewModel.uiState.collectAsState()
    // Get cached values for initial display while loading
    val cachedUserName by profileViewModel.cachedUserName.collectAsState()
    val cachedPlayerId by profileViewModel.cachedPlayerId.collectAsState()

    Scaffold(
//        topBar = {
//            CenterAlignedTopAppBar(
//                title = { Text("Profile", fontWeight = FontWeight.Bold) },
////                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
//////                    containerColor = , // Use theme color
////                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
////                )
//            )
//        }
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    // Show basic info from cache while loading full stats
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .align(Alignment.TopCenter), // Align top
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        CircularProgressIndicator(modifier = Modifier.size(80.dp), strokeWidth = 6.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(cachedUserName ?: "Loading Name...", style = MaterialTheme.typography.headlineSmall)
                        Text(cachedPlayerId?.let { "@$it" } ?: "Loading ID...", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("Loading Stats...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                is ProfileUiState.Error -> {
                    // Keep error display centered
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.CloudOff, contentDescription = "Error", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Error Loading Profile", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Text(state.message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { profileViewModel.fetchUserProfile() }) {
                            Text("Retry")
                        }
                    }
                }
                is ProfileUiState.Success -> {
                    // Display full profile when data is loaded
                    ProfileContent(
                        user = state.user,
                        viewModel = profileViewModel,
                        nestedNavController = nestedNavController,
                        mainNavController = mainNavController,
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProfileContent(
    user: User,
    viewModel: ProfileViewModel,
    nestedNavController: NavController,
    mainNavController: NavController,
    modifier: Modifier = Modifier // Apply padding from Scaffold here
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp), // Add padding inside content area
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfileHeaderSection(user)
        Spacer(modifier = Modifier.height(24.dp))
        StatsSection(user, viewModel) // Pass viewModel for calculations
        Spacer(modifier = Modifier.height(24.dp))
        ActionButtonsSection(viewModel, nestedNavController, mainNavController) // Pass viewmodel for logout
        Spacer(modifier = Modifier.height(16.dp))
        // Member Since - placed at the bottom
        val joinDate = viewModel.formatJoinDate(user.createdAt)
        Text(
            text = "Member Since: $joinDate",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp)) // Bottom padding
    }
}

@Composable
fun ProfileHeaderSection(user: User) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(GreenAccent,DarkSurface, )
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
             .background(gradientBrush) // Optional subtle gradient background
            .padding(top = 16.dp, bottom = 16.dp)
            .clip(RoundedCornerShape(12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {
        // Use AsyncImage (from Coil library) if you plan to load URLs later
        // implementation("io.coil-kt:coil-compose:2.5.0") // Or latest version
        Image(
            // Load the drawable resource using painterResource
            painter = painterResource(id = R.drawable.whatsapp_image_2025_04_06_at_09_04_08_6671107a), // <<< Use your drawable ID
            contentDescription = "User Avatar", // Content description is important
            modifier = Modifier
                .size(110.dp) // Keep the size
                .clip(CircleShape) // Keep the circular shape
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape), // Keep the border
            contentScale = ContentScale.Crop // Keep the scaling
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = user.name,
            style = MaterialTheme.typography.headlineMedium, // Slightly larger
            fontWeight = FontWeight.SemiBold // Adjust weight
        )
        Text(
            text = "@${user.playerId}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatsSection(user: User, viewModel: ProfileViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Game Statistics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) // Subtle card bg
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem("Wins", user.wins.toString(), Icons.Outlined.EmojiEvents, Color(0xFF4CAF50))
                    StatItem("Losses", user.losses.toString(), Icons.Outlined.SentimentVeryDissatisfied, Color(0xFFE57373)) // Softer Red
                    StatItem("Draws", user.draws.toString(), Icons.Outlined.Handshake, Color.Gray)
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), thickness = 0.5.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    StatItem("Total Played", user.totalGamesPlayed.toString(), Icons.Outlined.SportsEsports)
                    StatItem("Points", user.points.toString(), Icons.Outlined.StarOutline, MaterialTheme.colorScheme.primary)
                    // Optional: Display Win Rate
                    val winRate = viewModel.calculateWinRate(user.wins, user.totalGamesPlayed)
                    StatItem(
                        "Win Rate",
                        winRate?.let { "%.1f%%".format(Locale.US, it) } ?: "-",
                        Icons.Outlined.TrendingUp,
                        winRate?.let { MaterialTheme.colorScheme.tertiary } ?: Color.Gray // Color based on existence
                    )
                }
            }
        }
    }
}

// StatItem remains largely the same, maybe adjust icon size/style
@Composable
fun RowScope.StatItem(label: String, value: String, icon: ImageVector, valueColor: Color = LocalContentColor.current) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(32.dp), // Slightly larger icon
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall, // Make value stand out more
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall, // Smaller label
            color = Color.Gray
        )
    }
}

@Composable
fun ActionButtonsSection(
    viewModel: ProfileViewModel,
    nestedNavController: NavController,
    mainNavController: NavController
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // Use OutlinedButton for secondary actions
        OutlinedButton(
            onClick = { nestedNavController.navigate(BottomNavItem.Friends.route) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.PeopleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Friends & Requests", modifier = Modifier.weight(1f)) // Text takes available space
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logout Button - Filled, distinct color
        Button(
            onClick = {
                viewModel.logout {
                    SocketManager.disconnect()
                    mainNavController.navigate(Routes.SIGN_IN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError) // Clearer error indication
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Logout")
        }
    }
}