package com.example.hectoclash.ui.theme.screens


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.hectoclash.data.models.FriendListItem
import com.example.hectoclash.viewmodels.FriendsListViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun FriendsListScreen(
    navController: NavController, // For back navigation primarily
    viewModel: FriendsListViewModel = viewModel(), // Use factory if needed
    onViewProfile: (userId: String) -> Unit,
    onChallengeFriend: (friendId: String, friendName: String) -> Unit,
) {
    val friendsList by viewModel.friendsList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val actionFeedback by viewModel.actionFeedback.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show feedback messages
    LaunchedEffect(actionFeedback) {
        actionFeedback?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearActionFeedback()
        }
    }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar("Error: $it", duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    // Group friends by status
    val pendingRequests = friendsList.filter { it.status == "pending" }
    val requestedRequests = friendsList.filter { it.status == "requested" }
    val acceptedFriends = friendsList.filter { it.status == "accepted" }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Friends & Requests") },
                navigationIcon = {
                    // Use mainNavController passed to HomeScreen to pop back
                    // Or if FriendsListScreen is part of bottom nav, maybe no back button?
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchFriendsList() }, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading && friendsList.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null && !isLoading) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error loading friends: $error", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.fetchFriendsList() }) { Text("Retry") }
                }
            } else if (friendsList.isEmpty() && !isLoading) {
                Text("No friends or requests yet.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section: Friend Requests Received
                    if (pendingRequests.isNotEmpty()) {
                        stickyHeader { SectionHeader("Friend Requests (${pendingRequests.size})") }
                        items(pendingRequests, key = { it._id }) { friend ->
                            FriendRequestItem(
                                item = friend,
                                onAccept = { viewModel.acceptRequest(friend._id) },
                                onReject = { viewModel.removeOrRejectFriend(friend._id, isPending = true) },
                                onViewProfile = { onViewProfile(friend._id) }
                            )
                        }
                    }

                    // Section: Friend Requests Sent
                    if (requestedRequests.isNotEmpty()) {
                        stickyHeader { SectionHeader("Requests Sent (${requestedRequests.size})") }
                        items(requestedRequests, key = { it._id }) { friend ->
                            FriendSentItem(
                                item = friend,
                                onCancel = { viewModel.removeOrRejectFriend(friend._id, isPending = false) }, // Treat cancel same as remove
                                onViewProfile = { onViewProfile(friend._id) }
                            )
                        }
                    }

                    // Section: Accepted Friends
                    if (acceptedFriends.isNotEmpty()) {
                        stickyHeader { SectionHeader("Friends (${acceptedFriends.size})") }
                        // Sort friends: Online first, then alphabetically
                        val sortedFriends = acceptedFriends.sortedWith(compareByDescending<FriendListItem> { it.isOnline == true }.thenBy { it.name })
                        items(sortedFriends, key = { it._id }) { friend ->
                            FriendAcceptedItem(
                                item = friend,
                                onChallenge = { onChallengeFriend(friend._id, friend.name) },
                                onRemove = { viewModel.removeOrRejectFriend(friend._id, isPending = false) },
                                onViewProfile = { onViewProfile(friend._id) }
                            )
                        }
                    }
                }
            }
        }
    }
}


// --- List Item Composables ---

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}


@Composable
fun FriendRequestItem(
    item: FriendListItem,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onViewProfile: () -> Unit
) {
    FriendCardBase(item = item, onViewProfile = onViewProfile) {
        // Action buttons for pending request
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAccept,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp) // Smaller padding
            ) { Text("Accept") }
            OutlinedButton(
                onClick = onReject,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                )
            ) { Text("Reject", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun FriendSentItem(
    item: FriendListItem,
    onCancel: () -> Unit,
    onViewProfile: () -> Unit
) {
    FriendCardBase(item = item, onViewProfile = onViewProfile) {
        // Action button for sent request
        OutlinedButton(
            onClick = onCancel,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) { Text("Cancel") }
    }
}

@Composable
fun FriendAcceptedItem(
    item: FriendListItem,
    onChallenge: () -> Unit,
    onRemove: () -> Unit,
    onViewProfile: () -> Unit
) {
    FriendCardBase(item = item, onViewProfile = onViewProfile) {
        // Action buttons for accepted friend
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onChallenge,
                enabled = item.isOnline == true, // Enable only if online
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("Challenge") }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.PersonRemove, contentDescription = "Remove Friend", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}


// Base card structure
@Composable
fun FriendCardBase(
    item: FriendListItem,
    onViewProfile: () -> Unit,
    actions: @Composable RowScope.() -> Unit // Slot for action buttons
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onViewProfile),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: Avatar, Name, Status Icon
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Icon(
                        Icons.Filled.AccountCircle, // Placeholder avatar
                        contentDescription = "User",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    if (item.status == "accepted" && item.isOnline == true) {
                        // Online indicator for accepted friends
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFF4CAF50), CircleShape) // Green dot
                                .offset(x = 2.dp, y = 2.dp) // Adjust position
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(item.name, fontWeight = FontWeight.SemiBold)
                    Text(item.playerId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Right side: Actions (passed as a composable lambda)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                actions()
            }
        }
    }
}