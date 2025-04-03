package com.example.hectoclash.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hectoclash.data.local.TokenManager

import com.example.hectoclash.data.models.OnlineUserResponse

import com.example.hectoclash.viewmodels.OnlineUsersViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayOnlineScreen(
    onBackClick: () -> Unit,
    onChallengeUser: (OnlineUserResponse) -> Unit,
    viewModel: OnlineUsersViewModel = viewModel()
) {
    val onlineUsers by viewModel.onlineUsers.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState(null)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val userPreferences = TokenManager.getInstance(context)

    // Get current user ID
    var currentUserId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        currentUserId = userPreferences.getUserId.first() ?: ""
        // Fetch online users initially
        viewModel.fetchOnlineUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Online Players") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Person, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchOnlineUsers() }) {
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
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.fetchOnlineUsers() }) {
                            Text("Retry")
                        }
                    }
                }
                onlineUsers.isEmpty() -> {
                    Text(
                        text = "No players online right now. Be the first!",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column {
                        Text(
                            text = "Choose an opponent to challenge",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(onlineUsers.filter { it._id != currentUserId }) { user ->
                                UserItem(user = user, onClick = { onChallengeUser(user) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserItem(user: OnlineUserResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(4.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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
                    fontSize = 16.sp
                )
                Text(
                    text = "Player ID: ${user.playerId}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = onClick) {
                Text("Challenge")
            }
        }
    }
}