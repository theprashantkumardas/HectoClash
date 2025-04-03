package com.example.hectoclash.ui.theme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hectoclash.data.local.TokenManager
import com.example.hectoclash.data.models.LeaderboardEntry
import com.example.hectoclash.viewmodels.LeaderboardViewModel
import com.example.hectoclash.viewmodels.ViewModelFactory
import kotlinx.coroutines.flow.firstOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val context = LocalContext.current

    // Collect state from ViewModel
    val leaderboardEntries by viewModel.leaderboardEntries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Get current user's player ID
    val currentPlayerId = remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        TokenManager.getInstance(context).getPlayerId.firstOrNull()?.let {
            currentPlayerId.value = it
        }
    }

    // Current date and time
    val currentDateTime = "2025-04-03 10:33:30"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with date
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Leaderboard",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentDateTime,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Table Headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rank",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(45.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Player ID",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Left
                    )
                    Text(
                        text = "Matches",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(60.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Won",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(45.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Loss",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(45.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Points",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(55.dp),
                        textAlign = TextAlign.Center
                    )
                }

                if (leaderboardEntries.isEmpty() && !isLoading) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No leaderboard data available",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.fetchLeaderboard() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Refresh")
                            }
                        }
                    }
                } else {
                    // Leaderboard list - sorted by points
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(leaderboardEntries.sortedByDescending { it.points }) { entry ->
                            LeaderboardRow(
                                entry = entry,
                                currentUser = currentPlayerId.value
                            )
                            Divider(color = Color.LightGray, thickness = 0.5.dp)
                        }
                    }
                }
            }

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error message
            error?.let { errorMessage ->
                SnackbarHost(
                    hostState = remember { SnackbarHostState() }.also {
                        LaunchedEffect(errorMessage) {
                            it.showSnackbar(errorMessage)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun LeaderboardRow(entry: LeaderboardEntry, currentUser: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp)
            .background(
                if (entry.playerId == currentUser)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else
                    Color.Transparent
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        Text(
            text = "#${entry.rank}",
            modifier = Modifier.width(45.dp),
            textAlign = TextAlign.Center,
            fontWeight = if (entry.rank <= 3) FontWeight.Bold else FontWeight.Normal,
            color = when (entry.rank) {
                1 -> Color(0xFFFFD700) // Gold
                2 -> Color(0xFFC0C0C0) // Silver
                3 -> Color(0xFFCD7F32) // Bronze
                else -> MaterialTheme.colorScheme.onSurface
            }
        )

        // Player ID with profile pic
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = entry.profilePicRes),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.Gray, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = entry.playerId,
                fontWeight = if (entry.playerId == currentUser) FontWeight.Bold else FontWeight.Normal,
                color = if (entry.playerId == currentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Matches
        Text(
            text = "${entry.matches}",
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center
        )

        // Won
        Text(
            text = "${entry.won}",
            modifier = Modifier.width(45.dp),
            textAlign = TextAlign.Center,
            color = Color(0xFF4CAF50)  // Green color for wins
        )

        // Loss
        Text(
            text = "${entry.loss}",
            modifier = Modifier.width(45.dp),
            textAlign = TextAlign.Center,
            color = Color(0xFFF44336)  // Red color for losses
        )

        // Points
        Text(
            text = "${entry.points}",
            modifier = Modifier.width(55.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}