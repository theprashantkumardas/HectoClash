package com.example.hectoclash.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hectoclash.data.models.UserProfileResponse
import com.example.hectoclash.viewmodels.UserProfileViewModel
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.hectoclash.utils.SocketManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: NavController,
    viewModel: UserProfileViewModel // Injected via factory in NavHost
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val actionFeedback by viewModel.actionFeedback.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // Show feedback messages
    LaunchedEffect(actionFeedback) {
        actionFeedback?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearActionFeedback() // Clear after showing
        }
    }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar("Error: $it", duration = SnackbarDuration.Long)
            viewModel.clearError() // Clear after showing
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(profile?.name ?: "User Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading && profile == null) { // Show loading only on initial load
                CircularProgressIndicator()
            } else if (profile != null) {
                UserProfileContent(profile!!, viewModel, isLoading) // Pass isLoading for button states
            } else if (error != null && !isLoading) {
                // Show error state if profile is null and not loading
                Text("Could not load profile.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun UserProfileContent(
    profile: UserProfileResponse,
    viewModel: UserProfileViewModel,
    isActionLoading: Boolean // Indicate if a friend action is in progress
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.Person, contentDescription = "User Avatar", modifier = Modifier.size(80.dp))
        Text(profile.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Player ID: ${profile.playerId}", style = MaterialTheme.typography.bodyLarge)
        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // --- Stats ---
        Text("Stats", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Wins", profile.wins.toString())
            StatItem("Losses", profile.losses.toString())
            StatItem("Draws", profile.draws.toString())
            StatItem("Rating", profile.rating.toString())
        }


        Spacer(modifier = Modifier.height(24.dp))

        // --- Friend Action Button ---
        FriendActionButton(
            status = profile.friendshipStatus,
            isLoading = isActionLoading, // Use the specific loading state for actions
            onSendRequest = { viewModel.sendFriendRequest() },
            onAcceptRequest = { viewModel.acceptFriendRequest() },
            onRejectRequest = { viewModel.removeFriend("Rejecting...") }, // Provide context
            onCancelRequest = { viewModel.removeFriend("Cancelling...") },
            onRemoveFriend = { viewModel.removeFriend("Removing Friend...") }
        )

        Spacer(modifier = Modifier.height(16.dp))
        // You could add a button to challenge directly from profile too
        if (profile.friendshipStatus == "friends") {
            Button(onClick = { /* TODO: Implement challenge from profile? */
                SocketManager.emitChallengeUser(profile._id) // Example
            }, enabled = !isActionLoading) {
                Icon(Icons.Default.VideogameAsset, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Challenge Friend")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun FriendActionButton(
    status: String,
    isLoading: Boolean,
    onSendRequest: () -> Unit,
    onAcceptRequest: () -> Unit,
    onRejectRequest: () -> Unit,
    onCancelRequest: () -> Unit,
    onRemoveFriend: () -> Unit
) {
    Box(modifier = Modifier.height(50.dp)) { // Ensure consistent height
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            when (status) {
                "none" -> Button(onClick = onSendRequest) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Friend")
                }
                "request_sent" -> Button(
                    onClick = onCancelRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Filled.Cancel, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cancel Request")
                }
                "request_received" -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onAcceptRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) // Or a green color
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Accept")
                    }
                    Button(
                        onClick = onRejectRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reject")
                    }
                }
                "friends" -> Button(
                    onClick = onRemoveFriend,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.PersonRemove, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Remove Friend")
                }
                else -> Text("Unknown Status") // Fallback
            }
        }
    }
}