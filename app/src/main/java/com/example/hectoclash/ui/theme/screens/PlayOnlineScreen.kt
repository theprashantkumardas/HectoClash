package com.example.hectoclash.ui.theme.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hectoclash.data.local.TokenManager

import com.example.hectoclash.data.models.OnlineUserResponse
import com.example.hectoclash.data.models.ReceiveChallengeData
import com.example.hectoclash.utils.SocketManager

import com.example.hectoclash.viewmodels.OnlineUsersViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayOnlineScreen(
    onBackClick: () -> Unit,
    onViewProfile: (userId: String) -> Unit, // Add callback to view profile
    // onChallengeUser callback is now handled internally by emitting socket event
    viewModel: OnlineUsersViewModel = viewModel() // Use Hilt or manual Factory if needed
) {
    // Observe StateFlow from ViewModel
    val onlineUsers by viewModel.onlineUsers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val incomingChallenge by viewModel.incomingChallenge.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    val context = LocalContext.current
    val tokenManager = remember { TokenManager.getInstance(context) }
    val snackbarHostState = remember { SnackbarHostState() } // For showing feedback

    // Get current user ID
    var currentUserId by remember { mutableStateOf("") }

    LaunchedEffect(key1 = tokenManager) {
        currentUserId = tokenManager.getUserId.first() ?: ""
        // Initial fetch is now handled in ViewModel's init block
        // viewModel.fetchOnlineUsers()
        Log.d("PlayOnlineScreen", "Current User ID: $currentUserId")
    }

    // Show feedback messages (like challenge rejected) in a Snackbar
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearFeedbackMessage() // Clear message after showing
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }, // Add SnackbarHost
        topBar = {
            TopAppBar(
                title = { Text("Online Players") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        // Use appropriate back icon
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.fetchOnlineUsers() },
                        enabled = !isLoading // Disable refresh while loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when {
                isLoading && onlineUsers.isEmpty() -> { // Show loading only if list is empty initially
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(onClick = { viewModel.fetchOnlineUsers() }) {
                            Text("Retry")
                        }
                    }
                }
                onlineUsers.isEmpty() && !isLoading -> {
                    Text(
                        text = "No players online right now.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
//                else -> {
//                    // Filter out the current user from the list
//                    val displayUsers = onlineUsers.filter { it._id != currentUserId }
//
//                    if (displayUsers.isEmpty() && currentUserId.isNotEmpty()) {
//                        Text(
//                            text = "You are the only player online!",
//                            modifier = Modifier.align(Alignment.Center)
//                        )
//                    } else if (displayUsers.isNotEmpty()) {
//                        Column {
//                            Text(
//                                text = "Choose an opponent to challenge",
//                                style = MaterialTheme.typography.titleMedium,
//                                modifier = Modifier.padding(bottom = 16.dp)
//                            )
//                            LazyColumn(
//                                verticalArrangement = Arrangement.spacedBy(10.dp)
//                            ) {
//                                items(displayUsers, key = { user -> user._id }) { user ->
//                                    UserItem(
//                                        user = user,
//                                        // onClick now emits the challenge event
//                                        onClick = {
//                                            Log.d("PlayOnlineScreen", "Challenge button clicked for ${user.name}")
//                                            viewModel.viewModelScope.launch {
//                                                snackbarHostState.showSnackbar("Challenging ${user.name}...")
//                                            }
//                                            SocketManager.emitChallengeUser(user._id)
//                                        }
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
                else -> {
                    val displayUsers = onlineUsers.filter { it._id != currentUserId }
                    // ... empty check ...
                    if (displayUsers.isNotEmpty()) {
                        Column {
                            // ... "Choose opponent" text ...
                            LazyColumn( /* ... */ ) {
                                items(displayUsers, key = { user -> user._id }) { user ->
                                    UserItem(
                                        user = user,
                                        onChallengeClick = { // Renamed for clarity
                                            Log.d("PlayOnlineScreen", "Challenge button clicked for ${user.name}")
                                            // Show Snackbar feedback immediately
                                            viewModel.viewModelScope.launch {
                                                snackbarHostState.showSnackbar("Challenging ${user.name}...")
                                            }
                                            SocketManager.emitChallengeUser(user._id)
                                        },
                                        onViewProfileClick = { // New handler
                                            Log.d("PlayOnlineScreen", "View Profile clicked for ${user.name}")
                                            onViewProfile(user._id) // Navigate using callback
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Incoming Challenge Dialog ---
            incomingChallenge?.let { challenge ->
                IncomingChallengeDialog(
                    challengeData = challenge,
                    onAccept = {
                        Log.d("PlayOnlineScreen", "Accepting challenge from ${challenge.challengerName}")
                        viewModel.respondToChallenge(challenge.challengerId, true)
                    },
                    onDecline = {
                        Log.d("PlayOnlineScreen", "Declining challenge from ${challenge.challengerName}")
                        viewModel.respondToChallenge(challenge.challengerId, false)
                    },
                    onDismiss = {
                        Log.d("PlayOnlineScreen", "Dismissing challenge dialog (implies decline)")
                        viewModel.respondToChallenge(challenge.challengerId, false) // Treat dismiss as decline
                        // Or use viewModel.clearIncomingChallenge() if dismiss shouldn't auto-decline
                    }
                )
            }
        }
    }
}

@Composable
fun UserItem(
    user: OnlineUserResponse,
    onChallengeClick: () -> Unit,
    onViewProfileClick: () -> Unit // Add callback
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        // onClick is now on the Button, not the whole card
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // Pushes button to the end
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f) // Take available space before buttons
                    .clickable(onClick = onViewProfileClick) // Make user info clickable
                    .padding(end = 8.dp) // Add padding before buttons
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null, // Decorative
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = user.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = user.playerId, // Display Player ID
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons
            Row {
                // Challenge Button - Moved inside Card's Row
                Button(onClick = onChallengeClick) {
                    Icon(Icons.Default.VideogameAsset, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Challenge")
                }
            }



        }
    }
}


@Composable
fun IncomingChallengeDialog(
    challengeData: ReceiveChallengeData,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, // Call onDismiss when clicking outside or back button
        icon = { Icon(Icons.Default.SportsKabaddi, contentDescription = "Challenge Icon") },
        title = { Text("Incoming Challenge!") },
        text = { Text("${challengeData.challengerName} wants to play HectoClash with you!") },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("Accept")
            }
        },
        dismissButton = {
            Button(
                onClick = onDecline,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Decline")
            }
        }
    )
}