package com.example.hectoclash.ui.theme.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed // Ensure this import is present
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
// import androidx.compose.ui.tooling.preview.Preview // Remove or update preview if needed
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hectoclash.data.models.LeaderboardEntry // Import correct model
import com.example.hectoclash.viewmodels.LeaderboardUiState
import com.example.hectoclash.viewmodels.LeaderboardViewModel
import java.util.Locale

// LeaderboardScreen Composable remains the same as provided previously
@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier,
    viewModel: LeaderboardViewModel = viewModel() // Get ViewModel instance
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle() // Collect state

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 16.dp) // Adjusted padding
    ) {
        // Title
        Text(
            text = "HectoClash Leaderboard", // Updated Title
            style = MaterialTheme.typography.headlineMedium, // Slightly larger
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center // Center the title
        )

        // Content based on UI State
        when (val state = uiState) {
            is LeaderboardUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is LeaderboardUiState.Success -> {
                if (state.leaderboard.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Leaderboard is empty.")
                    }
                } else {
                    LeaderboardList(entries = state.leaderboard) // Pass data here
                }
            }
            is LeaderboardUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error loading leaderboard", color = MaterialTheme.colorScheme.error)
                        Text(state.message, fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.fetchHectoChallengeLeaderboard() }) { // Retry button
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}


// CORRECTED LeaderboardList function
@Composable
fun LeaderboardList(entries: List<LeaderboardEntry>) {
    Card( // Wrap in a Card for visual structure like the image
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) { // Match dark header bg
            // Header Row
            LeaderboardHeader() // Header remains the same

            // Leaderboard Entries List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface) // Lighter background for items
            ) {
                // *** Use entry.userId as the key ***
                itemsIndexed(entries, key = { _, entry -> entry.userId }) { index, entry ->
                    LeaderboardItem(rank = index + 1, entry = entry) // Item Composable remains the same
                    if (index < entries.lastIndex) { // Don't add divider after last item
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

// LeaderboardHeader Composable remains the same as provided previously
@Composable
fun LeaderboardHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp), // Adjusted padding slightly
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Rank", Modifier.width(50.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Player", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) // Changed to Player (ID)
        Text("Played", Modifier.width(55.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("W", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp, color = Color(0xFF4CAF50)) // Abbreviated
        Text("L", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp, color = Color(0xFFF44336)) // Abbreviated
        Text("D", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp, color = Color.Gray) // Draw
        Text("Acc.", Modifier.width(50.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) // Accuracy
    }
}


// LeaderboardItem Composable remains the same as provided previously
@Composable
fun LeaderboardItem(rank: Int, entry: LeaderboardEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Adjust padding to match header horizontal padding
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#$rank",
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold, // Make rank bold
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary // Use primary color for rank
        )
        Text(
            text = entry.playerId, // Show PlayerID
            modifier = Modifier.weight(1f).padding(end=4.dp), // Give padding before next item
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = entry.totalChallengesPlayed.toString(), // Matches column
            modifier = Modifier.width(55.dp), // Match header width
            textAlign = TextAlign.Center,
            fontSize = 14.sp, // Consistent font size
            color = LocalContentColor.current.copy(alpha=0.8f)
        )
        Text(
            text = entry.wins.toString(), // Won column
            modifier = Modifier.width(35.dp), // Match header width
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = Color(0xFF4CAF50) // Green
        )
        Text(
            text = entry.losses.toString(), // Loss column
            modifier = Modifier.width(35.dp), // Match header width
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = Color(0xFFF44336) // Red
        )
        Text(
            text = entry.draws.toString(), // Draws
            modifier = Modifier.width(35.dp), // Match header width
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = Color.Gray
        )

        Text(
            // Format accuracy percentage
            text = entry.accuracy?.let { String.format(Locale.US, "%.1f%%", it) } ?: "-",
            modifier = Modifier.width(50.dp), // Match header width
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = LocalContentColor.current.copy(alpha = 0.8f)
        )
    }
}