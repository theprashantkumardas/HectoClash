package com.example.hectoclashapp.ui.theme.screens

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
import androidx.compose.material.icons.filled.SportsScore
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
    onViewProfile: (userId: String) -> Unit,
    viewModel: OnlineUsersViewModel = viewModel()
) {
    // Existing state collection
    val onlineUsers by viewModel.onlineUsers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val incomingChallenge by viewModel.incomingChallenge.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    val context = LocalContext.current
    val tokenManager = remember { TokenManager.getInstance(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var currentUserId by remember { mutableStateOf("") }

    LaunchedEffect(key1 = tokenManager) {
        currentUserId = tokenManager.getUserId.first() ?: ""
        Log.d("PlayOnlineScreen", "Current User ID: $currentUserId")
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearFeedbackMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Online Players") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Make refresh button more prominent
                    IconButton(
                        onClick = { viewModel.fetchOnlineUsers() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            // Increase icon size for better visibility
                            modifier = Modifier.size(28.dp),
                            // Use a contrasting color to make it stand out
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                isLoading && onlineUsers.isEmpty() -> {
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
//                        Button(onClick = { viewModel.fetchOnlineUsers() }) {
//                            Icon(
//                                Icons.Default.Refresh,
//                                contentDescription = null,
//                                modifier = Modifier.size(ButtonDefaults.IconSize)
//                            )
//                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
//                            Text("Refresh")
//                        }
                    }
                }
                onlineUsers.isEmpty() && !isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No players online right now",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
//                        Button(onClick = { viewModel.fetchOnlineUsers() }) {
//                            Icon(
//                                Icons.Default.Refresh,
//                                contentDescription = null,
//                                modifier = Modifier.size(ButtonDefaults.IconSize)
//                            )
//                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
//                            Text("Refresh")
//                        }
                    }
                }
                else -> {
                    val displayUsers = onlineUsers.filter { it._id != currentUserId }

                    if (displayUsers.isEmpty() && currentUserId.isNotEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "You are the only player online!",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.fetchOnlineUsers() }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text("Refresh")
                            }
                        }
                    } else if (displayUsers.isNotEmpty()) {
                        Column {
                            Text(
                                text = "Choose an opponent to challenge",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Increased spacing between items to 16.dp
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(displayUsers, key = { user -> user._id }) { user ->
                                    UserItem(
                                        user = user,
                                        onChallengeClick = {
                                            Log.d("PlayOnlineScreen", "Challenge button clicked for ${user.name}")
                                            viewModel.viewModelScope.launch {
                                                snackbarHostState.showSnackbar("Challenging ${user.name}...")
                                            }
                                            SocketManager.emitChallengeUser(user._id)
                                        },
                                        onViewProfileClick = {
                                            Log.d("PlayOnlineScreen", "View Profile clicked for ${user.name}")
                                            onViewProfile(user._id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

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
                        viewModel.respondToChallenge(challenge.challengerId, false)
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
    onViewProfileClick: () -> Unit
) {
    // Define light red color for the challenge button
    val lightRedColor = Color(0xFFFF6B6B) // Adjust this hex code for desired shade of light red

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Increased elevation for better visual separation
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onViewProfileClick)
                    .padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = user.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White // Changed username text color to white
                    )
                    Text(
                        text = user.playerId,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f) // Light white for player ID
                    )
                }
            }

            // Action Buttons
            Row {
                // Challenge Button with light red color
                Button(
                    onClick = onChallengeClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = lightRedColor, // Light red color
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.VideogameAsset,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
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
        onDismissRequest = onDismiss,
        containerColor = Color.DarkGray, // Change background to grey
        title = {
            Text(
                "Incoming Challenge!",
                color = Color.White
            )
        },
        text = {
            Text(
                "${challengeData.challengerName} wants to play HectoClash with you!",
                color = Color.White.copy(alpha = 0.8f)
            )
        },
        // Switch the order - dismissButton is shown first (left)
        dismissButton = {
            Button(
                onClick = onDecline,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Decline")
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("Accept")
            }
        }
    )
}